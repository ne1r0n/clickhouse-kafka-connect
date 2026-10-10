---
phase: "14"
slug: "dependency-bumps-ci"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-10"
---

# Phase 14 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: authored at plan time. `14-01-PLAN.md` through `14-04-PLAN.md` each carry a parseable
`<threat_model>` block. This is a **retroactive** audit run from plan 15-04 (D-09). It verifies each
declared mitigation against the tree as it stands today (`sync/v1.7.0`, HEAD `b2c132ad`) and against
live data read on the audit day, and adds the one threat the audit surfaced after the plans were written
(T-14-16). **No file outside `.planning/` was modified by this audit.**

The ID `T-14-SC` appears in all four plans with differing text and disposition (14-01 and 14-04: `low`,
`accept`, nothing installed; 14-02 and 14-03: `medium`, `mitigate`, Maven version bumps). It is recorded
once, at the stricter reading (`medium`, `mitigate`).

Audit method: the `gsd-security-auditor` sub-agent could not be spawned (the executor has no sub-agent
tool), so the audit ran inline, following `secure-phase.md` and the Phase 11 precedent. Verdicts rest on
the current source, named pinning tests, and measurements taken on the audit day (2026-10-10).

Measurements taken on the audit day:

- **OSV re-query**, `POST https://api.osv.dev/v1/query`, unauthenticated, Maven ecosystem, run at
  2026-10-10T10:51Z against the catalog version of every bumped coordinate (`gradle/libs.versions.toml`):

  | Coordinate | Version | Advisories |
  |------------|---------|------------|
  | `at.yawk.lz4:lz4-java` | 1.12.0 | none |
  | `com.fasterxml.jackson.core:jackson-databind` | 2.22.3 | none |
  | `com.fasterxml.jackson.core:jackson-core` | 2.22.3 | none |
  | `com.squareup.okhttp3:okhttp` | 5.5.0 | none |
  | `org.json:json` | 20260814 | none |
  | `org.mockito:mockito-junit-jupiter` | 5.24.0 | none |
  | `org.mockito:mockito-core` | 5.24.0 | none |

  Positive controls, so an empty answer is not an artefact of the query: `at.yawk.lz4:lz4-java` 1.10.4
  returned six advisories (GHSA-343h-94h5-c4wr, GHSA-4v53-57pg-c464, GHSA-6cx8-rjf8-pr8g,
  GHSA-gm45-99xc-r7wv, GHSA-mcr4-qmvw-px4g, GHSA-xx22-p4ch-683r); `jackson-databind` 2.22.2 returned two
  (GHSA-cxp5-3px4-pw24, GHSA-wv8q-qhhj-9h54); `org.lz4:lz4-java` 1.8.0 returned eight. These match
  `MERGE-CHECKLIST-v1.7.0.md` Recorded Fact 33. The plan's own five-coordinate loop (task 2 `OSV-CLEAN` gate) printed `OSV-CLEAN` at 10:57Z
  (second query, six minutes after the table above, same result).
- **`dependencyInsight`** (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew dependencyInsight ...`):
  `runtimeClasspath` resolves `jackson-databind:2.22.3` and `jackson-core:2.22.3`; `runtimeClasspath`
  and `clickhouseDependencies` each show exactly one `at.yawk.lz4:lz4-java` headline, `1.12.0`
  (`at.yawk.lz4:lz4-java:1.10.2 -> 1.12.0` from `client-v2:0.9.6`, and `runtimeClasspath` substitutes
  `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.12.0`); `testRuntimeClasspath` resolves
  `mockito-core:5.24.0` and `org.json:json:20260814`; `integrationTestRuntimeClasspath` resolves
  `okhttp:5.5.0` and `okhttp-jvm:5.5.0`. `testRuntimeClasspath` still resolves `org.lz4:lz4-java:1.8.0`
  (backlog 999.2).
- **Workflow structure and fork-count step**: the plan 14-01 `ruby` check printed `WORKFLOW-OK` and its
  stub-`nproc` check printed `FORKCOUNT-OK`; a scan of every `run:` body in `tests.yaml` found no `${{`
  expression (`NO-EXPR-IN-RUN`); `grep -c 'secrets\.' .github/workflows/tests.yaml` is 0; the Phase 14
  diff of `tests.yaml` against `c61d90ce` adds no `uses:` line.
- **Live tests**, `./gradlew test --rerun` against `clickhouse/clickhouse-server:latest` (server
  `26.9.14.10`), client V2: `Lz4RoundTripLiveTest` 2 tests, 0 skipped, 0 failed (including
  `lz4FactoryLoadsFromTheMaintainedFork`, code source `.../at.yawk.lz4/lz4-java/1.12.0/.../lz4-java-1.12.0.jar`);
  `NetworkCompressionPinLiveTest` 9 tests, 0 failed; `WideIntegerTypeGateLiveTest` 3 tests, 0 failed;
  `ClickHouseSinkTaskWithSchemaTest` 97 tests, 13 skipped (its named skips), 0 failed;
  `RowBinaryUnsupportedTypeGateTest`, `ColumnTest` and `ClickHouseWriterColumnContextTest` 0 failed.
  Total for the call: 163 tests, 150 succeeded, 0 failed, 13 skipped.
- **Publication evidence re-read**: `git ls-remote --heads origin custom_build` prints `c61d90ce…`
  (unchanged); `git ls-remote --tags origin` lists only `pre-v1.6.0-sync` and `v1.6.0_gg1`, no 1.7.0 tag;
  `git config push.followTags` is unset; `git ls-files .planning/.gsd-ws-arg` is empty; the GitHub REST
  API returns exactly one `tests.yaml` run for head SHA `54b5ce2f…`, id `38037397969`, `completed`,
  `success`.
- **Commit shape**: `git show --numstat` prints `1 1 gradle/libs.versions.toml` for each of the five bump
  commits `cf448fcb`, `60006d49`, `7c85dc73`, `32e91452`, `7bdf8328`; `git diff --quiet 32e91452 7bdf8328^
  -- src build.gradle.kts gradle .github` holds (`ORDER-OK`).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| `git push` → GitHub Actions runner | Workflow file content decides what runs on every branch push of the public fork | workflow definition |
| Test JVM → ClickHouse container | The test client sends harness credentials and receives compressed bytes | test credentials, compressed rows |
| Gradle test classpath | Two jars provide `net.jpountz.lz4.*`; classpath order decides which one runs | classes |
| Maven Central → Gradle cache / shipped Confluent archive | Bumped artifacts are fetched; jackson and lz4-java ship at runtime | third-party jars |
| OSV API → audit record | Advisory status is read unauthenticated from a third-party aggregator | advisory ids |
| ClickHouse JSONEachRow output → `org.json` (tests) | Read-back values pass through `org.json` before assertions | test rows |
| Local repository → public origin | A push publishes code and planning history | commits, refs |
| GitHub REST API → evidence | Run data are read unauthenticated and rate-limited | run metadata |
| Human → executor | The push is authorised only by a direct human answer | consent |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-14-01 | Tampering | `tests.yaml` new run bodies | medium | mitigate | Verified. The trigger is `push: branches: ['**']` only (`tests.yaml:14-16`), so a tag push starts no run. The two new run bodies ("Report test fork count", "Compile integration-test sources") contain no `${{` expression (the only expressions are in the `concurrency.group`, and use `github.workflow`, `github.event_name`, `github.event.number` and `github.sha`, none attacker-controlled strings), no secret reference (`grep -c 'secrets\.'` is 0), and the Phase 14 diff adds no `uses:` line. Pinned by the plan 14-01 `ruby` check (`WORKFLOW-OK`) and the stub-`nproc` check (`FORKCOUNT-OK`), both re-run on the audit day. | closed |
| T-14-02 | Information Disclosure | `Lz4RoundTripLiveTest` logs, fork-count step output | low | mitigate | Verified. The test's `LOGGER` calls (`Lz4RoundTripLiveTest.java:107`, `:131`, `:135`, `:172`) log the jar location, the compression setting with the server version, the row count and byte length, and the block count; no credential and no payload. The step computes `nproc` and a derived fork count and writes only `runner nproc=N, derived maxParallelForks=M (...)` to stdout, `$GITHUB_STEP_SUMMARY` and the `::notice` annotation (`tests.yaml:39-48`); it reads `CLICKHOUSE_CLUSTER_NAME` only for presence (`${VAR+set}`), never its value. The jar location is a filesystem path of the build host, accepted as part of the plan's disclosure. | closed |
| T-14-03 | Tampering | `org.lz4` jar shadowing `at.yawk.lz4` on `testRuntimeClasspath` | medium | mitigate | Verified. `Lz4RoundTripLiveTest#lz4FactoryLoadsFromTheMaintainedFork` (`:102`) asserts `LZ4Factory`'s code source contains the at.yawk.lz4 marker; on the audit day it passed and logged the `at.yawk.lz4/lz4-java/1.12.0` jar. A classpath-order change would fail it loudly. | closed |
| T-14-04 | Denial of Service | tag-only pushes no longer run `tests.yaml` | low | accept | Accepted — see AR-01. Re-measured: `release.yaml`, `pre-release.yaml` and `increment-version.yml` are `workflow_dispatch`; `integrationTests.yaml` and `test-head.yaml` are `schedule` plus `workflow_dispatch`; none is tag-triggered. | closed |
| T-14-05 | Tampering / Denial of Service | lz4-java in the shipped Confluent archive | high | mitigate | Verified, measured on the audit day. The catalog has `lz4Java = "1.12.0"` (`libs.versions.toml:10`). OSV, queried 2026-10-10T10:51Z, returns no advisory for `at.yawk.lz4:lz4-java` 1.12.0; the positive control 1.10.4 returns six, so the query works. `dependencyInsight` shows exactly one `at.yawk.lz4:lz4-java` headline, `1.12.0`, on both `runtimeClasspath` and `clickhouseDependencies`; the old `org.lz4:lz4-java:1.8.0` coordinate is substituted to it on `runtimeClasspath`. The bump is a one-line edit (`cf448fcb`, `1 1 gradle/libs.versions.toml`). | closed |
| T-14-06 | Denial of Service | `org.lz4:lz4-java` 1.8.0 on `testRuntimeClasspath` | low | accept | Accepted — see AR-02. Re-measured: `testRuntimeClasspath` still resolves `org.lz4:lz4-java:1.8.0` (OSV returns eight advisories for it, none fixed on that coordinate), but the shipped `runtimeClasspath` and `clickhouseDependencies` are clean, and `lz4FactoryLoadsFromTheMaintainedFork` proves the test JVM loads `at.yawk.lz4`. Tracked as backlog 999.2. | closed |
| T-14-07 | Spoofing | okhttp 5.5.0 artifacts signed with a new key | low | accept | Accepted — see AR-03. Re-measured: `gradle/verification-metadata.xml` does not exist, so Gradle does not verify signatures today; okhttp resolves only on `integrationTestRuntimeClasspath` (`okhttp:5.5.0`, `okhttp-jvm:5.5.0`). OSV returns no advisory for `okhttp` 5.5.0. | closed |
| T-14-08 | Denial of Service | jackson-databind 2.22.2 in the shipped archive (CVE-2026-91776, CVE-2026-91777) | high | mitigate | Verified, measured on the audit day. The catalog has `jackson = "2.22.3"` (`libs.versions.toml:5`). `dependencyInsight` on `runtimeClasspath` shows `jackson-databind:2.22.3` and `jackson-core:2.22.3`. OSV returns no advisory for `jackson-databind` 2.22.3 and `jackson-core` 2.22.3; the positive control `jackson-databind` 2.22.2 returns GHSA-cxp5-3px4-pw24 and GHSA-wv8q-qhhj-9h54, so the fix is real, not an empty answer. The bump is a one-line edit (`32e91452`). | closed |
| T-14-09 | Tampering | silent read-back change from `org.json`'s new number-length limit | low | mitigate | Verified. `orgJson = "20260814"` (`libs.versions.toml:25`), a one-line edit (`7bdf8328`) with `ORDER-OK` between the Jackson and org.json runs (so only that line differs). Recorded Fact 36 holds the D-06 per-testcase comparison (152 test cases, differing list empty). Re-run on the audit day: `WideIntegerTypeGateLiveTest` 3/3 pass, `RowBinaryUnsupportedTypeGateTest`, `ColumnTest`, `ClickHouseWriterColumnContextTest` and `ClickHouseSinkTaskWithSchemaTest` 0 failures. OSV returns no advisory for `org.json:json` 20260814. | closed |
| T-14-10 | Repudiation | a green run attributed to the wrong tree or server | low | mitigate | Verified. `ORDER-OK` re-computed (`git diff --quiet 32e91452 7bdf8328^ -- src build.gradle.kts gradle .github` holds), and each Phase 14 run records its server version and commit in `MERGE-CHECKLIST-v1.7.0.md` Recorded Facts 33-36 (server `26.9.14.10` for the lz4 run, matching the audit-day server version). | closed |
| T-14-11 | Information Disclosure | `git push` publishing local-only tags or files | high | mitigate | Verified, re-measured. Origin's tag list is `pre-v1.6.0-sync` and `v1.6.0_gg1` (Phase 11), no tag from this milestone; the local tag `pre-v1.7.0-sync` is not on origin; `push.followTags` is unset; `.planning/.gsd-ws-arg` is untracked. Recorded Fact 37 records the before/after tag-list `cmp` as byte-identical. | closed |
| T-14-12 | Elevation of Privilege | a push made without consent | high | mitigate | Verified. `14-04-SUMMARY.md` records decision D-13: the user answered "push" directly to the orchestrator at the blocking-human checkpoint, and the orchestrator ran Task 3 inline because the executor refuses relayed approvals. The pushed ref (`sync/v1.7.0`, `54b5ce2f`) is on origin and nothing else moved. | closed |
| T-14-13 | Tampering | `custom_build` moved by mistake | high | mitigate | Verified, re-measured. `git ls-remote --heads origin custom_build` prints `c61d90ce5dfb32ea5876dffacc8bb54d8846739a`, the pre-milestone head. | closed |
| T-14-14 | Repudiation | wrong CI run attributed as evidence | medium | mitigate | Verified, re-measured. The GitHub REST API, filtered to head SHA `54b5ce2f3fbe7aafb544f53e95283ddede9aa7b1` and path `.github/workflows/tests.yaml`, returns exactly one run: id `38037397969`, event `push`, `completed`, `success`, 2026-10-10T08:18:04Z to 08:24:36Z. | closed |
| T-14-15 | Information Disclosure | CI logs on the public repository | low | accept | Accepted — see AR-04. The repository and its runs are public; `tests.yaml` contains no `secrets.` reference and the new step prints CPU counts only. | closed |
| T-14-16 | Elevation of Privilege | `tests.yaml` has no `permissions:` block and its actions are pinned by mutable tag (new, found by this audit) | low | mitigate | Found by this audit; not in any plan. `tests.yaml` declares no top-level `permissions:` (the upstream `v1.7.0` file has none either), so the job's `GITHUB_TOKEN` takes the repository default, and `actions/checkout@v4`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6` and `actions/upload-artifact@v4` are pinned by tag, not commit SHA. Both are pre-existing: Phase 14 added no `uses:` line and no permission. The workflow reads no secret and runs on branch pushes of a repository with no untrusted-fork trigger, so the exposure is limited. Below the `high` block threshold, so it does not count toward `threats_open`. Not fixed here: adding `permissions: contents: read` is a CI change outside this plan's remit, carried to the approval bundle for a user decision. | open — below high threshold (non-blocking) |
| T-14-SC | Tampering | Maven dependency version bumps (supply chain) | medium | mitigate | Verified. All five bumped coordinates (`at.yawk.lz4:lz4-java`, `com.fasterxml.jackson.core:jackson-databind`/`jackson-core`, `com.squareup.okhttp3:okhttp`, `org.json:json`, `org.mockito:mockito-core`/`mockito-junit-jupiter`) are existing coordinates whose `version.ref` line changed; each bump commit is a one-line catalog edit (`git show --numstat`: `1 1`), no new module appeared in Phase 14, and no package was installed. The versions are exactly the ones OSV was queried for above, all with no advisory on the audit day. The 14-01 and 14-04 reading of `T-14-SC` (nothing installed) also holds. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on (`high`) count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Totals: 17 threats (T-14-01..16 plus T-14-SC) — 16 closed, 1 open below the block threshold
(T-14-16, low), **0 open at or above `high`**.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-14-04 | Tag-only pushes no longer run `tests.yaml`. Every release workflow is `workflow_dispatch`, and Phase 15 publishes the SHA on a branch, so nothing depends on a tag-triggered test run. The known side effect (the `pre-v1.7.0-sync` tag still runs the old `c61d90ce` workflow once) is recorded in Phase 15 D-13. | plan 14-01 (plan-time disposition), re-measured by audit 15-04 | 2026-10-10 |
| AR-02 | T-14-06 | `org.lz4:lz4-java:1.8.0` is on the test classpath only and is not shipped; the maintained fork is what the test JVM loads (pinned). Removing it needs a `build.gradle.kts` change outside Phase 14's scope; tracked as backlog 999.2. | plan 14-02 (plan-time disposition), re-measured by audit 15-04 | 2026-10-10 |
| AR-03 | T-14-07 | No `verification-metadata.xml`, so artifact signatures are not checked today. Artifacts come from Maven Central over HTTPS; okhttp is integration-test only. | plan 14-02, re-measured by audit 15-04 | 2026-10-10 |
| AR-04 | T-14-15 | The repository and its CI runs are already public; the new step prints CPU counts only and no `tests.yaml` step reads a secret. | plan 14-04, re-measured by audit 15-04 | 2026-10-10 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-10 | 17 | 16 | 1 (T-14-16, low, below the `high` block threshold) | /gsd-secure-phase 14 (retroactive audit, plan 15-04 task 2, run inline; ASVS L1; mitigations verified against the tree at `sync/v1.7.0` HEAD `b2c132ad`; OSV queried 2026-10-10T10:51Z) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed (no open threat at or above `high`)
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-10

---

## Carried Forward (non-blocking)

Recorded so this register, the review dispositions and the Phase 15 approval bundle do not drift apart.

- **T-14-16** (open, low): no `permissions:` block and tag-pinned actions in `tests.yaml`, both
  pre-existing and unchanged by Phase 14. A user decision is needed only if the user wants a CI change
  before publication; none was made.
- **Backlog 999.2** (T-14-06, accepted): the test classpath still carries `org.lz4:lz4-java:1.8.0`.
- **14-REVIEW WR-01, WR-02 and IN-01..IN-04** keep their `14-REVIEW-DISPOSITION.md` dispositions
  (deferred). None is a security threat: WR-01 is a test that does not prove the response leg was
  compressed, WR-02 a fork-count step that recomputes the value instead of reading Gradle's.
- The OSV verdict is a point-in-time reading (2026-10-10T10:51Z). The Phase 15 approval bundle should
  quote this date, not treat the result as current at publication.
