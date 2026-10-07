---
phase: 11-full-suite-ci-publication
reviewed: 2026-10-07T00:00:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
  - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java
  - .github/workflows/tests.yaml
  - .planning/scripts/verify-merge-invariants.sh
findings:
  critical: 0
  warning: 5
  info: 4
  total: 9
status: issues_found
---

# Phase 11: Code Review Report

**Reviewed:** 2026-10-07
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

I reviewed the Phase 11 diff (`4f8aea0f..sync/v1.6.0`) for the six files. The scope is the D-17 `network_compression_method=lz4` pin in `ClickHouseHelperClient`, its two pin tests, the CI workflow edit, a Javadoc-only fixture edit, and the invariant script hardening.

**Coverage of the D-17 pin.** I grepped `src/main` for every construction site of a ClickHouse client. Every V1 site (`ClickHouseHelperClient` ping/version/query/showTables/describeTable/alterTable, `ClickHouseWriter.getClient()`, and `KeeperStateProvider.getStateRecord`) goes through `chc.getDefaultClientOptions()`. Every V2 request goes through the single `Client` built in `createClientV2()`. No unpinned production path exists today.

**Precedence.** In V2 the client-level `serverSetting` is applied first, so an operator's `clickhouse_setting_*` in `jdbcConnectionProperties` wins, and a per-request `insertSettings.serverSetting` wins over both. In V1 I decompiled client 0.9.6. `ClickHouseRequest` seeds its settings from `ClientConfig.getCustomSettings()`. It then merges any `CUSTOM_SETTINGS` option key by key into that map. So an operator `custom_settings` should add to the default rather than replace it. I could not run this because Docker is unavailable in the review environment, and no test pins it (see WR-03).

**Result.** I found no blocker. The main concerns are test-reliability gaps in the regression pin (WR-03 to WR-05). There is also a startup failure mode for the pin that T-11-38 undersells (WR-02), and a real defect in the published concurrency group (WR-01).

## Warnings

### WR-01: CI concurrency group keyed on `github.sha` with `cancel-in-progress` cancels legitimate runs and cancels nothing useful

**File:** `.github/workflows/tests.yaml:8-12`
**Issue:**
- The workflow triggers `on: [push]`. The group key is `github.workflow-github.sha` (the `pull_request` arm is dead code, because there is no `pull_request` trigger).
- A branch push and a tag push of the same commit land in the same group. With `cancel-in-progress: true`, the later one cancels the earlier one. This is the incident observed during publication: the tag push cancelled the branch run on the same SHA.
- The cancelled run is left as a visible "cancelled" check on that commit. That is poor evidence for the CI-06 requirement.
- Conversely, successive pushes to a branch have different SHAs, so they never cancel each other. The setting therefore delivers none of its intended benefit (superseding stale runs) and only produces the tag/branch collision. It is pure downside as written.

**Fix:** Key on the ref so superseding pushes to one branch cancel each other and a tag never collides with a branch:
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```
Alternatively, drop `cancel-in-progress`, or add `tags-ignore: ['**']` under `on.push` if tag runs are not wanted. Also remove the unreachable `pull_request` arm, or add the trigger.

### WR-02: Pin is unconditional and non-removable, and on V2 it rides on the connect-time ping, so a rejecting profile fails startup with a misleading error

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:115-116, 187, 239-251`
**Issue:**
- T-11-38 frames the residual risk as `readonly=1` being impossible for a writing user, so low and accepted. That understates where the setting is sent.
- In client-v2 0.9.6, `Client.ping(long)` is a real `SELECT 1 FORMAT TabSeparated` query (confirmed in bytecode). It carries the client-level server settings.
- Any server-side rejection of `network_compression_method` therefore makes `pingV2()` return false. Examples are a settings constraint (`CONSTRAINTS ... READONLY`), a profile with `readonly=2` plus a locked setting, or a SQL-compatible proxy that rejects unknown URL settings.
- The connector then reports only "Unable to ping ClickHouse instance." The server error that names the setting is swallowed. The operator has no hint that the connector's own new default is the cause.
- There is no opt-out. An operator can override the value but cannot omit the setting. For V2 the override is `clickhouse_setting_network_compression_method=<value>` in `jdbcConnectionProperties`, which is a different setting from the documented `clickhouseSettings`. For V1 it is `custom_settings`.
- The T-11-38 pre-rollout check list should include "ping and DESCRIBE", not just inserts.

**Fix:**
- Add a connector config (for example `networkCompressionMethod`, default `lz4`, empty to disable) in `ClickHouseSinkConfig`, and apply the pin only when it is non-empty.
- At minimum, log the ping failure cause at WARN, and document in T-11-38 and the README that `jdbcConnectionProperties` is the override channel.

### WR-03: The common V1 operator case (`custom_settings` that does not mention `network_compression_method`) is not pinned by any test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java:109-126`, `NetworkCompressionPinUnitTest.java:57-64`
**Issue:**
- `operatorOverrideReachesServerOnV1` sets `network_compression_method` itself plus `max_threads`. It proves the override wins and that `max_threads` merges. It never asserts that the lz4 default survives an operator `custom_settings` that omits the key.
- The V2 side has this test (`unrelatedOperatorOptionsKeepLz4`). The V1 side does not.
- The correct behavior depends on undocumented client-0.9.6 internals: `CUSTOM_SETTINGS` merged key by key into the request settings. A client bump that changed this to replace-whole-string would silently re-break every V1 deployment that uses `custom_settings`, on 26.9+ servers.
- This is the core contract of the fix ("every client the connector builds sends lz4"), and it is unpinned for a likely configuration.

**Fix:** Add a live test with `custom_settings=max_threads%3D3` only. Assert `getSetting('network_compression_method') = 'lz4'` and `max_threads = 3` in the same query.

### WR-04: The pins do not exercise the production V1 call sites, so a dropped `.options(getDefaultClientOptions())` still passes every test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java:74-94`, `NetworkCompressionPinUnitTest.java:39-47`
**Issue:**
- The unit test asserts only the map returned by `getDefaultClientOptions()`.
- The live V1 tests hand-build their own `ClickHouseClient.builder().options(chc.getDefaultClientOptions())`. They never call `queryV1`, `showTablesV1`, `describeTableV1`, `ClickHouseWriter.getClient()` or `KeeperStateProvider`. They therefore test the test's own client construction, not production code.
- `chc.ping()` on V1 does not prove the fix: V1 ping is a plain HTTP ping (`/ping`) with no compressed response.
- Removing the `.options(...)` call from any one V1 site, or adding a new `ClickHouseClient.builder()` without it, would leave the whole pin suite green. That is exactly the "sync silently drops the setting" regression the tests claim to guard against.

**Fix:**
- Drive production methods in the live test. For example, call `chc.versionV1()`, `chc.describeTableV1(...)`, and a `KeeperStateProvider` or `ClickHouseWriter` V1 insert and read-back against `latest`.
- Alternatively, add a source-level guard test that fails if any `ClickHouseClient.builder()` in `src/main` is not followed by `.options(chc.getDefaultClientOptions())`.

### WR-05: `operatorOverrideReachesServerOnV2` can pass vacuously

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java:96-107`
**Issue:**
- The assertion sits inside `for (GenericRecord record : records)` with no row-count check.
- If the query returns zero rows, the test passes without asserting anything. The sibling `v2PingAndQueryRoundTrip` does assert `rows == 1`; this one does not.
- The V1 sibling (`operatorOverrideReachesServerOnV1`) asserts on `firstRecord()`, which would throw on no row. The two operator-override tests are inconsistent.

**Fix:**
```java
int rows = 0;
for (GenericRecord record : records) {
    rows++;
    assertEquals(OPERATOR_METHOD, record.getString("m"));
}
assertEquals(1, rows);
```

## Info

### IN-01: Stale Javadoc in `ClickHouseVersionDefaultUnitTest` contradicts the workflow after D-09

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseVersionDefaultUnitTest.java:12-13`
**Issue:**
- The class comment still says "CI pins `CLICKHOUSE_VERSION: latest` in its job environment". Commit `51119b26` removed that pin, and the `ClickHouseTestHelpers` Javadoc was updated to say so.
- The file is outside the stated review list but belongs to the same D-09 change, so it is now internally inconsistent.

**Fix:** Reword to "CI sets no version, so CI and local runs both resolve through this constant", matching `ClickHouseTestHelpers`.

### IN-02: Operator `clickhouseSettings` cannot override the pin for non-insert requests, and this is undocumented

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1162, 1382, 1523, 1600`; `ClickHouseHelperClient.java:105-116`
**Issue:**
- `clickhouseSettings` is applied only to insert requests (V2 `insertSettings.serverSetting`, V1 `request.set`).
- An operator who sets `network_compression_method` there overrides it only for inserts. Ping, DESCRIBE, `system.columns` and Keeper state reads stay on lz4.
- This is harmless today (lz4 is the safe value), but it is surprising and not documented next to the constants.

**Fix:** Add a short comment on the constants naming the override channels (`jdbcConnectionProperties` on V2, `custom_settings` on V1, `clickhouseSettings` for inserts only).

### IN-03: CI now resolves the floating `latest` tag purely through code defaults, so the failure class that caused D-17 will recur

**File:** `.github/workflows/tests.yaml:28-32`, `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java:124`
**Issue:**
- Dropping the env pins is a documented decision (D-09), and `ClickHouseVersionDefaultUnitTest` guards the default.
- CI always `alwaysPull`s `latest`. The next upstream default change will turn the whole suite red with no code change, as 26.9 did (654 tests, 281 failures).
- Nothing in the workflow records which server version a run used, other than a test log line.

**Fix:** Log `SELECT version()` into the job summary (the live test already logs it), or add a scheduled canary run so the break is not first seen on a release push.

### IN-04: `verify-merge-invariants.sh` still collapses a `git rev-list` failure into a "zero commits" diagnosis

**File:** `.planning/scripts/verify-merge-invariants.sh:36`
**Issue:**
- `RANGE_COMMITS="$(git rev-list ... 2>/dev/null || true)"` hides why the range is empty. A missing `pre-v1.6.0-sync` tag or a bad `RANGE_END` is reported as "range contains zero commits".
- It fails closed, so it is not a false pass. But it is the same class of error masking the patch removed for `git grep`, and it misleads whoever debugs it.

**Fix:** Capture the exit status and stderr separately and report them, for example:
```bash
RANGE_COMMITS="$(git rev-list "${PRE_TAG}..${RANGE_END}" 2>&1)" || {
  echo "INVARIANT 2 VIOLATION: git rev-list failed: ${RANGE_COMMITS}" >&2; fail=1; RANGE_COMMITS=""; }
```

---

_Reviewed: 2026-10-07_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
