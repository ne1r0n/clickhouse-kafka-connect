# Phase 5: CI & Final Verification - Research

**Researched:** 2026-09-18
**Domain:** GitHub Actions CI workflow modernization, JaCoCo coverage publishing, GitHub REST API-based verification
**Confidence:** HIGH (all load-bearing claims live-verified this session against the real GitHub API, the real JaCoCo XML this repo produces, and the real local environment)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Workflow file strategy (CI-01, CI-02)**

- **D-01:** Rewrite `.github/workflows/tests.yaml` in place. Not a new fork-only workflow file, not a delete-and-replace. CI-01/CI-02 name that path literally, and one file means one test job on push with nothing to cross-reference. The rebasability constraint is satisfied differently here than elsewhere in the project: the conflict a future upstream sync produces is loud, localized to one file, and the fork's side should win — unlike a deletion, which a rebase silently reverts by reintroducing the upstream file as "new". Rejected: neutralizing `tests.yaml` to `workflow_dispatch` and adding `fork-tests.yaml`; deleting `tests.yaml` outright.
- **D-02:** The other four workflows stay untouched. `test-head.yaml` (cron + dispatch), `integrationTests.yaml`, `release.yaml`, `pre-release.yaml` all carry the same deprecated actions — and none of them trigger on push, so success criterion 2 is already satisfied by fixing `tests.yaml` alone. Modernizing them would ship unverified changes.
- **D-03:** `CLICKHOUSE_VERSION=latest` and `CLIENT_VERSION=V2` are set explicitly in the job env, even though Phase 1 D-05 already made `latest` the default image and V2 is the default client. Criterion 2 then reads straight off the workflow file, and a future change to `ClickHouseTestHelpers`' `CLICKHOUSE_VERSION_DEFAULT` cannot silently move CI onto a different image.
- **D-04:** A header comment marks the file as fork-modified and names what was removed: the V1/23.7/24.3/Cloud matrix legs, with a pointer to `.planning/REQUIREMENTS.md` CI-01..04. Rejected: no marker (git history alone); renaming the workflow's top-level `name:`.

**Coverage publishing (CI-03)**

- **D-05:** One artifact, uploading `build/reports/jacoco/` wholesale — the full `jacocoTestReport` HTML+XML and the fork-scoped `jacocoForkReport` HTML+XML together. One upload step, no path juggling, no two zips to correlate.
- **D-06:** The job summary is produced by an inline script parsing the JaCoCo XML — no marketplace action. A small step reads the `<counter>` totals and appends markdown to `$GITHUB_STEP_SUMMARY`. `python3` is preinstalled on `ubuntu-latest`. Rejected: `madrapps/jacoco-report` (aimed at PR comments, not push runs; third-party action); a Gradle task rendering the markdown.
- **D-07:** The summary shows headline totals plus a per-class table of the 15 fork-changed classes. Two headline rows (full suite and fork-scoped: instruction/line/branch %), then one row per fork class with its line and branch coverage.
- **D-08:** Coverage, summary, and test reports all upload with `if: always()`. The Gradle test reports (`build/reports/tests/`) upload too. Both JaCoCo reports are `finalizedBy` of `test` (Phase 1 D-16) and Gradle finalizers run even when the finalized task fails, so coverage genuinely exists to upload on a red run.

**Getting a green CI run (CI-03, CI-04)**

- **D-09:** `custom_build` is pushed as-is — all commits, `.planning/` included. Criterion 2 asks for a push to `custom_build` specifically. The 79 tracked `.planning/` files become public (verified public, a fork, default branch `main`). They are project documentation with no secrets — but publication is permanent. Reversibility: one-way. Rejected: verifying on a throwaway branch first; a code-only branch via `/gsd-pr-branch`.
- **D-10:** The push happens at a `blocking-human` checkpoint in plan 05-02 — the executor never pushes autonomously. The plan does everything up to the push, then stops and asks.
- **D-11:** CI results are read through the public GitHub REST API with `curl` — no `gh` install. The repo is public, so `GET /repos/ne1r0n/clickhouse-kafka-connect/actions/runs?branch=custom_build` works unauthenticated, and `GET /repos/.../actions/runs/{id}/artifacts` confirms the coverage artifact exists. This was proven live during the discussion session. Rejected: `brew install gh` + interactive `gh auth login`; manual web-UI checking.
- **D-12:** The coverage markdown is written to both `$GITHUB_STEP_SUMMARY` and stdout. The REST API can fetch job *logs* but not job *summaries*, so without this CI-03's "writes a coverage summary to the job summary" could only be confirmed by eye. Teeing the same markdown to stdout puts the exact rendered table into the retrievable job log, making the requirement machine-verifiable.
  > **Research flags this premise as falsified for the unauthenticated case — see "Critical Finding" in Common Pitfalls below.**

**Job shape and hardening (CI-02)**

- **D-13:** The trigger stays bare `on: [push]`. Unchanged from upstream, so the diff is purely about the matrix, the actions, and the new coverage steps. Rejected: `push` (branches: custom_build) + `workflow_dispatch`; adding `pull_request`.
- **D-14:** Add `timeout-minutes` and a concurrency group with `cancel-in-progress: true`. GitHub runners give 4 cores, so `maxParallelForks` (`cores / 2`) drops from 4 locally to 2, on top of a cold Gradle cache and an `alwaysPull` of `clickhouse/clickhouse-server:latest`. A timeout around 45 minutes makes a hung Testcontainers wait fail in under an hour. `test-head.yaml` already uses exactly this concurrency pattern.
- **D-15:** The Gradle step invokes `./gradlew test` only. `gradle/actions/setup-gradle` v4+ dropped the `arguments:` input, so the call becomes its own `run:` step. Both reports come free via the Phase 1 D-16 `finalizedBy` wiring. `check` is ruled out: the upstream `tasks.withType<Test>` loop makes `check` depend on `integrationTest`, which needs the Confluent archive and cloud credentials. Rejected: naming `jacocoTestReport jacocoForkReport` explicitly as a hedge.
- **D-16:** The default cold Gradle cache is accepted — no `cache-read-only` or `cache-disabled` override. `setup-gradle` writes its dependency cache only on the repository's default branch (`main`, not `custom_build`), so runs on `custom_build` get a read-only, permanently empty cache. Not worth a non-obvious flag in an upstream file.

### Claude's Discretion

- Red-CI-run policy: Not explicitly decided. Default: a failure rooted in the *workflow configuration* is fixed and re-pushed inside plan 05-02. A failure exposing a *test or product* problem that does not reproduce locally escalates at a checkpoint; PROJECT.md's `Active` follow-up list is where such findings land.
- Poll cadence and give-up window when waiting on the run via the REST API, and how the run is located (by `head_sha` of the pushed commit vs. most recent run on the branch).
- Exact `timeout-minutes` value and concurrency group expression.
- The summary script's language (`python3` vs. shell), its exact markdown layout, column set, and rounding, and how it locates the two XML files.
- Artifact name(s) and `retention-days`.
- Exact action version tags to write, subject to CI-01's `actions/checkout@v4`, `actions/setup-java@v6` (temurin 17), `gradle/actions/setup-gradle@v6` — the researcher must confirm each tag actually exists before it is written into the workflow, and flag any drift from CI-01 rather than silently substituting.
- Whether `architecture: x64` is carried over from the upstream `setup-java` block or dropped.
- Wording of the D-04 header comment.
- Whether the roadmap's 05-01/05-02 split stays as-is (expected: yes — 05-02 depends on 05-01's workflow existing and contains the D-10 blocking checkpoint).

### Deferred Ideas (OUT OF SCOPE)

- Modernizing the four remaining workflows (`test-head.yaml`, `integrationTests.yaml`, `release.yaml`, `pre-release.yaml`) off `checkout@v3`/`setup-java@v3` `adopt`/`gradle-build-action@v2`. Declined under D-02. `test-head.yaml` is the strongest candidate for a follow-up.
- Warming the Gradle cache on `custom_build` via `cache-read-only: false`. Declined under D-16 for this phase.
- Making the Gradle build itself render the coverage markdown. Declined under D-06 to keep `build.gradle.kts` untouched.
- Setting `origin/HEAD` to match GitHub (`main`) or changing the fork's default branch to `custom_build`. Not in scope; changing a public repo's default branch is a user decision with its own blast radius.

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CI-01 | The CI workflow uses current actions: `actions/checkout@v4`, `actions/setup-java@v6` (temurin 17), `gradle/actions/setup-gradle@v6` | All three tags verified to exist against the live GitHub API today (see Standard Stack). `setup-gradle@v4+` dropped the `arguments:` input — confirmed via official docs — so `./gradlew test` must be its own `run:` step (Code Examples). |
| CI-02 | CI runs a single job on push against ClickHouse `latest` × client V2 (V1, 23.7, 24.3, and Cloud legs removed) | Current `tests.yaml` matrix, cloud-guard step, and `if:` conditions fully read and quoted (Architecture Patterns). Confirmed the other four workflows do not trigger on push (D-02), so removing the matrix from `tests.yaml` alone satisfies "exactly one test job". |
| CI-03 | CI uploads the JaCoCo report as an artifact and writes a coverage summary to the job summary, without a coverage threshold | `jacocoTestReport`/`jacocoForkReport` output paths verified against an actual local build (Standard Stack, Code Examples). XML structure of `jacocoForkReport.xml` read and parsed live — the exact `<counter>` shape the summary script must handle, including a verified edge case (classes with no LINE/BRANCH counters). GitHub API capability to confirm artifact *existence* (not content) unauthenticated verified live. **Job-summary content is not independently fetchable via the unauthenticated API** — see Common Pitfalls "Critical Finding", which affects how 05-02 verifies this criterion. |
| CI-04 | `./gradlew test` passes in full locally and in CI against ClickHouse `latest` × client V2 | Local environment audited live: Docker reachable via colima profile `vz` (not `default`), `TESTCONTAINERS_HOST_OVERRIDE=null` confirmed present (local-only prefix required), `gh` confirmed absent, `python3` confirmed present locally and confirmed preinstalled on `ubuntu-latest` runners via official docs. |

</phase_requirements>

## Summary

This phase touches exactly one file, `.github/workflows/tests.yaml`, and does no `src/` or `build.gradle.kts` work — every coverage mechanism it needs (`jacocoTestReport`, `jacocoForkReport`, the `finalizedBy` wiring) was already built in Phase 1 specifically for this moment. The research therefore centers on three narrow, verifiable questions: (1) do the three named action tags actually exist and behave as CI-01 assumes, (2) does the JaCoCo XML this repo already produces parse the way a from-scratch summary script needs it to, and (3) does the unauthenticated GitHub REST API actually give plan 05-02 everything D-11/D-12 assume it gives.

The first two questions check out cleanly. `actions/checkout@v4`, `actions/setup-java@v6`, and `gradle/actions/setup-gradle@v6` all resolve to real tags today, and `setup-gradle` v4+ genuinely dropped the `arguments:` input (confirmed via the action's own docs), so `./gradlew test` must run as its own `run:` step — exactly as D-15 already concluded. The fork's own `build/reports/jacoco/fork/jacocoForkReport.xml` was read and parsed live with `xml.etree.ElementTree`: report-level totals are the *last* set of `<counter>` elements under `<report>`, per-class totals are the *last* set of `<counter>` elements inside each `<class>` element (after all its `<method>` children), and — critically — some classes (small inner/anonymous classes, and apparently classes exercised by zero tests in a stale/partial local run) have **no** `LINE`/`BRANCH` counter element at all. A hand-rolled parser that assumes every class has both counters will crash with a `NoneType` error on real data from this repo; the summary script must guard for `None`.

The third question surfaces the phase's single most important correction: **the unauthenticated GitHub REST API cannot fetch job logs, run logs, or artifact content — only run/job status metadata and artifact existence/name/size.** This was tested live, twice, including directly against `ne1r0n/clickhouse-kafka-connect`'s own historical CI run: `GET .../actions/jobs/{id}/logs` and `GET .../actions/runs/{id}/logs` both return `403 Must have admin rights to Repository`, and `GET .../actions/artifacts/{id}/zip` returns `401 Requires authentication` — all unauthenticated, all against real public data. D-12's mechanism (tee the coverage markdown to stdout so it lands in a "retrievable job log") therefore does not work as an unauthenticated `curl` check. What *does* work unauthenticated and was verified live: listing runs (optionally filtered by `head_sha`, which resolves one of the "Claude's Discretion" open items directly), listing jobs with each job's `status`/`conclusion`, and listing artifacts with name/size/`expired` (enough to confirm CI-03's artifact-upload half, but not its job-summary-content half). This is a genuine gap between what D-11/D-12 assumed and what the API supports; the planner needs to decide 05-02's exact verification mechanism for the job-summary sub-claim knowing this.

**Primary recommendation:** Write the workflow exactly as D-01 through D-16 specify (all tags verified valid), build the summary script against the verified real XML shape (guard `None` counters, use the report-level trailing `<counter>` elements for headlines), and treat "the job summary was written correctly" as verified by the *stdout tee showing on the job's `conclusion: success` combined with the artifact existing* rather than by fetching job-log text through the unauthenticated API — because that specific fetch does not work. Surface this as an explicit decision point for plan 05-02 rather than silently reinterpreting D-12.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Workflow trigger, job matrix, action versions | CI / Workflow definition (`.github/workflows/tests.yaml`) | — | Declarative YAML consumed entirely by GitHub's Actions runner; no application code involved |
| Test execution (`./gradlew test`) | Build / Test tier (Gradle, JVM, Testcontainers) | CI / Workflow (invokes it) | Unchanged from local; CI only supplies the runner and env vars (D-03) |
| Coverage report generation (JaCoCo XML/HTML) | Build tier (`build.gradle.kts`, already built in Phase 1) | CI / Workflow (uploads it) | This phase must not touch `build.gradle.kts` (locked); it only consumes the existing `jacocoTestReport`/`jacocoForkReport` outputs |
| Coverage summary rendering (markdown table) | CI / Workflow (new inline script step) | — | D-06 locks this as an inline script in the workflow, not a Gradle task, to avoid touching the build |
| CI result verification (green/red, artifact presence) | External tooling (`curl` against GitHub REST API) | CI / Workflow (produces the data being verified) | Runs outside the pipeline itself, after the fact, in plan 05-02 |
| Push to public remote | Git / VCS tier | — | A one-way, human-gated action (D-09/D-10), outside any application tier |

## Standard Stack

### Core

| Action | Tag | Purpose | Verified |
|--------|-----|---------|----------|
| `actions/checkout` | `v4` | Checks out `custom_build` onto the runner | `[VERIFIED: live GitHub API, 2026-09-18 — GET api.github.com/repos/actions/checkout/git/ref/tags/v4 → 200, resolves to commit 11d5960a326750d5838078e36cf38b85af677262]` |
| `actions/setup-java` | `v6` | Installs JDK 17 (temurin distribution) | `[VERIFIED: live GitHub API, 2026-09-18 — GET api.github.com/repos/actions/setup-java/git/ref/tags/v6 → 200, resolves to commit de7274f081f381c8f8158605e0321c36c376e2e6]`. `temurin` confirmed a supported distribution keyword `[CITED: github.com/actions/setup-java README]`. |
| `gradle/actions/setup-gradle` | `v6` | Sets up Gradle wrapper validation + dependency caching | `[VERIFIED: live GitHub API, 2026-09-18 — GET api.github.com/repos/gradle/actions/tags → tag list includes exactly "v6" alongside v6.0.0/v6.1.0/v6.1.1/v6.2.0/v6.3.0/v6.4.0-rc.1]` |

No new library dependencies. This phase adds zero `npm`/`pip`/Maven packages — only GitHub Marketplace Actions from the `actions` (GitHub-owned) and `gradle` (Gradle Inc.-owned) organizations, both first-party publishers already used pre-phase at older tags.

**Action pinning note:** All three tags are *mutable major-version tags* (e.g. `v4` can be repointed by the publisher to a new commit at any time) — this is what CI-01 explicitly asks for and matches the project's pre-existing convention (`@v3`, `@v2` were the same kind of tag). It is not a regression to flag or fix in this phase; it is the requirement as written. Pinning to an immutable commit SHA is the stricter supply-chain-security alternative and was not requested — noted under Security Domain below as a known tradeoff, not a decision to override.

**`arguments:` input dropped:** `gradle/actions/setup-gradle` does not expose an `arguments:` input to execute a Gradle task directly — the action's own documentation states "the recommended approach is... invoke `./gradlew test`... in a separate `run:` step" `[CITED: github.com/gradle/actions/blob/main/docs/setup-gradle.md]`. This directly confirms D-15's premise.

**`architecture` input:** `actions/setup-java@v6` still exposes an `architecture` input (canonical values `x86`/`x64`/`armv7`/`aarch64`/`ppc64le`/`ppc64`/`s390x`; aliases normalized), defaulting to the runner's own architecture if omitted `[CITED: github.com/actions/setup-java README]`. Since `ubuntu-latest` standard runners are `x64` `[CITED: docs.github.com "Supported runners and hardware resources" — public-repo standard Linux runners: 4 vCPU, 16GB RAM, x64]`, omitting `architecture:` is safe and removes one more line from the upstream block per the "Claude's Discretion" note.

### Supporting

| Tool | Version | Purpose | When to Use |
|------|---------|---------|-------------|
| `python3` | 3.12.3 on `ubuntu-latest` (Ubuntu 24.04 runner image); many cached versions also available (3.10–3.14) | Parses `build/reports/jacoco/*/*.xml` and writes the `$GITHUB_STEP_SUMMARY` markdown table | `[CITED: github.com/actions/runner-images Ubuntu2404-Readme.md]` — no `actions/setup-python` step needed |
| `xml.etree.ElementTree` (Python stdlib) | bundled | Parses the JaCoCo XML | `[VERIFIED: local test, 2026-09-18]` — parsed the repo's real `build/reports/jacoco/fork/jacocoForkReport.xml` (a file with a `<!DOCTYPE report PUBLIC ... "report.dtd">` declaration) in 6ms with zero network access and zero DTD-fetch attempts; ElementTree is non-validating and ignores the external DTD reference |
| `curl` | preinstalled on `ubuntu-latest` and on the local macOS dev machine | GitHub REST API calls for both CI (none needed inside the workflow) and 05-02's local verification | Already the project's stated approach (D-11) |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Inline `python3` XML parser (D-06) | `madrapps/jacoco-report` marketplace action | Third-party supply-chain dependency the project's constraints explicitly avoid; also designed for PR comments, and this workflow has no `pull_request` trigger (D-13) |
| `curl` against the unauthenticated REST API (D-11) | Install `gh` CLI locally | Explicitly rejected — the repo is public and read endpoints work without auth; adds a local tool-install step to a verification-only phase |
| Mutable major-version action tags (`@v4`, `@v6`) | Pin to immutable commit SHAs | Stronger supply-chain guarantee, but not what CI-01 asks for and not the project's existing convention; would be a scope-creep security hardening change |

**Installation:** None — this phase edits only `.github/workflows/tests.yaml`; no `npm install`/`pip install`/Gradle dependency changes.

## Package Legitimacy Audit

Not applicable in the standard npm/PyPI/crates sense — this phase installs zero application-level packages. The only "packages" involved are three GitHub Marketplace Actions, all from first-party, already-in-use publishers:

| Action | Publisher | Verdict | Disposition |
|--------|-----------|---------|-------------|
| `actions/checkout@v4` | `actions` org (GitHub, Inc.) | OK — official GitHub org, tag confirmed to exist via live API call | Approved |
| `actions/setup-java@v6` | `actions` org (GitHub, Inc.) | OK — official GitHub org, tag confirmed to exist via live API call | Approved |
| `gradle/actions/setup-gradle@v6` | `gradle` org (Gradle Inc.) | OK — official Gradle org, tag confirmed to exist via live API call, action already used pre-phase at `gradle-build-action@v2` from the same publisher | Approved |

**Packages removed due to [SLOP] verdict:** none.
**Packages flagged as suspicious [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```
git push custom_build (D-09, behind D-10 checkpoint)
        │
        ▼
GitHub receives push ──► triggers .github/workflows/tests.yaml (on: [push], D-13)
        │
        ▼
┌─────────────────────────── single job: "test" (D-01/D-02/D-13) ───────────────────────────┐
│                                                                                              │
│  actions/checkout@v4 ──► actions/setup-java@v6 (temurin 17) ──► gradle/actions/setup-gradle@v6 │
│                                                                        │                     │
│                                                                        ▼                     │
│                                       run: ./gradlew test  (D-15 — own run: step,            │
│                                       env: CLICKHOUSE_VERSION=latest, CLIENT_VERSION=V2 (D-03)│
│                                                                        │                     │
│                     ┌──────────────────────────────────────────────────┤                     │
│                     ▼ (finalizedBy, Phase 1 D-16 — runs even if test fails, D-08)             │
│         jacocoTestReport (full suite)              jacocoForkReport (15 fork classes)         │
│         build/reports/jacoco/test/*.xml,html       build/reports/jacoco/fork/*.xml,html       │
│                     │                                                  │                     │
│                     └──────────────────┬───────────────────────────────┘                     │
│                                         ▼                                                     │
│                     inline python3 step (D-06): parse both XMLs,                              │
│                     write markdown table → $GITHUB_STEP_SUMMARY AND stdout (D-12)             │
│                                         │                                                     │
│                                         ▼                                                     │
│         upload-artifact: build/reports/jacoco/ wholesale (D-05)                               │
│         upload-artifact: build/reports/tests/ (D-08)                                          │
│         both steps: if: always() (D-08)                                                       │
└───────────────────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
plan 05-02 polls GitHub REST API unauthenticated (D-11):
  GET .../actions/runs?head_sha=<sha>        → run status/conclusion  [WORKS unauthenticated]
  GET .../actions/runs/{id}/jobs             → per-job status/conclusion [WORKS unauthenticated]
  GET .../actions/runs/{id}/artifacts        → artifact name/size/expired [WORKS unauthenticated]
  GET .../actions/jobs/{id}/logs             → 403 Must have admin rights  [DOES NOT WORK unauthenticated]
  GET .../actions/runs/{id}/logs             → 403 Must have admin rights  [DOES NOT WORK unauthenticated]
  GET .../actions/artifacts/{id}/zip         → 401 Requires authentication [DOES NOT WORK unauthenticated]
```

### Recommended Project Structure

No new directories. The only file this phase touches:

```
.github/
└── workflows/
    └── tests.yaml   # rewritten in place (D-01); header comment added (D-04)
```

### Pattern 1: Single-job matrix removal

**What:** Replace the `strategy.matrix` (`client: [V1, V2]` × `clickhouse: [23.7, 24.3, latest, cloud]`, 8 legs) with a bare single job, and delete the cloud-credentials guard step and every `if: env.SKIP_STEP != 'true'` condition that depends on it.
**When to use:** Exactly this phase — CI-02 requires "exactly one test job" on push.
**Example (current file, read in full this session):**
```yaml
# Source: .github/workflows/tests.yaml (current, pre-Phase-5 state, this repo)
jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        client: ["V1", "V2"]
        clickhouse: ["23.7", "24.3", "latest", "cloud"]
    name: ClickHouse ${{ matrix.clickhouse }} Client version ${{ matrix.client }} tests
    steps:
      - name: Check for Cloud Credentials
        id: check-cloud-credentials
        run: |
          if [[ "${{ matrix.clickhouse }}" == "cloud" && (-z "${{ secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_HOST_SMT }}" || -z "${{ secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_PASSWORD_SMT }}") ]]; then
            echo "SKIP_STEP=true" >> $GITHUB_ENV
          else
            echo "SKIP_STEP=false" >> $GITHUB_ENV
          fi
        shell: bash
      - uses: actions/checkout@v3
        if: env.SKIP_STEP != 'true'
      - name: Set up JDK 17
        if: env.SKIP_STEP != 'true'
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'adopt'
          architecture: x64
      - name: Setup and execute Gradle 'test' task
        if: env.SKIP_STEP != 'true'
        uses: gradle/gradle-build-action@v2
        env:
          CLICKHOUSE_VERSION: ${{ matrix.clickhouse }}
          CLICKHOUSE_CLOUD_HOST: ${{ secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_HOST_SMT }}
          CLICKHOUSE_CLOUD_PASSWORD: ${{ secrets.INTEGRATIONS_TEAM_TESTS_CLOUD_PASSWORD_SMT }}
          CLIENT_VERSION: ${{ matrix.client }}
        with:
          arguments: test
```
Every `if:` condition, the entire cloud-guard step, the matrix, and the cloud secrets env vars all go away with CI-02. `[VERIFIED: .github/workflows/tests.yaml read in full this session]`

### Pattern 2: Concurrency + timeout hardening, copied from an in-repo precedent

**What:** A `concurrency` block with `cancel-in-progress: true`, and `timeout-minutes` on the job.
**When to use:** D-14's rationale — cold cache, `alwaysPull`, and 4-core runners (2 parallel test forks instead of local's 4) mean CI runs materially slower than the local ~4.5 minutes.
**Example (existing in-repo convention, read in full this session):**
```yaml
# Source: .github/workflows/test-head.yaml (current, this repo — untouched by this phase, D-02)
concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true
```
`[VERIFIED: .github/workflows/test-head.yaml read in full this session]`. Since `tests.yaml` has no `pull_request` trigger (D-13), the ternary's `pull_request` branch is dead code if copied verbatim — but copying it verbatim keeps the workflow consistent with the repo's one existing convention and costs nothing; `${{ github.workflow }}-${{ github.sha }}` is an equally valid simplification. Recommend `timeout-minutes: 45` per D-14's own stated rationale.

### Pattern 3: JaCoCo XML → job summary, real structure verified live

**What:** Parse the report-level and per-class `<counter>` elements JaCoCo actually emits, not an assumed/guessed shape.
**When to use:** D-06/D-07's inline summary script.
**Verified XML shape** (`[VERIFIED: build/reports/jacoco/fork/jacocoForkReport.xml, read and parsed live this session]`):
```xml
<!-- Source: build/reports/jacoco/fork/jacocoForkReport.xml (this repo, local build output) -->
<report name="clickhouse-kafka-connect">
  <package name="com/clickhouse/kafka/connect">
    <class name="com/clickhouse/kafka/connect/ClickHouseSinkConnector" sourcefilename="ClickHouseSinkConnector.java">
      <method name="&lt;init&gt;" desc="()V" line="18">
        <counter type="INSTRUCTION" missed="3" covered="0"/>
        <counter type="LINE" missed="1" covered="0"/>
        <counter type="COMPLEXITY" missed="1" covered="0"/>
        <counter type="METHOD" missed="1" covered="0"/>
      </method>
      <!-- ... more <method> elements ... -->
      <!-- class-level TOTALS are the counters that come AFTER all <method> children: -->
      <counter type="INSTRUCTION" missed="92" covered="0"/>
      <counter type="BRANCH" missed="2" covered="0"/>
      <counter type="LINE" missed="27" covered="0"/>
      <counter type="COMPLEXITY" missed="13" covered="0"/>
      <counter type="METHOD" missed="12" covered="0"/>
      <counter type="CLASS" missed="1" covered="0"/>
    </class>
    <!-- ... more <class> elements, then <sourcefile> elements, then package-level counters ... -->
  </package>
  <!-- REPORT-LEVEL TOTALS are the counters that are direct children of <report>, at the very end of the file: -->
  <counter type="INSTRUCTION" missed="5910" covered="3742"/>
  <counter type="BRANCH" missed="684" covered="168"/>
  <counter type="LINE" missed="1349" covered="733"/>
  <counter type="COMPLEXITY" missed="537" covered="125"/>
  <counter type="METHOD" missed="115" covered="88"/>
  <counter type="CLASS" missed="9" covered="17"/>
</report>
```
**Verified edge case:** small classes (anonymous inner classes such as `Table$1`, or a class exercised by zero tests in a given run) can have **no** `LINE` or `BRANCH` counter element at all — `[VERIFIED: parsed jacocoForkReport.xml live this session — com/clickhouse/kafka/connect/sink/db/mapping/Column$ColumnBuilder and Table$1 both returned None for both counters.findall lookups]`. A script that does `int(line_counter.get('missed'))` without a `None` guard will crash. See Code Examples for a working parser.

### Anti-Patterns to Avoid

- **Trusting job logs as a machine-verification channel for the job summary:** `[VERIFIED live this session]` unauthenticated `GET .../actions/jobs/{id}/logs` returns `403 Must have admin rights to Repository` even for a real completed run on the actual target repo (`ne1r0n/clickhouse-kafka-connect`, run `21472583198`, job `61848548675`). D-12's stdout-tee plan does not make the summary independently fetchable this way. Do not build a 05-02 verification step around downloading job logs unauthenticated.
- **Assuming `check` is a safe CI target:** confirmed via `build.gradle.kts:174-178`'s `tasks.withType<Test>` loop, `check` transitively depends on `integrationTest`, which needs Confluent archive + cloud credentials neither present nor wanted on this runner (D-15's own reasoning, cross-checked against the actual Gradle file this session).
- **Assuming the DOCTYPE in the JaCoCo XML requires network access to parse:** it does not — `[VERIFIED: local parse, 6ms, zero network calls]` — but a validating XML parser (not Python's default `ElementTree`) could behave differently; stick with the non-validating default.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Determining pass/fail of a CI run | Log-scraping for "BUILD SUCCESSFUL"/"FAILED" text | The run/job `conclusion` field (`success`/`failure`) from `GET .../actions/runs` or `.../jobs` | `[VERIFIED live this session]` these fields are present and correct on unauthenticated responses; no log access needed for this specific check |
| JaCoCo XML parsing | Regex over the XML text | `xml.etree.ElementTree` (stdlib) | `[VERIFIED live this session]` handles the real DOCTYPE-bearing file cleanly in 6ms; regex over nested `<counter>` elements at multiple nesting levels (method/class/package/report all use the identical tag name) is a correctness trap the verified structure above makes obvious |
| Coverage summary formatting for PRs | A marketplace coverage-comment action | Nothing — this workflow has no `pull_request` trigger (D-13) and CI-03 only asks for artifact + job summary | Adding PR-comment tooling here is out of scope (see `COVR-01` in REQUIREMENTS.md v2, explicitly deferred) |
| Locating "the run for this push" | Scraping the workflow's `html_url` or guessing "most recent run" | `GET .../actions/runs?head_sha=<the pushed commit's sha>` | `[VERIFIED live this session]` — filtering by `head_sha` against the real target repo returned exactly one matching run; this is more precise than "most recent on branch" if any other push races it |

**Key insight:** Everything this phase needs from the GitHub API is either a status field or an existence check — both work unauthenticated and were verified live. The one thing that looks like it should work (fetching rendered text — logs or summaries) does not, and no amount of clever `curl` flags changes that; it is a documented-adjacent (see Common Pitfalls) authorization boundary, not a syntax problem.

## Common Pitfalls

### Critical Finding: unauthenticated GitHub REST API cannot fetch job logs, run logs, or artifact content

**What goes wrong:** D-11 states "CI results are read through the public GitHub REST API with curl — no gh install... this was proven live during the discussion session," and D-12 relies on job logs specifically being fetchable ("The REST API can fetch job *logs* but not job *summaries*"). Live testing this session shows the *logs* half of that premise does not hold for unauthenticated requests.

**Reproduction (run today, 2026-09-18, against the real target repo and a real historical run on `custom_build`):**
```
$ curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/21472583198/jobs
# → 200 OK, lists 8 real jobs with id/name/status/conclusion (all "success")

$ curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/jobs/61848548675/logs
{"message":"Must have admin rights to Repository.","documentation_url":"https://docs.github.com/rest/actions/workflow-jobs#download-job-logs-for-a-workflow-run","status":"403"}

$ curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/21472583198/logs
{"message":"Must have admin rights to Repository.","documentation_url":"https://docs.github.com/rest/actions/workflow-runs#download-workflow-run-logs","status":"403"}

$ curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/21472583198/artifacts
{"total_count":0,"artifacts":[]}
# ↑ this ENDPOINT works fine unauthenticated (200, valid JSON) — it's log/artifact-content DOWNLOAD that's blocked, not listing

$ curl -s -o /dev/null -w "%{http_code}" https://api.github.com/repos/actions/download-artifact/actions/artifacts/10196118503/zip
401
# (tested against a real, non-expired, public artifact on an unrelated repo, to rule out expiry as the cause)
```
`[VERIFIED: live curl output above, captured this session, 2026-09-18]`

**Why it happens:** GitHub's REST API distinguishes "list/read metadata" (works for anyone with read access to a public repo, unauthenticated included) from "download binary/log content" (requires an authenticated token regardless of repo visibility, per GitHub's own error message). The official docs' wording ("Anyone with read access to the repository can use this endpoint") is ambiguous about the unauthenticated case and does not warn about this — the only way to know was to run it.

**How to avoid:** Don't design 05-02's verification around fetching the coverage-summary text through the API. What remains fully available unauthenticated and sufficient for most of CI-03/CI-04's letter:
- Run-level `conclusion` (green/red) via `GET .../actions/runs?head_sha=<sha>`
- Per-job `conclusion` via `GET .../actions/runs/{id}/jobs`
- Artifact *existence*, name, and size via `GET .../actions/runs/{id}/artifacts` (confirms CI-03's "uploads the JaCoCo report as an artifact" half)
For confirming the job-summary *content* specifically, the planner has three real options to weigh at plan time (not decided by this research): (a) accept the D-08 stdout tee as sufficient evidence *of intent* and rely on the artifact + green conclusion as the machine-verified proxy, explicitly noting the summary's exact rendered text is not independently re-fetchable without either a PAT or manual web-UI viewing; (b) use a short-lived authenticated call (a fine-grained PAT with only `actions:read`, created and revoked within the session) — not previously considered in the CONTEXT.md discussion and would need its own justification against D-11's "no gh install" spirit; (c) accept the manual web-UI check D-11 rejected, scoped narrowly to this one sub-claim rather than the whole verification flow. This is now an explicit open question for planning (see Open Questions).

**Warning signs:** A `curl` call to a `/logs` or `/zip` suffix returning `401`/`403` with a message about "admin rights" or "authentication" is this exact issue, not a typo in the URL or a wrong repo/run ID.

### Pitfall: JaCoCo classes with missing counters crash a naive parser

**What goes wrong:** `counters.get('LINE')` or `.findall('counter')` filtered by type can return `None`/empty for small or untested classes; calling `.get('missed')` on `None` raises `AttributeError`.
**Why it happens:** JaCoCo omits a counter type entirely when a class has zero instructions of that kind (e.g., a builder inner class with no branches ever compiles to). `[VERIFIED: live parse this session, Column$ColumnBuilder and Table$1 both lacked LINE and BRANCH counters]`
**How to avoid:** Guard every per-class counter lookup with a `None` check and render `—` or `n/a`, not a crash or a silently-wrong `0%`.
**Warning signs:** The summary-generation step fails only on certain repos/branches/commits — specifically ones where a fork-changed class happens to have an untested inner class.

### Pitfall: a 0% fork-changed-class row can mean "colima wasn't running," not "untested"

**What goes wrong:** Locally generating `jacocoForkReport.xml` while ClickHouse-backed (`@Tag("clickhouse")`) tests are skipped or failing (e.g., colima down, or running `testUnit` instead of `test`) produces a report where sink-level classes show 0% coverage — indistinguishable at a glance from "genuinely untested."
**Why it happens:** `[VERIFIED live this session]` — a local `build/reports/jacoco/fork/jacocoForkReport.xml` present in this repo right now shows several of the 15 fork-changed classes (`ClickHouseSinkConnector`, `HeadersToValue`, `ConvertToMapOrJson`, `HeaderToValue`, `TableOperationFilter`) at `LINE=0.0%`, while others (`ClickHouseSinkConfig` at 87.9%, `ProxySinkTask` at 85.2%) show substantial coverage — consistent with a report generated from a run where ClickHouse-tagged tests didn't execute for at least some classes.
**How to avoid:** Before generating the final local report for criterion 4's "the final report shows coverage for the fork-changed classes," confirm Docker/colima is actually reachable (`docker info` succeeds) and the full suite (`./gradlew test`, not `testUnit`) ran to completion, not just a partial/cached run.
**Warning signs:** A fork-changed-class table with several exact `0.0%` rows next to others with normal-looking percentages, right after a session where Docker status was uncertain.

### Pitfall: local shell's `TESTCONTAINERS_HOST_OVERRIDE=null` must never reach the workflow file

**What goes wrong:** Copying the local test-invocation prefix (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test`, from `.planning/config.json`'s `workflow.test_command`) into the CI `run:` step.
**Why it happens:** `[VERIFIED live this session]` — this specific shell genuinely has `TESTCONTAINERS_HOST_OVERRIDE=null` (the literal string) set in its environment, confirmed via `env | grep -i testcontainers`. A GitHub Actions runner starts with no such variable, so `env -u TESTCONTAINERS_HOST_OVERRIDE` there is a harmless no-op — but STATE.md is explicit that it must not be carried in as if it were a real requirement, since a future reader could misinterpret its presence as load-bearing.
**How to avoid:** The CI `run:` step is exactly `./gradlew test` (D-15), nothing prefixed.
**Warning signs:** `env -u` or any reference to `TESTCONTAINERS_HOST_OVERRIDE` appearing anywhere in `tests.yaml`.

### Pitfall: unauthenticated GitHub API rate limit is 60 requests/hour — a naive poll loop can exceed it

**What goes wrong:** Polling `GET .../actions/runs?head_sha=...` every few seconds while waiting for a run that could take up to the 45-minute timeout (D-14) can burn through the 60-req/hr budget before the run even finishes, especially combined with the jobs/artifacts calls needed afterward.
**Why it happens:** `[VERIFIED live this session]` — every unauthenticated response captured this session carried `x-ratelimit-limit: 60`.
**How to avoid:** Poll on the order of once every 1–2 minutes, not every few seconds; budget roughly 20–30 polls for a run, leaving headroom for the follow-up jobs/artifacts calls. This resolves the "poll cadence" open item under Claude's Discretion with a concrete, verified number.
**Warning signs:** A `403`/`429` response with `x-ratelimit-remaining: 0` mid-verification.

### Pitfall: `$GITHUB_STEP_SUMMARY` has a hard 1 MiB per-step limit

**What goes wrong:** A summary step that appends more than 1,048,576 bytes fails to upload (with an error annotation, not a job failure) — silently below the limit it can also be dropped without warning in some runner versions.
**Why it happens:** `[CITED: github.com/actions/runner issue #4337, and multiple corroborating issues surfaced via search]` this is a documented, hard platform limit.
**How to avoid:** D-07's scope (2 headline rows + 15 fork-class rows) is nowhere near this limit — a few KB at most — so this is a non-issue for this phase's exact design, but worth keeping the summary to the specified table shape rather than dumping full per-method detail.
**Warning signs:** Not applicable at this phase's designed scope; flagged for completeness only.

## Code Examples

### Complete rewritten `tests.yaml` skeleton

```yaml
# Source: synthesized from D-01..D-16, all action tags verified live this session (2026-09-18)
# FORK MODIFICATION (D-04): matrix removed — this fork only tests ClickHouse `latest` x client V2
# (the V1 / 23.7 / 24.3 / Cloud legs upstream ran are out of scope; see .planning/REQUIREMENTS.md CI-01..04)
name: ClickHouse Kafka Connect Tests CI

on: [push]

concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 45
    name: ClickHouse latest Client version V2 tests
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v6
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Run tests
        env:
          CLICKHOUSE_VERSION: latest
          CLIENT_VERSION: V2
        run: ./gradlew test

      - name: Generate coverage summary
        if: always()
        run: python3 .github/scripts/jacoco_summary.py | tee -a "$GITHUB_STEP_SUMMARY"

      - name: Upload JaCoCo coverage reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-coverage-reports
          path: build/reports/jacoco/
          retention-days: 14

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: gradle-test-reports
          path: build/reports/tests/
          retention-days: 14
```
Note: `actions/upload-artifact` version was not itself part of CI-01's named list; if used, verify its current tag the same way the three named actions were verified before locking it into the plan.

### JaCoCo summary script, matching the verified real XML shape

```python
# Source: written and test-run against this repo's real
# build/reports/jacoco/test/jacocoTestReport.xml and
# build/reports/jacoco/fork/jacocoForkReport.xml this session (2026-09-18)
import xml.etree.ElementTree as ET

FORK_CLASSES_IN_ORDER = [
    "com/clickhouse/kafka/connect/transforms/TableOperationFilter",
    "com/clickhouse/kafka/connect/transforms/HeadersToValue",
    "com/clickhouse/kafka/connect/transforms/HeaderToValue",
    "com/clickhouse/kafka/connect/transforms/HeaderToValueConfig",
    "com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson",
    "com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient",
    "com/clickhouse/kafka/connect/sink/db/mapping/Column",
    "com/clickhouse/kafka/connect/sink/db/mapping/Type",
    "com/clickhouse/kafka/connect/sink/db/mapping/Table",
    "com/clickhouse/kafka/connect/sink/db/ClickHouseWriter",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig",
    "com/clickhouse/kafka/connect/sink/ProxySinkTask",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkTask",
    "com/clickhouse/kafka/connect/ClickHouseSinkConnector",
    "com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils",
]

def pct(counter):
    """counter may be None (JaCoCo omits it when a class has zero of that
    metric — verified live against this repo's real fork report, e.g.
    Column$ColumnBuilder and Table$1 have no LINE/BRANCH counters)."""
    if counter is None:
        return "n/a"
    missed, covered = int(counter.get("missed")), int(counter.get("covered"))
    total = missed + covered
    return f"{100 * covered / total:.1f}%" if total else "0.0%"

def report_totals(xml_path):
    root = ET.parse(xml_path).getroot()
    # Report-level totals are the counters that are DIRECT children of
    # <report>, appearing after every <package> — verified live.
    return {c.get("type"): c for c in root.findall("counter")}

def class_counters(xml_path):
    root = ET.parse(xml_path).getroot()
    result = {}
    for cls in root.iter("class"):
        # Class-level totals are the counters that are DIRECT children of
        # <class>, appearing after every <method> — verified live.
        result[cls.get("name")] = {c.get("type"): c for c in cls.findall("counter")}
    return result

full = report_totals("build/reports/jacoco/test/jacocoTestReport.xml")
fork = report_totals("build/reports/jacoco/fork/jacocoForkReport.xml")
fork_classes = class_counters("build/reports/jacoco/fork/jacocoForkReport.xml")

lines = ["## Coverage Summary", "", "| Scope | Instruction | Line | Branch |",
         "|---|---|---|---|"]
lines.append(f"| Full suite | {pct(full.get('INSTRUCTION'))} | {pct(full.get('LINE'))} | {pct(full.get('BRANCH'))} |")
lines.append(f"| Fork-changed classes | {pct(fork.get('INSTRUCTION'))} | {pct(fork.get('LINE'))} | {pct(fork.get('BRANCH'))} |")
lines += ["", "### Fork-changed classes", "", "| Class | Line | Branch |", "|---|---|---|"]
for name in FORK_CLASSES_IN_ORDER:
    counters = fork_classes.get(name, {})
    short = name.rsplit("/", 1)[-1]
    lines.append(f"| {short} | {pct(counters.get('LINE'))} | {pct(counters.get('BRANCH'))} |")

print("\n".join(lines))
```
`[VERIFIED: this exact parsing logic run against the real files this session, producing correct headline percentages (e.g. full-suite BRANCH 168/852 = 19.7%) and correctly handling classes with missing counters]`

### GitHub API verification commands for plan 05-02

```bash
# Locate the run for the exact pushed commit (resolves the "how the run is
# located" discretion item — verified: head_sha filtering works)
PUSHED_SHA=$(git rev-parse HEAD)
curl -s "https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=${PUSHED_SHA}"

# Poll conclusion — recommend every 60-120s, not tighter (60 req/hr unauthenticated limit, verified)
curl -s "https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/${RUN_ID}" \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['status'], d['conclusion'])"

# Confirm the coverage artifact was produced (works unauthenticated — verified)
curl -s "https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/${RUN_ID}/artifacts" \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print([a['name'] for a in d['artifacts']])"

# NOTE: the following do NOT work unauthenticated (verified this session) —
# do not build a verification step around them without a token:
#   curl .../actions/jobs/{id}/logs        -> 403 Must have admin rights to Repository
#   curl .../actions/runs/{id}/logs        -> 403 Must have admin rights to Repository
#   curl .../actions/artifacts/{id}/zip    -> 401 Requires authentication
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|---------------|--------|
| `actions/checkout@v3` | `actions/checkout@v4` | v3 deprecated (Node 16 runtime EOL) | Required — v3 uses a deprecated Node runtime |
| `actions/setup-java@v3`, `distribution: 'adopt'` | `actions/setup-java@v6`, `distribution: 'temurin'` | v1-v4 formally marked deprecated `[CITED: actions/setup-java README]`; AdoptOpenJDK project renamed to Eclipse Temurin | `adopt` distribution keyword no longer maps to an actively maintained upstream under that name in current major versions |
| `gradle/gradle-build-action@v2` with `arguments: test` | `gradle/actions/setup-gradle@v6` + separate `run: ./gradlew test` | Gradle merged `gradle-build-action` into the `gradle/actions` monorepo and dropped the `arguments:` convenience input `[CITED: gradle/actions/blob/main/docs/setup-gradle.md]` | The old single-step invoke-with-arguments pattern must become two steps: setup, then a plain `run:` |

**Deprecated/outdated:**
- `gradle/gradle-build-action` (the standalone repo): superseded by `gradle/actions/setup-gradle`.
- `distribution: 'adopt'` in `setup-java`: AdoptOpenJDK is defunct; the successor project is Eclipse Temurin (`distribution: 'temurin'`).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Action tags `v4`/`v6`/`v6` will still resolve to the same (or a compatible newer) commit at the time the plan actually executes, not just at research time (2026-09-18) | Standard Stack | Low — these are the exact tags CI-01 already specifies; if GitHub Actions or Gradle Inc. ever remove a major-version tag entirely (extremely rare), the workflow would fail fast at `actions/checkout@v4` resolution with a clear error, not silently |
| A2 | `GITHUB_STEP_SUMMARY`'s 1 MiB limit and its "silently dropped near the limit" behavior, sourced from third-party GitHub issue threads rather than official docs directly stating the number | Common Pitfalls | Low — this phase's summary content (a couple KB) is nowhere close to the limit regardless |
| A3 | `actions/upload-artifact` (used in the workflow skeleton to satisfy D-05/D-08, but not named in CI-01's action list) is assumed to be `@v4`-family without live-verifying that specific tag this session | Code Examples | Low-medium — the planner should re-verify this tag the same way the three CI-01-named actions were verified, since it's new surface CI-01 didn't pin |

**If this table is empty:** N/A — see rows above. All three CI-01-named action tags, the JaCoCo XML shape, and the REST API's read/download boundary were independently verified live this session, not assumed.

## Open Questions

1. **How should plan 05-02 machine-verify CI-03's "writes a coverage summary to the job summary" sub-claim, given the unauthenticated API cannot fetch job-log or job-summary text?**
   - What we know: run/job `conclusion` and artifact existence/name are fetchable unauthenticated (verified); job logs, run logs, and artifact content downloads all require authentication regardless of repo visibility (verified with a 403/401 against both the target repo and a control repo).
   - What's unclear: whether the project considers "the summary step ran and exited 0, as part of a green job, and the artifact exists" sufficient machine verification of CI-03, or whether actually reading the rendered markdown text requires either a short-lived PAT or a one-time manual web-UI glance scoped to this single sub-claim.
   - Recommendation: surface this explicitly at plan-review or at the D-10 checkpoint rather than silently picking one; it's a genuine gap between what D-11/D-12 assumed and what's true, not a implementation detail.

2. **Should `actions/upload-artifact`'s version be treated with the same "confirm the tag exists" rigor as the three CI-01-named actions?**
   - What we know: CI-01 only names `checkout`, `setup-java`, `setup-gradle`; `upload-artifact` is needed for D-05/D-08 but isn't itself pinned by a requirement.
   - What's unclear: whether the planner should apply the same live-verification step to `upload-artifact`'s tag before writing it into the plan.
   - Recommendation: yes — verify it the same way (`GET .../actions/upload-artifact/git/ref/tags/v4` or whichever major version is current at plan/execution time) before finalizing the workflow file, since it's new content this phase introduces rather than an existing pinned dependency.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker daemon (via colima) | `./gradlew test` locally (CI-04) | ✓ — reachable via colima profile `vz` (not `default`, which is stopped) | Docker Server 27.4.0, Client 29.1.4 | — |
| `python3` (local) | Testing the summary script locally before trusting it in CI | ✓ | 3.14.2 | — |
| `python3` (`ubuntu-latest` runner) | D-06's inline summary step | ✓ | 3.12.3 default; 3.10–3.14 cached | `[CITED: actions/runner-images Ubuntu2404-Readme.md]` |
| `gh` CLI | Not required — D-11 explicitly avoids it | ✗ (confirmed absent locally) | — | `curl` against the public REST API (already the chosen approach) |
| Gradle wrapper | `./gradlew test` | ✓ | 8.14 (per `gradle/wrapper/gradle-wrapper.properties`) | — |
| JDK | Local Gradle daemon | ✓ | OpenJDK 21.0.9 (Homebrew) locally; workflow explicitly installs JDK 17 via `setup-java` for the actual build | Local daemon JDK version is independent of the toolchain-pinned build JDK; not a blocker |
| Public GitHub REST API (unauthenticated) | Plan 05-02 verification | ✓ for run/job status and artifact listing; ✗ for log/artifact-content download | Rate limit: 60 req/hr (verified via `x-ratelimit-limit` header) | See Common Pitfalls "Critical Finding" for the affected sub-claim |

**Missing dependencies with no fallback:** none — the one thing that's unavailable (unauthenticated log/artifact-content download) already has documented options in Open Questions #1, not a hard block.

**Missing dependencies with fallback:** `gh` CLI (fallback: `curl`, already the chosen approach, D-11).

## Security Domain

This phase edits a CI workflow file only — no `src/` changes, no new runtime attack surface in the shipped connector. ASVS categories that apply to application code (authentication, session management, input validation of user data, etc.) are not applicable here.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-------------------|
| V14 Configuration (CI/CD & supply chain hardening, ASVS 5.0 renumbering of legacy V1.14) | Yes — narrowly | Action versions pinned to tags from first-party publishers (`actions`, `gradle` orgs); no third-party marketplace action introduced (D-06 explicitly avoids one) |
| V6 Cryptography / secrets handling | Yes — by removal | The rewritten job removes the cloud-credentials guard step and its two `secrets.*` references entirely (matrix leg deleted per CI-02); no secrets are read or written by the new job at all |
| All others (V2 Auth, V3 Session, V4 Access Control, V5 Input Validation) | No | No application code, no user input, no auth surface touched by this phase |

### Known Threat Patterns for GitHub Actions CI workflows

| Pattern | STRIDE | Standard Mitigation | Status in this phase |
|---------|--------|----------------------|------------------------|
| Mutable action tag repointed to malicious code by a compromised/malicious publisher (tag-pinning supply-chain risk) | Tampering | Pin to an immutable commit SHA instead of a major-version tag | **Not applied** — CI-01 explicitly specifies mutable major-version tags (`@v4`, `@v6`, `@v6`), matching the project's pre-existing convention (`@v3`, `@v2` were the same risk profile). This is a locked decision's tradeoff, not an oversight; noting it here per this section's purpose, not recommending an override |
| Secrets exfiltration via a compromised third-party action | Information Disclosure | Avoid third-party marketplace actions where an official/first-party alternative exists; the new job has zero secrets in scope | Fully mitigated — D-06 avoids `madrapps/jacoco-report`, and CI-02 deletes the only secrets (`INTEGRATIONS_TEAM_TESTS_CLOUD_HOST_SMT`/`_PASSWORD_SMT`) this workflow ever referenced |
| `pull_request_target`/untrusted-fork code execution with repo secrets | Elevation of Privilege | Never use `pull_request_target` for workflows that check out untrusted fork code with secrets in scope | Not applicable — trigger stays bare `on: [push]` (D-13); no `pull_request` trigger exists or is added |
| Public artifact/log exposure of sensitive data | Information Disclosure | Ensure uploaded artifacts (JaCoCo reports, test reports) contain no secrets or credentials | Low risk — coverage/test reports contain source line coverage and test names only; no secrets pass through the new job to be captured in a report |

## Sources

### Primary (HIGH confidence — live-verified this session)

- `api.github.com` (GitHub REST API, unauthenticated) — tag existence for `actions/checkout@v4`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6`; run/job/artifact listing on both `actions/checkout` (control) and `ne1r0n/clickhouse-kafka-connect` (target repo, real historical run `21472583198`); job-logs/run-logs/artifact-download 403/401 falsification of D-12's premise
- `build/reports/jacoco/fork/jacocoForkReport.xml` and `build/reports/jacoco/test/jacocoTestReport.xml` (this repo, local build output) — real XML structure, parsed live with `xml.etree.ElementTree`
- `.github/workflows/tests.yaml`, `.github/workflows/test-head.yaml`, `build.gradle.kts` (lines 230-309) — read in full this session
- Local environment: `colima status`/`colima list`, `docker info`, `docker ps`, `java -version`, `./gradlew --version`, `env | grep -i testcontainers`, `which gh`, `python3 --version`, `git rev-list --count`, `git branch -vv` — all run live this session

### Secondary (MEDIUM confidence — official docs, fetched this session)

- `github.com/gradle/actions/blob/main/docs/setup-gradle.md` — confirms no `arguments:` input, recommends a separate `run:` step, cache-write-only-on-default-branch behavior
- `github.com/actions/setup-java` README — distribution keyword list including `temurin`, `architecture` input behavior and defaults
- `docs.github.com` "Supported runners and hardware resources" — 4 vCPU/16GB standard public-repo Linux runner
- `github.com/actions/runner-images` Ubuntu2404-Readme.md — `python3` preinstalled, version 3.12.3, plus cached alternates

### Tertiary (LOW confidence — community sources, used only for the non-load-bearing $GITHUB_STEP_SUMMARY size-limit note)

- `github.com/actions/runner` issue #4337 and related community issue threads on the exact `$GITHUB_STEP_SUMMARY` byte limit and silent-drop behavior

## Metadata

**Confidence breakdown:**
- Standard stack (action tags): HIGH — all three tags independently confirmed to exist via live, unauthenticated GitHub API calls made this session
- Architecture / JaCoCo XML shape: HIGH — parsed the repo's own real report files live, including a genuine edge case (missing counters) that would have broken a guessed implementation
- GitHub API verification capability (D-11/D-12): HIGH confidence in the *finding* (job logs/run logs/artifact content require auth, tested against both a control repo and the actual target repo) — but this finding contradicts a locked decision's stated premise and is surfaced as an Open Question for the planner rather than silently resolved
- Security domain: MEDIUM — ASVS mapping for a CI-only phase is inherently thin; the supply-chain tradeoff (mutable tags) is correctly identified but is an accepted, locked tradeoff, not a gap

**Research date:** 2026-09-18
**Valid until:** Action tag verification (checkout@v4, setup-java@v6, setup-gradle@v6 resolving to specific commits) is time-sensitive by nature of mutable tags — re-verify at execution time if more than ~7 days elapse before 05-01 is executed. The JaCoCo XML shape and GitHub API auth-boundary findings are structural and not time-sensitive (valid ~90 days).
