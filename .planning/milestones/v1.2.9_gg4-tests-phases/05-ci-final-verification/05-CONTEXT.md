# Phase 5: CI & Final Verification - Context

**Gathered:** 2026-09-18
**Status:** Ready for planning

<domain>
## Phase Boundary

The fork's GitHub Actions test workflow becomes a **single** `latest` × client-V2 job on current
actions that publishes JaCoCo coverage, and the whole suite is confirmed green **both locally and in a
real CI run**.

- `.github/workflows/tests.yaml` modernized and reduced to one job — CI-01, CI-02
- JaCoCo artifact upload + coverage summary in the job summary, no threshold — CI-03
- `./gradlew test` green locally and in CI on `latest` × V2 — CI-04

Four requirements across the roadmap's two plans (05-01 workflow, 05-02 verification).

This is the last phase of the milestone: **no new tests are written here.** Phases 2–4 pinned every fork
behavior; this phase only proves the pinned suite runs where it needs to run.

Out of this phase:

- Any new test, or any change to an existing test — Phases 1–4, complete
- Any `src/main/` change — no production code is touched in this phase at all
- Any `build.gradle.kts` change — Phase 1 settled the build config (D-13/D-14/D-16); the fork-class
  include list already covers every class Phases 2–4 touched (Phase 4 D-17 verified this)
- A coverage threshold or any coverage gate — locked report-only in PROJECT.md § Key Decisions
- Test matrix for 23.7 / 24.3 / client V1 / ClickHouse Cloud — locked Out of Scope in PROJECT.md
- The four `Active` follow-ups carried in PROJECT.md (R-01 silent data loss, the dead V2 error
  classification, WR-01..03, P3-WR-01..03) — unscheduled, each needs its own plan

</domain>

<decisions>
## Implementation Decisions

### Workflow file strategy (CI-01, CI-02)

- **D-01:** **Rewrite `.github/workflows/tests.yaml` in place.** Not a new fork-only workflow file, not a
  delete-and-replace. CI-01/CI-02 name that path literally, and one file means one test job on push with
  nothing to cross-reference. The rebasability constraint is satisfied differently here than elsewhere in
  the project: the conflict a future upstream sync produces is loud, localized to one file, and the fork's
  side should win — unlike a deletion, which a rebase silently reverts by reintroducing the upstream file
  as "new".

  Rejected: neutralizing `tests.yaml` to `workflow_dispatch` and adding `fork-tests.yaml` (still edits the
  upstream file, and leaves two workflows to reason about); deleting `tests.yaml` outright.

- **D-02:** **The other four workflows stay untouched.** `test-head.yaml` (cron + dispatch),
  `integrationTests.yaml`, `release.yaml`, `pre-release.yaml` all carry the same deprecated actions
  (`actions/checkout@v3`, `actions/setup-java@v3` with `distribution: adopt`,
  `gradle/gradle-build-action@v2`) — and **none of them trigger on push**, so success criterion 2 ("a push
  to `custom_build` triggers exactly one test job") is already satisfied by fixing `tests.yaml` alone.
  Modernizing them would ship unverified changes: every one of them needs secrets or a manual dispatch
  this phase never exercises.

- **D-03:** **`CLICKHOUSE_VERSION=latest` and `CLIENT_VERSION=V2` are set explicitly in the job env**,
  even though Phase 1 D-05 already made `latest` the default image and V2 is the default client. Criterion
  2 then reads straight off the workflow file, and a future change to `ClickHouseTestHelpers`'
  `CLICKHOUSE_VERSION_DEFAULT` cannot silently move CI onto a different image.

- **D-04:** **A header comment marks the file as fork-modified** and names what was removed: the
  V1/23.7/24.3/Cloud matrix legs, with a pointer to `.planning/REQUIREMENTS.md` CI-01..04. Whoever hits
  the rebase conflict D-01 accepts needs to know instantly which side is intentional and why.

  Rejected: no marker (git history alone); renaming the workflow's top-level `name:`.

### Coverage publishing (CI-03)

- **D-05:** **One artifact, uploading `build/reports/jacoco/` wholesale** — the full
  `jacocoTestReport` HTML+XML and the fork-scoped `jacocoForkReport` HTML+XML together. Criterion 4 wants
  the fork-changed-class coverage reviewable, and the full-suite report is the context that makes those
  numbers interpretable. One upload step, no path juggling, no two zips to correlate.

- **D-06:** **The job summary is produced by an inline script parsing the JaCoCo XML — no marketplace
  action.** A small step reads the `<counter>` totals and appends markdown to `$GITHUB_STEP_SUMMARY`.
  `python3` is preinstalled on `ubuntu-latest`, so per-class XML parsing is cheap and does not need a
  shell XML hack. This keeps the project's "only new build dependency is the JaCoCo plugin" posture intact
  and adds no third-party supply-chain dependency to a fork that runs in production.

  Rejected: `madrapps/jacoco-report` (aimed at PR comments, not push runs; third-party action); a Gradle
  task rendering the markdown (a `build.gradle.kts` change in a phase that otherwise touches no build
  logic).

- **D-07:** **The summary shows headline totals plus a per-class table of the 15 fork-changed classes.**
  Two headline rows (full suite and fork-scoped: instruction / line / branch %), then one row per fork
  class with its line and branch coverage. This satisfies criterion 4's "coverage for the fork-changed
  classes" without downloading the artifact, while keeping the full-suite baseline for comparison.

- **D-08:** **Coverage, summary, and test reports all upload with `if: always()`.** The Gradle test
  reports (`build/reports/tests/`) upload too. This phase runs a never-before-exercised workflow on a
  runner nobody has run this suite on; a red first run with no artifacts is a blind debug. Both JaCoCo
  reports are `finalizedBy` of `test` (Phase 1 D-16) and Gradle finalizers run even when the finalized
  task fails, so coverage genuinely exists to upload on a red run.

### Getting a green CI run (CI-03, CI-04)

- **D-09:** **`custom_build` is pushed as-is — all 98 commits, `.planning/` included.** Criterion 2 asks
  for a push to `custom_build` specifically, and this also clears the STATE.md blocker where GSD worktree
  isolation degrades to sequential execution because `origin/HEAD` is 98 commits stale. The 79 tracked
  `.planning/` files become public on `github.com/ne1r0n/clickhouse-kafka-connect` (verified public, a
  fork, default branch `main`). They are project documentation with no secrets — but publication is
  permanent.
  — **Reversibility:** one-way — a public push cannot be meaningfully retracted; GitHub retains
  force-pushed objects and third parties (forks, caches, scrapers) may already hold them. Only a new
  commit history plus a repo-level intervention would undo it, and the planning trail would already have
  been readable.

  Rejected: verifying on a throwaway branch first (two runs, same eventual publication); a code-only
  branch via `/gsd-pr-branch` (criterion 2's `custom_build` push is then never exercised and the 98
  commits stay local indefinitely).

- **D-10:** **The push happens at a `blocking-human` checkpoint in plan 05-02 — the executor never pushes
  autonomously.** The plan does everything up to the push, then stops and asks. This mirrors the fork's
  own escalation pattern from Phases 2–4 (D-01: production-affecting changes stop for a user decision); a
  98-commit first push to a public production fork sits squarely inside it, even with D-09's approval
  already given. The checkpoint is the last chance to review the diff before it becomes public.

- **D-11:** **CI results are read through the public GitHub REST API with `curl` — no `gh` install.**
  The repo is public, so `GET /repos/ne1r0n/clickhouse-kafka-connect/actions/runs?branch=custom_build`
  works unauthenticated (60 req/hr, ample), and `GET /repos/.../actions/runs/{id}/artifacts` confirms the
  coverage artifact exists. **This was proven live during the discussion session** — an unauthenticated
  API call from this environment returned the repo metadata (`default_branch: main`, `private: false`,
  `fork: true`), so the approach is verified rather than assumed.

  Rejected: `brew install gh` + interactive `gh auth login` (a tool install in a verification phase);
  manual web-UI checking (nothing machine-verified).

- **D-12:** **The coverage markdown is written to both `$GITHUB_STEP_SUMMARY` and stdout.** The REST API
  can fetch job *logs* but not job *summaries*, so without this CI-03's "writes a coverage summary to the
  job summary" could only be confirmed by eye. Teeing the same markdown to stdout puts the exact rendered
  table into the retrievable job log, making the requirement machine-verifiable.

### Job shape and hardening (CI-02)

- **D-13:** **The trigger stays bare `on: [push]`.** Unchanged from upstream, so the diff is purely about
  the matrix, the actions, and the new coverage steps. With the matrix gone, "exactly one test job" holds
  on any branch, so criterion 2 needs no trigger narrowing — and every extra semantic change to an
  upstream file widens the rebase surface D-01 already accepted.

  Rejected: `push` (branches: custom_build) + `workflow_dispatch`; adding `pull_request` (this fork takes
  no PRs today, and pushes to a PR branch would double-run).

- **D-14:** **Add `timeout-minutes` and a concurrency group with `cancel-in-progress: true`.** GitHub
  runners give 4 cores, so `maxParallelForks` (`cores / 2`) drops from 4 locally to 2, on top of a cold
  Gradle cache and an `alwaysPull` of `clickhouse/clickhouse-server:latest` — the ~4.5-minute local run
  could be several times that. A timeout around 45 minutes makes a hung Testcontainers wait fail in under
  an hour instead of burning the 6-hour default. `test-head.yaml` already uses exactly this concurrency
  pattern, so it is an established convention in this repo rather than an import.

- **D-15:** **The Gradle step invokes `./gradlew test` only.** `gradle/actions/setup-gradle` v4+ dropped
  the `arguments:` input, so the call becomes its own `run:` step. Both reports come free via the Phase 1
  D-16 `finalizedBy` wiring, which also survives a failing `test` (D-08). `check` is ruled out in
  practice: the upstream `tasks.withType<Test>` loop makes `check` depend on `integrationTest`, which
  needs the Confluent archive and cloud credentials and would fail on the runner.

  Rejected: naming `jacocoTestReport jacocoForkReport` explicitly as a hedge against the finalizedBy
  wiring being lost.

- **D-16:** **The default cold Gradle cache is accepted — no `cache-read-only` or `cache-disabled`
  override.** `setup-gradle` writes its dependency cache only on the repository's default branch, which
  for this fork is `main`, not `custom_build` — so runs on `custom_build` get a read-only, permanently
  empty cache. Criterion 3's run is cold either way, and the cost is roughly a minute of dependency
  download against a multi-minute suite plus a full ClickHouse image pull. Not worth a non-obvious flag in
  an upstream file.

### Claude's Discretion

- **Red-CI-run policy.** Not explicitly decided. Default: a failure rooted in the *workflow configuration*
  (bad action input, missing path, runner-environment mismatch, Testcontainers setup) is fixed and
  re-pushed inside plan 05-02 — that is the phase's own job. A failure exposing a *test or product*
  problem that does not reproduce locally escalates at a checkpoint rather than being chased with
  speculative CI pushes; PROJECT.md's `Active` follow-up list is where such findings land.
- Poll cadence and give-up window when waiting on the run via the REST API, and how the run is located
  (by `head_sha` of the pushed commit vs. most recent run on the branch).
- Exact `timeout-minutes` value and concurrency group expression.
- The summary script's language (`python3` vs. shell), its exact markdown layout, column set, and
  rounding, and how it locates the two XML files.
- Artifact name(s) and `retention-days`.
- Exact action version tags to write, subject to CI-01's `actions/checkout@v4`,
  `actions/setup-java@v6` (temurin 17), `gradle/actions/setup-gradle@v6` — **the researcher must confirm
  each tag actually exists before it is written into the workflow**, and flag any drift from CI-01 rather
  than silently substituting.
- Whether `architecture: x64` is carried over from the upstream `setup-java` block or dropped.
- Wording of the D-04 header comment.
- Whether the roadmap's 05-01 / 05-02 split stays as-is (it is expected to: 05-02 depends on 05-01's
  workflow existing, and 05-02 contains the D-10 blocking checkpoint, so it cannot be autonomous).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements

- `.planning/ROADMAP.md` § "Phase 5: CI & Final Verification" — goal, the 4 success criteria, and the
  plan split 05-01 (workflow) / 05-02 (final verification)
- `.planning/REQUIREMENTS.md` § "CI" (CI-01..04, lines 127–130) plus § Out of Scope — CI-01 pins the
  exact action tags; CI-03 pins "no coverage threshold"
- `.planning/PROJECT.md` § Constraints, § Key Decisions, § Context — the `latest` × V2 constraint,
  "Coverage: report only, no CI threshold", rebasability, the "CI today" paragraph describing the fork's
  Actions state, and the `Active` follow-up list that CI findings must not silently absorb

### Prior phase decisions that bind this one

- `.planning/phases/01-test-infrastructure-green-baseline/01-CONTEXT.md` — **the most load-bearing prior
  context for this phase.** D-05 (`latest` default image + `alwaysPull`), D-13 (the 15-class
  `forkChangedClasses` include list), D-14 (the separate `jacocoForkReport` task and its XML path, written
  so "Phase 5 CI can publish or summarize its XML directly"), D-16 (JaCoCo on `test` only, reports wired
  via `finalizedBy`, `testUnit` kept out of `check`), D-07 (proxy tests pinned to ClickHouse 23.8 — they
  run in CI too and pull a second image)
- `.planning/phases/04-json-insert-path-connector-plumbing/04-CONTEXT.md` — D-17 confirms no
  `build.gradle.kts` change was needed because every class Phases 2–4 touched is already in the Phase 1
  D-13 include list; the same holds here
- `.planning/STATE.md` § Blockers/Concerns — `gh` is not installed (Phase 5 entry); worktree isolation
  degrades to sequential while `custom_build` has unpushed commits, which D-09's push resolves;
  `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` is a **local shell quirk only** and must NOT be
  carried into the workflow file
- `.planning/config.json` — `workflow.test_command` is
  `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` with `test_gate_timeout: 2400`; that is the local
  gate, not the CI invocation (D-15)

### Files this phase edits or reads

- `.github/workflows/tests.yaml` — **the only file this phase modifies.** Today: `on: [push]`, an 8-cell
  `client × clickhouse` matrix, the cloud-credentials `SKIP_STEP` guard step and its `if:` conditions on
  every subsequent step, `actions/checkout@v3`, `actions/setup-java@v3` (`adopt`, `architecture: x64`),
  `gradle/gradle-build-action@v2` with `arguments: test`. The matrix, the cloud guard, all the `if:`
  conditions, and the cloud secrets env vars all go away with CI-02
- `.github/workflows/test-head.yaml` — **not modified** (D-02), but read for its `concurrency` block,
  which D-14 follows as the in-repo convention
- `build.gradle.kts` — **not modified.** Read `jacoco { toolVersion = "0.8.15" }` (:237), the
  `forkChangedClasses` list (:245–276), `jacocoTestReport` (:278–284), `jacocoForkReport` with its XML at
  `build/reports/jacoco/fork/jacocoForkReport.xml` and HTML at `build/reports/jacoco/fork/html`
  (:287–300), and the `finalizedBy` wiring on `test` (:303–305)

### Environment facts established during discussion

- `git remote -v` → `git@github.com:ne1r0n/clickhouse-kafka-connect.git`; `custom_build` is **98 commits
  ahead** of `origin/custom_build` (last pushed `22f055c`, 2026-01-29)
- GitHub API: repo is **public**, a **fork**, `default_branch: main`. Local `origin/HEAD` points at
  `custom_build`, which is a stale *local* setting and does not reflect GitHub — this is what makes D-16's
  cache read-only
- 79 `.planning/` files are tracked by git; `.gitignore` excludes neither `.planning/` nor `.gsd/`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- **`jacocoForkReport`** (`build.gradle.kts:287–300`) — built in Phase 1 *specifically* for this phase.
  Its XML lands at `build/reports/jacoco/fork/jacocoForkReport.xml` and contains per-class `<counter>`
  elements for exactly the 15 fork-changed classes D-07's table needs. No new Gradle work required.
- **`jacocoTestReport`** — standard layout under `build/reports/jacoco/test/`, giving D-07's full-suite
  headline row from the same single `test` run.
- **`test-head.yaml`'s `concurrency` block** — the in-repo precedent D-14 mirrors, so the pattern is
  copied rather than invented.
- **`python3` on `ubuntu-latest`** — preinstalled, so D-06's XML parsing needs no `setup-python` step and
  no shell XML parsing.

### Established Patterns

- **Coverage is report-only, everywhere.** JaCoCo hangs off `test` via `finalizedBy` and is explicitly
  disabled on `testUnit` and `integrationTest`; there is no `jacocoTestCoverageVerification` rule
  anywhere. CI must not introduce one (CI-03).
- **`check` is not a usable CI target on this fork.** The upstream `tasks.withType<Test>` loop
  (`build.gradle.kts:174–178`) adds `check.dependsOn(this)` for every `Test` task, which drags in
  `integrationTest` — Confluent archive plus cloud credentials. `test` is the only safe entry point.
- **Every ClickHouse-backed test class inherits `@Tag("clickhouse")` from `ClickHouseBase`**, and the
  shared container is one-per-JVM-fork with `alwaysPull`. On a 4-core runner that means 2 forks, so **2
  ClickHouse containers** plus the proxy tests' separate 23.8 container.
- Tests run with `file.encoding=windows-1252` deliberately (`build.gradle.kts:179`) — that is intended
  behavior, not a CI problem to fix.

### Integration Points

- `.github/workflows/tests.yaml` is the single integration point for 05-01. No `src/`, no
  `build.gradle.kts`, no test file changes anywhere in this phase.
- 05-02 integrates with the GitHub REST API (read-only, unauthenticated) and with `git push` behind the
  D-10 checkpoint.

</code_context>

<specifics>
## Specific Ideas

- **The `env -u TESTCONTAINERS_HOST_OVERRIDE` prefix is local-only.** It exists because *this Claude Code
  shell* sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"` (STATE.md). It must not leak
  into the workflow file — a GitHub runner has no such variable, and the prefix there would be noise that
  future readers misread as a real requirement.
- **The public API call was executed during this discussion and returned real data**, which is why D-11
  is stated as verified rather than planned. The same call shape works for runs and artifacts.
- **`jacocoForkReport` was designed for this phase in Phase 1.** D-14's rationale literally reads "Phase 5
  CI can publish or summarize its XML directly". The planner should treat that as an instruction already
  given, not a new idea.
- **Criterion 2 is about job *count*, not trigger scope.** Once the matrix is gone, one job per push
  follows automatically — which is why D-13 leaves `on: [push]` alone. The four other workflow files were
  checked individually and none of them fire on push, so the criterion holds repo-wide.
- **Gradle finalizers run even when the finalized task fails.** That is what makes D-08's `if: always()`
  coverage upload meaningful rather than an empty directory on a red run.
- **The fork's GitHub default branch is `main`, but nothing in this project targets `main`.** All 98
  commits are on `custom_build`. Do not "helpfully" retarget anything at `main`.

</specifics>

<deferred>
## Deferred Ideas

Surfaced during discussion, not decided — kept so they are not lost:

- **Modernizing the four remaining workflows** (`test-head.yaml`, `integrationTests.yaml`,
  `release.yaml`, `pre-release.yaml`) off `checkout@v3` / `setup-java@v3` `adopt` /
  `gradle-build-action@v2`. Declined under D-02 because none run on push and none can be verified by this
  phase. `test-head.yaml` is the strongest candidate for a follow-up: it runs nightly on a cron against
  `CLICKHOUSE_VERSION=head` and nobody is watching it.
- **Warming the Gradle cache on `custom_build`** via `cache-read-only: false`, worthwhile only because
  the fork's GitHub default branch is `main`. Declined under D-16 for this phase; revisit if CI run times
  become a nuisance after the milestone.
- **Making the Gradle build itself render the coverage markdown**, so the same summary is available
  locally and in CI. Declined under D-06 to keep `build.gradle.kts` untouched in this phase.
- **Setting `origin/HEAD` to match GitHub** (`main`) or changing the fork's default branch to
  `custom_build`. The local/remote mismatch is what makes D-16's cache read-only; neither change is in
  scope here, and changing a public repo's default branch is a user decision with its own blast radius.

</deferred>

---

*Phase: 05-ci-final-verification*
*Context gathered: 2026-09-18*
