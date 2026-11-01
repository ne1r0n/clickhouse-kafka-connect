---
phase: 38-release-publication
plan: "04"
subsystem: release
tags: [allow-list, archive-compare, gates, full-suite, pinned-image, evidence]

requires:
  - phase: 38-release-publication
    provides: "plan 38-01 to 38-03: VERSION v1.7.0_gg6, the frozen CHANGELOG section, FORK-DELTA and rollout note (E-10, frozen at 5031bb00), evidence E-01..E-10"
provides:
  - ".planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt: 31 rows derived from the 242 differences of two archives rebuilt on the final tree; the compare against it exits 0 with RESULT: PASS"
  - "38-COMMAND-EVIDENCE.md entries E-11..E-13 (all pass): the allow-list in both polarities, the Docker-free gates on one head, two full suites on one pinned latest image and the go decision"
affects: [38-05]

plan_head_before: ae71d671759a78ae4634d7f869dd6f3d18049fd0
plan_head_after: 048a9ba28163e05a5bef6c7ebb2feff443cb1420

actuals:
  tokens: 8639
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "allow-list rows derived from the compare's own UNALLOWED set by owner group, cross-checked (not copied) against a plan-time candidate"
    - "crash rule for the OpenJ9 test JVM: read the javacore, delete crash files by name, re-run once, then escape to -PjavaVersion=21 for both suites"

key-files:
  created:
    - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt
  modified:
    - .planning/phases/38-release-publication/38-COMMAND-EVIDENCE.md

key-decisions:
  - "Both full suites run on JDK 21 (-PjavaVersion=21) because the default JDK 17 (IBM OpenJ9 17.0.9+5) crashed twice on suite 1; the Docker-free testUnit of E-12 ran on the default JDK 17 and is green there"
  - "The reasons of the six ClickHouseSinkConfig inner-class rows and of the ClickHouseClientBuilder row say 'javap shows only shifted line numbers', because javap -v -p on both archives showed no other difference (the candidate's 'InnerClasses attribute' reason was not borne out)"

requirements-completed: [RLS-03, RLS-06]

coverage:
  - id: D1
    description: "The gg6 allow-list is generated last from archives rebuilt on the final tree: one row per owner group (31 rows), every one of the 242 differences named by exactly one row, no stale row, the compare exits 0 with RESULT: PASS and the same 31 globs as the plan-time candidate"
    requirement: "RLS-03"
    verification:
      - kind: other
        ref: "38-COMMAND-EVIDENCE.md E-11; 38-allowlist-check.py rows/cover/same-globs; compare_confluent_archives.py exit 0 RESULT: PASS; plan verify P38-04-T1-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "The compare turns red on a stale row (exit 1, STALE-ROW), on a deleted native row (exit 1, UNALLOWED) and on a leading-wildcard row (exit 2); the shipped-lists test (58 tests) and the release-workflow guard (37 tests) are green with the new list"
    requirement: "RLS-03"
    verification:
      - kind: other
        ref: "E-11 mutation_stale, mutation_deleted, mutation_wildcard, compare_xml, relguard_xml"
        status: pass
    human_judgment: false
  - id: D3
    description: "On one gated head (ea613ec3) the Docker-free testUnit passes with the daemon unreachable at 1672 tests and 2 skips, the anchors mirror reads rows=172 bad=0, and the tail and cumulative merge gates end result=PASS with anchor_rows=172"
    requirement: "RLS-06"
    verification:
      - kind: other
        ref: "E-12; plan verify P38-04-T2-OK and P38-04-T2-TESTS-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "Two full --rerun suites on ClickHouse latest 26.10.3.7 with client V2, one pinned image digest read three times, both 2270 tests, 0 failures, 0 errors, 18 named skips, the Docker-only delta 598, one JVM line (JDK 21) for both"
    requirement: "RLS-06"
    verification:
      - kind: other
        ref: "E-13; plan verify P38-04-T3-OK"
        status: pass
    human_judgment: false
  - id: D5
    description: "Whether running both full suites on JDK 21 instead of the default JDK 17 is an acceptable record for the release gate"
    verification: []
    human_judgment: true
    rationale: "The default JDK 17 on this host is an OpenJ9 build that crashed twice in the Mockito inline mock maker (a host and JVM fault, not a test result); whether the JDK 21 record satisfies the user's reading of ROADMAP criterion 4 is the user's call"

duration: 51min
completed: 2026-10-31
status: complete
---

# Phase 38 Plan 04: The gg6 allow-list and the release gates Summary

**The v1.7.0_gg6 allow-list was generated last from archives rebuilt on the final tree (31 rows covering all 242 differences once, compare PASS, three mutations red), and the same head then passed the Docker-free gates (testUnit 1672 tests with the daemon unreachable, both merge gates PASS) and two full suites (2270 tests, 18 skips, one pinned image) on JDK 21 after the default JDK crashed twice: decision go.**

## Gate

- `gated_head`: `ea613ec35ebade7a2182cac4e6ecd5d86d099def` (the head every gate ran on; the commits after it, `3f195713`, `048a9ba2` and this SUMMARY's, touch only `.planning/phases/38-release-publication`).
- `last_non_planning_commit` (the SHA the user handoffs call the gated SHA): `927ac07c30274c46b2330d597d417f9f1eda8f8b`.
- Allow-list commit (`last_scripts_commit`): `cc928221b7f2a19ccad3fa2f160f970b35c671e9`; it is the last commit that touched `.planning/scripts`.
- `gate_decision`: `go` (E-12 `docker_free_decision` go, E-13 `gate_decision` go, `gate_failures` none).
- `testUnit` (Docker unreachable, default JDK 17): 1672 tests, 0 failures, 0 errors, 2 skipped, equal to the Phase 37 verifier's 1672; a second cache-busted run (the plan's own verify) read the same.
- Full suites (ClickHouse `latest`, client V2, both `-PjavaVersion=21`): suite 1 and suite 2 each 2270 tests, 0 failures, 0 errors, 18 skipped (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`), 2m 44s and 2m 43s. Docker-only delta 2270 - 1672 = 598, equal to Phase 37's 2231 - 1633. The three live classes that back note rows (`BufferedOffsetCommitLiveTest` 4, `Client011BehaviourPinLiveTest` 6, `NetworkCompressionPinLiveTest` 9) are green in both.
- Image: `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, read before run 1, after run 1 and after run 2 (the same value; also Phase 37's), `SELECT version()` = `26.10.3.7` each time. Nothing was pulled.
- JVM line: both full suites on JDK 21 (Homebrew 21.0.12.1); `testUnit` on the default JDK 17.
- Merge gates: anchors mirror `rows=172 bad=0`; tail gate `provenance_paths=27 anchor_commits=259 anchor_rows=172 marker_commits=266 anchor_exempt=0 result=PASS`; cumulative gate `provenance_paths=21 anchor_commits=1270 anchor_rows=172 marker_commits=1336 anchor_exempt=19 result=PASS`.
- Scope: the non-`.planning` change set since the phase start is exactly `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg5.md`, `docs/ROLLOUT-v1.7.0_gg6.md`; `.planning/scripts` changed in `confluent-archive-allowlist-v1.7.0_gg6.txt` and `fork-anchors.txt`; `src/main` differs from `v1.7.0_gg5` by 7 files, 112 insertions, 31 deletions (FORK-DELTA's numbers); the FORK-DELTA raw numbers equal a fresh run (331/39665/1161 and 40/2989/272); origin `custom_build` still `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, nothing pushed.

## Allow-list

- Rows: 31 (`OK rows=31`); `cover`: `OK differences=242 covered=242 uncovered=0 overlapping=0 stale_rows=0`; the compare against it exits 0 with `RESULT: PASS`, 0 `UNALLOWED`, 0 `STALE-ROW`, `MANIFEST.MF: differs only in Implementation-Version`, nested jars 0, Kafka classes 0.
- Archives (rebuilt from an export of tag `v1.7.0_gg5` with its own wrapper and from an export of the tip): 7801 and 7933 inner entries, 12453556 and 13558634 bytes; byte-identical (SHA-256 equal) to the archives E-05 measured, and the compare of the E-05 tip archive with the new one reads PASS.
- Candidate cross-check: `same-globs` reports `a=31 b=31 only_a=0 only_b=0`. The globs are identical to the plan-time candidate; the reasons are worded independently. One reason in the candidate was not borne out (inner classes "change with the InnerClasses attribute"): `javap -v -p` of the six `ClickHouseSinkConfig` inner classes and of `ClickHouseHelperClient$ClickHouseClientBuilder` on both archives shows only shifted line numbers, so the rows say that.
- Mutations (scratch copies, never the repository file): stale row `zz/nothing/*` gave exit 1 with `STALE-ROW zz/nothing/*`; the deleted `linux/amd64/libzstd-jni-1.5.7-23.so` row gave exit 1 with `UNALLOWED ONLY-B linux/amd64/libzstd-jni-1.5.7-23.so`; an added `*.class` row gave exit 2 (the loader refuses a glob that starts with a wildcard).

## Task Commits

1. **Task 1: the allow-list** - `cc928221` (the list), `ea613ec3` (E-11)
2. **Task 2: Docker-free gates** - `3f195713` (E-12)
3. **Task 3: two full suites** - `048a9ba2` (E-13)

**Plan metadata:** the commit that carries this SUMMARY.

## Deviations from Plan

**1. [Rule 3 - Blocking] The full suites needed the JDK escape**
- **Found during:** Task 3, suite 1.
- **Issue:** the default JDK 17 (IBM OpenJ9 17.0.9+5) crashed on suite 1 twice, exit 255 through the Mockito inline mock maker (`retransformClasses0`; a `traceassert` dump, then a `gpf` dump), the known fault with the host's swap nearly full.
- **Fix:** per the crash rule, each crash file set (an 8 GB core dump, javacore, Snap, jitdump) was deleted by name, `./gradlew --stop` run and the run repeated once; when it recurred, both suites were run with `-PjavaVersion=21` so they share one JVM line. E-13 records all attempts and the stacks. The Docker-free `testUnit` stayed on the default JDK 17 (it passes there) and E-12 says so.
- **Commit:** `048a9ba2`.

**2. [Rule 1 - Bug] Inner-class reasons in the list**
- **Found during:** Task 1, step 4.
- **Issue:** the research text says the `ClickHouseSinkConfig` inner classes "change through the InnerClasses attribute"; `javap` on the real archives shows only shifted line numbers for them and for `ClickHouseClientBuilder`.
- **Fix:** the seven rows (and one header line) say "javap shows only shifted line numbers"; the globs are unchanged. The scratch extract was removed.
- **Commit:** `cc928221`.

**3. [Plan wording] Verify and key details**
- `junit --class` takes the class name without a star (the helper adds it); a first attempt with `*Name` matched nothing and was repeated correctly. `guards_failed` holds `OK` exactly, as the plan's expect clause says (the helper prints more; the first E-11 draft held the whole line and the expect check caught it before the commit).
- The T1 and T2 verify clauses on `git status --porcelain -- ... "${E}"` need the evidence committed first, so each verify block was run after its evidence commit; all three print their OK lines on the final tip (`P38-04-T1-OK`, `P38-04-T2-OK`, `P38-04-T2-TESTS-OK`, `P38-04-T3-OK`).
- `empty.txt` in the new archive directory is a byte copy of E-05's comment-only list; the "compare_version_table" key holds the one differing line (18 `VERSION` lines in all), as in E-05.

**Total deviations:** 3 (1 blocking, 1 bug in a plan-time description, 1 wording note). **Impact:** none on scope; the full-suite JVM line is JDK 21, not the preferred default JDK 17.

## Issues Encountered

- Two OpenJ9 JVM crashes on the default JDK 17 (see deviation 1); no crash file remains (`crash_files_left: none`) and no 8 GB core dump is left on the disk.
- The cumulative gate took about 31 minutes while it shared the host with the tail gate and two `testUnit` runs.
- `.planning/config.json` and `.planning/state.json` stay modified in the working tree and were never staged.

## Known Stubs

None.

## Threat Flags

None. The plan adds one allow-list file (a data file read by the release workflow) and evidence; no endpoint, auth path or file access.

## User Setup Required

None - no external service configuration required.

## Open for the orchestrator

- **JVM line (decision for the user):** both full suites ran on JDK 21 because the default JDK 17 on this host crashed twice (E-13 holds both accounts). ROADMAP criterion 4 says two full runs on one pinned build; that holds, on JDK 21. The preferred record (default JDK 17) was not obtained and is not claimed. `digest_same_as_phase37` is `yes`.
- **Candidate difference:** none in the glob set (31 = 31, same globs); only the wording of the inner-class reasons differs from the candidate and is explained above.
- **Gate head versus tip:** the gates ran on `ea613ec3`; HEAD now differs from it only by `.planning/phases/38-release-publication` commits (E-12, E-13 and this SUMMARY), so `git diff ea613ec3 HEAD -- . ':(exclude).planning'` and `-- .planning/scripts` are empty.
- **Not touched:** `STATE.md`, `ROADMAP.md`, `REQUIREMENTS.md` (RLS-03 and RLS-06 can be marked after verification; the user's reading of the JDK note comes first), the broken-windows ledger.
- Origin still holds the user's two `scratch/clt-07-*` branches; they were read, never touched.

## Handoffs

- **Plan 38-05:** the gated SHA for the user pre-checks is `last_non_planning_commit` `927ac07c30274c46b2330d597d417f9f1eda8f8b`; the gated head is `ea613ec35ebade7a2182cac4e6ecd5d86d099def`; the scripts commit (the allow-list) is `cc928221b7f2a19ccad3fa2f160f970b35c671e9`. The pushed SHA may differ from the gated head only under `.planning` outside `.planning/scripts`.
- **Archive size for the dry-run report:** `final_tip_bytes` 13558634 (baseline 12453556); the runner's archive may differ from a local one by a few bytes, so compare with the compare tool, not a digest.
- The allow-list is valid for exactly this tree: any later change of `src`, `build.gradle.kts`, `gradle` or `README.md` makes a row `STALE-ROW` or a difference `UNALLOWED`.

## Next Phase Readiness

Plan 38-05 can start: E-11, E-12 and E-13 are `pass`, the anchors mirror reads `rows=172 bad=0`, origin `custom_build` is unchanged, nothing is pushed or tagged, and the tree is clean apart from the two orchestrator-owned state files.

## Self-Check: PASSED

- Files exist: `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt`, `38-COMMAND-EVIDENCE.md` (entries E-11 to E-13).
- Commits `cc928221`, `ea613ec3`, `3f195713`, `048a9ba2` are ancestors of `HEAD`.
- `P38-04-T1-OK`, `P38-04-T2-OK`, `P38-04-T2-TESTS-OK` and `P38-04-T3-OK` print on the tip before this SUMMARY.

---
*Phase: 38-release-publication*
*Completed: 2026-10-31*
