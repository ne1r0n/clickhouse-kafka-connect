# Phase 29: Milestone close record

This file holds everything the `v1.7.0_gg4-hardening` milestone leaves open once `v1.7.0_gg4` is published. `/gsd-complete-milestone` reads it for `MILESTONES.md` Known gaps and for the Active list in `PROJECT.md`. It was written by plan 29-05 (D-05, D-06).

Only decisions are written here. No record value, host, credential or token appears in it.

## Carried review advisories

The 16 findings below come from `25-REVIEW.md`, `26-REVIEW.md`, `27-REVIEW.md` and `28-REVIEW.md`. Each is carried for the reason D-05 gives: Phase 29 is a release phase, and it edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file to close a finding. Each row of the four `*-REVIEW-DISPOSITION.md` files that names a finding below is `deferred` and points here. The one finding of those reviews that Phase 29 did fix is in the next section.

### 25-REVIEW WR-01: The archive-step guard accepts a `createConfluentArchive` call that does not verify anything

- **Reason:** `TestsWorkflowPinGuardUnitTest` accepts `./gradlew createConfluentArchive -x verifyConfluentArchive`, a `--dry-run` call, a call followed by `|| true` and a step with an `if:`, and does not assert that the archive step runs before the tests. The guard could therefore miss a regression of the archive check in `tests.yaml`. The real workflow has none of those shapes today, and the archive check itself still runs and fails the build on a nested jar. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of the archive step in `tests.yaml`, of `TestsWorkflowPinGuardUnitTest`, or a Gradle or Shadow bump that touches the archive task.
- **Source:** 25-REVIEW.md

### 25-REVIEW WR-02: `ConnectApiReferencesUnitTest` silently drops inherited-member references it cannot resolve, and resolves them on the test classpath

- **Reason:** `ConnectApiReferencesUnitTest` skips a Kafka member that it cannot resolve through a connector-owned class, and resolves members on the test classpath, whose Kafka version can differ from the `4.4.0` compile pin. A connector call to an inherited Kafka method that the resolved Kafka lacks would not reach the reference-list comparison. The current reference list was checked statically against the production worker's jars and against `4.1.1` and `4.4.0` when the dependencies were absorbed, and the rollout note states only that. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `ConnectApiReferencesUnitTest` or `connect-api-references.txt`, a change of the Connect API pin, or a bump of the Confluent test stack.
- **Source:** 25-REVIEW.md

### 25-REVIEW WR-03: The reference derivation only reads Class/Field/Method constants, so a Kafka type used only in a descriptor, signature or annotation is never checked

- **Reason:** The reference derivation reads Class, Field and Method constants only, so a Kafka type that appears solely in a descriptor, a signature or an annotation is not checked. Every current Kafka type also appears as a Class constant, so the gap is not reachable today; the wording "every referenced class and member" is stronger than what the test guarantees. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `ConnectApiReferencesUnitTest`, or a connector change that uses a Kafka type only in a signature or an annotation.
- **Source:** 25-REVIEW.md

### 25-REVIEW IN-01: A stale version directory under `build/confluentArchive` turns into an unexplained "found 2" failure

- **Reason:** After a `VERSION` bump without `clean`, the old version directory under `build/confluentArchive` is zipped as well, and the archive check fails with `found 2` without naming the jars. The failure is correct and the effect is an unclear message. The Phase 29 archive builds all ran `clean`. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next `VERSION` bump built without `clean`, or the next edit of `prepareConfluentArchive` or `ConfluentArchiveCheck`.
- **Source:** 25-REVIEW.md

### 25-REVIEW IN-02: `ConfluentArchiveCheck` coverage edges

- **Reason:** `ConfluentArchiveCheck` does not flag a Kafka class under `META-INF/versions/<n>/`, a nested `.zip` or `.war`, and keeps the first 20 violations in discovery order. None of these occurs in the archive built at the release tip (the compare reports 0 nested jars and 0 Kafka classes). Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `ConfluentArchiveCheck`, or a Shadow bump that changes how multi-release entries are written.
- **Source:** 25-REVIEW.md

### 25-REVIEW IN-03: Brittle exact-text assertions in the new guards

- **Reason:** The pin guard requires the literal line `mainClass.set("...")` and `ConnectApiReferencesUnitTest` names five fixed anchor members. Both fail with a misleading message after a legitimate refactor and never miss a regression. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** a Kotlin DSL rewrite of the archive task (DEPS-F3), or a connector change that stops using one of the five anchor members.
- **Source:** 25-REVIEW.md

### 25-REVIEW IN-04: Build and repository hygiene carried by the merges

- **Reason:** Four hygiene points: (a) `implementation(libs.guava)` widens the compile classpath only to override a transitive version, (b) a typo in a `.gitignore` comment, (c) `gradle-wrapper.properties` has no `distributionSha256Sum`, and (d) `pre-release.yaml` builds with Gradle `7.4.2`. Point (d) has its documentation half in this plan: `docs/FORK-DELTA.md` records the pin and DEPS-F4 covers the upstream workflows. Points (a) to (c) are build-side edits. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next Gradle wrapper bump (c), the next edit of the guava lines (a), or the first edit of `pre-release.yaml` (d).
- **Source:** 25-REVIEW.md

### 26-REVIEW IN-02: Production dead code kept only for tests

- **Reason:** Both `shouldSkipDescribeRow` overloads have no caller in `src/main`; production runs `DescribeTableBuilder`. The review's own status is "stands by decision": `fork-anchors.txt` pins the method at a count and FORK-DELTA and the merge checklists name it as the sync landmark, so removing it means editing the anchor row in the same commit. The disposition changes from `skipped` to `deferred` so that it names this record. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of the `shouldSkipDescribeRow` anchor row or of `ClickHouseHelperClient`'s describe code, or an upstream sync that touches that method.
- **Source:** 26-REVIEW.md

### 27-REVIEW WR-01: Unknown-enum-label failure is not actionable for the operator, and the new column-named messages never reach any output

- **Reason:** An unknown `Enum8` or `Enum16` label is reported as `Failed to write column ... java.lang.IllegalArgumentException` with its message withheld, so an operator reads the class and the column and not the lookup-miss sentence. The rollout note says so in item 3, and the CHANGELOG states the class change. Showing the sentence needs a pass-through exception type, which is a user decision that the earlier phase did not take and which a release phase does not make. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the user's decision on a pass-through exception type, an operator report that confuses this text with a connector defect, or the next edit of `Column.convertEnumValues`.
- **Source:** 27-REVIEW.md

### 27-REVIEW IN-01: `passesThroughUnchanged` short-circuits before the Jackson check, so the exclusion is a property of the call sites and not of the predicate

- **Reason:** `passesThroughUnchanged` returns true for a `ConnectException` or an exact `RuntimeException` before it walks the cause chain, so the Jackson exclusion holds because the innermost write converts a Jackson exception first. No path reaches a wrapper around a Jackson exception today, and no test pins that shape. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of the `Map`, `Array` or `Tuple` write lambdas or of `passesThroughUnchanged` in `ClickHouseWriter`.
- **Source:** 27-REVIEW.md

### 27-REVIEW IN-02: `JsonAsStringKeyResidualLiveTest` name and Javadoc intro no longer describe what it pins

- **Reason:** `JsonAsStringKeyResidualLiveTest` keeps a name and an introduction that describe the old residual, while it now pins that the `Map` key reaches no destination. FORK-DELTA names the class in several rows, and a rename touches those references too. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of that test, or the next change of the FORK-DELTA rows that name it.
- **Source:** 27-REVIEW.md

### 27-REVIEW IN-03: `SourcePlanningIdGuardUnitTest` has a narrow scope and a broad single-letter prefix list

- **Reason:** `SourcePlanningIdGuardUnitTest` scans `src/main/java` only and its pattern has single-letter prefixes, so a token of a future upstream sync (for example `R-1` in a comment) could fail `testUnit` with a message that suggests a planning leak. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** an upstream sync whose comment trips the guard, or the next edit of `SourcePlanningIdGuardUnitTest`.
- **Source:** 27-REVIEW.md

### 28-REVIEW IN-01: `DeclaredNullability.ESCAPED_NEWLINE` is a public production constant that only tests read

- **Reason:** `DeclaredNullability.ESCAPED_NEWLINE` is a public constant that only tests read, and one Javadoc paragraph wraps mid-sentence. No behaviour is affected. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `DeclaredNullability` or of `DeclaredNullabilityFixtures`.
- **Source:** 28-REVIEW.md

### 28-REVIEW IN-02: `ForkCoverageScopeUnitTest` cannot detect a fork class missing from the list, and `SinkTaskStatistics` is one

- **Reason:** `ForkCoverageScopeUnitTest` pins list equality and a named minimum, not completeness, and `SinkTaskStatistics` is a fork class outside the list. The documentation half is written in plan 29-05 Task 2: FORK-DELTA's coverage-report row says why `SinkTaskStatistics` is out of scope and what the guard pins. The list is in `build.gradle.kts` and the Javadoc in a test; both stay as they are. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `jacocoForkReport` or of `ForkCoverageScopeUnitTest`, or a new fork class.
- **Source:** 28-REVIEW.md

### 28-REVIEW IN-04: The server's escape set for enum labels is now measurable, but the pin and the wording still say "not measured"

- **Reason:** The server half is done in plan 29-05 Task 2: the escapes the server writes for a backspace, a form feed, a NUL and a bell (`29-COMMAND-EVIDENCE.md` E-24) are measured, and FORK-DELTA Known costs item 11 states them instead of "expected to write". The test half stays: `RawNameMatrix` has no entry for those four characters and `ColumnTest` pins the escapes on type text only, so a server release that changes the set is not noticed by a test. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `RawNameMatrix` or of the `ColumnTest` enum pins, or a server release that changes the escapes measured in E-24.
- **Source:** 28-REVIEW.md

### 28-REVIEW IN-05: `DescribeUnreadableEnumMockServerTest` never closes the helper clients and does not tie the null to the enum

- **Reason:** `DescribeUnreadableEnumMockServerTest` does not close the helper clients it builds and its null assertion is satisfied by any exception in the describe, and `RawIdentifierNameLiveTest` hard-codes `Enum8` in a message prefix. These are test-hygiene points with no effect on the shipped connector. Phase 29 is a release phase and makes no edit of `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` (D-05).
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of either test, or an `Enum16` entry added to the matrix.
- **Source:** 28-REVIEW.md

## Fulfilled in Phase 29

### 28-REVIEW IN-03: A record still using the escaped name form is stored with the column DEFAULT and no log line

- **Status:** fulfilled
- **Evidence:** the rename notice is the first bullet under `## Important Changes` in the `1.7.0_gg4` section of `CHANGELOG.md` (it names `CREATE TABLE`, the renamed escaped forms, the DEFAULT stored with no log line and the order of the rename), and it is the opening paragraph of `docs/ROLLOUT-v1.7.0_gg4.md`, within its first lines, before the table of contents. This is the finding's documented option of promoting the entry to a notice at the top of the release. The runtime signal that the finding names as the other option is not built (NAME-F1 below).
- **Commit:** `fae7e42a` (plan 29-04 Task 1, the note with the rename notice in its opening) and `aad30ee6` (plan 29-05 Task 1, the CHANGELOG section with the rename notice first).

## Other carried items

### The `insertFormat=json` route's Jackson text is not measured

- **Reason:** The rollout note and the CHANGELOG say that an explicit `insertFormat=json` route can raise a Jackson failure and that whether its text carries a `Map` key into the task status, the dead letter queue or the logs was not measured. Phase 27 measured the JSON-as-string route only.
- **Owner:** the next milestone
- **Re-open trigger:** a report of record text in a task status on that route, or the next edit of the explicit JSON insert path.

### Rows stored by earlier releases are not repaired

- **Reason:** Rows that an earlier release stored short, shifted, or with a column DEFAULT for a root field that used the real name are not repaired by this release, and the connector has no way to find them. The note says so for each change. Repair is an operator task on the operator's own tables.
- **Owner:** the operators of the affected tables
- **Re-open trigger:** an operator report of a row that needs repair.

### Next-milestone requirements

- **DEPS-F1:** ClickHouse client `0.10.0` (Dependabot PR #4); the PR stays open and unignored.
- **DEPS-F2:** protobuf-java 4.x (Dependabot PR #10); revisit when its CI is green.
- **DEPS-F3:** Kotlin DSL deprecations that Gradle `9.8.1` reports with `--warning-mode all`.
- **DEPS-F4:** the upstream workflows still on old actions, which includes the `pre-release.yaml` Gradle `7.4.2` pin recorded in FORK-DELTA.
- **COV-F1:** client V1 coverage of the describe path in CI; today one manual run per change is the only evidence.
- **NAME-F1:** a one-release metadata-only WARN for a Kafka field that matches only the earlier escaped form; also the runtime half of 28-REVIEW IN-03. Reconsider if operators report missed renames.

### Inferred claims of the note

- **Bundled patch bumps change no byte sent to ClickHouse.** The note labels this inferred. Re-open trigger: a live pin of the retry across the dependency change, or an operator report of a changed insert.
- **The order of the rename for records already in a topic** (pause the producers, let the old release consume what was produced under the earlier names, stop cleanly, switch the producers, upgrade, resume the producers; the rollback runs the same order in reverse, with the producers kept on the new names until the connector has drained and stopped). It is measured per record and inferred for a backlog. Re-open trigger: a live pin with a backlog, or an operator report of a record that kept its earlier names.

## Open user items after publication

All three are reminders. None of them blocks anything in this milestone (D-06).

### 1. Pull requests #4 and #10 stay open

- **Steps:** In the GitHub web interface open `ne1r0n/clickhouse-kafka-connect`, choose the **Pull requests** tab, and check that #4 (ClickHouse client `0.10.0`, branch `dependabot/gradle/clickHouseDriver-0.10.0`) and #10 (protobuf-java `4.36.2`, branch `dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2`) are still listed as open. Do not press **Merge pull request**, **Close pull request**, or comment `@dependabot ignore` on either.
- **Why it matters:** Each is a decision for a later milestone (DEPS-F1, DEPS-F2). A merge moves `custom_build` past the published tag, and a close or an ignore stops Dependabot from proposing the update again.
- Outcome: reminder only; nothing waits on it (D-06).

### 2. Check the next Dependabot pull request for the label comment (OPS-04)

- **Steps:** When the next monthly Dependabot run opens a pull request, open it from the **Pull requests** tab of `ne1r0n/clickhouse-kafka-connect` and read the conversation. It should carry the label `dependencies` in the right-hand sidebar and no comment saying that the labels could not be found. The state at the time of writing is in E-08 of `29-COMMAND-EVIDENCE.md`: the label exists, and no pull request had been opened after it was created.
- **Why it matters:** The label was created to remove the "labels could not be found" comment from Dependabot pull requests. Only a pull request opened after the label can show whether it worked, so this is the first check that can.
- Outcome: reminder only; nothing waits on it (D-06).

### 3. The Dependabot freeze ends with the tag

- **Steps:** After `v1.7.0_gg4` is published, a pull request merged with **Merge pull request** in the **Pull requests** tab of `ne1r0n/clickhouse-kafka-connect` is a merge on `custom_build`. Merge Dependabot pull requests when you want them, knowing what follows.
- **Why it matters:** Until the tag, `custom_build` stood still so that the release could be merged without a base change. After it, any merge on `custom_build` moves the base, and the next milestone opens with a DEPS-02-shaped merge of whatever landed.
- Outcome: reminder only; nothing waits on it (D-06).

## Milestone audit

Run `/gsd-audit-milestone` after Phase 29 verification and before `/gsd-complete-milestone`.

## Housekeeping

- Remove `/tmp/gsd29` and `/tmp/gsd29-scripts` and any leftover worktree (`git worktree list` shows the two probe worktrees `wt-gg3` and `wt-tip` until then).
- The local tags `pre-v1.7.0-deps`, `pre-v1.7.0-tail`, `baseline-v1.7.0_gg4` and the milestone tags stay local.
- Post-publication `.planning` commits stay on local `sync/v1.7.0`.
