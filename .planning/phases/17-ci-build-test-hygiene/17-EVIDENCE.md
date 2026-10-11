# Phase 17 Evidence

- Phase: 17 (CI, Build & Test Hygiene)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 17 plans. Nothing here was pushed.
- Entry format: one `### E-NN: <title>` section per entry with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail` or `Verdict: unmeasured (<reason>)`. Long Gradle output stays in `/tmp/gsd17/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | CI-14 | pass |
| E-02 | CI-11 | pass |
| E-03 | CI-11, CI-12, CI-13, CI-15, D-06 | pass |

### E-01: CI-14 tracer, reportTestForks read by the fork-count step

Commands as run (repo root, zsh, macOS; `nproc` supplied by a shim directory first on `PATH` because macOS has none):

1. Extract the `Report test fork count` step's `run` text from `tests.yaml` with Ruby into `/tmp/gsd17/exec/tracer/step.sh`.
2. `env -u TESTCONTAINERS_HOST_OVERRIDE PATH="/tmp/gsd17/exec/bin:$PATH" GITHUB_STEP_SUMMARY=/tmp/gsd17/exec/tracer/summary.md bash -e /tmp/gsd17/exec/tracer/step.sh`
3. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain reportTestForks | tail -n 1`

Step output and Gradle result:

```text
runner nproc=12, Gradle maxParallelForks=6 (read from the test task in build.gradle.kts)
::notice title=Test fork count::runner nproc=12, Gradle maxParallelForks=6 (read from the test task in build.gradle.kts)
gradle '6'
```

Step summary file content:

```text
### Test fork count
runner nproc=12, Gradle maxParallelForks=6 (read from the test task in build.gradle.kts)
```

Exactly one `::notice title=Test fork count::` line; its `maxParallelForks` equals Gradle's `6`. Result: `TRACER-FORK-COUNT-OK`.

Edges:

```text
$ CLICKHOUSE_CLUSTER_NAME=x ./gradlew -q --console=plain reportTestForks | tail -n 1
1
$ (cd fakegw && bash -e step.sh)   # fake gradlew exits 1 with no output; step exit status non-zero
::error::reportTestForks did not print an integer: ''
formula-left [] anchor-lines [1, 2, 3, 1, 1] timeout-anchor 1
```

`anchor-lines` order: `register<Test>("testUnit")` 1, `windows-1252` 2, `jacocoForkReport` 3, `withCapability("org.lz4:lz4-java")` 1, `tasks.register("reportTestForks")` 1. `fork-anchors.txt` and `src/main` are unchanged. Result: `TRACER-EDGES-OK`.

Timestamp: 2026-10-11T20:15Z (approximate; run between plan start 20:14:40Z and the E-01 commit)

Verdict: pass

### E-02: CI-11 action SHA resolution

Command as run, per action: `git ls-remote https://github.com/<repo> refs/tags/<tag> 'refs/tags/<tag>^{}'`, then `runs.using` from `https://raw.githubusercontent.com/<repo>/<sha>/action.yml` (`setup-gradle/action.yml` for gradle/actions), then the newest same-major tag from `git ls-remote --tags`.

```text
== actions/checkout v7.0.1
3d3c42e5aac5ba805825da76410c181273ba90b1	refs/tags/v7.0.1
== actions/upload-artifact v7.0.2
cf430e030ddbb5b0abf93d22962f4752f3646cd9	refs/tags/v7.0.2
== actions/setup-java v6.0.1
de7274f081f381c8f8158605e0321c36c376e2e6	refs/tags/v6.0.1
== gradle/actions v6.4.0
b9bee63ef481cc545e4517ec730544a4d8b1dfa0	refs/tags/v6.4.0
3f5f9adaf7d9fecd50b5935e54106014257a94e6	refs/tags/v6.4.0^{}
```

| Action | Tag | Tag type | Tag-object SHA | Pinned commit | runs.using | Newest same-major tag |
|--------|-----|----------|----------------|---------------|------------|-----------------------|
| actions/checkout | v7.0.1 | lightweight | (same as commit) | 3d3c42e5aac5ba805825da76410c181273ba90b1 | node24 | v7.0.1 |
| actions/upload-artifact | v7.0.2 | lightweight | (same as commit) | cf430e030ddbb5b0abf93d22962f4752f3646cd9 | node24 | v7.0.2 |
| actions/setup-java | v6.0.1 | lightweight | (same as commit) | de7274f081f381c8f8158605e0321c36c376e2e6 | node24 | v6.0.1 |
| gradle/actions (setup-gradle) | v6.4.0 | annotated | b9bee63ef481cc545e4517ec730544a4d8b1dfa0 | 3f5f9adaf7d9fecd50b5935e54106014257a94e6 (peeled `^{}`) | node24 | v6.4.0 (a `v6.4.0-rc.1` pre-release also exists) |

All four resolved commits equal the planning-time values, so no impostor-commit stop was raised. No newer same-major release exists, so nothing is left for Dependabot to bump yet.

Timestamp: 2026-10-11T20:16Z

Verdict: pass

### E-03: D-06 pin guard red then green, workflow and Dependabot YAML parse

Red run (before the edits), `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*TestsWorkflowPinGuardUnitTest'`, `5 tests completed, 5 failed`:

```text
every action in tests.yaml is pinned to a full commit SHA with a version comment FAILED
    AssertionFailedError: Action is not pinned to a 40-hex commit with a '# vX.Y.Z' comment: '- uses: actions/checkout@v4'
checkout does not persist the token in .git/config FAILED
    AssertionFailedError: Checkout step must set 'persist-credentials: false': '- uses: actions/checkout@v4'
dependabot.yml updates the github-actions ecosystem next to gradle FAILED
    AssertionFailedError: Missing '- package-ecosystem: "github-actions"' in .github/dependabot.yml
every job in tests.yaml runs on the pinned ubuntu-24.04 runner FAILED
    AssertionFailedError: runs-on must be the pinned runner, not 'ubuntu-latest' ==> expected: <ubuntu-24.04> but was: <ubuntu-latest>
the workflow token is read-only: one top-level permissions block, contents read FAILED
    AssertionFailedError: Exactly one 'permissions:' line is allowed, at the top level, so every job inherits it ==> expected: <1> but was: <0>
```

Green run (after the edits): test-results XML `tests,failures,errors,skipped ['5', '0', '0', '0']`; spotless IDE hook on the test file printed `IS CLEAN`.

Pin check against live tags and `action.yml`:

```text
actions/checkout v7.0.1 3d3c42e5aac5ba805825da76410c181273ba90b1 lightweight
actions/setup-java v6.0.1 de7274f081f381c8f8158605e0321c36c376e2e6 lightweight
gradle/actions/setup-gradle v6.4.0 3f5f9adaf7d9fecd50b5935e54106014257a94e6 annotated
actions/upload-artifact v7.0.2 cf430e030ddbb5b0abf93d22962f4752f3646cd9 lightweight
actions/upload-artifact v7.0.2 cf430e030ddbb5b0abf93d22962f4752f3646cd9 lightweight
uses 5 pinned 5 mismatch [] not-node24 [] wrong-major [] upload-pins [('cf430e030ddbb5b0abf93d22962f4752f3646cd9', 'v7.0.2')]
```

Ruby parse and byte checks:

```text
YAML-OK
gradle-prefix-identical True header-lines 18 timeout-anchor-lines 1
```

The parse confirms: trigger unchanged, `permissions` is exactly `contents: read` with no job-level block, runner `ubuntu-24.04`, `timeout-minutes: 45`, checkout `with: persist-credentials: false`, and Dependabot ecosystems `["gradle", "github-actions"]` with the D-03/D-04 fields. Results: `PIN-GUARD-GREEN`, `PINS-MATCH-TAGS-NODE24`, `WORKFLOW-AND-DEPENDABOT-OK`.

Timestamp: 2026-10-11T20:17Z

Verdict: pass
