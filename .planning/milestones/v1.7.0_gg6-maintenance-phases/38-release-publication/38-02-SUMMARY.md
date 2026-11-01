---
phase: 38-release-publication
plan: "02"
subsystem: release
tags: [rollout-note, labels, evidence, rollback-rehearsal, archive-remeasure]

requires:
  - phase: 38-release-publication
    provides: "plan 38-01: VERSION v1.7.0_gg6, the CHANGELOG section, final FORK-DELTA and evidence E-01..E-04"
  - phase: 37-ci-hardening-advisory-debt
    provides: "compare_confluent_archives.py, the evidence helper and the release record of the gg5 asset"
provides:
  - "docs/ROLLOUT-v1.7.0_gg6.md: the operator note with 29 labelled effect rows (B1-B8, C1-C3, D1-D14, E1-E4), the four defined labels, a six-step Before-you-upgrade list, the custom_ item as an upgrade-breaking change with its search recipe, the zstd platforms, the replay statement, a rollback whose fenced block ran unchanged on a dry-run merge, and the carried known limits"
  - "docs/ROLLOUT-v1.7.0_gg5.md line 111 corrected: the gg4 tag has no release asset"
  - "38-COMMAND-EVIDENCE.md entries E-05..E-07 (archive numbers, eight green pin classes, source reads, the custom_ rejection measured, final labels, rehearsal keys)"
affects: [38-03, 38-04, 38-05]

plan_head_before: 80225b7243f8fa1bd5ef88ae294ddecea3477fbd
plan_head_after: fda39e174383bce737f99c3d89173c91e4ad801e

actuals:
  tokens: 15467
  tasks: 3
  commits: 5

tech-stack:
  added: []
  patterns:
    - "one table row per inventory ID with the label of that row's evidence; split labels (claim: label; claim: label) where one row makes claims of different strength"
    - "rollback block extracted from the published note by script and run unchanged in a throwaway detached worktree"

key-files:
  created:
    - docs/ROLLOUT-v1.7.0_gg6.md
  modified:
    - docs/ROLLOUT-v1.7.0_gg5.md
    - .planning/phases/38-release-publication/38-COMMAND-EVIDENCE.md

key-decisions:
  - "Two labels are weaker than the E-05 decision, never stronger: D6 is split (insert and queryId texts read from the code; the non-ClickHouse HTTP error text keeps the research label) and D7 gains a third claim (task failure read from the code)"
  - "Step 1 of the note's rollback no longer claims a gradlew.bat exception (a fresh worktree of the release tip lists nothing); steps 2 and the forward path say that after the rollback git switch and git revert need git --attr-source with the empty tree"
  - "The custom_ rejection probe needs CLICKHOUSE_SKIP_USER_SETUP=1 so the image's default user answers through the published loopback port"

requirements-completed: [RLS-02]

coverage:
  - id: D1
    description: "docs/ROLLOUT-v1.7.0_gg6.md holds each of the 29 inventory rows once, each with one of the four defined labels, the four labels defined in backticks before the first section, no planning identifier, no destructive SQL, no private address, no unlisted host and no hex string outside the two allowed values"
    requirement: "RLS-02"
    verification:
      - kind: other
        ref: "38-note-check.py all docs/ROLLOUT-v1.7.0_gg6.md (OK rows=29 measured=20 code=15 derived=3 research=4; labels 4 of 4; forbidden 0; claims 0); plan verify P38-02-T3-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "The note's rollback block, extracted and run unchanged on a dry-run merge of the release tip onto 3a3a15a9, prints ok, leaves VERSION at v1.7.0_gg5, removes the note, matches the tag's tree, and a revert restores the merged tree"
    requirement: "RLS-02"
    verification:
      - kind: other
        ref: "38-COMMAND-EVIDENCE.md E-07 rehearsal keys; plan verify P38-02-T3-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "Both archives rebuilt from the tag export and the release tip equal the research on every figure; eight Docker-free pin classes are green with Docker unreachable; the public client sources and one throwaway container settled the research-only labels"
    requirement: "RLS-02"
    verification:
      - kind: other
        ref: "38-COMMAND-EVIDENCE.md E-05; plan verify P38-02-T1-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "Every row's label is no stronger than its evidence and the prose of each row says what the label claims"
    verification: []
    human_judgment: true
    rationale: "Whether a label is true for its source is the independent review of plan 38-03, not a helper's job"

duration: 15min
completed: 2026-10-31
status: complete
---

# Phase 38 Plan 02: The rollout note Summary

**docs/ROLLOUT-v1.7.0_gg6.md now states all 29 operator-visible effects of the release with the label each earns (20 rows carry measured, 15 read from the code, 3 derived, 4 measured in research), flags `custom_` keys as the one upgrade-breaking item with a search recipe and a measured server rejection, and carries a rollback whose own block ran unchanged on a dry-run merge.**

## Performance

- **Duration:** about 15 min (12:55Z to 13:10Z)
- **Tasks:** 3 (all auto)
- **Files:** 3 (the note, the gg5 note, the evidence file)

## Accomplishments

- E-05: both archives rebuilt the way the release workflow builds them (`createConfluentArchive` on an export of tag `v1.7.0_gg5` and of the release tip), eight Docker-free pin classes green with Docker unreachable (60 tests), the public 0.11.0 and 0.9.6 client sources read, one loopback-only throwaway container measured the `custom_` rejection, and every research-only row got its final label.
- E-06: the note's frame, the four labels defined before the first section, the six-step upgrade list and sections B and C (rows B1-B8, C1-C3), committed alone.
- E-07: sections D and E, the release identity, the rollback (block extracted from the note and run unchanged), the known limits, and the one-line correction of the gg5 note; the note's own gate prints four OK lines.

## Task Commits

1. **Task 1: re-measures** - `174d5682` (E-05)
2. **Task 2: note part 1** - `3c1f0031` (note), `d3bf1885` (E-06)
3. **Task 3: note part 2** - `7112974f` (note and gg5 correction), `fda39e17` (E-07)

**Plan metadata:** the commit that carries this SUMMARY.

## Labels

Per-label row counts from `38-note-check.py rows` (a split row counts under each label it uses): measured 20, read from the code 15, derived from Kafka consumer-group semantics (not measured) 3, measured in research (not re-run) 4.

- measured (20): B1, B5, B6, B7, B8, C1, D1, D2, D3, D4, D5, D7, D9, D11, D12, D13, D14, E1, E3, E4 (the split rows carry a second label too).
- read from the code (15): B5, B6, B8, C2, C3, D5, D6, D7, D8, D9, D10, D11, E2, E3, E4.
- derived (3): B2, B3, B4.
- measured in research (not re-run) (4): D2 (cadence), D6 (non-ClickHouse HTTP error text), D8 (client-side failure), D9 (worker behaviour).

Strengthened by E-05 (key that earned it):

- D5 tail: research to read from the code (`ua_format_found: yes`, the strings `lv:java`, `os:` and `hc:ahc_` in `WorkloadIdentitySupport` of the 0.11.0 sources, and `jvm:` and `Apache-HttpClient` in the 0.9.6 sources).
- D6 insert and queryId texts: research to read from the code (`log_text_found: yes`, `log_old_text_site`).
- D7 rejection: research to measured (`d7_rejection_measured: yes`, answer `Code: 115 ... UNKNOWN_SETTING` from a throwaway ClickHouse 26.10.3.7 container).
- D8 default method: research to read from the code (`compression_default_value: ZSTD`).
- D10: stays read from the code, now backed on both client lines (`socket_options_site`, `socket_options_old_default`).

Weakened or split against the E-05 decision (E-07 `label_reasons`): D6 (the 0.9.6 sources hold no text for the old non-ClickHouse HTTP error, so that claim keeps the research label) and D7 (a third claim, task failure, is read from `Utils.DEFAULT_RETRIABLE_ERROR_CODES`).

## Archive numbers

From E-05, on archives built from an export of tag `v1.7.0_gg5` and of the release tip (all equal to the planning-time research):

- inner entries: 7801 (gg5) and 7933 (tip), delta 132
- bytes: 12453556 and 13558634
- SHA-256 (recorded only): `efa6cca4f30d182eea52565f331581f2c4ccee30c99e91ce0f053b341629e0fe` and `a1b9108859fc73b84ad6ac6931c387bd10800dde9fec8b6a488e8bb5b402f1cf`
- compare with a comment-only list: exit 1, `totals files-a 7399 files-b 7513 only-a 4 only-b 118 changed 120 same 7275`, 242 `UNALLOWED` lines, one differing version line (`at.yawk.lz4:lz4-java 1.12.0 -> 1.12.1`, of 18 `VERSION` lines in all), `MANIFEST.MF: differs only in Implementation-Version`, nested jars in b 0, Kafka classes in b 0
- the archives are kept in `/tmp/gsd38/w/arch/` (`base.zip`, `tip.zip`, `empty.txt`) for plan 38-04's cross-check

## Facts

- Eight pin classes (60 tests, 0 failures, `DOCKER_HOST` unreachable): `BufferedOffsetCommitUnitTest` 15, `AdditionalRetriableErrorCodesPinUnitTest` 19, `ClientEndpointAndOptionsPinUnitTest` 4, `ClientErrorShapePinUnitTest` 7, `ClientInsertWirePinUnitTest` 2, `ClientTimeoutPrefixPinUnitTest` 2, `ClientCatalogPinUnitTest` 2, `NetworkCompressionPinUnitTest` 9. No full suite was run: the plan changes no Java file.
- Upstream states read (GET): issue 777 `open`; pull request 786 `closed`, `merged: false`. `src/main` since gg5: 7 files, 112 insertions, 31 deletions.
- Scope: `git diff --name-only L0 HEAD -- . ':(exclude).planning'` lists exactly `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg5.md`, `docs/ROLLOUT-v1.7.0_gg6.md`; no src, test, build, gradle or `.github` change; nothing pushed, no tag, no mutating `gh` call, no image pulled.
- The probe container ran by digest with `--pull=never`, published on loopback only, and was removed; the image digest was unchanged.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The probe container needed a user setup option**
- **Found during:** Task 1, step 4.
- **Issue:** the image's `default` user refuses any request that arrives through the published port (error 194, a password is required), with or without the `custom_` parameter.
- **Fix:** `-e CLICKHOUSE_SKIP_USER_SETUP=1` on the same loopback-only throwaway container, started by digest; the control `SELECT 1` answered `1` and the `custom_myparam` request answered error 115. The first attempt (answered 194) is recorded and not used.
- **Commit:** `174d5682`.

**2. [Rule 2 - Missing critical] The 0.9.6 client sources were read as well**
- **Found during:** Task 1, step 3.
- **Issue:** rows D5, D6 and D10 each claim something about the old client line, and the plan reads only the 0.11.0 jar; a label must not be stronger than what was read.
- **Fix:** the public 0.9.6 `client-v2` sources jar was downloaded into its own directory and read as data (`python3 -I`, `zipfile`, never extracted); the extra keys are `ua_old_format_site`, `log_old_text_site`, `socket_options_old_default`.
- **Commit:** `174d5682`.

**3. [Rule 1 - Bug] Two labels weakened against E-05 (D6, D7)**
- **Found during:** Task 3, writing the rows.
- **Issue:** the old text of a non-ClickHouse HTTP error is not in the 0.9.6 sources, and that a code 115 task failure follows is code reading, not a measurement.
- **Fix:** D6 and D7 carry split labels (see the Labels section); both changes only weaken a label; recorded as `label_d6_final`, `label_d7_final` in E-07.
- **Commit:** `7112974f`, `fda39e17`.

**4. [Rule 1 - Bug] The note's rollback guidance about `gradlew.bat` was stale for gg6**
- **Found during:** Task 3, the rehearsal.
- **Issue:** the plan asks for the previous note's `gradlew.bat` exception "exactly as worded". The release tip has the line endings normalised, so a fresh checkout lists nothing, while the rolled-back tree (the tag's CRLF blob) lists ` M gradlew.bat` and then `git switch` and `git revert` stop with "local changes would be overwritten". The first two rehearsal attempts did not even merge for the same reason (the merge aborted on that file, and the block then had nothing to commit because HEAD was the tag).
- **Fix:** the rehearsal ran the merge and the revert with `git --attr-source=<empty tree>` (the block itself ran plain and printed `ok`); the note's step 1 drops the stale exception, and step 2 and the forward path say to run `git switch` and `git revert` as `git --attr-source="$(git hash-object -t tree /dev/null)" ...` (git 2.40 or later); the forward path (switch back, fast-forward, revert) was measured with and without the option. Recorded in E-07 `gradlew_bat_finding`.
- **Commit:** `7112974f`.

**5. [Plan wording] Time-scoped verify clauses**
- **Found during:** the plan-level verification (the pattern of 38-01 deviation 4).
- **Issue:** T1 requires the non-`.planning` change set to be exactly `CHANGELOG.md VERSION docs/FORK-DELTA.md`, T2 requires `rows --only B C` and `claims --allow GG5` to pass; all three become false once task 3 lands (the note then holds D and E rows and the gg5 asset digest, and two more docs changed).
- **Fix:** each verify passed as written at its own task tip (`P38-02-T1-OK`, `P38-02-T2-OK`, `P38-02-T3-OK`); at the final tip T3 passes as written and T1 and T2 pass with only those clauses adjusted or dropped.

**6. [Plan wording] `compare_version_table` is the one differing line**
- **Issue:** the compare tool prints 18 `VERSION` lines and 17 read the same version on both sides; the plan's planning-time value is the single differing line.
- **Fix:** the key holds that line and `compare_version_lines_total: 18` records the rest.

**Total deviations:** 6 (1 blocking, 1 missing-critical, 2 bugs, 2 plan-wording). **Impact:** none on scope; two labels are weaker than planned, the rollback guidance is more accurate than planned.

## Issues Encountered

- Swap use was high (about 16 of 17 GB) at the start; no OpenJ9 crash, javacore or core dump appeared in the three Gradle runs (two archive builds and the pin run).
- Other directories (`hyg-verify-*`, `hygiene-selftest`) appeared under `/tmp/gsd38/w/` while this plan ran; they are not this plan's and were left alone.

## Known Stubs

None.

## Threat Flags

None. The note adds no endpoint, auth path or file access; the one container was loopback-only and removed.

## User Setup Required

None - no external service configuration required.

## Open for the reviewer

- Rows that still say `measured in research (not re-run)`: D2 cadence (needs a real server with a one-second `max_execution_time` on both client lines), D6 non-ClickHouse HTTP error text (the old text is not in the public 0.9.6 `client-v2` sources, so it needs a stub probe), D8 client-side failure (a 1,000,000-byte incompressible body through a probe) and D9 worker behaviour (needs a Connect worker image and a platform matrix).
- Upstream states read: issue 777 `open`, pull request 786 `closed` and `merged: false`; the note says they are re-read when the release is published (plan 38-05 should re-read them).
- The gg5 correction changed exactly one line (111: the gg4 tag has no release asset, the only asset so far is gg5's). The same note's section 2 (line 39) still says "No GitHub release is published for `v1.7.0_gg5`: the release is its two tags", which now contradicts line 111 and the published release. The plan allows one changed line, so it was not edited: the orchestrator should decide whether to extend the correction (a one-clause edit).
- The rollback guidance about `gradlew.bat` (deviation 4) goes beyond the plan's wording and should be read by the reviewer; the claim "a fresh checkout of the release tip lists nothing" was measured on the release tip, not on the merge commit.
- B8's Direct-path and many-to-one claims are labelled measured, but the probe that measured them is a scratch test of the maintainers that is not in the repository (the note says so).

## Handoffs

- **Plan 38-03 (independent review):** files in scope are `docs/ROLLOUT-v1.7.0_gg6.md`, `docs/ROLLOUT-v1.7.0_gg5.md` (line 111), the `1.7.0_gg6` section of `CHANGELOG.md` and `docs/FORK-DELTA.md`; the evidence map is `38-COMMAND-EVIDENCE.md` E-05 (labels and archive numbers), E-06 (B and C rows) and E-07 (rehearsal, label reasons, scope). The completeness checklists are `35-ROLLOUT-ITEM.md`, `36-ROLLOUT-ITEMS.md` and the FUT-05 item of `34-03-SUMMARY.md`; label strength must match each source.
- **Plan 38-04:** the archive numbers above and `/tmp/gsd38/w/arch/` hold the research-equal figures; any later change to `src`, `build.gradle.kts`, `gradle` or `README.md` moves them.
- **Plan 38-05:** the rollback rehearsal is repeated on the final dry-run merge; the merge onto `3a3a15a9` aborts on ` M gradlew.bat` in a fresh worktree unless git's attributes are switched off (`git --attr-source=<empty tree> merge ...`), and the same applies to the revert; the note's block itself runs plain. The two upstream states are re-read at publication.
- **Orchestrator wording changes:** REQUIREMENTS.md RLS-02 and ROADMAP Phase 38 criterion 2 should name the four labels (`measured`, `read from the code`, `derived from Kafka consumer-group semantics (not measured)`, `measured in research (not re-run)`); nothing else changes.

## Next Phase Readiness

Plan 38-03 can start: the note is complete and gated, the tree is clean apart from the two orchestrator-owned state files, the anchors mirror reads `rows=172 bad=0`.

## Self-Check: PASSED

- Files exist: `docs/ROLLOUT-v1.7.0_gg6.md`, `docs/ROLLOUT-v1.7.0_gg5.md`, `38-COMMAND-EVIDENCE.md`.
- Commits `174d5682`, `3c1f0031`, `d3bf1885`, `7112974f`, `fda39e17` are ancestors of `HEAD`.
- `P38-02-T1-OK`, `P38-02-T2-OK` and `P38-02-T3-OK` print (T1 and T2 at their own tips and, with the time-scoped clauses adjusted, at the final tip).

---
*Phase: 38-release-publication*
*Completed: 2026-10-31*
