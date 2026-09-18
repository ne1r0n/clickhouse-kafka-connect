# Phase 5: CI & Final Verification - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-18
**Phase:** 05-ci-final-verification
**Areas discussed:** Workflow file strategy, Coverage publishing, Getting a green run, Job shape & hardening

---

## Workflow file strategy

### Q1 — `tests.yaml` is an upstream file. Where should the fork's single-job CI live?

| Option | Description | Selected |
|--------|-------------|----------|
| Rewrite tests.yaml in place | Matches CI-01/CI-02 literally. One file, one test job on push. Costs a loud, easy-to-resolve conflict on a future upstream sync — and the fork's version should win on that sync anyway. | ✓ |
| Neutralize tests.yaml + add fork-tests.yaml | Trigger becomes workflow_dispatch only; real job in a new fork-only file. Keeps upstream file nearly pristine, but still edits it and leaves two files to reason about. | |
| Delete tests.yaml, add fork-tests.yaml | Cleanest single source of truth, no dead upstream file. But a rebase silently reintroduces the upstream matrix workflow as "new". | |

**User's choice:** Rewrite tests.yaml in place
**Notes:** → CONTEXT.md D-01.

### Q2 — Four other workflows carry the same deprecated actions. None trigger on push. In scope?

| Option | Description | Selected |
|--------|-------------|----------|
| Leave all four alone | CI-01 names only tests.yaml; criterion 2 already satisfied since none run on push. Minimal, rebase-friendly. | ✓ |
| Also modernize test-head.yaml | The only other one that runs `gradle test`, nightly against CLICKHOUSE_VERSION=head. Consistency, but nobody in this project watches it. | |
| Modernize all five | No deprecated action left anywhere. Thorough, but release/pre-release/integrationTests are untestable in this phase, so changes would ship unverified. | |

**User's choice:** Leave all four alone
**Notes:** → CONTEXT.md D-02. `test-head.yaml` recorded as the strongest follow-up candidate under Deferred Ideas.

### Q3 — Should the workflow still set `CLICKHOUSE_VERSION` / `CLIENT_VERSION` explicitly?

| Option | Description | Selected |
|--------|-------------|----------|
| Set both explicitly | Criterion 2 readable straight off the workflow file; a future change to ClickHouseTestHelpers' default can't silently move CI onto another image. | ✓ |
| Rely on the harness defaults | Smallest workflow (Phase 1 D-05 already defaults to `latest`, client defaults to V2), but CI's target becomes invisible in the workflow file. | |
| Pin ClickHouse only | Pins the thing that actually varies; V1 is dead code for this fork anyway. | |

**User's choice:** Set both explicitly
**Notes:** → CONTEXT.md D-03.

### Q4 — Should `tests.yaml` carry a marker that it's fork-modified?

| Option | Description | Selected |
|--------|-------------|----------|
| Header comment naming what was removed | Fork-modified, matrix reduced to latest × V2, V1/23.7/24.3/Cloud dropped, pointer to REQUIREMENTS.md CI-01..04. Whoever hits the rebase conflict knows which side is intentional. | ✓ |
| No marker | Clean YAML, no project commentary — git history already records it. | |
| Rename the workflow instead | Change top-level `name:` so it's obvious in the Actions tab. Visible in the UI, not just the file. | |

**User's choice:** Header comment naming what was removed
**Notes:** → CONTEXT.md D-04.

---

## Coverage publishing

### Q1 — Which JaCoCo report gets uploaded as the CI artifact?

| Option | Description | Selected |
|--------|-------------|----------|
| Both, one artifact | Upload build/reports/jacoco/ wholesale. Criterion 4 wants fork-class coverage reviewable; the full report gives it meaning. One step, no path juggling. | ✓ |
| Fork report only | Smallest artifact; the 15 fork classes are all this project claims coverage for. Loses upstream-regression visibility. | |
| Both, as two named artifacts | Clearer download targets, at the cost of two upload steps and two zips to correlate. | |

**User's choice:** Both, one artifact
**Notes:** → CONTEXT.md D-05.

### Q2 — How should the job-summary coverage be produced?

| Option | Description | Selected |
|--------|-------------|----------|
| Inline script parsing the JaCoCo XML | Reads `<counter>` totals, appends markdown to $GITHUB_STEP_SUMMARY. Zero third-party actions, matching the "only new dependency is JaCoCo" constraint. | ✓ |
| Marketplace action (madrapps/jacoco-report) | Purpose-built and prettier, but aimed at PR comments, and adds a supply-chain dependency to a production fork's CI. | |
| Gradle task writes the summary | Identical locally and in CI, but a build.gradle.kts change in a phase that otherwise touches no build logic. | |

**User's choice:** Inline script parsing the JaCoCo XML
**Notes:** → CONTEXT.md D-06. `python3` is preinstalled on ubuntu-latest, so per-class parsing needs no extra setup step.

### Q3 — What should the job summary show?

| Option | Description | Selected |
|--------|-------------|----------|
| Headline totals + per-class fork table | Two headline rows (full + fork: instruction/line/branch %) plus a 15-row fork-class table. Satisfies criterion 4 without downloading the artifact. | ✓ |
| Headline totals only | Shortest script; per-class detail stays in the downloadable HTML. | |
| Fork per-class table only | Focused on what the project is accountable for, but loses the baseline for comparison. | |

**User's choice:** Headline totals + per-class fork table
**Notes:** → CONTEXT.md D-07.

### Q4 — What happens to coverage and test-report steps when the suite fails?

| Option | Description | Selected |
|--------|-------------|----------|
| Always upload coverage + test reports | `if: always()` plus build/reports/tests/. The first real CI run is on a runner nobody has tested this suite on; a red run with no artifacts is a blind debug. | ✓ |
| Coverage only on success | Artifact always represents a green run, but a failing first run leaves only console log. | |
| Always upload coverage, no test reports | Gradle already prints full stack traces (TestExceptionFormat.FULL), so HTML adds little. | |

**User's choice:** Always upload coverage + test reports
**Notes:** → CONTEXT.md D-08. Gradle finalizers run even when the finalized task fails, so coverage genuinely exists on a red run.

---

## Getting a green run

### Q1 — How do we get the real green CI run, given the push publishes 98 commits incl. `.planning/` to a public fork?

| Option | Description | Selected |
|--------|-------------|----------|
| Push custom_build as-is | Matches criterion 2 literally and clears the STATE.md worktree-isolation blocker. Publishes .planning/ publicly — project docs, no secrets, but permanent. | ✓ |
| Verify on a throwaway branch, then push custom_build | First exercise of a never-tested workflow happens somewhere disposable. Two runs, same eventual publication. | |
| Code-only branch via /gsd-pr-branch | Fork remote stays free of planning docs, but criterion 2's custom_build push is never exercised and 98 commits stay local. | |

**User's choice:** Push custom_build as-is
**Notes:** → CONTEXT.md D-09, rated **one-way**. Context presented before the choice: repo is public, 98 commits ahead of `origin/custom_build` (last pushed `22f055c`, Jan 2026), 79 tracked `.planning/` files, `.gitignore` excludes neither `.planning/` nor `.gsd/`.

### Q2 — Who actually runs `git push origin custom_build` during plan 05-02?

| Option | Description | Selected |
|--------|-------------|----------|
| Blocking-human checkpoint | Plan does everything up to the push, then stops. Mirrors the fork's own D-01 escalation pattern from Phases 2–4. | ✓ |
| Executor pushes autonomously | Fastest, already authorized — but removes the last chance to review the 98-commit diff before it's public and permanent. | |
| You push manually, outside the plan | Total control over timing, but the plan can't self-verify end to end. | |

**User's choice:** Blocking-human checkpoint
**Notes:** → CONTEXT.md D-10.

### Q3 — `gh` isn't installed. How does the plan read the CI run's result?

| Option | Description | Selected |
|--------|-------------|----------|
| Public REST API via curl | Repo is public, so runs?branch=custom_build works unauthenticated (60 req/hr). Then runs/{id}/artifacts confirms the artifact. No install, no auth, scriptable. | ✓ |
| Install gh and authenticate | Nicer ergonomics (gh run watch), but auth is interactive and it's a tool install in a verification phase. | |
| You check the web UI and report back | Zero tooling, but nothing about criterion 3 is machine-verified. | |

**User's choice:** Public REST API via curl
**Notes:** → CONTEXT.md D-11. The approach was **proven live during this session** — an unauthenticated API call returned `default_branch: main`, `private: false`, `fork: true`. That call also surfaced the default-branch fact that drove Job shape Q4.

### Q4 — How do we prove CI-03's job summary actually rendered, given the API can't fetch summaries?

| Option | Description | Selected |
|--------|-------------|----------|
| Echo the same markdown to stdout | Job logs ARE retrievable via the public API, so the plan can machine-verify the exact rendered table. One extra `tee`. | ✓ |
| API proves green + artifact; you eyeball the summary | Less machinery, but CI-03's second half rests on a manual look. | |
| Also write the markdown into the artifact | Downloadable proof surviving the 90-day log window, but checking means downloading and unzipping. | |

**User's choice:** Echo the same markdown to stdout
**Notes:** → CONTEXT.md D-12.

---

## Job shape & hardening

### Q1 — The workflow fires on every push to every branch. What should trigger it?

| Option | Description | Selected |
|--------|-------------|----------|
| Keep bare `on: [push]` | Diff stays purely about the matrix and actions. With the matrix gone, "exactly one job" holds on any branch. Minimum rebase surface. | ✓ |
| push (custom_build) + workflow_dispatch | Saves runner minutes and adds a manual re-run button, but an extra semantic change to an upstream file, and feature branches go untested. | |
| push + pull_request + workflow_dispatch | Most conventional, but this fork takes no PRs and pushes to a PR branch would double-run. | |

**User's choice:** Keep bare `on: [push]`
**Notes:** → CONTEXT.md D-13.

### Q2 — Add job guards, given 4-core runners, a cold cache, and an `alwaysPull` of clickhouse:latest?

| Option | Description | Selected |
|--------|-------------|----------|
| timeout-minutes + concurrency | ~45-minute timeout so a hung Testcontainers wait fails in under an hour instead of burning the 6-hour default, plus cancel-in-progress. test-head.yaml already uses this concurrency pattern. | ✓ |
| timeout-minutes only | Guards the hang; this fork sees infrequent pushes so stacked runs aren't a real problem. | |
| Neither — keep the diff minimal | Smallest rebase surface, at the cost of a 6-hour worst case. | |

**User's choice:** timeout-minutes + concurrency
**Notes:** → CONTEXT.md D-14. `maxParallelForks` is `cores / 2`, so it drops from 4 locally to 2 on a runner.

### Q3 — What should the Gradle step invoke?

| Option | Description | Selected |
|--------|-------------|----------|
| `./gradlew test` only | Both reports already `finalizedBy` of `test` (Phase 1 D-16), and finalizers run even on failure. Closest to upstream's `arguments: test`. | ✓ |
| `./gradlew test jacocoTestReport jacocoForkReport` | Names the reports so CI can't silently stop producing coverage if the finalizedBy wiring is lost in a rebase. | |
| `./gradlew check` | Ruled out: the upstream `tasks.withType<Test>` loop makes `check` depend on `integrationTest`, which needs the Confluent archive and cloud credentials. | |

**User's choice:** `./gradlew test` only
**Notes:** → CONTEXT.md D-15. `gradle/actions/setup-gradle` v4+ dropped the `arguments:` input, so this becomes its own `run:` step.

### Q4 — `setup-gradle` writes its cache only on the default branch, which for this fork is `main`, not `custom_build`. Change that?

| Option | Description | Selected |
|--------|-------------|----------|
| Accept the cold cache | Criterion 3's run is cold either way; ~1 minute of dependency download against a multi-minute suite plus a full image pull. Zero extra knobs in an upstream file. | ✓ |
| Set cache-read-only: false | Every run after the first gets meaningfully faster, but it's a non-obvious flag whose reason isn't visible at the call site. | |
| Disable Gradle caching entirely | Identically cold and fully reproducible every run, no cache-poisoning surface. Slowest. | |

**User's choice:** Accept the cold cache
**Notes:** → CONTEXT.md D-16. The default-branch fact was discovered mid-discussion via the same public API call used for D-11.

---

## Claude's Discretion

- Red-CI-run policy (workflow-config failures fixed and re-pushed inside 05-02; test/product failures escalate)
- Poll cadence, give-up window, and how the run is located (by `head_sha` vs. latest run on branch)
- Exact `timeout-minutes` value and concurrency group expression
- Summary script language, markdown layout, column set, rounding, and how it locates the two XML files
- Artifact name(s) and `retention-days`
- Exact action version tags, subject to CI-01 — researcher must confirm each tag exists before writing it
- Whether `architecture: x64` is carried over from the upstream `setup-java` block
- Wording of the D-04 header comment
- Whether the roadmap's 05-01 / 05-02 plan split stays as-is

## Deferred Ideas

- Modernizing the four remaining workflows (`test-head.yaml` is the strongest candidate — nightly cron against `CLICKHOUSE_VERSION=head`, unwatched)
- Warming the Gradle cache on `custom_build` via `cache-read-only: false`
- Making the Gradle build itself render the coverage markdown, so it's available locally too
- Setting `origin/HEAD` to match GitHub (`main`), or changing the fork's default branch to `custom_build`
