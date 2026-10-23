---
phase: 29-release-publication
plan: "01"
subsystem: release
tags: [release, version-bump, confluent-archive, shadow-9, jmx, anchors-gate, advisories, kafka-4.4.0]

requires:
  - phase: 28-raw-identifier-names-in-discovery
    provides: the final src/main of v1.7.0_gg4 and the 28 suite totals the new base supersedes
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: the Phase 25 archive allow-list, compare script, worker evidence E-12 and the cumulative anchors gate
provides:
  - "VERSION v1.7.0_gg4 with its anchor row in one commit (112fbdb5), generated constant verified after clean"
  - "29-COMMAND-EVIDENCE.md E-01..E-09: the measured inputs for the gg4 rollout note, CHANGELOG and FORK-DELTA"
  - ".planning/scripts/confluent-archive-allowlist-29.txt (50 rows) under which the gg3 and gg4 archives compare RESULT: PASS"
  - "the phase base (testUnit 1278, full suite 1865 tests / 18 skipped, latest x V2) as the reference of the 29-07 gate"
  - "the cumulative anchors gate PASS on the bump commit (15 min 23 s)"
  - "the first recorded read of the origin freeze (custom_build still 69cd680b, left count 0)"
affects: [29-02, 29-03, 29-04, 29-05, 29-06, 29-07, 29-08]

actuals:
  tokens: 11945
  tasks: 4
  commits: 9

tech-stack:
  added: []
  patterns:
    - "Phase 29 allow-list file with exact-path fork-class rows instead of editing the compare script"
    - "Measure on the final tree after the bump commit, then write documents"

key-files:
  created:
    - .planning/scripts/confluent-archive-allowlist-29.txt
    - .planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md
  modified:
    - VERSION
    - .planning/scripts/fork-anchors.txt

key-decisions:
  - "Archive identity gg3 vs gg4 is pinned by a new allow-list with 21 exact com/clickhouse rows, each backed by a source file that changed since v1.7.0_gg3 (PT-13)"
  - "The rollback target 69cd680b builds a green archive with 25 nested jars (PRE-NESTED-JARS=25); the note must say to redeploy the archive built at v1.7.0_gg3 and never build one from the rolled-back tip (PT-03)"
  - "The earlier OSV {} answers for httpclient5 5.6.2 were transient; the repeated later reads match research, so E-07 was corrected in a follow-up commit rather than worded around"

requirements-completed: [PUB-11, OPS-05, OPS-06, ROLL-04]

plan_head_before: 8246a195889da5c9165c6f70f6a8d4b146a57db8
plan_head_after: c8c0ad2abf3deb2f6e7acb69eca8bf0f02095817
duration: 27min
completed: 2026-10-23
status: complete
---

# Phase 29 Plan 01: Version bump, archive identity and measured release inputs Summary

**VERSION v1.7.0_gg4 landed with its anchor row in one commit, and the MBean and client name, the gg3-to-gg4 archive compare (PASS under a new 50-row allow-list), the rollback tree's 25-nested-jar hazard, the latest x V2 phase base, the cumulative anchors gate, the Kafka 4.4.0 / KIP-1161 / advisory re-reads and the origin freeze are all recorded as E-01..E-09.**

## Performance

- **Duration:** 27 min (17:25:33Z to about 17:52Z)
- **Tasks:** 4 (1 tracer, 3 auto)
- **Files modified:** 4 (VERSION, fork-anchors.txt, the new allow-list, the new evidence file)

## Accomplishments

- Bump commit `112fbdb5` changes exactly `VERSION` and `.planning/scripts/fork-anchors.txt` line 41. After `clean compileJava` the generated `ARTIFACT_VERSION` is `v1.7.0_gg4`; the anchors mirror reads `rows=94 bad=0` (E-01).
- A live task on the gg4 tip registers MBeans `...version=v1.7.0_gg4` (per-task and per-topic) and sends the user agent `clickhouse-kafka-connect/v1.7.0_gg4 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.5`; tag `v1.7.0_gg3` shows `v1.7.0_gg3` and `Apache-HttpClient/5.6.2`. Server `26.9.19.9`, image `sha256:bdb5c911a23d...` created 2026-10-14T18:05:21Z (E-02).
- Archive compare of `createConfluentArchive` at tag `v1.7.0_gg3` (7376 inner entries) against the gg4 tip (7399): with the unchanged Phase 25 list 22 `UNALLOWED` lines (the manifest, `Version.class` and 20 fork classes, all under `com/clickhouse/`, no `STALE-ROW`); with the new list `RESULT: PASS`, `nested jars in b: 0`, `kafka classes in b: 0`, `MANIFEST.MF: differs only in Implementation-Version`. The compare script and the Phase 25 list are untouched (E-03).
- Rollback-tree hazard: `createConfluentArchive` at `69cd680b` is `BUILD SUCCESSFUL` with 135 inner file entries and `PRE-NESTED-JARS=25`, the connector class present; the archive at tag `v1.7.0_gg3` holds none. `release.yaml` and `pre-release.yaml` would build it, `tests.yaml` builds no archive (E-04).
- Phase base in `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 CLICKHOUSE_CLUSTER_NAME` unset: testUnit 1278 tests / 0 failed / 2 skipped; full suite 1865 tests in 103 XML files / 0 failed / 18 skipped; every `ClickHouseTestHelpers Version:` line reads `latest`; one server version `26.9.19.9`; `ConnectApiReferencesUnitTest` 3/3 (E-05).
- Cumulative anchors gate `verify-merge-invariants.sh 54e63648 HEAD` on `503b70f8` ends `anchor_rows=94 ... result=PASS` (15 min 23 s together with the self-test, which ends `selftest: no failures`) (E-09).
- D-10 and OPS-06 re-reads by source with status codes: Kafka 4.4.0 notes (source tree 200, website page 404, Maven Central lists 4.4.0), KIP-1161 under `Upgrading to 4.2.0` lines 168-173, `type_list_keys: 7`, `validlist_in_src_main: 0`, worker `cp-kafka-connect-base:7.9.7` (Kafka 3.9) cited from 25 E-12 (E-06); guava `CVE-2026-102554` / `GHSA-xxph-c9ww-hj94` and httpclient5 `CVE-2026-64607` / `GHSA-hjcp-jmpx-g3qm` with their versions (E-07).
- OPS reads: `ops04: pending` (no Dependabot PR after the label), `ops01: disabled_manually, 0 schedule runs after the disable`, `origin_custom_build_tip: 69cd680bf548bfb0329142752d37f8a6c3190c26`, `left_count: 0` (E-08).

## Task Commits

1. **Task 1: bump, generated constant, MBean and client name** - `112fbdb5` (chore, bump), `547c37a1` (docs, E-01 and E-02)
2. **Task 2: archive compare and rollback-tree hazard** - `ebc1740a` (docs, allow-list plus E-03 and E-04)
3. **Task 3: phase base, then the cumulative gate** - `503b70f8` (docs, E-05), `ca1c6adf` (docs, E-09)
4. **Task 4: re-reads** - `d56ef46a` (docs, E-06 and E-07), `feb6e8a0` (docs, E-07 correction), `522affff` (docs, E-08), `c8c0ad2a` (docs, E-08 HEAD reference)

**Plan metadata:** the SUMMARY commit that follows this file.

## Files Created/Modified

- `VERSION` - `v1.7.0_gg4`
- `.planning/scripts/fork-anchors.txt` - line 41 `VERSION<TAB>v1.7.0_gg4<TAB>1<TAB>BLD-07`
- `.planning/scripts/confluent-archive-allowlist-29.txt` - Phase 25 third-party rows verbatim, 21 exact-path `com/clickhouse/` rows, the manifest row
- `.planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md` - E-01..E-09 (E-09 sits after E-05 in time; entries are found by heading)

## Decisions Made

- The 21 fork rows name only requirements the git history ties to the class's source file (DESC-07/08, NAME-03/04/06/07, WRT-17/18/19, the comment-only shift for `SinkTaskStatistics`, the release identity); nested and builder classes say only that their source file changed.
- The note must warn against building an archive from the rolled-back branch tip (E-04, `PRE-NESTED-JARS=25`).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] E-07 stated a transient OSV answer as the record**
- **Found during:** Task 4, after the first E-06/E-07 commit
- **Issue:** The first OSV version queries (about 17:31Z, during the re-index of the guava CVE record) returned `{}` for httpclient5 `5.6.2`; E-07 recorded that as a deviation from research R2.5 and drew a wording limit from it.
- **Fix:** Repeated the queries at 17:50Z, twice each, in three request forms; they return `GHSA-hjcp-jmpx-g3qm` for `5.6.2` and nothing for `5.6.5`, matching research. E-07 now carries a "Correction of the first read" paragraph instead of the wrong deviation.
- **Files modified:** `.planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md`
- **Verification:** RECHECKS-RECORDED passes on the committed tip.
- **Committed in:** `feb6e8a0`

**2. [Rule 1 - Bug] E-08 named the wrong HEAD for the left-right count**
- **Found during:** final review of E-08
- **Fix:** E-08 now says the count was measured with HEAD at `d56ef46a`.
- **Committed in:** `c8c0ad2a`

---

**Total deviations:** 2 auto-fixed (both evidence corrections, no source or script change)
**Impact on plan:** None on the measured results. Two extra evidence commits against the planned four task-level groups.

Other facts that differ in detail from the plan text, all within what the plan allowed: the unchanged Phase 25 allow-list yields 22 `UNALLOWED` lines (the 20 fork classes of research R5.1, plus `Version.class` and the manifest); the guava CVE's OSV record `modified` moved to 2026-10-23T17:27:08Z, with no change of identity or fixed version.

## Issues Encountered

None blocking. The OpenJ9 crash did not occur; no javacore or core files were produced. The probe ran on the Semeru 17.0.9 toolchain (user agent `jvm:17.0.9`).

## Known Stubs

None. No source file was created or modified.

## Threat Flags

None. No new network endpoint, auth path or file access was introduced; evidence holds constants and sentinels only; all external reads were unauthenticated; nothing was pushed, tagged, merged or commented on.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Worktrees `/tmp/gsd29/wt-gg3` (tag `v1.7.0_gg3`) and `/tmp/gsd29/wt-tip` (`547c37a1`, bump-commit tree) and `/tmp/gsd29/probe/Phase29RolloutProbe.java` are kept for plans 29-02 and 29-03; `wt-pre` is removed. `wt-tip` must be moved to the then-current HEAD before measurements (only `.planning/` differs today).
- The 29-07 gate recomputes `testunit_base_class_counts_sha256: 6f209d0b95b008fbf1fa3daeb75fcaee5a4a743b0b355b7affeef25a2557a254` and `suite_base_class_counts_sha256: 3d849967dc1c29e230820a5c94f2a22626b6f9fe758ad868ca8f30fcce0d980f`.
- Local state outside this plan's scope remains dirty and untouched: `gradlew.bat`, `.planning/state.json`, `.planning/milestone.lock`.

## Self-Check: PASSED

Files verified on disk: `VERSION`, `.planning/scripts/fork-anchors.txt`, `.planning/scripts/confluent-archive-allowlist-29.txt`, `.planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md`. Commits verified as ancestors of HEAD: `112fbdb5`, `547c37a1`, `ebc1740a`, `503b70f8`, `ca1c6adf`, `d56ef46a`, `feb6e8a0`, `522affff`, `c8c0ad2a`. Verify commands printed VERSION-GG4-OK, ANCHORS-MIRROR-OK, MBEAN-PROBE-OK, ARCHIVE-COMPARE-OK, ROLLBACK-TREE-HAZARD-RECORDED, PHASE-BASE-OK, CUMULATIVE-GATE-29-01-OK and RECHECKS-RECORDED.

---
*Phase: 29-release-publication*
*Completed: 2026-10-23*
