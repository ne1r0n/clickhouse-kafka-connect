---
phase: "08"
slug: "orchestration-jmx-re-plumbing"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-09-19"
---

# Phase 08 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: `register_authored_at_plan_time: true` — all three of `08-01-PLAN.md`,
`08-02-PLAN.md` and `08-03-PLAN.md` carried a parseable `<threat_model>` block. Verification
therefore confirms the planned mitigations exist; it does not scan for new threats.
`08-03-SUMMARY.md`'s `## Threat Flags` section records "None".

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Kafka Connect worker config → connector process | The `name` prop is operator/deployer-supplied and crosses into the connector as an unvalidated `String` rendered into a JMX ObjectName. Not attacker-reachable data-plane input. | Deployer-chosen identifier |
| Connector process → platform MBeanServer | The rendered ObjectName is published to the JVM platform MBean server, which an operator may expose over JMX/RMI. | Metric names and values |
| Connector process → SLF4J log sink | The rendered name is emitted in lifecycle log lines that may ship to a centralized log store. | Connector identifier — no credentials, no record data |
| Test JVM → ClickHouse container | Testcontainers starts a real ClickHouse instance; the suite writes and reads real data across a local network boundary. | Throwaway test data |
| Test JVM → Docker daemon | Testcontainers controls container lifecycle through the Docker socket, including the Ryuk reaper. | Container lifecycle control |
| Test JVM → platform MBeanServer | Live JMX tests register/unregister real MBeans on the shared platform server. | Test MBean registrations |
| Planning record → Phase 11 publication gate | `.planning/MERGE-CHECKLIST.md` gates publication of the merged fork; Phase 11 asserts zero pending rows. | Publication authorization |
| Recorded Facts → operators | Fact 11 is the only channel communicating the operator-visible ObjectName change. | Operational change notice |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-08-01 | Denial of Service | `SinkTaskStatistics.buildMBeanName` → `MBeanServerUtils.registerMBean` | low | accept | Operator-supplied `name` with `,`/`=`/quote yields `MalformedObjectNameException`, swallowed with a WARN; data path unaffected, only JMX metrics go silent. Deferred since Phase 4 D-05. | closed |
| T-08-02 | Spoofing | Platform MBeanServer namespace | low | mitigate | `connector=%s` segment added to the task-level name (`SinkTaskStatistics.java:176`) and inherited by the per-topic name via `getMBeanName() + ",topic="` (`:189`) — composed, not duplicated. Pinned by `MBeanNameUnitTest` and `MBeanRegistrationLiveTest`. | closed |
| T-08-03 | Information disclosure | `LOGGER.info("Register MBean [{}]", name)` and siblings | low | accept | Connector name reaches logs via existing parameterized SLF4J statements; a deployer-chosen identifier, never a credential or record data. No new log statement added. | closed |
| T-08-04 | Tampering | `ClickHouseSinkTask.start()`'s raw `props.get("name")` | low | accept | Value taken verbatim by explicit decision (D-03); absent-vs-empty is a pinned contract and normalization would break JMX-02. Only sink is a `String.format` — no SQL, shell, or deserializer downstream. | closed |
| T-08-05 | Information disclosure | `src/testFixtures` ClickHouse test helper charset fix | low | accept | UTF-8 decode affects only how test code interprets bytes already returned to it; widens no trust boundary, reaches no production path. | closed |
| T-08-06 | Denial of Service | Testcontainers Ryuk reaper | medium | mitigate | `env -u TESTCONTAINERS_HOST_OVERRIDE` prefixes the Gradle invocations — verified present in 2 `.planning/config.json` commands. | closed |
| T-08-07 | Tampering | Shared platform MBeanServer across test classes | low | accept | `ClickHouseBase`'s `@AfterEach` MBean leak sweep is Phase 10 / HARN-02, not this phase. Already-registered-MBean exceptions are counted in the suite log so leakage is observed, not absorbed. | closed |
| T-08-08 | Information disclosure | `/tmp/gsd-08-02-fullsuite.log` | low | accept | Captures ephemeral Testcontainers connection details only — no production credential; written under `/tmp`, never committed. | closed |
| T-08-09 | Repudiation | Re-baselined pass condition | medium | mitigate | Per-class greenness asserted for all four classes this phase changed, in addition to the aggregate bound; every remaining failure and skip named with its owning phase in `08-02-SUMMARY.md`'s Failure and Skip Inventories. Accepted at UAT tests 7 and 8. | closed |
| T-08-10 | Elevation of privilege | `.planning/MERGE-CHECKLIST.md` row status cells | medium | mitigate | Per-row verification: eight Phase-8-owned rows read `done`; the two Phase-10-owned rows this phase touched remain `pending` and carry a "Phase 8" annotation. Removed-line count bounded. Accepted at UAT test 6. | closed |
| T-08-11 | Repudiation | Recorded Fact 11 | medium | mitigate | Fact 11 carries old→new rendered ObjectName strings as literals for both task and per-topic levels, plus the `version=` source change — verified verbatim in `.planning/MERGE-CHECKLIST.md`. | closed |
| T-08-12 | Information disclosure | Recorded Facts content | low | accept | Facts quote source excerpts and ObjectName strings; the connector name appears only as a `<name>` placeholder, never a deployment's real value. No credential, connection string, or hostname. | closed |
| T-08-SC | Tampering | npm/pip/cargo installs | low | accept | Zero new external dependencies across all three plans (RESEARCH.md § "Standard Stack"); `build.gradle.kts` edited only to delete a test-compile exclude line. No install task exists to gate, so no Package Legitimacy Audit applies. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above `workflow.security_block_on` (high) count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-08-01 | T-08-01 | Malformed operator `name` degrades JMX metrics only, never the data path. Quoting the name would change ObjectNames operators already scrape — needs its own decision (deferred since Phase 4 D-05). | Phase 8 plan author | 2026-09-19 |
| R-08-02 | T-08-03 | Connector name in logs is a deployer identifier, not a secret; no new log statement introduced. | Phase 8 plan author | 2026-09-19 |
| R-08-03 | T-08-04 | Verbatim `props.get("name")` is a pinned contract (D-03); no injection sink downstream. | Phase 8 plan author | 2026-09-19 |
| R-08-04 | T-08-05 | Charset fix is test-side only. | Phase 8 plan author | 2026-09-19 |
| R-08-05 | T-08-07 | MBean leak sweep is Phase 10 / HARN-02's requirement; leakage is observed via suite-log counting meanwhile. | Phase 8 plan author | 2026-09-19 |
| R-08-06 | T-08-08 | Suite log holds throwaway container details under `/tmp`, never committed. | Phase 8 plan author | 2026-09-19 |
| R-08-07 | T-08-12 | Recorded Facts contain placeholders, not real deployment values. | Phase 8 plan author | 2026-09-19 |
| R-08-08 | T-08-SC | No dependency added or package installed in any Phase 8 plan. | Phase 8 plan author | 2026-09-19 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-09-19 | 13 | 13 | 0 | /gsd-secure-phase (verify:post hook from /gsd-verify-work 08) |

**Audit note.** During the same session, `/gsd-verify-work 08` amended the notes cell of the
`ProcessingTest.java` row in `.planning/MERGE-CHECKLIST.md` (UAT test 1 follow-up). This touches
T-08-10's component, but the row's **status cell was not changed** — it read `done` before and
after — so Phase 11's zero-pending publication gate is unaffected. Recorded here rather than
reopening the threat.

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-09-19
