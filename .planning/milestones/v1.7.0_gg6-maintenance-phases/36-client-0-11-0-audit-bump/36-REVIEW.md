---
phase: 36-client-0-11-0-audit-bump
reviewed: 2026-10-31T00:00:00Z
depth: standard
files_reviewed: 22
files_reviewed_list:
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg1.md
  - gradle/libs.versions.toml
  - src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ClientCatalogPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryClientParityLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryParityCases.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Client011BehaviourPinLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapePinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapes.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientInsertWirePinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientTimeoutPrefixPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
  - src/test/java/com/clickhouse/kafka/connect/sink/util/ServerExceptions.java
  - src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsTest.java
  - src/test/resources/rowbinary-golden-sha256.txt
findings:
  critical: 0
  warning: 2
  info: 6
  total: 8
status: issues_found
---

# Phase 36: Code Review Report

**Reviewed:** 2026-10-31
**Depth:** standard
**Files Reviewed:** 22
**Status:** issues_found

## Summary

Phase 36 bumps the ClickHouse client to 0.11.0, makes two small `createClientV2` fixes in `src/main`, and adds pin tests and a Datagen harness fix. The two `src/main` changes are correct and minimal. The endpoint is now `protocol://host:port`, and the database goes through `setDefaultDatabase`. `ignore_unknown_config_key=true` is set before the operator's `jdbcConnectionProperties` loop, so the operator's value is applied last and overrides it. No existing test was deleted or weakened. The `UtilsTest` and `AdditionalRetriableErrorCodes*` edits only swap the `ServerException` constructor call for a reflective factory. The `NetworkCompression*` edits are Javadoc only.

I found no critical issues. Two warnings concern test reliability: a golden-hash test whose input depends on the compile-time source encoding, and a documented behaviour (operator override of the ignore flag) that no test pins. The info items are diagnostics and test-hygiene points.

Per the scope notes, I did not report F-1, F-2 or F-3 from `36-FOLLOWUPS.md`, or the pre-existing Spotless violations.

## Warnings

### WR-01: Golden RowBinary hashes depend on the compile-time source encoding, and the Javadoc says otherwise

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryParityCases.java:21` and `:104-105`
**Issue:** The class Javadoc says "Non-ASCII text is written as unicode escapes so the bytes do not depend on a source or default encoding". Lines 104 and 105 contain raw non-ASCII literals: `"unicodé 中文 😀"` and `"é"`.

`build.gradle.kts` sets no `options.encoding` for `JavaCompile`, and there is no `gradle.properties`. Gradle therefore compiles with the daemon JVM's default charset. On JDK 17 that charset follows the locale. A CI runner or developer shell with `LANG=C` or `POSIX` would compile these literals to replacement characters.

The `string` and `fixedstring8` rows of `rowbinary-golden-sha256.txt`, in both the `plain` and `defaults` variants, would then differ. The test would fail with a message pointing at a writer or `clickhouse-data` change, which is wrong. A re-bless in that environment would silently change the golden file.

The test JVM's `file.encoding=windows-1252` setting (`build.gradle.kts:225`) only affects runtime, not javac. So it does not cover this.

**Fix:** Use escapes in the sources, as the Javadoc already claims:
```java
l.add(c("string", "String", STR, "", "hello", "unicod\u00e9 \u4e2d\u6587 \ud83d\ude00", "a\u0000b"));
l.add(c("fixedstring8", "FixedString(8)", STR, "abc", "12345678", "\u00e9"));
```
Alternatively, add `tasks.withType<JavaCompile> { options.encoding = "UTF-8" }`. That change would touch `build.gradle.kts` and the fork-anchor inventory, so the escape fix is the smaller one.

### WR-02: The documented "operator's own `ignore_unknown_config_key` still wins" is pinned by no test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java:27`, `:55-61`; `docs/FORK-DELTA.md` (new `createClientV2` row); `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:203-213`
**Issue:** The FORK-DELTA row states that the connector sets `ignore_unknown_config_key=true` "before it applies the `jdbcConnectionProperties` keys one by one, so an operator's own `ignore_unknown_config_key` in that string still wins". This is the only ordering contract in the change, and it is what lets an operator turn strict key checking back on.

`IGNORE_KEY` appears in the tests only as a substring in an `assertFalse(rawUri.contains(...))`. Nothing builds a client with `?ignore_unknown_config_key=false&ssl=true` and asserts the failure. Nothing builds one with `...=true` and a V1-only key and asserts it passes.

The fork-anchors rows added for CLT-03 count lines in `ClickHouseHelperClient.java`. They do not check order. Moving `clientBuilder.setOption(IGNORE_UNKNOWN_CONFIG_KEY, "true")` below the loop would keep every test and anchor green. The connector's default would then overwrite the operator's setting, and the documented behaviour would be false.

**Fix:** Add a case to `ClientEndpointAndOptionsPinUnitTest`:
```java
@Test
@DisplayName("CLT-03: an operator's ignore_unknown_config_key=false in jdbcConnectionProperties makes a V1-only key fail the build")
void operatorIgnoreFlagWins() {
    assertThrows(RuntimeException.class, () -> insertOnce("?ignore_unknown_config_key=false&ssl=true"));
}
```
Check the thrown type against 0.11.0 (`ClientMisconfigurationException` per the FORK-DELTA text) and assert on that type.

## Info

### IN-01: `createClientV2` still logs a "ClickHouse URL" the client no longer receives

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:170-183`
**Issue:** `url` and `tmpJdbcConnectionProperties` are now built only for `LOGGER.info("ClickHouse URL: {}", url)`. The client is given `endpoint` instead. The log line therefore prints a path, query string and `?`-prefixing that the V2 client never sees. That can mislead whoever reads it while diagnosing a 0.11.0 path or option issue.

The line also writes `jdbcConnectionProperties` verbatim to INFO. An operator can put credentials there, for example V1-style `user` and `password` keys, and AGENTS.md says not to log secrets.

**Fix:** Log the real endpoint and drop the properties from the log:
```java
LOGGER.info("ClickHouse endpoint: {}, database: {}", endpoint, database);
```
Delete the now-unused `url` and `tmpJdbcConnectionProperties` locals in `createClientV2`.

### IN-02: `v1OnlyPropertiesAreAccepted` does not check the `ssl=true` it names

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java:55-61`
**Issue:** The display name says `ssl=true and sslmode=strict ... never reach the server`. The assertions check only that the raw URI lacks `sslmode` and `ignore_unknown_config_key`. A client that forwarded `ssl=true` to the server would pass. `otherV1OnlyKeysAreAccepted` only checks that the URI starts with `/?`, which is also true when `load_balancing_policy` or `failover` leak.

**Fix:** Parse the raw query and assert that none of the supplied V1-only keys appear as parameters:
```java
for (String key : List.of("ssl", "sslmode")) {
    assertFalse(query.contains(key + "="), query);
}
```
Do the same for the keys in `OTHER_V1_ONLY_PROPERTIES`.

### IN-03: Stale "changes this constructor (Phase 36)" comment

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java:84`
**Issue:** The comment `// Client 0.11.0 changes this constructor (Phase 36).` was a marker for work not yet done. The line below it now calls `ServerExceptions.create`, which already handles the change. The comment describes a to-do that is finished.

**Fix:** Delete it, or replace it with `// ServerExceptions picks the constructor of the resolved client line.`

### IN-04: `ClientErrorShapes` has an inverted assertion and a two-line assumption in `clientVersion()`

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapes.java:51`, `:61`
**Issue:** `assertFalse(!m.find(), ...)` is a double negative for `assertTrue(m.find(), ...)`. `clientVersion()` is `line() == Line.V0_9_6 ? "0.9.6" : "0.11.0"`. Adding a third `Line` would make it return `"0.11.0"` for the new line. `ClientInsertWirePinUnitTest` uses it to build the expected `User-Agent` prefix. That test would then fail with a misleading message, or pass for the wrong reason.

**Fix:** Use `assertTrue(m.find(), ...)`. Put the version string on the enum, or return `m.group(1)` from a shared parse:
```java
enum Line { V0_9_6("0.9.6"), V0_11_0("0.11.0"); final String version; Line(String v) { version = v; } }
```

### IN-05: `v1StillCannotDecodeZstd` hardcodes `http://` and ignores the base config's SSL setting

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Client011BehaviourPinLiveTest.java:129`
**Issue:** The sibling `build(...)` helper in the same class honours `csc.isSslEnabled()`. This test builds its V1 node URL with a literal `http://`. If `getBaseProps()` ever points at an HTTPS endpoint, as the cloud mode of `ClickHouseBase` does, the test fails on connect. That is a different failure from the "Magic is not correct" it asserts. The project constraint is local `latest` only, so this is latent.

**Fix:**
```java
String scheme = csc.isSslEnabled() ? "https" : "http";
String url = String.format("%s://%s:%d/%s", scheme, csc.getHostname(), csc.getPort(), csc.getDatabase());
```

### IN-06: `ConfluentPlatform.generateData` wait loop uses strict inequality and leaves small hygiene gaps

**File:** `src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java:511-531`
**Issue:** There are four small points in the rewritten loop.
- The loop exits on `finished == totalWorkers`. If a status trace ever repeats the marker text, so that the count exceeds `totalWorkers`, the loop spins for the full 4 minutes and then reports "0 of N finished" style failure. `finished < totalWorkers` is the safer condition, though not provably needed today (the nightly passed 7 of 7).
- `Pattern.compile("Stopping connector: generated the configured")` is recompiled on every poll, and the literal, `2 * 1000` and `120` are magic values. AGENTS.md asks for static string constants.
- `long finished = 0;` is overwritten before it is read.
- The `finally` calls `deleteConnectors`, which can throw `IOException`. That would replace the more useful "Data generation failed" `RuntimeException` that is propagating.

**Fix:**
```java
private static final Pattern DATAGEN_DONE = Pattern.compile("Stopping connector: generated the configured");
...
long finished;
do { ... } while (finished < totalWorkers && loopCount < DATAGEN_MAX_POLLS);
...
} finally {
    try { deleteConnectors(connectorName); } catch (IOException e) { LOGGER.warn("Could not delete {}", connectorName, e); }
}
```

---

_Reviewed: 2026-10-31_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
