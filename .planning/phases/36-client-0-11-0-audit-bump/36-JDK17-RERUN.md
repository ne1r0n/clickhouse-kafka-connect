# Phase 36: suite 2 re-run on JDK 17

Date: 2026-10-29 (UTC). Branch `sync/v1.7.0`, head 57440183 (gated head outside `.planning`: a8b61f15). No source change.

Why: in E-12 suite 2 crashed twice on the default JVM (exit 255, known intermittent OpenJ9/Mockito fault) and was recorded green only on `-PjavaVersion=21`, while suite 1 ran on JDK 17. This run puts suite 2 on JDK 17 as well.

## Command

```
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd36/exec/suite2-gate-jdk17.log 2>&1
```

Docker: colima profile `vz`. `./gradlew --stop` was run first. No `-PjavaVersion` flag, so the build's default (JDK 17 toolchain) applied.

## Result

- exit: 0 (`BUILD SUCCESSFUL in 3m 9s`)
- JVM: the build default, JDK 17 (no `-PjavaVersion` override)
- tests: 2072, failures: 0, errors: 0, skipped: 18 (files=122)
- skips helper: `OK skips: named=7 state_mismatch_cases=11 unexpected=0`
- ClickHouse server version: 26.10.3.7
- ClickHouse image digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce (same as E-12)
- crash history: none; the first run completed, no exit 255, no core, javacore or Snap files appeared

## Verdict

pass. Both full suites now have a green record on the same JVM line (JDK 17), with the same counts and the same image as E-12. origin `custom_build` unchanged at 3a3a15a9ae064ffe09c696b8bb87bce23419d463.
