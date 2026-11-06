---
phase: 49-release-publication
plan: "02"
subsystem: release
tags: [release, rollout-note, rollback-rehearsal, archive-compare, evidence]
status: complete

requires:
  - phase: 49-release-publication
    provides: plan 49-01 (VERSION v1.7.0_gg8, completed CHANGELOG section, final FORK-DELTA, E-01 to E-04, the measured JUnit snapshots)
provides:
  - docs/ROLLOUT-v1.7.0_gg8.md with 22 labelled effect rows, Before you upgrade, the closes table and the known limits
  - 49-rehearse.sh, the frozen dry-run merge and rollback rehearsal (md5 recorded in E-05)
  - the first measurement of the archive difference set (17 entries) for the allow-list plan
  - evidence entries E-05 to E-07
affects: [49-03, 49-04, 49-05]

actuals:
  tokens: 15744
  tasks: 3
  commits: 8
plan_head_before: d577944d5ba3419a0363b61b8a157002536f8dd4
plan_head_after: cdb872649eb47fd6e5ce54810a9b2871b89903a3

tech-stack:
  added: []
  patterns:
    - "the rollback block extracted from the published note by the checker and run unchanged by a frozen script on a dry-run --no-ff merge in a throwaway detached worktree"
    - "every duration of a timeout row checked against the fork's item 14 and every case count against the JUnit snapshots"

key-files:
  created:
    - docs/ROLLOUT-v1.7.0_gg8.md
    - .planning/phases/49-release-publication/49-rehearse.sh
  modified:
    - .planning/phases/49-release-publication/49-COMMAND-EVIDENCE.md

key-decisions:
  - "Row U1 states a pinned behaviour and no change: the DESCRIBE reader is byte-identical to v1.7.0_gg7 and the four live cases pass on an export of that tag"
  - "Row U2 follows the CHANGELOG and FORK-DELTA wording: the writer's own exception is rethrown and a Jackson link in its cause chain is rebuilt value-free"
  - "The rollback list adds row P1 and the closes table adds two rows (gg7 row R6 and the gg7 CHANGELOG stop() sentence); each is accurate and was left out of the plan's lists"

requirements-completed: [RLS-02, RLS-03]

coverage:
  - id: D1
    description: "the rollback block of the note, extracted by the checker, runs unchanged on a dry-run merge of the committed tip onto the published gg7 commit: prints ok, leaves VERSION v1.7.0_gg7, no copy of the note and a tree equal to pre-v1.7.0_gg8; a revert restores the merged tree; worktree and branch gone"
    requirement: "RLS-03"
    verification:
      - kind: other
        ref: "bash 49-rehearse.sh merge <note_commit_1> <new dir> (MODE attr and MODE plain): rehearsal: PASS twice; P49-02-T1-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "the archive difference set between local rebuilds of v1.7.0_gg7 and the tip is measured: 17 UNALLOWED entries, totals files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498, equal to the projection path for path"
    requirement: "RLS-02"
    verification:
      - kind: other
        ref: "compare_confluent_archives.py with a comment-only allow-list (E-05 compare_unallowed, compare_unallowed_paths, compare_matches_projection: yes)"
        status: pass
    human_judgment: false
  - id: D3
    description: "sections A and B: 14 rows, every duration in item 14 of FORK-DELTA, every case count equal to the JUnit snapshots"
    requirement: "RLS-03"
    verification:
      - kind: other
        ref: "P49-02-T2-OK (49-note-check.py rows --only U T: OK rows=14; durations and case counts)"
        status: pass
    human_judgment: true
    rationale: "the structure, the numbers and the labels' form are checked mechanically; whether each label is true for its source is the fresh-context review of plan 49-03"
  - id: D4
    description: "sections C and D, Before you upgrade, the closes table and the known limits: 22 rows in all, seven mutations each turn the checker red"
    requirement: "RLS-03"
    verification:
      - kind: other
        ref: "P49-02-T3-OK (49-note-check.py all: OK rows=22; ids: hits=0; unresolved=0; links ok; 7 mutations exit 1)"
        status: pass
    human_judgment: true
    rationale: "same as D3: accuracy of each claim against its source is judged by plan 49-03"
---

# Phase 49 Plan 02: The gg8 operator note, the rehearsal script and the first archive compare Summary

`docs/ROLLOUT-v1.7.0_gg8.md` exists with 22 labelled effect rows, and its rollback block, extracted from the note and run unchanged by the frozen `49-rehearse.sh` on a dry-run merge of the committed tip onto the published gg7 commit, printed `ok` in both modes; the archive difference set against `v1.7.0_gg7` is measured at 17 entries, equal to the projection. Nothing was pushed.

Start about 13:18Z, end about 13:38Z on 2026-11-06, about 20 minutes, 3 tasks, 3 files changed (the note, the script, the evidence file).

## Accomplishments

- Task 1 (tracer): `49-rehearse.sh` written to its specification and self-tested (2 of 2 usage errors), both archives rebuilt from clean exports and compared, the `v1.7.0_gg7` release asset read and checked against a download, the note's preamble and section E committed, and the block rehearsed on a dry-run merge in both modes (E-05).
- Task 2: sections A (U1 to U4) and B (T1 to T10); the 28 durations of the T rows occur in item 14 and the 12 case counts stated equal the JUnit snapshots (E-06).
- Task 3: sections C (S1 to S4) and D (P1), Before you upgrade, the table of what the release closes from the gg7 note, the known limits; the full checker, id scan, path resolver, links, hygiene and seven mutations (E-07).

## Facts

| Fact | Value |
|------|-------|
| `rehearse_commit` | `dc91ab0c2b9ae2a1e8fdc022fb073704d1df8257` (the script alone), `rehearse_md5` `67f30f31a1353a607aaed8d8f09767a7` (unchanged at every later verify) |
| note commits | `b46157003f4aed427b3ba9dc613765df17708320` (note_commit_1), `06df048b7ac209ed89fbe005a2e311f4aacf30d6` (note_commit_1b), `48835d5414a4ac0943084ed405b6c9bc253bc535` (note_commit_2), `1ee7017b83b7b589e63d801a717837b8e71ea9d3` (note_commit_3) |
| evidence commits | E-05 `be542eed`, E-06 `88a83176`, E-07 `cdb87264` |
| rehearsed commit | `b4615700` (note_commit_1), parents of the dry-run merge `94834763` and it; both modes `rehearsal: PASS`; `d0_gradlew_bat_trap` `no`; status clean before and after the block |
| `d0_block_sha256` | `2f30bd7d6b0109b22caaba63ff968a02f3f3eedfc11ec8ec4b46474012f67991` (the final note's block hashes the same) |
| archives | `v1.7.0_gg7` rebuild 7932 inner entries, 13564144 bytes; tip rebuild 7935 inner entries, 13572233 bytes; default toolchain, no JVM crash, `-PjavaVersion=21` not needed |
| `compare_unallowed` | 17 (derived expectation 17) |
| `compare_totals` | `totals files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498` (derived expectation equal) |
| `compare_owner_split` | `connector classes 16; manifest 1; anything else 0`; version table 18 libraries, no change; manifest differs only in `Implementation-Version` |
| `compare_matches_projection` | `yes`, path for path |
| gg7 release asset | `clickhouse-kafka-connect-v1.7.0_gg7.zip`, 13564238 bytes, `sha256:3dbdcf03e5825073b7b0535718c9998a5e5754c0ae05a17502bbf4c6d69472af`, uploader `github-actions[bot]`; the download's SHA-256 equals it; the asset against the local rebuild of the tag lists 6 changed connector classes (`files-a 7512 files-b 7512 only-a 0 only-b 0 changed 6 same 7506`), so the note says to download the asset rather than rebuild it |
| source facts | `client_version` default `V1`; insert timeout default `TimeUnit.MINUTES.toMillis(4)` (240000 ms), minimum `ConfigDef.Range.atLeast(100)`; `Version.ARTIFACT_VERSION` read by `ClickHouseHelperClient` and `SinkTaskStatistics`; `src/main` since gg7 `6 files changed, 428 insertions(+), 72 deletions(-)`; `ESCAPED_NEWLINE` in `src/main` 0 |
| rows | 22 (U 4, T 10, S 4, P 1, D 3); by label form `measured` 15, `read from the code` 15, `derived` 1 (a mixed row counts for each form it uses) |
| checks | `OK rows=22`, `OK labels: 3 of 3`, `OK forbidden: 0 lines`, `OK closes: 7 of 7`, `OK rollback`, `OK claims: 0 hex strings outside the allowed set`, `ids: hits=0`, `unresolved=0`, `OK links: 35 relative targets checked, 0 broken` (the CHANGELOG and FORK-DELTA links to the note now resolve), hygiene strict clean, secrets clean |
| mutations | seven, each `exit=1` on its own subcommand (row deleted, T4 scope widened, label `assumed`, an identifier appended to S4, `T14` renamed in the closes table, the `read-tree` tag changed, a 64-hex string appended to D2) |
| gates on the tip | `P49-02-T1-OK`, `P49-02-T2-OK`, `P49-02-T3-OK`; anchors mirror `rows=218 bad=0`; non-`.planning` paths changed since L0 are exactly `CHANGELOG.md VERSION docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg8.md`; `pre-v1.7.0_gg8` still `94834763`; origin `custom_build` still `94834763`; no worktree, no `rollback-v1.7.0_gg8` branch, no `v1.7.0_gg8` tag |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Source strength] Row U1 states a pinned behaviour, not a change**
- **Found during:** Task 2, reading the sources for U1
- **Issue:** the research's U1 reads "fields whose column names contain `/` are no longer lost". `ClickHouseFieldDescriptor.java` is byte-identical at `v1.7.0_gg7` and at `HEAD` (`git diff --quiet` exits 0), and the merge of upstream's tail kept the fork's `fromJsonRow`, so upstream's slash fix changed nothing in this fork's runtime. I copied `SlashColumnRoundTripTest` into a scratch export of `v1.7.0_gg7` (adding the test-scope `connect-json` dependency) and its four live cases passed.
- **Fix:** the row says the reader is the one of `v1.7.0_gg7`, the round trip is measured on both clients and the release adds tests; the label is `round trip on both clients: measured; reader unchanged since v1.7.0_gg7: read from the code`; the at-a-glance item says the behaviour is pinned by tests.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg8.md`
- **Commit:** `48835d54`

**2. [Rule 1 - Source strength] Row U2 does not say "rethrown unchanged"**
- **Found during:** Task 2
- **Issue:** the plan's row text says the writer's exception is "rethrown unchanged"; FORK-DELTA and the CHANGELOG (as corrected in plan 49-01) say a Jackson link in the cause chain is rebuilt without any record value.
- **Fix:** the row says the writer's own exception is rethrown, its class and retry classification unchanged, with a Jackson link rebuilt value-free; "no record value" is limited to the cases the tests drive.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg8.md`
- **Commit:** `48835d54`

**3. [Rule 1 - Source strength] Case counts and a label differ from the research**
- **Found during:** Task 2
- **Issue:** the JUnit snapshots hold 27 cases for `InsertTimeoutHelpTextPinUnitTest` (research 18) and 7 for `ClickHouseSinkTaskStopUnitTest` (research and FORK-DELTA item 13 six). The research's "30 of 31 turn red against upstream's decode-all" has no snapshot behind it.
- **Fix:** the note states the measured 27 and no count for the stop test, and no red-case claim.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg8.md`
- **Commit:** `48835d54`, `1ee7017b`

**4. [Rule 2 - Completeness] Three additions the plan's lists left out**
- **Found during:** Task 1 (rollback list), Task 3 (closes table)
- **Issue:** row P1 is reversed by a rollback (the constant returns to the jar) but the plan's list stopped at D1; gg7 row R6 ("a decision on changing them is still open") and the gg7 CHANGELOG sentence that the task's `stop()` stops everything even when one part throws are both corrected by this release (the gg8 CHANGELOG says so for the second).
- **Fix:** the "Rolling back brings back" list and row D3 name P1; the closes table has an R6 row and a CHANGELOG-`stop()` row, pointing at S3 and S1.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg8.md`
- **Commit:** `b4615700`, `1ee7017b`

**Total deviations:** 4 auto-fixed (3 source strength, 1 completeness). **Impact:** wording only; every plan verify passes and no measured number moved from its derived expectation.

## Open for the orchestrator (review brief)

**Contradictions between the note's sources and CHANGELOG or FORK-DELTA (recorded, not fixed here; plan 49-03 owns them):**

1. **CHANGELOG Bug Fixes bullet on `/` (U1).** It says a field name with `/` "now round-trips" through the `DESCRIBE` reader on both clients. The reader is unchanged since `v1.7.0_gg7` and the four live cases pass on the `v1.7.0_gg7` export (E-06 `slash_cases_on_gg7_export`), so an operator sees no change. Suggested wording: the behaviour is pinned by new tests.
2. **CHANGELOG issue numbers.** It names the two upstream items #838 and #640; FORK-DELTA names upstream's pull requests #839 and #857. A read-only look at upstream shows #838 is the issue behind #839, and #857 is the pull request "include Kafka record context in serialization errors", while #640 is an issue titled "Dates out of a range causes negative number check to fail", which does not obviously belong to #857. The note names the pull requests 839 and 857 and no issue.
3. **Case counts.** FORK-DELTA item 13 says `ClickHouseSinkTaskStopUnitTest` has six cases, two red on the unchanged tree; the snapshot holds seven. The note states no count for it.
4. **`ESCAPED_NEWLINE` location.** The CHANGELOG and FORK-DELTA item 20 say it moved "into the test fixtures"; it is now in `src/test/.../DeclaredNullabilityFixtures.java` (a fixtures class in the test source set, not the `testFixtures` set). The note says "the tests' own fixture class".

**Labels weakened or limited from the research:** U1 (see deviation 1), U2 (deviation 2), S3 (the Connect worker measurement is labelled `measured (Kafka 3.9 line only)`; "no bound on the redelivery" is `read from the code`), T6 (`escape after the send: measured; cut during the send: measured (stub only)`), T8 (the duplicate with `exactlyOnce=false` is `derived`, token reuse `read from the code`), S2 (both-throw is `measured (one probe, not pinned)`).

**Checked from the 49-01 carry:** the FORK-DELTA test-tree row classifies the 28 differing files (14 named, 8, 6; the categories sum to 28), and the 8 files it calls byte-identical to upstream's tail `83808df9` are, by `git diff --quiet` at `HEAD`; the 6 mixed files were not re-derived (plan 49-03's check). The note does not cite that row. The CHANGELOG record-context wording ("a Jackson link rebuilt value-free") is matched by row U2.

**Other facts the reviewer should know:** `d0_gradlew_bat_trap` was `no` (status clean after the block). The archive builds ran on the default toolchain with no crash (`-PjavaVersion=21` not needed). The U1 measurement used a scratch export of `v1.7.0_gg7` with the test copied in and the `connect-json` test dependency added; nothing from scratch is committed. The heading of the CHANGELOG stays `Unreleased`. The note holds no planning identifier (checker `forbidden`, id scan `hits=0`) and no host or connector name; the retry-budget decision is stated in words and pointed at items 8 and 24.

## Handoffs

- **Plan 49-03 (review):** the five files under review are `docs/ROLLOUT-v1.7.0_gg8.md`, the `1.7.0_gg8` section of `CHANGELOG.md`, `docs/FORK-DELTA.md` (diff base `94834763`), and the gg6 and gg1 notes as edited earlier. Deliberate items: the three label forms and one qualifier; every client V1 claim scoped to V1 on purpose; T6 carried as live-measured for the escape and stub-only for the cut; U1 as a pinned, unchanged behaviour; the closes table's two extra rows and the P1 line of the rollback list; the retry-budget decision named in words only; the heading date rule. Contradictions 1 to 4 above are for it to rule on.
- **Plan 49-04 (allow-list):** `compare_unallowed` 17 and `compare_unallowed_paths` (the 14 exact connector paths of the earlier measurement plus `sink/Version.class`, `util/jmx/SinkTaskStatistics.class` and `META-INF/MANIFEST.MF`), totals `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`, version table unchanged, to be regenerated on the final tree and matched; the note's row D2 states 7932 and 7935 entries, 13564144 and 13572233 bytes and the 17 entries, and must be re-read if the final tree changes the archive.
- **Plan 49-05 (publication):** `49-rehearse.sh` with `rehearse_md5` `67f30f31a1353a607aaed8d8f09767a7`; `bash 49-rehearse.sh merge <TIP> <new dir> [<message file>]` and `at <COMMIT> <new dir>`, `MODE=attr|plain`; the note's block hashes to `2f30bd7d6b0109b22caaba63ff968a02f3f3eedfc11ec8ec4b46474012f67991` and must still hash to it. Row D3 and the rehearsal paragraph name the rehearsed commit `b4615700` as an earlier commit than the release tip; the merge-commit rehearsal on the real tip is plan 49-05's.
- **Orchestrator:** STATE.md, ROADMAP.md and REQUIREMENTS.md were not touched by this plan (hard rule); `.planning/config.json` and `.planning/state.json` stay modified and unstaged.

## Self-Check: PASSED

Created files exist (`docs/ROLLOUT-v1.7.0_gg8.md`, `.planning/phases/49-release-publication/49-rehearse.sh`); the eight commits `dc91ab0c`, `b4615700`, `06df048b`, `be542eed`, `48835d54`, `88a83176`, `1ee7017b` and `cdb87264` are ancestors of `HEAD`; `P49-02-T1-OK`, `P49-02-T2-OK` and `P49-02-T3-OK` printed on the committed tip; `md5` of `49-rehearse.sh` equals E-05; no worktree, rollback branch or `v1.7.0_gg8` tag remains; nothing was pushed.
