---
phase: 25-dependency-absorption-upstream-mini-sync
verified: 2026-10-21T18:10:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - .github/workflows/tests.yaml
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-01-PLAN.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-01-SUMMARY.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-02-PLAN.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-02-SUMMARY.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-03-PLAN.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-03-SUMMARY.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-04-PLAN.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-04-SUMMARY.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-05-PLAN.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-05-SUMMARY.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-06-PLAN.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-06-SUMMARY.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-07-PLAN.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-07-SUMMARY.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-08-PLAN.md
  - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-08-SUMMARY.md
  - .planning/scripts/fork-anchor-exemptions.txt
  - .planning/scripts/fork-anchors.txt
  - build.gradle.kts
  - docs/FORK-DELTA.md
  - gradle/libs.versions.toml
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  - src/test/resources/connect-api-references.txt
covered_digest: "v3:sha256:9acef1e25cbbd1bba9a9d766f0d83262a251179e7e1431c3f32533895b2463cc"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 25: Dependency Absorption & Upstream Mini-Sync Verification Report

**Phase Goal:** Every later phase builds, tests and gates on the post-absorption tree (`origin/custom_build`'s nine Dependabot merges plus upstream's untagged tail). The shipped Confluent archive is intact under Shadow 9.6.1, `connect-api` 4.4.0 is compile-time only against the Kafka 3.9 worker, CI cannot publish an archive of nested jars unnoticed, and no connector behaviour changes.
**Verified:** 2026-10-21 (HEAD `37421ed2`, branch `sync/v1.7.0`)
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1 (DEBT-13, DEBT-14): `ktsCode` is string- and comment-aware; the pin guard accepts a trailing-comment `runs-on`, rejects commented-out or guarded fork-count text, accepts valid `dependabot.yml` layouts, derives fixtures from the production constant; each seen red first; DEBT-13 precedes the first authored `build.gradle.kts` edit | VERIFIED | `ktsCode` (TestsWorkflowPinGuardUnitTest.java:459) is a six-state scanner (CODE/STRING/CHAR/RAW_STRING/LINE_COMMENT/BLOCK_COMMENT) with fixtures for `/*` in a string, raw string and `//` comment (lines 1089-1135). DEBT-13 commit `92b2be1e` (18:16) precedes the first authored build edit `bddb838d` (19:12). E-03/E-04 and E-17/E-18 record red sets equal to the prediction (5 red of 39, then 39 green) with accept-set tables. I re-ran `TestsWorkflowPinGuardUnitTest`, `ConnectApiReferencesUnitTest`, `ConfluentArchiveCheckUnitTest` with `--rerun`: 48/48 pass. No assertion was weakened beyond the DEBT-14-mandated accept-set widening, which is tabulated in E-18. |
| 2 | SC2 (DEPS-02, DEPS-03): nine Dependabot merges in one merge commit, tip re-read before merge, anchor edit in the same commit, unedited gate passes, SHA-pinned exemption rows from the gate's output, suite green with the same 18 skips, spotless baseline-equal, `--warning-mode all` recorded | VERIFIED | Merge `50f56289` has parents `a884032d` (= tag `pre-v1.7.0-deps`) and `69cd680b`; `git rev-list a884032d..69cd680b --first-parent` lists the nine PR merges (#6-#9, #11-#15). It carries the `fork-anchors.txt` edit (`kafka = "4.4.0"`). `ls-remote` read recorded at E-07 line 255 immediately before the merge. `verify-merge-invariants.sh` has no diff since the phase start. I ran the cumulative default-env gate `verify-merge-invariants.sh 54e63648 HEAD` (see Spot-Checks) and the tail gate (PASS). 18 SHA-pinned rows in `fork-anchor-exemptions.txt`. E-19/E-24: 1303 tests, 0 failed, 18 skipped, identical named skip set. E-08: spotless numstat byte-equal to `v1.7.0_gg3` (46 lines), six deprecation warning kinds recorded, wrapper jar SHA-256 equals the published value. |
| 3 | SC3 (BLD-15, BLD-16, CI-20): archive has zero nested jars and no Connect API classes; compare against `v1.7.0_gg3` differs only by the allow-list; `MANIFEST.MF` identical; bytecode evidence against worker 3.9 and 4.1.1/4.4.0 jars; `tests.yaml` builds the archive and the guard pins it; FORK-DELTA and comments say the worker supplies the API | VERIFIED | Independent rebuild (`createConfluentArchive --rerun-tasks`): `verifyConfluentArchive: 7798 inner entries, no nested jar, no Kafka API class`. I unzipped it: exactly one `lib/*-confluent.jar`, 0 `org/apache/kafka/` entries inside, 0 nested `.jar`, 1982 `com/google/common/` entries (guava present), manifest holds only Implementation-Title/Version. I re-ran `compare-confluent-archives.py` against the kept `v1.7.0_gg3` archive (7376 entries): `MANIFEST.MF: identical`, `nested jars in b: 0`, `kafka classes in b: 0`, `RESULT: PASS`, no UNALLOWED or STALE-ROW line. Shadow repair is in `build.gradle.kts:364-370` (`configurations.add(clickhouseDependencies)`, `addMultiReleaseAttribute.set(false)`), `createConfluentArchive` is `finalizedBy("verifyConfluentArchive")` (line 415), `tests.yaml:72-73` runs `./gradlew createConfluentArchive` before `Run tests`. I re-ran `connect-api-refs.py` against the current build classes with the 4.4.0 compile classpath and the extracted `cp-kafka-connect-base:7.9.7` jars: `derived classes 39 members 137`, `REFERENCE worker classes-missing 0 members-missing 0`. The 4.1.1/4.4.0 reference results are E-12. The `onPartitionsRevoked` comment (ClickHouseSinkTask.java:150-154), FORK-DELTA lines 34/70 and the three DLQ-emulation Javadocs (`InMemoryDLQ`, `ThrowableTexts`, `ValueFreeDlqHeadersUnitTest`) all say "the worker supplies the Connect API at runtime". `addMultiReleaseAttribute` decision recorded (E-10, `false`). |
| 4 | SC4 (SYNC-11, SYNC-12, DEBT-15): tail merged as one mechanical merge behind the unedited gate with a full-SHA `UPSTREAM_REF`, conflicts exactly `.gitignore` and `libs.versions.toml`, shared keys keep fork values, guava added, `httpclientTest` gone, checklist recorded, `dependencyInsight` shows guava 33.7.2, slf4j-reload4j from the catalog with its anchor moved, FORK-DELTA labels advisories by source, two `--rerun` suite runs green | VERIFIED | Merge `38265d26` has parents `bb88f23e` (= tag `pre-v1.7.0-tail`) and `a6f1df83`. `conflicted-paths-v1.7.0-tail.txt` lists exactly `.gitignore` and `gradle/libs.versions.toml`; the gate's cross-check passes. The catalog holds `kafka = "4.4.0"`, `gson 2.14.0`, `jackson 2.22.4`, `lombok 1.18.48`, `lz4Java 1.12.0`, `guava = "33.7.2-jre"`, no `httpclientTest`. `refs/upstream/*` is empty. `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` has the resolution table. I ran `dependencyInsight`: guava `33.4.6-jre -> 33.7.2-jre` on `runtimeClasspath`, slf4j-reload4j 2.0.20. `slf4j-reload4j` is `libs.slf4j.reload4j` (build.gradle.kts:107) with the catalog anchor row at fork-anchors.txt:43-44. FORK-DELTA lines 109-110 label each fact as NVD or OSV with the query date. E-24: two `--rerun` runs, each 1303 tests / 0 failed / 18 skipped, ordered non-overlapping; baseline tag `baseline-v1.7.0_gg4` = `aa450709`; `git diff aa450709 HEAD -- . ':!.planning'` is empty, so the runs cover the current tip. |
| 5 | SC5 (OPS-04): user created the `dependencies` label and said so directly; record states either a label-clean Dependabot PR or pending the next monthly run | VERIFIED | E-02 records the answer verbatim ("just created the label", given directly to the orchestrator). I re-read the API: `GET /labels/dependencies` returns 200 with `name: "dependencies"`. E-25 records `Verdict: pending (no Dependabot PR opened since 2026-10-21T15:11:05Z; next monthly Dependabot run; Phase 29 re-checks)`, which is the second branch the criterion allows. The REQUIREMENTS.md OPS-04 wording explicitly allows "pending the next monthly run". |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

No connector behaviour change: `git diff a884032d HEAD -- src/main` is four comment lines in `ClickHouseSinkTask.java` (the `onPartitionsRevoked` comment), nothing else.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `build.gradle.kts` (`confluentJar`, `verifyConfluentArchive`) | Shadow 9 repair and guard finalizer | VERIFIED | lines 364-370, 415, 421-430; ran green |
| `gradle/libs.versions.toml` | merged catalog | VERIFIED | values as above |
| `.github/workflows/tests.yaml` | archive step before tests | VERIFIED | lines 72-73, header comment 20-21 |
| `src/test/java/.../ci/ConfluentArchiveCheck.java` + `ConfluentArchiveCheckUnitTest` | unit-tested archive rules | VERIFIED | run by Gradle finalizer; unit test passes |
| `src/test/java/.../ci/ConnectApiReferencesUnitTest.java`, `src/test/resources/connect-api-references.txt` | blessed 176-entry list (39 C + 137 M) | VERIFIED | test passes; script re-derives the same 39/137 |
| `.planning/scripts/compare-confluent-archives.py`, `confluent-archive-allowlist-25.txt`, `connect-api-refs.py` | evidence tooling | VERIFIED | executed above |
| `.planning/scripts/fork-anchor-exemptions.txt`, `fork-anchors.txt` | SHA-pinned rows, moved anchors | VERIFIED | 18 DEPS-03 rows; gate passes |
| `.planning/MERGE-CHECKLIST-v1.7.0-tail.md`, `conflicted-paths-v1.7.0-tail.txt` | merge 2 record | VERIFIED | present, consistent with gate |
| `25-COMMAND-EVIDENCE.md` | E-01..E-25 | VERIFIED | all entries pass except E-25 (pending, permitted) |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `createConfluentArchive` | `verifyConfluentArchive` | `finalizedBy` | WIRED | the rebuild printed the guard line |
| `tests.yaml` step | `createConfluentArchive` | `run: ./gradlew createConfluentArchive` | WIRED | pinned by `TestsWorkflowPinGuardUnitTest` |
| `verifyConfluentArchive` | `ConfluentArchiveCheck` | `mainClass` on the test classes dir | WIRED | executed in the rebuild |
| merge commit M1 | `fork-anchors.txt` | same-commit anchor edit | WIRED | `kafka = "4.4.0"`; gate PASS |
| `build.gradle.kts` slf4j | catalog `slf4j-reload4j` | `libs.slf4j.reload4j` | WIRED | dependencyInsight 2.0.20 |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Cumulative anchors gate | `.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (default env, run fresh by me) | `invariants: provenance_paths=21 anchor_commits=622 anchor_rows=72 marker_commits=681 anchor_exempt=19 result=PASS` | PASS |
| Tail gate | `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df83... CONFLICTED_PATHS_FILE=...-tail.txt verify-merge-invariants.sh 38265d26 HEAD` | `invariants: provenance_paths=3 anchor_commits=13 anchor_rows=72 marker_commits=25 anchor_exempt=0 result=PASS` | PASS |
| Archive build + guard | `./gradlew createConfluentArchive --rerun-tasks` | `7798 inner entries, no nested jar, no Kafka API class` | PASS |
| Archive compare | `compare-confluent-archives.py gg3.zip now.zip allowlist` | `RESULT: PASS`, manifest identical | PASS |
| Worker API refs | `connect-api-refs.py ... --reference worker=7.9.7 jars` | `classes-missing 0 members-missing 0` | PASS |
| New guards | `testUnit --rerun --tests` on the three ci classes | 48 tests, 0 failed | PASS |
| `testUnit` | `./gradlew testUnit` | 860 tests, 858 succeeded, 2 skipped | PASS |
| Guava / slf4j resolution | `dependencyInsight` | guava 33.7.2-jre, slf4j-reload4j 2.0.20 | PASS |

The cumulative gate ran about 10 minutes; the fresh PASS above matches E-07's recorded PASS (same anchor_exempt=19; the commit and row counts are higher now because the range grew). An earlier ad-hoc run of mine with the wrong environment (a v1.7.0 conflicted-paths file against the merge-1 shape) reported FAIL; that was my misparameterisation, not a defect, since the documented DEPS-03 gate is the default-env command. I did not re-run the full `test` suite or the 12-minute gate self-test; both are covered by recorded runs (E-07, E-19, E-24) at a tree whose non-`.planning` delta to HEAD is empty.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DEBT-13 | 25-02 | string/comment-aware `ktsCode` | SATISFIED | SC1 |
| DEPS-02 | 25-03, 25-07 | nine Dependabot merges, suite green, spotless equal | SATISFIED | SC2 |
| DEPS-03 | 25-03 | anchors invariant on merged tree, SHA-pinned rows | SATISFIED | SC2 |
| BLD-15 | 25-04, 25-08 | archive intact under Shadow 9.6.1 | SATISFIED | SC3 |
| BLD-16 | 25-05 | `connect-api` 4.4.0 compile-time only | SATISFIED | SC3 |
| CI-20 | 25-06 | CI archive step plus pin | SATISFIED | SC3 |
| DEBT-14 | 25-06 | pin-guard advisories | SATISFIED | SC1 |
| SYNC-11 | 25-07 | tagless mini-sync | SATISFIED | SC4 |
| DEBT-15 | 25-08 | slf4j literal into the catalog | SATISFIED | SC4 |
| SYNC-12 | 25-08 | post-sync baseline | SATISFIED | SC4 |
| OPS-04 | 25-01, 25-08 | label created, observation recorded | SATISFIED (observation pending, as the requirement allows) | SC5 |

All 11 IDs appear in a plan's `requirements:` frontmatter and in REQUIREMENTS.md (all ticked, traceability rows "Phase 25 / Complete"). No orphaned requirement is mapped to Phase 25.

### Anti-Patterns Found

No `TBD`, `FIXME` or `XXX` was added in any non-planning file touched by the phase. No stubs: every new file has substantive, executed content.

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `TestsWorkflowPinGuardUnitTest.java` | 247-314 | `gradlewCall` accepts `-x verifyConfluentArchive`, `--dry-run`, `|| true`, and a step-level `if:`; step order is not asserted (25-REVIEW WR-01) | Warning | The CI-20 pin is weaker than its intent; the step exists and runs today |
| `ConnectApiReferencesUnitTest.java` | 183-187, 251-278 | unresolved inherited members are skipped; reflection runs on the test classpath, where `kafka-clients` resolves to `7.9.1-ccs` (I confirmed with `dependencyInsight`) while `connect-api` is 4.4.0 (25-REVIEW WR-02) | Warning | The regression pin is a weaker proof than its Javadoc says; the phase proof itself (offline bytecode check against the worker jars) is independent and re-confirmed |
| `ConnectApiReferencesUnitTest.java` | 223-239 | only Class/Field/Method constants are read; a Kafka type used only in a descriptor or annotation is not checked (25-REVIEW WR-03) | Warning | Possible blind spot in the reference list |

The three warnings are 25-REVIEW findings with disposition `open` (recorded, untriaged). None contradicts a success criterion as written; they bear on how durable the two new guards are.

### Human Verification Required

None. The one operator-dependent item (OPS-04 observation) is `pending` by the criterion's own terms and is re-checked in Phase 29.

### Gaps Summary

No gaps. The post-absorption tree holds both merges with the expected parents, local-only rollback and baseline tags (none on origin), the unedited gate, a repaired and re-measured Confluent archive, a bytecode proof that the connector references nothing missing from the Kafka 3.9 worker API, and a CI step plus pin that fail on nested jars. `src/main` changes by four comment lines only.

Items worth carrying forward (not blocking): triage the three open 25-REVIEW warnings (the pin-guard bypass shapes in particular, since the guard's purpose is to stop a silent regression); the CI run of the new `tests.yaml` step has not happened because nothing is pushed, so Phase 29's published-SHA CI run is the first live check; `pre-release.yaml` still pins Gradle 7.4.2 (25-REVIEW IN-04d).

---

_Verified: 2026-10-21_
_Verifier: Claude (gsd-verifier)_
