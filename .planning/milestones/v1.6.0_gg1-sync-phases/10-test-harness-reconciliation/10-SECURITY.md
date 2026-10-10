---
phase: "10"
slug: "test-harness-reconciliation"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-09-20"
---

# Phase 10 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: authored at plan time — all three of `10-01-PLAN.md`, `10-02-PLAN.md` and
`10-03-PLAN.md` carry a parseable `<threat_model>` block. This audit verifies the declared
mitigations exist; it does not scan for new threats.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| test JVM → platform MBean server | `ClickHouseBase`'s `@AfterEach` sweep queries and unregisters names on the JVM-wide `ManagementFactory.getPlatformMBeanServer()`, a process-global registry shared with everything else in the fork | JMX ObjectNames (non-sensitive) |
| test JVM → shared ClickHouse container | 25 test classes issue DDL/DML against one long-lived server instead of a throwaway per class; server-global state crosses class boundaries for the first time | test fixture rows, per-class `kafka_connect_test_*` databases |
| Docker host → container registry | `PullPolicy.alwaysPull()` on the floating `latest` tag fetches fresh image content on every local run | container image content (unpinned by digest) |
| container filesystem → `build/reports/tests/` | `copyServerLogs` extracts ClickHouse server logs out of the container onto the build host | server logs containing test query text and the literal container password `test_password` |
| executor → developer's Docker daemon | Plan 10-02 stops and restarts colima profile `vz`, taking the developer's container runtime down for the duration of one build | none (availability only) |
| this phase → the milestone's publication gate | `MERGE-CHECKLIST.md`'s zero-pending assertion is what Phase 11 gates publication of `custom_build` on; a row flipped here is a claim Phase 11 will not re-derive | bookkeeping claims |
| this phase → other phases' bookkeeping | Six pending rows and three cross-phase notes belong to Phases 6, 7 and 8 and sit in the same two files plan 10-03 edits | bookkeeping claims |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-10-01 | Tampering | `ClickHouseBase.cleanupLeakedMBeans()` | low | mitigate | Sweep pattern is verbatim `com.clickhouse:type=ClickHouseKafkaConnector,*` at `ClickHouseBase.java:182` — not widened. Pinned by `ClickHouseTestHarnessTest.perTopicMBeanWasSwept()` (`@Order(7)`), whose javadoc explicitly forbids pre-emptive widening. | closed |
| T-10-02 | Denial of Service | shared container lifecycle in `setup()`/`tearDown()` | medium | mitigate | `tearDown()` calls `ClickHouseTestHelpers.dropDatabase` under `usesSharedContainer()` (`ClickHouseBase.java:139-146`); a failed drop is logged at ERROR and swallowed so one class cannot abort a whole fork. Pinned by `classGetsFreshIsolatedDatabase()`, which asserts zero *other* `kafka_connect_test_%` databases survive. Detection: plan 10-02's two consecutive clean full runs. | closed |
| T-10-03 | Elevation of Privilege | `ClickHouseHelperClientTest`'s `CREATE USER` / `GRANT ... ON *.*` under a now-shared server | low | accept | Accepted — see Accepted Risks Log AR-01. | closed |
| T-10-04 | Tampering | `PullPolicy.alwaysPull()` against the floating `latest` tag | medium | accept | Accepted — see Accepted Risks Log AR-02. | closed |
| T-10-05 | Information Disclosure | `copyServerLogs` writing into `build/reports/tests/` | low | accept | Accepted — see Accepted Risks Log AR-03. | closed |
| T-10-06 | Repudiation | the `@AfterEach` sweep silently hiding a genuine leak regression | low | mitigate | Every swept MBean is logged at WARN with its full ObjectName — `ClickHouseBase.java:184`, `LOGGER.warn("Unregistering leaked MBean left by a test: {}", name)` — per D-09a. A silent DEBUG-level unregister was considered and rejected. | closed |
| T-10-07 | Denial of Service | stopping colima profile `vz` for the Docker-free gate | low | mitigate | Plan 10-02 task 1's verify captures the build's exit status into `RC`, runs `colima start -p vz`, asserts `docker info` succeeds again, and only then evaluates `RC`. Confirmed in `10-02-SUMMARY.md`: `docker info` failed before the run and succeeded after. | closed |
| T-10-08 | Tampering | the floating `latest` tag pulled fresh on every run | medium | accept | Accepted — see Accepted Risks Log AR-02 (same root risk as T-10-04, recorded once). | closed |
| T-10-09 | Repudiation | recording a green gate that a later reader cannot reconstruct | medium | mitigate | Evidence asserted from JUnit result XML (`build/test-results/**/TEST-*.xml`) with a freshness marker, not console text; both runs snapshotted separately; run 2 forced with `--rerun` past Gradle's `UP-TO-DATE` cache; executed-class sets compared byte for byte (57/57); skips inventoried by name (18 → 18). `BUILD SUCCESSFUL` alone was never accepted. | closed |
| T-10-10 | Information Disclosure | full-suite logs and `build/reports/tests/server_*.log` written during the runs | low | accept | Accepted — see Accepted Risks Log AR-03 (same root risk as T-10-05, recorded once). | closed |
| T-10-11 | Repudiation | flipping `MERGE-CHECKLIST.md` rows | high | mitigate | Verified post-hoc: zero Phase-10-owned rows remain `pending`, and exactly six pending rows remain overall — all owned by Phases 6 and 8 (`MERGE-CHECKLIST.md:60-65`), the rows plan 10-03 was bounded not to touch. Flips ran only in wave 3, after plan 10-02's gate was green. | closed |
| T-10-12 | Tampering | editing `REQUIREMENTS.md` | medium | mitigate | Verified post-hoc: total requirement count is still 53, exactly six remain open after the seven HARN closures, and `P4-WR-01` still exists under Future Requirements. Requirement text was appended to, never rewritten. | closed |
| T-10-13 | Repudiation | closing four requirements that no code in this phase touched | medium | mitigate | The `REQUIREMENTS.md:104-110` correction blockquote names the re-measured command evidence per requirement and states plainly which requirements closed on evidence rather than on work. Independently confirmed by UAT test 1 (a human stranger-read), recorded `pass` in `10-UAT.md`. | closed |
| T-10-14 | Information Disclosure | command output pasted into planning documents | low | accept | Accepted — see Accepted Risks Log AR-04. | closed |
| T-10-SC | Tampering | npm/pip/cargo installs (supply chain) | high | mitigate | Asserted and verified: zero changes to `gradle/libs.versions.toml` across the phase's commits, and `build.gradle.kts` changed by deletions only (11 lines removed, 0 added). No manifest or lockfile gained a dependency; no install task exists. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on (`high`) count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-10-03 | `CREATE USER` / `GRANT ... ON *.*` runs against a test-only, ephemeral container that never outlives the JVM fork, with timestamped usernames that cannot collide. Out of this phase's stated file scope; recorded as an accepted cost of D-01 per RESEARCH Pitfall 4 rather than silently inherited. | plan 10-01 (D-01) | 2026-09-20 |
| AR-02 | T-10-04, T-10-08 | The ClickHouse image is fetched by tag, not digest, so its content is not pinned and two runs minutes apart could exercise different server builds. Pinning by digest would directly contradict the milestone's locked constraint (ClickHouse `latest` × client V2 only) and Phase 11's criterion 1. D-06's two-server attribution run is the detection path for a server-content change that breaks the suite. | plan 10-01 (D-06), plan 10-02 | 2026-09-20 |
| AR-03 | T-10-05, T-10-10 | ClickHouse server logs and full-suite logs carry test query text and the literal container password `test_password`. Destination is gitignored build output plus `/tmp` on the developer's own machine; the container is ephemeral and no production credential is ever present. Diagnosis of a live-suite failure depends on these logs, which is why D-03 keeps the copy. | plan 10-01 (D-03), plan 10-02 | 2026-09-20 |
| AR-04 | T-10-14 | Command output pasted into planning documents consists of tracked file paths, compile-task results and a version-catalog line. No credential, host or connection string is involved. | plan 10-03 | 2026-09-20 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-09-20 | 15 | 15 | 0 | /gsd-secure-phase (orchestrator, ASVS L1 short-circuit — register authored at plan time, grep-depth verification) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-09-20

---

## Carried Forward (non-blocking)

`WR-01` (code review, `ClickHouseBase.java:139-146`) is the same surface as T-10-02 seen from the
review side: `tearDown()`'s database-drop failure is logged and swallowed, so under the shared
container a transient drop failure can strand a database and make a *different, innocent* later
class fail its isolation assertion with no attributable cause. Accepted as a cost in
`MERGE-CHECKLIST.md` Recorded Fact 16; its only detection mechanism today is a clean full-suite
run. Not a blocking threat — recorded here so the two records do not drift apart.
