# Phase 14: Dependency Bumps & CI - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-09
**Phase:** 14-dependency-bumps-ci
**Areas discussed:** lz4 proof & fallback, Bump breakage policy, CI-08 trigger shape, Pushing sync/v1.7.0

---

## lz4 proof & fallback

| Option | Description | Selected |
|--------|-------------|----------|
| Test-only V2 client | A new live test with compressClientRequest/compressServerResponse(true) (LZ4), typed insert and read-back on latest, committed | ✓ |
| One-off probe only | The same experiment, recorded as a Recorded Fact and not committed | |
| Connector path | A full ClickHouseSinkTask with compression forced on via config (V1 ?compress=true was a no-observation in Phase 12) | |

| Option | Description | Selected |
|--------|-------------|----------|
| Step down, then stop | Try the newest advisory-free 1.11.x (≥ 1.11.2); if that fails too, blocking checkpoint | ✓ |
| Stop immediately | Any 1.12.0 failure is a blocking checkpoint | |
| Keep 1.10.4 | Record BLD-10 as blocked and continue | |

| Option | Description | Selected |
|--------|-------------|----------|
| Test first, then bump | Test commit green on 1.10.4, then a pure version-edit commit | ✓ |
| Same commit | Test and bump together | |

| Option | Description | Selected |
|--------|-------------|----------|
| Record a lookup | GitHub Advisory/OSV lookup recorded with source and date, plus dependencyInsight | ✓ |
| Trust the target | Only dependencyInsight | |

**Notes:** The scout found that the connector's clients never run lz4-java, because of `compress=false` / `compressServerResponse(false)` since Phase 12. That is why the round-trip needs an explicitly compressing client.

---

## Bump breakage policy

| Option | Description | Selected |
|--------|-------------|----------|
| Stop for you | Blocking checkpoint with cause and options | ✓ |
| Fix forward if test-only | Adapt the test and continue; stop only for main code or fixture values | |
| Hold that bump | Drop the bump, defer, continue | |

| Option | Description | Selected |
|--------|-------------|----------|
| Live suite after bump | Full latest × V2 run for org.json; diff Int256/UInt256/Decimal256 | ✓ |
| testUnit + compile only | Roadmap minimum; live check left to Phase 15 | |

| Option | Description | Selected |
|--------|-------------|----------|
| Full live suite | Jackson commit gated by a full run | ✓ |
| JSON-route subset | testUnit plus named JSON-route live classes | |

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, one full run | Phase-end full run plus testUnit plus invariants script | ✓ |
| No, leave to Phase 15 | Per-commit gates only | |

---

## CI-08 trigger shape

| Option | Description | Selected |
|--------|-------------|----------|
| Branches only | `on: push: branches: ['**']`; keep the SHA group and cancel-in-progress | ✓ |
| Branches only + no cancel | Same trigger, with cancel-in-progress: false | |
| Tags only for publish | Keep both triggers and skip branch refs whose SHA is tagged (racy) | |

| Option | Description | Selected |
|--------|-------------|----------|
| Static + one branch run | Reasoning note plus one observed run on the pushed scratch-branch SHA | ✓ |
| Static only | Diff and note, no CI run | |

**Notes:** Keying the group by ref would let both runs complete, which still violates "exactly one". The residual risk is two branches pushed at the same SHA; it is deferred as an operational rule.

---

## Pushing sync/v1.7.0

| Option | Description | Selected |
|--------|-------------|----------|
| Push, after a checkpoint | Push once all commits have landed and local gates are green, behind blocking approval; amends Phase 12 D-01 | ✓ |
| Don't push | Keep D-01; CI-09 recorded as unmeasured | |
| Push a differently named branch | A throwaway ref, deleted afterwards | |

| Option | Description | Selected |
|--------|-------------|----------|
| Separate steps | An nproc/maxParallelForks print step plus a dedicated compileIntegrationTestJava step; no build.gradle.kts change | ✓ |
| Gradle prints it | A lifecycle log line in build.gradle.kts, and one combined step | |

| Option | Description | Selected |
|--------|-------------|----------|
| Diagnose, stop before fixing | Read logs through the REST API, classify, checkpoint; any fix and re-push approved by the user | ✓ |
| Fix CI-only issues forward | Fix tests.yaml-only causes, re-push and continue | |

---

## Claude's Discretion

- Order of the bump commits, within the D-02 and D-13 constraints
- Whether `jacksonAnnotations = "2.22"` changes
- Whether the dead `pull_request` concurrency expression is cleaned up
- The D-01 test class name, location and shape
- Where the Phase 14 evidence is recorded

## Deferred Ideas

- Guarding against two branches pushed at the same SHA (residual CI-08 risk), recorded as an operational rule for Phase 15
- Client `0.10.0` (still a future requirement)
