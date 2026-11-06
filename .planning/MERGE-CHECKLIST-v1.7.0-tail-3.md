# Merge Resolution Checklist — upstream tail 83808df9 (tagless mini-sync 3)

This document is the checked-in resolution record for merge 4 of the fork's sync lineage on branch `sync/v1.7.0`: the merge commit M4 = `12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10` takes upstream's untagged tail `83808df9a0993cf49b2ceaa4633c782e550c0180` (upstream PRs #839 and #857's merges plus three dependency bumps, 18 commits past `9bed6f7970670a55c66be8efda246889e1f95bd0`, no tag) onto the Phase 44 tip. The merge base is `9bed6f7970670a55c66be8efda246889e1f95bd0` (the previous tail). The rollback tag is the local, never-pushed `pre-v1.7.0_gg8` = `baa404cd5ed95c556db0acff84def1024c3bafe3` (M4's first parent); the name is the reserved name of the gg8 release, reused on purpose because the tag marks the pre-merge tip of this release's line. `UPSTREAM_REF` is the full SHA above, never `upstream/main`. The record lands in the commit after M4, because a new file inside the merge commit would fail the tail gate's provenance invariant. It sits beside `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md` (Recorded Facts 44-45).

Three rules govern it:

1. Mechanical merge, per-hunk resolution: Section 1 is generated from `.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt` (the committed output of `git merge-tree --write-tree --name-only` over M4's two parents) and Section 2 from `git diff --name-status 9bed6f7970670a55c66be8efda246889e1f95bd0 83808df9a0993cf49b2ceaa4633c782e550c0180`. Neither is hand-transcribed.
2. No fork dependency bump rides inside M4 (the three upstream bumps and the connect-json library line are upstream content the user approved, and the three bumps arrive as no-ops), and no `.planning` commit happens between `git merge --no-commit` and M4.
3. Recorded Facts continue the global numbering at 46, because the tail-2 checklist ends at 45, so a citation "Recorded Fact N" is never ambiguous.

## Section 1 — Conflicted

| path | predicted class | actual class | hunks | resolution shape | owning phase | fork behaviour at risk | adjudicating test | status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| .github/workflows/tests.yaml | CONFLICT content | conflict | 1 | the fork (HEAD) side: the fork's file has one `build:` job and no cloud job; upstream's secret rename (`INTEGRATIONS_TEAM_TESTS_CLOUD_HOST_SMT` to `CONNECTORS_TEAM_TESTS_CLOUD_HOST`) lives in a job the fork does not have | 45 | the CI-05 `timeout-minutes: 45` anchor and the fork's ClickHouse `latest` only matrix | the CI-05 anchor row; tail gate invariants 1-3 | done |
| CHANGELOG.md | CONFLICT content | conflict | 2 | the fork (HEAD) side in both hunks (the top of the file and the `Known Issues` block); upstream's `# 1.7.1, Unreleased` block and its `## Bug Fixes` bullets (the `/` column names and the serialization record context) are not carried: U-3 puts that text in Phase 49, in the fork's words. The result is byte-identical to M4's first parent | 45 (U-3: Phase 49 writes the upstream bullets) | the fork's release notes and the gg section stack | tail gate invariants 1 and 2 (no authored line, no marker) | done |
| VERSION | CONFLICT content | conflict | 1 | the fork (HEAD) side `v1.7.0_gg7`; Phase 49 bumps it through `bump_version.py` | 45 (Phase 49 bumps it) | the BLD-07 `VERSION` anchor row | the BLD-07 anchor row; tail gate invariants 1-3 | done |
| gradle/libs.versions.toml | CONFLICT content | conflict | 1 | the fork (HEAD) side (`shadow = "9.6.1"`, upstream `8.3.5`); the `commonsLang3`, `slf4j` and `spotless` lines auto-merge as no-ops (HEAD already holds 3.21.0, 2.0.20 and 8.10.4) and the new `kafka-connect-json` library line auto-merges in; both stay. The user approved these four coordinates landing in M4 (plan-check answer 4) | 45 | the fork's pinned plugin and library coordinates (BLD-08 rows) | the BLD-08 anchor rows; tail gate invariants 1-3 | done |
| src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java | CONFLICT content | conflict | 1 | the fork (HEAD) side: the fork's `fromJsonRow`, which decodes only `name` and keeps the other strings as literal JSON text, wins over upstream's one-line `readValue(json, ...)` | 45 (NAME-03; plan 45-02 edits the one test red by design) | NAME-03: names-only decode in `fromJsonRow`, message never embeds the row | the two NAME-03 anchor rows; `FieldDescriptorEscapeTest` (one method red by design on the bare merge, repaired in plan 45-02 under U-2) | done |
| src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java | CONFLICT content | conflict | 3 | the fork (HEAD) side in all three hunks (imports; the `describeTableV1` body; the `describeTableV2` body): the fork already routes both clients through `readDescribeRows` with `StandardCharsets.UTF_8`, so #839's transport change is already in the fork's shape | 45 (HLP-04 / NAME-03) | HLP-04 and NAME-03: one UTF-8 reader for both clients, `readDescribeRows` | the HLP-04 and NAME-03 anchor rows; `RawNameMatrixLiveTest`; `SlashColumnRoundTripTest` | done |

## Section 2 — Every path upstream changed

| path | upstream change | predicted class | actual class | fork-touched | review | owning phase | status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| .github/workflows/tests.yaml | M | CONFLICT content | conflict | yes | resolved in Section 1 | 45 | done |
| CHANGELOG.md | M | CONFLICT content | conflict | yes | resolved in Section 1 | 45 | done |
| VERSION | M | CONFLICT content | conflict | yes | resolved in Section 1 | 45 | done |
| build.gradle.kts | M | auto-merged | auto-merged | yes | upstream's one line `testImplementation(libs.kafka.connect.json)` taken as merged (test scope; plan 45-05 proves it is test-only) | 45 | done |
| gradle/libs.versions.toml | M | CONFLICT content | conflict | yes | resolved in Section 1; the three bumps and the connect-json library line also listed in Section 2a | 45 | done |
| src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java | M | auto-merged | auto-merged | yes | high-risk file: #857 taken as merged in M4 (mechanical), re-expressed afterwards under U-1 in plan 45-03 | 45 | done |
| src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java | M | CONFLICT content | conflict | yes | resolved in Section 1 | 45 | done |
| src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java | M | CONFLICT content | conflict | yes | resolved in Section 1; the auto-merged `describeTableV1` hunk is in Section 2a | 45 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java | A | auto-merged | auto-merged | no | upstream-owned test, taken as merged; does not compile on the fork (`new SinkTaskStatistics(0)`), edited in plans 45-02 and 45-03 under U-2 | 45 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java | A | auto-merged | auto-merged | no | upstream-owned test, taken as merged; one method asserts upstream's decoded JSON and is edited to the fork's literal JSON in plan 45-02 under U-2 | 45 | done |
| src/test/java/com/clickhouse/kafka/connect/sink/db/SlashColumnRoundTripTest.java | A | auto-merged | auto-merged | no | upstream-owned test, taken as merged; its live run is plan 45-05 | 45 | done |

## Section 2a — Auto-merged hunks in fork-touched files

| file | auto-merged hunk | resolution | why |
| --- | --- | --- | --- |
| src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java (`describeTableV1`) | #839 adds `BufferedReader reader = new BufferedReader(new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8))` as a second try-resource while the body still calls `readDescribeRows(builder, response.getInputStream())`; the `reader` variable is never read | resolved to the fork side inside M4: the fork's `.executeAndWait()) {` try header restored by an explicit edit | it moves the HLP-04 anchors `StandardCharsets.UTF_8` 1 to 2 and `InputStreamReader` 2 to 3, and would fail invariant 3 on M4 (the anchors file cannot be edited inside M4); the fork already has one UTF-8 reader for both clients |
| src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java (imports) | upstream removes the `ClickHouseRecord`, `ClickHouseValue` and `JsonProcessingException` import lines the fork no longer uses | taken as merged: the merged file shows the fork's import block unchanged, and the file is byte-identical to the fork side (M4's first parent) | compiles; no behaviour |
| src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java | #857: `writeRecordsRowBinary` private to package-private, new `writeColumnWithRecordContext` and `serializationFailure`, `import ...SinkRecord` | taken as merged in M4 (mechanical); re-expressed per U-1 in plan 45-03 (UPS-02) | high-risk file; the outer `DataException` of #857 reaches the retry classification and WRT-10, and `WriterFailureClassificationUnitTest` cannot see it (see Section 3) |
| build.gradle.kts | `testImplementation(libs.kafka.connect.json)` | taken as merged | test scope only; plan 45-05 proves it |
| gradle/libs.versions.toml | the `commonsLang3` 3.21.0, `slf4j` 2.0.20 and `spotless` 8.10.4 bumps and the `kafka-connect-json` library line | taken as merged; the three bumps are no-ops (HEAD already holds the values) and the library line is the one added line (`1 0` numstat against M4's first parent) | user-approved upstream coordinates (plan-check answer 4); the connect-json jar is checked against Maven Central in E-04 |

## Section 3 — Breaks that merge without a marker

The research predicted these breaks, none carries a conflict marker. Evidence entry E-04 measures the compile part on the bare merge before any repair; each has its repair plan:

1. Two javac errors in `compileTestJava`, both in `ClickHouseWriterRecordContextTest` (`new SinkTaskStatistics(0)`; the fork's only constructor is `(String connectorName, int taskId)`): plan 45-02, U-2. `compileJava`, `compileTestFixturesJava` and `compileIntegrationTestJava` pass.
2. One `testUnit` red, `FieldDescriptorEscapeTest#preservesEscapedCommentsAndDefaultExpressions`, that no compile gate sees: it asserts the decoded values where the fork keeps literal JSON text: plan 45-02, U-2.
3. One live red, `WriterErrorPathLiveTest` WRT-10, caused by #857's outer `DataException` around the serialization failure: plan 45-03, U-1.
4. The 216-case `WriterFailureClassificationUnitTest` cannot see #857, because `ScriptedWriteFailureWriter` never runs `writeRecordsRowBinary`: the new Docker-free pins of plan 45-03 close that gap.

## Gate commands

Standing tail gate for Phases 46-49 (the unedited script; run it with each gate):

```bash
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 <range end>
```

Self-test (expected last line `selftest: no failures`):

```bash
bash .planning/scripts/verify-merge-invariants-selftest.sh
```

## Recorded Facts

46. **Merge-4 conflict list — `observed`, 2026-11-04, plan 45-01.** `git merge-tree --write-tree --name-only HEAD 83808df9a0993cf49b2ceaa4633c782e550c0180` exited 1 and printed exactly the six paths of Section 1 (`.github/workflows/tests.yaml`, `CHANGELOG.md`, `VERSION`, `gradle/libs.versions.toml`, `ClickHouseFieldDescriptor.java`, `ClickHouseHelperClient.java`); `ClickHouseWriter.java` and `build.gradle.kts` auto-merged. `git ls-remote upstream main` printed `83808df9a0993cf49b2ceaa4633c782e550c0180` at the pre-flight read and again immediately before the merge, and the newest upstream `v*` tag is `v1.7.0` with no `v1.7.1` tag.

47. **Merge-4 resolution — `observed`, 2026-11-04, plan 45-01.** Resolved as Sections 1 and 2a record. M4 changes exactly the six paths `build.gradle.kts`, `gradle/libs.versions.toml`, `ClickHouseWriter.java` and the three new upstream tests, and adds no `.planning` file; the unedited tail gate result is evidence entry E-03.
