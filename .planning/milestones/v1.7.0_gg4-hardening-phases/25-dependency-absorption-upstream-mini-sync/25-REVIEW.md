---
phase: 25-dependency-absorption-upstream-mini-sync
reviewed: 2026-10-21T17:40:26Z
depth: standard
files_reviewed: 17
files_reviewed_list:
  - .github/workflows/tests.yaml
  - .gitignore
  - build.gradle.kts
  - docs/FORK-DELTA.md
  - gradle/libs.versions.toml
  - gradle/wrapper/gradle-wrapper.jar
  - gradle/wrapper/gradle-wrapper.properties
  - gradlew.bat
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/dlq/InMemoryDLQ.java
  - src/test/java/com/clickhouse/kafka/connect/sink/util/ThrowableTexts.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ValueFreeDlqHeadersUnitTest.java
  - src/test/resources/connect-api-references.txt
findings:
  critical: 0
  warning: 3
  info: 4
  total: 7
status: issues_found
---

# Phase 25: Code Review Report

**Reviewed:** 2026-10-21T17:40:26Z
**Depth:** standard
**Files Reviewed:** 17
**Status:** issues_found

## Summary

Reviewed the phase delta (`443c8abc..HEAD`) per file. The Shadow 9 repair in `build.gradle.kts` (`configurations.add`, `addMultiReleaseAttribute=false`, `addShadowJarToAssembleLifecycle=false`), the `verifyConfluentArchive` finalizer, `ConfluentArchiveCheck` and its unit test are logically sound: the zip walk, the violation ordering and the exit codes match the assertions in `ConfluentArchiveCheckUnitTest`. The `ClickHouseSinkTask.java` change is comment-only and its claim matches the code. The blessed list is internally consistent (39 `C` rows plus 137 `M` rows = the 176 references stated in FORK-DELTA). The constant-pool parser in `ConnectApiReferencesUnitTest` handles every tag from the class-file spec, including the two-slot long/double entries.

No blocker was found. The defects are in the two guards the phase added: the workflow pin guard accepts command lines that disable the very check it protects, and the Kafka reference guard has two blind spots that can let an unlisted reference through silently. `gradle-wrapper.jar` was not read (binary, verified in phase evidence E-08). `gradlew.bat` was reviewed at the committed blob: it is the stock Gradle 9.8.1 template with the LF to CRLF difference explained by `.gitattributes` (`*.bat text eol=crlf`), no defect.

## Warnings

### WR-01: The archive-step guard accepts a `createConfluentArchive` call that does not verify anything

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:247-254` (rule `gradlewCall`), `278-294` (`gradlewStepProblem`), `301-314` (`archiveStepProblem`)
**Issue:** `gradlewCall` only requires the task name as a whole word anywhere after `./gradlew `. I ran the exact regex against these lines and all four match, so `archiveStepProblem` returns empty for them:

- `./gradlew createConfluentArchive -x verifyConfluentArchive` (disables the CI-20 finalizer; the whole point of the guard)
- `./gradlew test -x createConfluentArchive` (task is excluded, nothing is built)
- `./gradlew --dry-run createConfluentArchive` (nothing executes)
- `./gradlew createConfluentArchive || true` (failure swallowed; the fixtures reject `true || ./gradlew ...` but not this order)

`archiveStepProblem` also checks only `continue-on-error` inside the step block. A step-level `if: false` (or any other `if:`) passes. The order claim in docs/FORK-DELTA.md ("after it and before the tests") is not asserted anywhere, so moving the step after `Run tests` still passes. The same weakness applies to `forkCountStepProblem` through the shared `gradlewCall`.
**Fix:** After the task name, reject exclusion and no-op switches and shell tail operators, and assert `if:` absence and ordering:
```java
private static final Pattern BYPASS =
        Pattern.compile("\\s(?:-x|--exclude-task|-m|--dry-run)\\b|\\|\\||;\\s*(?:true|exit 0)\\b");
// in gradlewStepProblem: called only if the matching code line does not match BYPASS
// in archiveStepProblem: fail on any line matching "^\\s*if:" in the block, and fail unless
// the step header index is below the index of the "Run tests" step header
```
Add fixtures for the four lines above to `archiveRulesRejectMissingRenamedOrUnfinalizedShapes`.

### WR-02: `ConnectApiReferencesUnitTest` silently drops inherited-member references it cannot resolve, and resolves them on the test classpath

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java:183-187` and `251-278`
**Issue:** A member referenced through a connector-owned class (for example `ClickHouseSinkConfig.getBoolean(...)`, inherited from `AbstractConfig`) is mapped to its Kafka declaring type by reflection on `ClickHouseSinkConnector.class.getClassLoader()`, the test runtime classpath. Two consequences:

1. `declaringKafkaType` returns `Optional.empty()` both when the connector class itself declares the member (correct to skip) and when no type in the chain declares it (a hole). The second case is dropped without any signal, so a connector call to an inherited Kafka 4.4.0 method that the resolved test-classpath Kafka lacks never reaches the blessed-list comparison. That is the exact class of reference this guard exists to catch (a member the production Kafka 3.9 worker lacks).
2. The test runtime classpath is not the compile classpath. `build.gradle.kts` pulls the Confluent 7.9.1 schema-registry and converter stack into `testImplementation`/`testFixturesApi`, and Gradle conflict resolution takes the highest version, so `kafka-clients`/`connect-api` on the test classpath is plausibly the 7.9.1-ccs build, not `4.4.0`. I could not run Gradle to confirm the resolved version; `dependencies --configuration testRuntimeClasspath` will. The derived set then depends on which Kafka the tests happen to see, while the blessed header claims it is derived from the 4.4.0 compile classes.

**Fix:** Make an unresolved member a failure instead of a skip, so the case cannot hide:
```java
// return a small result: DECLARED_BY_KAFKA(type) | DECLARED_BY_CONNECTOR | NOT_FOUND
// and in derivedReferences: if NOT_FOUND -> collect and fail with
// "Cannot resolve <owner>.<name>:<descriptor> on the test classpath"
```
Also assert (or log in the failure message) the resolved `org.apache.kafka.connect.sink.SinkTask` code-source version so a classpath drift is visible.

### WR-03: The reference derivation only reads Class/Field/Method constants, so a Kafka type used only in a descriptor, signature or annotation is never checked

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java:223-239`
**Issue:** `readConstantPool` records Kafka classes only from tag-7 (Class) constants and members only from tags 9/10/11 whose owner is a Kafka class. A Kafka type that appears solely in a connector method or field descriptor, a `NameAndType` descriptor of a non-Kafka owner, a `Signature` attribute, or an annotation is never added. The class Javadoc and FORK-DELTA state the test re-derives "every `org/apache/kafka` class and member the compiled connector references". The verifier can load such a type for an assignability check, so a worker lacking it fails with `NoClassDefFoundError` at load time, which is the failure mode BLD-16 names. Likelihood is low today, because every current type also appears as a Class constant, but the guarantee as worded does not hold.
**Fix:** Also scan every `Utf8` that is the descriptor of a field/method/NameAndType (and `Signature` attributes) for `Lorg/apache/kafka/...;` tokens and add them as `C` entries:
```java
private static final Pattern KAFKA_TYPE_IN_DESCRIPTOR = Pattern.compile("L(org/apache/kafka/[^;<]+)[;<]");
// for each tag-12 NameAndType descriptor utf8 and each class member descriptor:
//   matcher.find() -> entries.add(CLASS_ENTRY + group(1))
```
Then re-bless the list (it will grow only if such types exist). If the project decides the gap is acceptable, narrow the Javadoc and the FORK-DELTA wording instead.

## Info

### IN-01: A stale version directory under `build/confluentArchive` turns into an unexplained "found 2" failure

**File:** `build.gradle.kts:396-415` (`prepareConfluentArchive`, `createConfluentArchive`), `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java:82-84`
**Issue:** `createConfluentArchive` zips the whole `build/confluentArchive` directory, while `prepareConfluentArchive` copies into a per-version subdirectory and never removes siblings. After a `VERSION` bump without `clean` (FORK-DELTA already says the generated version file needs `clean`), the old `clickhouse-kafka-connect-<old>/lib/*.jar` is zipped too. The old build would have shipped both silently; the new finalizer fails correctly, but the message `expected exactly one jar under lib/ in the archive, found 2` does not name the jars.
**Fix:** List the jar entry names in the shape violation (`SHAPE_PREFIX + libJars + ": " + names`) and update the one test assertion that pins the text, or make `prepareConfluentArchive` delete its parent directory before copying.

### IN-02: `ConfluentArchiveCheck` coverage edges

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java:43,61,71-76,114`
**Issue:** (a) `startsWith("org/apache/kafka/")` misses `META-INF/versions/<n>/org/apache/kafka/...`. (b) Only `*.jar` is treated as nested; a nested `.zip`/`.war` passes. (c) `limit(MAX_LISTED)` keeps the first 20 violations in discovery order, so 20 nested-jar lines hide any Kafka-class lines (the count in the header still shows the total). None is a defect for the current Shadow output; they are cheap to close.
**Fix:** Use `name.startsWith(KAFKA_PACKAGE) || name.contains("/" + KAFKA_PACKAGE)`, extend the suffix test to `.jar|.zip|.war`, and group the listing by violation prefix before truncating.

### IN-03: Brittle exact-text assertions in the new guards

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:67-68`, `src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java:63-69,81-84`
**Issue:** `archiveTaskProblem` requires the literal line `mainClass.set("...")`, so the equally valid Kotlin assignment `mainClass = "..."` fails the guard with a misleading "must run ..." message. The five anchors in `everyReferencedKafkaClassAndMemberIsInTheBlessedList` (for example `Headers.size`) fail the test when the connector legitimately stops using that exact member, with a message about the derivation, not the refactor.
**Fix:** Accept both spellings with one regex (`mainClass(?:\\.set\\()?\\s*=?\\s*\\(?"<class>"`), and make the anchors a kind check (at least one `C`, one interface `M`, one field `M`, one inherited `M`) instead of fixed names.

### IN-04: Build and repository hygiene carried by the merges

**File:** `build.gradle.kts:97,117`, `.gitignore:46-49`, `gradle/wrapper/gradle-wrapper.properties`, `.github/workflows/pre-release.yaml` (not in the diff)
**Issue:** (a) `implementation(libs.guava)` puts Guava on the compile classpath although no source file uses `com.google.common`; it exists only to override a transitive version. A resolution constraint expresses that without widening the compile surface. (b) `.gitignore` comment reads "ecliose" (upstream typo). (c) `gradle-wrapper.properties` has no `distributionSha256Sum`, so the 9.8.1 distribution zip is not pinned even though the wrapper jar hash was verified. (d) `pre-release.yaml` still builds with `gradle-version: '7.4.2'`, which cannot configure a build that now requires Shadow 9 and Spotless 8; it is intentionally left as upstream ships it, but this phase's Shadow bump makes the breakage certain, not just likely.
**Fix:** (a) `dependencies { constraints { implementation("com.google.guava:guava:...") } }` or keep the line with a comment naming the CVE. (c) Add `distributionSha256Sum=<published sha256 of gradle-9.8.1-bin.zip>`. (d) Record it in the Future Requirements entry for upstream workflows.

---

_Reviewed: 2026-10-21T17:40:26Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
