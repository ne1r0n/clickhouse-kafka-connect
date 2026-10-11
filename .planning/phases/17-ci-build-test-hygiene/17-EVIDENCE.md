# Phase 17 Evidence

- Phase: 17 (CI, Build & Test Hygiene)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 17 plans. Nothing here was pushed.
- Entry format: one `### E-NN: <title>` section per entry with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail` or `Verdict: unmeasured (<reason>)`. Long Gradle output stays in `/tmp/gsd17/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | CI-14 | pass |

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

Timestamp: 2026-10-11T20:22Z

Verdict: pass
