---
schema_version: 1
open_count: 3
waived_count: 0
fixed_count: 0
total_count: 3
last_updated: 2026-09-19T00:16:21.710Z
---

# Broken Windows Ledger

> Cross-phase defect register. With `workflow.windows_enforce` enabled, `/gsd-ship` blocks while `open_count > 0`.
> Waive with `gsd-tools windows waive <id> "<reason>"` (reason required).
> Mark fixed with `gsd-tools windows fixed <id>`.

| id | phase | kind | file | line | description | status | reason | recorded_at | resolved_at |
|----|-------|------|------|------|-------------|--------|--------|-------------|-------------|
| 1 | 07 | unrun-verify | src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java | 27 | compileTestJava fails: needs ProxySinkTask.buildMBeanName, dropped by the v1.6.0 mechanical merge and explicitly Phase 8's JMX re-plumb scope; does not extend ClickHouseBase so it blocks testUnit too | open |  | 2026-09-18T23:34:42.450Z |  |
| 2 | 07 | unrun-verify | src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java | 71 | compileTestJava fails: needs ClickHouseBase.sharedContainerStarts() and ClickHouseTestHelpers.query(...), Phase 10's shared-container harness contract, explicitly excluded from this plan by D-06 | open |  | 2026-09-18T23:34:42.669Z |  |
| 3 | 07 | deviation | build.gradle.kts |  | User-approved test-compile exclusion for MBeanNameUnitTest.java (Phase 8: needs ProxySinkTask.buildMBeanName) and ClickHouseTestHarnessTest.java (Phase 10: needs ClickHouseBase shared-container contract) — each owning phase must remove its own exclude line from sourceSets["test"].java when it lands the missing API. | open |  | 2026-09-19T00:16:21.710Z |  |

````json
[
  {
    "id": 1,
    "kind": "unrun-verify",
    "phase": "07",
    "file": "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java",
    "line": 27,
    "description": "compileTestJava fails: needs ProxySinkTask.buildMBeanName, dropped by the v1.6.0 mechanical merge and explicitly Phase 8's JMX re-plumb scope; does not extend ClickHouseBase so it blocks testUnit too",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-09-18T23:34:42.450Z",
    "resolved_at": null,
    "milestone": "v1.6.0"
  },
  {
    "id": 2,
    "kind": "unrun-verify",
    "phase": "07",
    "file": "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java",
    "line": 71,
    "description": "compileTestJava fails: needs ClickHouseBase.sharedContainerStarts() and ClickHouseTestHelpers.query(...), Phase 10's shared-container harness contract, explicitly excluded from this plan by D-06",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-09-18T23:34:42.669Z",
    "resolved_at": null,
    "milestone": "v1.6.0"
  },
  {
    "id": 3,
    "kind": "deviation",
    "phase": "07",
    "file": "build.gradle.kts",
    "line": null,
    "description": "User-approved test-compile exclusion for MBeanNameUnitTest.java (Phase 8: needs ProxySinkTask.buildMBeanName) and ClickHouseTestHarnessTest.java (Phase 10: needs ClickHouseBase shared-container contract) — each owning phase must remove its own exclude line from sourceSets[\"test\"].java when it lands the missing API.",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-09-19T00:16:21.710Z",
    "resolved_at": null,
    "milestone": "v1.6.0"
  }
]
````
