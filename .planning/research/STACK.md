# Stack Research

**Domain:** CI hygiene and build hardening for a Java 17 / Gradle 9.6.1 fork of the ClickHouse Kafka Connect sink (milestone `v1.7.0_gg2-hardening`)
**Researched:** 2026-10-10
**Confidence:** HIGH (every version, SHA, label and runner fact below was read from GitHub's API, `git ls-remote`, the actions' own `action.yml`, or the runner-images repo. The Lombok key and both Gradle mechanisms were reproduced locally.)

Scope: only what is NEW for this milestone. The existing stack (client 0.9.6 V2, Connect API 4.1.1, Jackson 2.22.3, lz4-java 1.12.0, Testcontainers 2.0.5, JUnit 5.14.4) is not re-researched. No dependency bumps are recommended. The user pre-approved the Node 24 action majors, SHA pinning, the `ubuntu-24.04` pin and the Lombok warning fix on 2026-10-10.

## Recommended Stack

### CI actions (`.github/workflows/tests.yaml`)

All four runtimes are verified `node24` by reading `runs.using` in each tag's `action.yml`. Pin to the commit SHA, with the tag in a trailing comment so a human or Dependabot can read it.

| Action | Today | Pin to | Commit SHA (verified two ways) | Runtime |
|--------|-------|--------|--------------------------------|---------|
| `actions/checkout` | `@v4` (node20) | `v7.0.1` (2026-07-20) | `3d3c42e5aac5ba805825da76410c181273ba90b1` | node24 |
| `actions/setup-java` | `@v6` | `v6.0.1` (2026-09-09) | `de7274f081f381c8f8158605e0321c36c376e2e6` | node24 |
| `gradle/actions/setup-gradle` | `@v6` | `v6.4.0` (2026-09-28) | `3f5f9adaf7d9fecd50b5935e54106014257a94e6` | node24 |
| `actions/upload-artifact` | `@v4` (node20) | `v7.0.2` (2026-10-07) | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` | node24 |

Target snippets:

```yaml
- uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
  with:
    persist-credentials: false
- uses: actions/setup-java@de7274f081f381c8f8158605e0321c36c376e2e6 # v6.0.1
- uses: gradle/actions/setup-gradle@3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0
- uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2
```

Why these exact choices:

- **`gradle/actions` is an annotated tag.** `git ls-remote` shows tag object `b9bee63e...` for `v6.4.0`, but the commit it dereferences to (`^{}`) is `3f5f9ada...`. Pin the commit. Pinning the tag-object SHA is the classic mistake, and it fails at "Set up job" with a not-found error.
- **No behaviour drift from SHA pinning on setup-java or setup-gradle.** The floating `v6` tags already resolve to `de7274f0...` (setup-java) and `3f5f9ada...` (setup-gradle). Pinning freezes what CI runs today. Only checkout and upload-artifact actually change code.
- **checkout `v7.0.1`, not `v6.1.0`.** `v6.1.0` and `v5.1.0` are backports of v7's one breaking change, published later the same day (2026-07-20), so they are not "newer". `v7.0.1` is the current line.
- **upload-artifact `v7.0.2`, not `v6.x`.** v6 added Node 24; v7 added ESM and an opt-in `archive: false` input. `v7.0.2` (Oct 7) only improves HTTP 429 retry handling. v7 is the current major.
- **Add `persist-credentials: false` to checkout.** The workflow never pushes. This is the least-privilege companion to the `permissions:` block. Nothing in the build calls `git` (`build.gradle.kts` reads the `VERSION` file only), and `fetch-depth: 1` is unchanged from v4.

### Breaking changes v4 to new majors (all benign for this workflow)

| Action | Change | Effect here |
|--------|--------|-------------|
| `checkout` v5 | Node 24 runtime | Needs runner >= 2.327.1. Hosted runners are always newer. |
| `checkout` v6 | Credentials persisted to a separate file under `$RUNNER_TEMP`, not `.git/config` | None. Nothing here reads git credentials. `persist-credentials: false` removes them anyway. |
| `checkout` v7 / v6.1 | Blocks checking out a fork PR under `pull_request_target` and `workflow_run`. Opt-in input `allow-unsafe-pr-checkout`. | None. The trigger is `push` only. |
| `checkout` v7 | ESM rewrite | Internal only. |
| `upload-artifact` v5 | "Supports Node 24", treated as breaking, still ran on node20 | None. |
| `upload-artifact` v6 | Runs on node24, min runner 2.327.1 | None on hosted runners. |
| `upload-artifact` v7 | ESM; new `archive` input (default `true`) | None. Our `path:` globs are directories, which still zip. |
| `upload-artifact` (v4 semantics, unchanged) | Hidden files excluded (`include-hidden-files: false`); artifact names immutable per run (`overwrite: false`) | `build/reports/jacoco/` and `build/reports/tests/` contain no dot-files or dot-dirs. The two artifact names are distinct. No change needed. |

One pre-existing wrinkle, not caused by this milestone: because names are immutable per run, "Re-run all jobs" can hit a 409 on upload. `overwrite: true` would fix it, but it changes behaviour, so leave it unless the user asks.

setup-gradle v6.4.0 adds annotations for the status of the Gradle version in use. Gradle `9.6.1` against a current `9.8.0` is two minors behind, and the rule flags "more than two minors behind", so no notice is expected. Expect one if the wrapper stays put for another minor.

### Runner label

| Technology | Value | Purpose | Why |
|------------|-------|---------|-----|
| `runs-on` | `ubuntu-24.04` | Pin the hosted image | `ubuntu-latest` starts moving to Ubuntu 26.04 on 2026-10-19, completing by 2026-11-19 (runner-images #14748). |

Facts about `ubuntu-24.04` (image `20261004.327.1`, read from the runner-images readme):

- Docker client 28.0.4, Compose 2.38.2. Testcontainers 2.0.5 has what it needs, and the daemon is running on hosted runners.
- Java 17.0.20 (Temurin build) is preinstalled as the default. `setup-java` with `temurin` 17 stays authoritative, and the Gradle toolchain is pinned to 17.
- Node 22 is the image default, but this does not matter because every action above bundles its own `node24`.
- `nproc` is 4 on the standard public runner (observed in run `38037397969`).

**Support horizon: about two more years (HIGH).** GitHub's stated policy is "we support (at maximum) 2 GA images and 1 beta image". 26.04 reached GA on 2026-09-17, which began the deprecation of the oldest image, 22.04 (deprecated 2026-09-17, retired 2027-04-17). So 24.04 is retired only after the next Ubuntu GA, 28.04, around September 2028. No official 24.04 end date exists yet, so that date is inferred from the policy.

**Why not jump to `ubuntu-26.04` now.** It is GA, but its default Java is 25 and its Docker client is 29.4.2. Docker 29.0 to 29.2 raised the daemon's minimum API to 1.44 and broke older docker-java. Testcontainers >= 2.0.2 is reported fixed and we are on 2.0.5, but that is untested on this suite. Treat 26.04 as its own later change: run one branch build on `ubuntu-26.04`, then decide. The user approved the 24.04 pin, not 26.04.

### Least-privilege `permissions:`

```yaml
permissions:
  contents: read
```

Place it at workflow level. There is one job, so no per-job override is needed. This answers T-14-16.

- **`contents: read` is sufficient.** GitHub documents that specifying any permission sets every unspecified scope to `none`. The job needs exactly one scope: `contents: read` for checkout.
- **Artifact upload, the Actions cache and setup-gradle caching do not use `GITHUB_TOKEN`.** They use the runtime/cache service token, so `actions:` and `packages:` are not needed.
- **`$GITHUB_STEP_SUMMARY` and `::notice::` are workflow commands.** They are parsed from stdout by the runner and need no token scope. `checks: write` is not needed for them.
- **Do not add the setup-gradle scopes.** The doc lists `contents: write` for dependency-graph submission (default `disabled`) and `pull-requests: write` for the PR comment (default `never`). Neither is enabled here.
- **Keep the existing `push` trigger.** Fork-PR token downgrade concerns do not apply.
- **Confidence on the permission claims is MEDIUM-HIGH.** They come from docs, not from a run with the block present. The first CI run after the change is the proof. Verify that it is green and that "Set up Gradle", both uploads and the summary step succeed.

### Lombok "Ambiguous: Jackson2 and Jackson3 exist"

| Item | Finding |
|------|---------|
| Emitted since | Lombok `1.18.44` (2026-03-11): "`@Jacksonized` now supports both Jackson2 and Jackson3; you'll get a warning until you configure which one". `1.18.46` (our catalog version) only adds "finishing touches". |
| Trigger in this repo | One class: `sink/db/helper/ClickHouseFieldDescriptor` (`@Data @Builder @Jacksonized @JsonNaming`, imports `com.fasterxml.jackson.*` only, so Jackson 2). |
| Key | `lombok.jacksonized.jacksonVersion` (list of enum `2`, `3`; unset default is "2 + warning"), confirmed with `java -jar lombok-1.18.46.jar config -g --verbose`. |
| Fix | Append `lombok.jacksonized.jacksonVersion += 2` to the root `lombok.config`. |

Current root `lombok.config` is two lines (`config.stopBubbling = true`, `lombok.addLombokGeneratedAnnotation = true`). Add the third line:

```properties
lombok.jacksonized.jacksonVersion += 2
```

**Verified in a scratch directory** (outside the repo) with `javac` plus the cached Lombok 1.18.46 and Jackson 2.22.3 jars. A class shaped like `ClickHouseFieldDescriptor` printed `warning: Ambiguous: Jackson2 and Jackson3 exist; define which variant(s) you want in 'lombok.config'` without the line, and compiled silently with it.

Use `+= 2`, not `+= 3`. The code is on `com.fasterxml.jackson`, and `3` would generate `tools.jackson` annotations that nothing here reads. Use `+=` rather than `=`, because the key is a list. It is a zero-byte-code change: the generated Jackson 2 builder annotations are the same as the unset default, so no behaviour test is needed beyond the existing `describeTable` tests and a compile with no warning.

### Gradle: drop `org.lz4:lz4-java:1.8.0` from `testRuntimeClasspath` (backlog 999.2)

**What the resolved graph shows** (from `dependencyInsight`, run offline):

- The only thing still carrying `org.lz4:lz4-java:1.8.0` is `testRuntimeClasspath`. The root entry is `org.apache.kafka:kafka-clients:7.9.1-ccs`, reached from `connect-api` and a dozen Confluent 7.9.1 modules (`kafka-schema-registry`, `kafka_2.13`, `kafka-server`, ...).
- On `runtimeClasspath` and `integrationTestRuntimeClasspath` the same dependency resolves as `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.12.0`. The selection reason there is "By conflict resolution: On capability org.lz4:lz4-java client-v2 uses the maintained at.yawk.lz4 fork". The capability rule in `build.gradle.kts` (`configurations.all { resolutionStrategy.capabilitiesResolution.withCapability("org.lz4:lz4-java") ... }`) is the one that does this.
- On `testRuntimeClasspath` that capability conflict is **not detected**. The `at.yawk` 1.12.0 selection shows only a version-conflict reason, and 1.8.0 survives as a second jar. Both jars ship the `net.jpountz.lz4` package, so classpath order decides which one wins at runtime. The root cause is not confirmed (the only structural difference from the integration-test graph is the full `kafka-schema-registry` server stack that `test` pulls in, so it is a Gradle capability-detection quirk). The fix does not depend on the cause.

**Recommended fix: a scoped exclude.**

```kotlin
// 999.2: the capability rule above is not applied on testRuntimeClasspath (Confluent
// 7.9.1 test stack), so org.lz4:lz4-java:1.8.0 rides along beside at.yawk.lz4. at.yawk is a
// drop-in (same net.jpountz.lz4 package) and is already declared, so drop the original.
configurations.named("testRuntimeClasspath") {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

Place it next to the existing capability-rule block.

**Tried both candidate mechanisms through a throwaway init script** (`./gradlew -I`, no repo edit), checking `dependencies --configuration testRuntimeClasspath`:

| Mechanism | Result on `testRuntimeClasspath` | Verdict |
|-----------|----------------------------------|---------|
| `exclude(group = "org.lz4", module = "lz4-java")` scoped to `testRuntimeClasspath` | The only lz4 left is `at.yawk.lz4:lz4-java:1.12.0`. `org.lz4` is gone. | **Recommended.** Matches the goal ("drops the duplicate"), is the smallest edit, and cannot touch `at.yawk.lz4` because the group differs. |
| `dependencySubstitution { substitute(module("org.lz4:lz4-java")).using(module("at.yawk.lz4:lz4-java:1.12.0")) }` | Works: shows `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.12.0`. | Valid fallback. It mirrors how `runtimeClasspath` reads, but hard-codes a version that would drift from the catalog unless built from `libs.versions.lz4Java`. |
| Widening the existing `withCapability` rule | Not tried, because that rule already works elsewhere and is upstream's. | Do not touch. It would enlarge the fork delta. |

Scoping notes: `test` and `testUnit` both use `sourceSets["test"].runtimeClasspath`, so one exclude covers both. The `testCompileOnly` listing in `dependencies` also shows 1.8.0, but that is the compile-only bucket and never reaches a runtime classpath. Do not widen to `configurations.all`: it is unnecessary and would hide the (working) capability resolution on the shipped classpaths. Nothing here is shipped, since `confluentJar` is built from `clickhouseDependencies`, not from `test`.

**Acceptance check** (Docker-free): `./gradlew -q dependencyInsight --configuration testRuntimeClasspath --dependency org.lz4` reports no match, and `--dependency lz4-java` shows only `at.yawk.lz4`. Then re-run `Lz4RoundTripLiveTest` and `testUnit`.

### Gradle: expose the effective `maxParallelForks` (14 WR-02)

Add a tiny task next to the `tasks.withType<Test>` block in `build.gradle.kts`:

```kotlin
tasks.register("printTestForks") {
    group = "verification"
    description = "Prints the effective maxParallelForks of the test task (CI reads this)"
    val forks = tasks.named<Test>("test").map { it.maxParallelForks }
    doLast { println(forks.get()) }
}
```

Prototyped through an init script and run on this machine (12 CPUs): `./gradlew -q printTestForks` printed exactly `6`. With `CLICKHOUSE_CLUSTER_NAME=x` set it printed `1`, so the cluster branch is honoured. It also works with `--configuration-cache`. Under `-q` it prints one line and nothing else.

Why a task and not shell arithmetic: the shell step (`nproc / 2`) is a second copy of a formula that lives in `build.gradle.kts`. That copy drifts if the formula changes. It also ignores the `CLICKHOUSE_CLUSTER_NAME` branch unless the shell repeats it. The value is read lazily via `tasks.named<Test>("test").map { ... }`, so it is the value the `withType<Test>` block actually assigned. The same expression applies to every `Test` task, because they all run the one formula.

CI step replacement:

```yaml
- name: Report test fork count
  run: |
    forks="$(./gradlew -q --console=plain printTestForks | tail -n 1)"
    msg="runner nproc=$(nproc), Gradle maxParallelForks=${forks} (read from build.gradle.kts)"
    echo "$msg"
    echo "### Test fork count" >> "$GITHUB_STEP_SUMMARY"
    echo "$msg" >> "$GITHUB_STEP_SUMMARY"
    echo "::notice title=Test fork count::${msg}"
```

`tail -n 1` guards against stray daemon or toolchain chatter. Keep `nproc` in the message as context only; it is no longer an input to any computation. The step's position (before "Compile integration-test sources") is fine: it makes this the first Gradle call and starts the daemon, which the compile step then reuses. Keep the `::notice` annotation, because job logs are unreadable without admin rights (CI-09).

## Installation

No new runtime or build dependencies. The edits are:

```text
.github/workflows/tests.yaml   runs-on, permissions, 4 SHA pins, persist-credentials, fork-count step
lombok.config                  + lombok.jacksonized.jacksonVersion += 2
build.gradle.kts               + exclude on testRuntimeClasspath, + printTestForks task
.github/dependabot.yml         OPTIONAL, see below (needs a user decision)
```

## Alternatives Considered

| Recommended | Alternative | When to use the alternative |
|-------------|-------------|-----------------------------|
| Commit-SHA pins plus a `# vX.Y.Z` comment | Floating major tags (`@v7`) | Only if Dependabot/Renovate maintenance is not wanted. The user approved SHA pinning, so this is not on the table. |
| `ubuntu-24.04` | `ubuntu-26.04` | After a trial run. Different default Java (25), Docker 29.4.2, Python 3.14. |
| `ubuntu-24.04` | `ubuntu-latest` | Never for a gate whose results must stay comparable over time. |
| Workflow-level `permissions: contents: read` | Job-level block plus `permissions: {}` at the top | Add this when a second job appears. With one job it is the same effective grant. |
| Scoped `exclude` | `dependencySubstitution` | If someone wants the redirect to stay visible in `dependencyInsight`. Build the target version from the catalog, not a literal. |
| `printTestForks` Gradle task | A `doFirst` in the `test` task that prints `::notice::` | Fewer CI lines, but it only fires if the test step runs, so a failure before `test` leaves no annotation. |
| `lombok.jacksonized.jacksonVersion += 2` | Remove `@Jacksonized` from the descriptor | Would change generated code; the config line changes nothing. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Pinning `gradle/actions` to `b9bee63e...` (the tag object) | Not a commit. The job fails to resolve the action. | The dereferenced commit `3f5f9ada...`. |
| `actions/checkout@v6.1.0` / `v5.1.0` as "latest" | Backports of v7's breaking change, not newer lines. | `v7.0.1`. |
| `lombok.jacksonized.jacksonVersion += 3` | Generates `tools.jackson` annotations. The code and runtime use Jackson 2. | `+= 2`. |
| `configurations.all { exclude(group = "org.lz4") }` | Hides the working capability rule on shipped classpaths; touches production resolution. | Exclude on `testRuntimeClasspath` only. |
| `permissions: write-all`, or `contents: write` "just in case" | Violates the purpose of T-14-16. Nothing needs it. | `contents: read`. |
| `overwrite: true` on uploads | Behaviour change, outside the approved list. | Leave the defaults. |
| A shell `nproc / 2` mirror | A second copy of the formula; drifts silently. | `printTestForks`. |

## Stack Patterns by Variant

**If the user later wants Ubuntu 26.04:**
- Change only `runs-on` and run one branch build before merging.
- Check Docker API compatibility with Testcontainers 2.0.5, JDK provisioning (default Java is 25, so keep `setup-java` 17), and the Python 3.14 requirement of `.github/scripts/jacoco_summary.py` (standard library only, so low risk).

**If a second job is added:**
- Set `permissions: {}` at workflow level and grant per job.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| `actions/checkout@v7.0.1`, `upload-artifact@v7.0.2`, `setup-java@v6.0.1`, `setup-gradle@v6.4.0` | Any hosted runner >= 2.327.1 | Hosted runners auto-update. All four are node24. |
| `ubuntu-24.04` (Docker 28.0.4) | Testcontainers 2.0.5, docker-java inside it | The suite already passed on this image (it is today's `ubuntu-latest`). |
| Lombok 1.18.46 | `lombok.jacksonized.jacksonVersion` | Key exists from 1.18.44. |
| Gradle 9.6.1 | `tasks.named<Test>().map`, `exclude` on a named configuration | Both exercised locally on 9.6.1. |
| `exclude(org.lz4)` | `at.yawk.lz4:lz4-java:1.12.0` | Same `net.jpountz.lz4` package, so Confluent/Kafka 7.9.1 test classes keep working. |

## Observations Not Acted On (need a user decision, per AGENTS.md)

- **Newer versions exist.** Gradle `9.8.0` is the current release (we use 9.6.1). Lombok `1.18.48` (2026-09-01) exists (we use 1.18.46). Noted only. Neither is needed for this milestone.
- **`.github/dependabot.yml` has only the `gradle` ecosystem.** After SHA pinning, nothing will propose action updates, so the pins would go stale silently. Adding a `package-ecosystem: "github-actions"` entry is the usual companion to pinning. It is not on the approved list, so ask first.
- **Other workflows are untouched and out of scope.** `integrationTests.yaml`, `release.yaml`, `pre-release.yaml` and `increment-version.yml` still use `@v3` / `gradle-build-action@v2` / `actions/create-release@v1` (node16-era) on `ubuntu-latest`, and `release.yaml` is manual. They are upstream's, not the CI that gates `custom_build`.
- **`test-head.yaml` appears dead.** It calls `./.github/workflows/tests.yaml` as a reusable workflow with `with:` inputs and `secrets: inherit`, but `tests.yaml` declares no `workflow_call` trigger or inputs. It is scheduled nightly. Check whether it fails or is disabled in the fork before the permissions block lands (reusable-workflow permissions can only narrow the caller's).

## Sources

- GitHub REST API, releases and `git/ref/tags` for `actions/checkout`, `actions/upload-artifact`, `actions/setup-java`, `gradle/actions`, read 2026-10-10 — versions and dates (HIGH)
- `git ls-remote` against the four repos, including `^{}` dereference — commit SHAs (HIGH)
- `action.yml` at each pinned tag (`raw.githubusercontent.com`) — `runs.using: node24`, input lists (HIGH)
- Release notes for checkout v6.0.0/v6.1.0/v7.0.0/v7.0.1, upload-artifact v5/v6/v7/v7.0.2, setup-java v6.0.0/v6.0.1, gradle/actions v6.3.0/v6.4.0, and `CHANGELOG.md` of checkout — breaking changes (HIGH)
- https://github.com/actions/runner-images/issues/14748 — `ubuntu-latest` to 26.04, 2026-10-19 to 2026-11-19 (HIGH)
- https://github.com/actions/runner-images/issues/14747 — Ubuntu 26.04 GA, 2026-09-17 (HIGH)
- https://github.com/actions/runner-images/issues/14254 — 22.04 deprecation 2026-09-17, retired 2027-04-17, "latest two stable versions" (HIGH)
- https://github.com/actions/runner-images (README) — label table and "at most 2 GA images" policy (HIGH)
- `Ubuntu2404-Readme.md` and `Ubuntu2604-Readme.md` in runner-images — Java, Docker, Node, Gradle versions per image (HIGH)
- https://github.blog/changelog/2026-09-17-ubuntu-26-generally-available-and-latest-migration — corroboration of GA and migration window (MEDIUM, search summary)
- Docker 29 min-API / Testcontainers >= 2.0.2 reports (web search) — only for the "why not 26.04 yet" caveat (MEDIUM)
- https://github.com/gradle/actions (`docs/setup-gradle.md` at v6.4.0) — token scopes, `dependency-graph` and `add-job-summary-as-pr-comment` defaults (HIGH)
- https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#permissions — unspecified scopes become `none` (HIGH)
- https://projectlombok.org/changelog — 1.18.44 / 1.18.46 / 1.18.48 entries (HIGH)
- `java -jar lombok-1.18.46.jar config -g --verbose` — exact key, type and default (HIGH)
- Scratch-directory `javac` reproduction of the warning and its removal (HIGH, local)
- `./gradlew dependencyInsight` / `dependencies` on `testRuntimeClasspath`, `runtimeClasspath`, `integrationTestRuntimeClasspath`, plus init-script trials of exclude, substitution and `printTestForks` (HIGH, local, Gradle 9.6.1)
- `.planning/PROJECT.md`, `.planning/ROADMAP.md` (backlog 999.2), `.github/workflows/*.y*ml`, `build.gradle.kts`, `gradle/libs.versions.toml`, `lombok.config` — repo state (HIGH)

---
*Stack research for: CI hygiene and build hardening, `v1.7.0_gg2-hardening`*
*Researched: 2026-10-10*
