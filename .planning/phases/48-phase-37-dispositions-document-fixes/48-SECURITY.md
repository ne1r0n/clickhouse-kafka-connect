---
phase: "48"
slug: phase-37-dispositions-document-fixes
status: verified
threats_open: 0
asvs_level: 1
created: "2026-11-06"
---

# Phase 48 — Security

> Threat register verified by gsd-security-auditor against HEAD `13c7e8e3` (31 rows: T-48-01 to T-48-26 plus five per-plan T-48-SC rows; plans 48-01 to 48-05 each carried a `<threat_model>`). Implementation is `git diff 92570854 HEAD` (B0 = `92570854`, "plans and plan-time helpers"). The review-fix commits `72928578` (48 WR-01), `7ae4284c` (48 WR-02) and `a968e486` (48 IN-01) are covered under T-48-06, T-48-08, T-48-17, T-48-18 and T-48-13 and in "Checks Asked For Beyond The Register".
>
> Live re-checks in this audit: full `testUnit` with `DOCKER_HOST=unix:///nonexistent.sock`, all 107 XML files written by this run: 2325 tests / 0 failures / 0 errors / 2 skipped. `ProcessingTest` 14/14, `SourcePlanningIdGuardUnitTest` 18/18, `AllowListWildcardRuleUnitTest` 14/14, `DescribeUnreadableEnumNullBindingUnitTest` 4/4, `ProxySinkTaskStopUnitTest` 6/6, `ClickHouseSinkTaskStopUnitTest` 7/7, `UnvettedRetriableCodesWarnUnitTest` 28/28, `RetriableExceptionPassThroughUnitTest` 40/40, `DeclaredNullabilityTest` 69/69, `DescribeUnreadableEnumMockServerTest` 2/2, `CompareConfluentArchivesScriptUnitTest` 58/58. Per-testcase delta against the E-01 base dump (md5 `2ceab8e0...`, equal to E-01): `removed=0 added=30`; the frozen 25-row expectation reads `match=no`, with the 5 WR-01 cases as the only extras, and the E-19 30-row expectation reads `match=yes`. Doc commits replayed from `48-doc-edits.json` on their parents: 4/4 byte-equal. Helper runs: `secrets` 0 on 12 files, `index` 19 rows, `rows` 18 pass, `anchors` `rows=218 bad=0`, `48-ledger-check.py check` `rows=57 required=57 problems=0`, `48-path-check.py HEAD docs/FORK-DELTA.md` `unresolved=0`, `48-id-scan.py HEAD docs/ROLLOUT-v1.7.0_gg6.md` `hits=0`.
>
> Not re-run: the full suite on ClickHouse `latest` (E-11 and E-17: 2923 / 0 / 0 / 18 skipped; after the review fix the total of 2928 is derived, not measured, per E-19), the ~25 min tail gate (E-11, E-17, E-19 PASS; the verifier also re-ran it) and the mutation runs (their XML snapshots under `/tmp/gsd48/exec` were re-read, see T-48-07).

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| evidence and helper files -> later gates and the verifier | facts are read back by read-only helpers; helpers and check scripts must not drift between calibration and use | counts, hashes, SHAs |
| `src/main` -> every deployment | a production diff beyond the approved 28 IN-01 move would reach operators unreviewed | one constant deletion |
| test JVM platform MBean server -> later tests | a case that leaves a task MBean registered can clash with later cases | MBean names |
| test suite -> the release path | the allow-list rule test reads the shipped lists; a list or script edit would change the published release check | allow-list rows |
| mutation worktrees -> the main tree | a mutation must never reach a commit | mutated sources (scratch) |
| published notes (gg6, gg1) -> operators | instructions, labels and measured values must not change | released text |
| FORK-DELTA -> the next syncer and Phase 49 | a wrong path or false claim misleads the next sync | documentation claims |
| plan-time JSON -> the doc commits | an executor rewrite would bypass the plan's fact check | old/new text pairs |
| ledger rows -> Phase 49 and milestone close | a closed row with no evidence, or a fixed pointer that does not carry the change, misstates the fork's state | IDs, commits, reasons |
| dry-run archives (/tmp) -> criterion 3 | the comparison must use the committed list and never leave a draft where it is read as shipped | archive diffs, draft rows |
| local repository -> origin | must not be crossed | refs |

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-48-01 | Repudiation | base measurements that later deltas rely on | medium | mitigate | E-01 baseline on the untouched base: testUnit 2295/0/0/2 with Docker unreachable, suite 2898/0/0/18 on ClickHouse 26.10.3.7 (digest recorded); the status dumps are kept, and their md5s (`2ceab8e0...`, `3552c431...`) equal E-01's `status_testunit_dump_md5` and `status_suite_dump_md5`; the helper re-reads `xml-base-testunit` as 2295 and `xml-base-suite` as 2898; this audit's live delta is computed against that dump | closed |
| T-48-02 | Tampering | check scripts edited after calibration | medium | mitigate | `48-path-check.py` md5 `641402547d...` and `48-id-scan.py` md5 `0922b7a9ab...` equal E-02's `path_check_md5` and `id_scan_md5`; both were committed once (`1f1fa429`) and `git log 92570854..HEAD` shows no later change | closed |
| T-48-03 | Tampering | plan-time helpers (evidence check, status, doc edits, ledger check) | medium | mitigate | md5s `7123d354...`, `41f14e07...`, `864361fc...`, `77ab37ef...` equal E-01's `helper_md5`, `status_helper_md5`, `doc_edits_md5`, `ledger_check_md5`; no commit since B0 touches them; `48-evidence-check.py` equals Phase 47's copy byte for byte | closed |
| T-48-04 | Information disclosure | evidence text carrying a token or record value | low | mitigate | helper `secrets` reads 0 token shapes on `48-COMMAND-EVIDENCE.md` and the other 11 phase and doc files; a broader scan of all 3255 added lines of the phase diff finds 0 credential shapes (password=, secret=, api key, Bearer, AKIA, PEM, GitHub, Slack, JWT) and 0 record-value shapes (`SinkRecord{`, `Struct{`, `value=`, `key=`); the 46 "token/secret" word hits are path-token and planning prose | closed |
| T-48-05 | Tampering | origin branches and tags (plan 48-01) | high | mitigate | `git ls-remote origin refs/heads/sync/v1.7.0 refs/tags/pre-v1.7.0_gg8 refs/tags/v1.7.0_gg8 'refs/heads/sync/*'` returns nothing; `gh api .../matching-refs/tags/pre-v1.7.0_gg8` length 0, `.../heads/sync/` `[]`, `.../tags/v1.7.0_gg8` length 0; E-01 `origin_pre_tag: absent`, `origin_sync_branch: absent`; `pre-v1.7.0_gg8` exists locally only, at `baa404cd` | closed |
| T-48-06 | Tampering | an allow-list row silencing a whole namespace (UF-37-3) | high | mitigate | `AllowListWildcardRuleUnitTest` (`07e65662`, hardened by `72928578`): `followsTwoLiteralSegments` (`:63`) strips `outer/` (`:41`) and counts non-empty segments ≥ 2; `everyShippedWildcardRowFollowsTwoLiteralSegments` (`:91`) reads every list with the prefix (min 5, `:43`); live 14/14 green; 5 lists, 49 wildcard rows, 0 under `outer/`; `com/*` mutation red 1 (E-04, snapshot re-read 9/1); WR-01 red 3 on the old rule (E-19, snapshot re-read 14/3); the new rule is strictly stricter (any glob it accepts the old rule also accepts); `.github/scripts/compare_confluent_archives.py` unchanged (md5 `a637e0cc...` = E-01) | closed |
| T-48-07 | Repudiation | a pin that passes when the behaviour breaks | medium | mitigate | red sets in E-03..E-08 match the plan (2, 1, 2, 7/7/1, 2, 1); this audit re-read every mutation XML snapshot: `nb` 4/2, `aw` 9/1, `ps` 6/2, `cs-mut1` 7/7, `cs-mut2` 7/7, `cs-mut3` 7/1, `uw` 28/2, `rp` 40/1 | closed |
| T-48-08 | Tampering | existing assertion changed or removed (D-3) | high | mitigate | since B0 and in every commit, numstat deletions are 0 for `ProxySinkTaskStopUnitTest` (+75), `ClickHouseSinkTaskStopUnitTest` (+12), `UnvettedRetriableCodesWarnUnitTest` (+26), `RetriableExceptionPassThroughUnitTest` (+34); `DescribeUnreadableEnumMockServerTest` is byte-identical to B0; the 3 deletions in `72928578` sit in a file new to this phase and replace the rule with a stricter one and the value lists with supersets; live delta `removed=0` | closed |
| T-48-09 | Tampering | a mutation committed or left in the main tree | medium | mitigate | E-03..E-08 record `git worktree add --detach /tmp/gsd48/wt-*` and `worktree remove --force && prune`, `*_worktree_removed: yes`; `git worktree list` shows only the main tree; no `/tmp/gsd48/wt-*` directory remains; `src/main` diff since B0 is only the approved 0/3 deletion; `.github` and `.planning/scripts` unchanged; working tree holds only the pre-existing `.planning/config.json` and `.planning/state.json` changes | closed |
| T-48-10 | Denial of service | MBean left registered by a new case | low | mitigate | unique connector names (`proxy-stop-unregister-failure` `:46`, `proxy-stop-unregister-and-close-fail` `:47`, `task-stop-mbean-precondition` `:52`); `finally { statistics.unregisterMBean(); }` (`ProxySinkTaskStopUnitTest.java:240`, `:264`); `UnregisterFailingStatistics` (`:72`) calls `super.unregisterMBean()` (`:82`) before it throws; the new `ClickHouseSinkTaskStopUnitTest` case (`:250`) uses the try-with-resources `Harness`; live green | closed |
| T-48-11 | Information disclosure | a test message or log assertion carrying a record value | low | mitigate | all messages are fixed constants; the binding test reads only throwable texts from the describe path, against a synthetic schema row; the stub binds `127.0.0.1:0` (`DescribeUnreadableEnumNullBindingUnitTest.java:53`) and stops in `@AfterEach` (`:67-70`); the 7 new or touched class reports contain 0 record or credential patterns | closed |
| T-48-12 | Tampering | production scope of 28 IN-01 (D-3) | high | mitigate | `git diff 92570854 HEAD -- src/main` is `DeclaredNullability.java` only, numstat 0/3 (Javadoc, constant, blank line), status `M`, nothing added; `Processing`, `state/` (incl. `KeeperStateProvider`), `ClickHouseWriter`, `ClickHouseSinkConfig`, `RangeContainer`, `ProxySinkTask`, `ClickHouseSinkTask`, `Utils` have an empty diff; no `src/main` reader of `ESCAPED_NEWLINE` remains | closed |
| T-48-13 | Tampering | an assertion value changed by the repoint | high | mitigate | each repointed file equals its parent once imports are set aside and `DeclaredNullability(.Fixtures).ESCAPED_NEWLINE` is normalised (3/3 equal); the constant text is unchanged (`"\\n"`, `DeclaredNullabilityFixtures.java:14`); `a968e486` changes Javadoc only (E-19 `in01_non_comment_lines_changed: 0`); `DeclaredNullabilityTest` 69/69 live; the two live readers green in the E-11 and E-17 suites (2923/0/0) | closed |
| T-48-14 | Repudiation | a move claimed red-first without a red | medium | mitigate | E-10 `red_compile_exit: 1`, `red_compile_errors: 8` (`DeclaredNullabilityTest`=6, `NullableTupleMatrixLiveTest`=1, `SimpleAggregateTupleRoundTripLiveTest`=1), `red_main_numstat: 0 3`, then green compile; matches the plan | closed |
| T-48-15 | Tampering | the anchors invariant | medium | mitigate | `fork-anchors.txt` unchanged (`.planning/scripts` empty diff); live mirror `rows=218 bad=0`; the zero row `declaredType.replace(ESCAPED_NEWLINE, SPACE)` (`fork-anchors.txt:86`) stays true; tail gate PASS `anchor_rows=218` in E-11, E-17 and E-19 (`a968e486`) | closed |
| T-48-16 | Information disclosure | a planning identifier written into `src/main` | low | mitigate | the production commit adds no line (0 added); `SourcePlanningIdGuardUnitTest` 18/18 green live | closed |
| T-48-17 | Tampering (documentation) | published note edit changing an instruction, label or measured value | high | mitigate | `5453a69c` (gg6, 3/3) and `eadacf83` (gg1, 2/2) replay byte-equal from `48-doc-edits.json` on their parents; the word diff changes only D7 (`Phase 39` -> `` `v1.7.0_gg7` ``), C2 and the notice (FUT-05 -> FORK-DELTA items 17/24) and the gg1 tree qualifiers; no instruction or number changes; gg7 note identical to its tag; `7ae4284c` is a separate review qualification, as the JSON `_note` allows; both notes go to the Phase 49 independent review (U-6) | closed |
| T-48-18 | Repudiation | a document claim the code or records do not support | medium | mitigate | `59c5975e` is an ancestor of `v1.7.0_gg7` and `ClientEndpointAndOptionsPinUnitTest` exists in that tag; FORK-DELTA items 17 (`:250`) and 24 (`:264`) exist as "by decision"; `ReleaseWorkflowGuardUnitTest.CHAIN` (`:107`) exists; the WR-02 wording matches the code: `RECORD_CONTEXT_FAILURE_LOG_FORMAT` (`ClickHouseWriter.java:626`, logged `:1574`), `WRITE_VALUE_FAILURE_LOG_FORMAT` (`:623`, only in the generic catch `:1092-1093`), flag-byte writes in `doWriteCol` outside that catch (`:1279-1309`); E-12, E-13 and E-19 record the read checks | closed |
| T-48-19 | Information disclosure | a planning identifier left in an operator note | medium | mitigate | `48-id-scan.py` on the gg6 note: `hits=0` at HEAD and at `5453a69c`, `hits=3` at B0 (`Phase 39` among them); gg1 `hits=26` at both B0 and HEAD (unchanged, out of DOC-01 scope) | closed |
| T-48-20 | Tampering | published allow-lists or raw numbers edited | medium | mitigate | the gg6 and gg7 lists are `cmp`-identical to their copies in tags `v1.7.0_gg6` and `v1.7.0_gg7`; `.planning/scripts` empty diff since B0, nothing untracked or ignored there; FORK-DELTA `## The raw numbers` section byte-equal to B0's (re-computed) | closed |
| T-48-21 | Repudiation | a ledger row closed with no evidence, or a fixed pointer not carrying the change | high | mitigate | `48-ledger-check.py check`: `rows=57 required=57 problems=0`; `counts`: `fixed=10 closed=47 pointers=14`; all 14 pointers are `commit`, ancestors of HEAD, and `git diff-tree` lists the row's file; 12 are after B0 and equal their evidence keys (`move_commit`, `nb_commit`, `aw_commit`, `ps_commit`, `uw_commit`, `rp_commit`, `cs_commit`, `gg6_commit`, `gg1_commit`, `fd_reviews_commit`), 2 earlier ones (`f13d8378`, `e979004e`) are ancestors; 0 hits for "owner: user" or "(the user)"; `git ls-files '*48-DISPOSITION-LEDGER.md'` prints one path, named in FORK-DELTA item 20 (`:256`) | closed |
| T-48-22 | Tampering | a namespace-silencing allow-list row, or a draft left in `.planning/scripts` | high | mitigate | the draft `/tmp/gsd48/exec/dry-draft-gg8-14.txt` has 14 exact rows and 0 wildcards, outside the repository; `.planning/scripts` holds the same 5 tracked lists and no gg8 file; gg6 and gg7 lists equal their tags; the compare script is unchanged | closed |
| T-48-23 | Repudiation | phase gate results | medium | mitigate | counts come from JUnit XML through the read-only helpers: E-17 `xml-gate-testunit` 2320/0/0/2 and `xml-gate-suite` 2923/0/0/18, re-read here; tail gate PASS at E-17 and at E-19 (`a968e486`); live testUnit here 2325/0/0/2, delta `removed=0 added=30`. **Recorded deviations from the plan-time text, not hidden:** (a) "only the committed 25 rows": the 5 extra rows are the WR-01 review cases, recorded in E-19 (`status_delta_frozen_expected ... match=no`, `status_delta_extended_expected ... match=yes`) and reproduced here; (b) "index must read pass for all 17 entries": the index has 19 rows and E-14 is the one fail row, kept unedited as the superseded first measurement under the amended plan (`50ff0c23`; E-15 `supersedes_entry: E-14`); `index ... 19 --done` therefore reads FAIL on E-14 by design, and every other row passes (`rows` 18 OK) | closed |
| T-48-24 | Tampering | exactly-once `BEFORE`/`AFTER` flag sequence | high | mitigate | `Processing.java`, `sink/state/` (incl. `KeeperStateProvider.java`), `ClickHouseWriter`, `ClickHouseSinkConfig`, `RangeContainer`, `ProxySinkTask`, `ClickHouseSinkTask` and `Utils` have an empty diff since B0; the only production change is 3 deleted lines of `DeclaredNullability.java`; `ProcessingTest` 14/14 in `xml-gate-testunit`, `xml-gate-suite` and `xml-move-suite`, and live here | closed |
| T-48-25 | Tampering | origin branches and tags (plan 48-05) | high | mitigate | same as T-48-05; E-17 `origin_pre_tag_at_gate: absent`, `origin_sync_branch_at_gate: absent`; no push entry in the reflog; no `refs/remotes/origin` ref for sync or gg8; `git branch -r --contains HEAD` is empty; `sync/v1.7.0` has no upstream; `origin/custom_build` is `94834763` (`ls-remote` agrees) | closed |
| T-48-26 | Information disclosure | evidence or ledger text carrying a token or record value | low | mitigate | helper `secrets` 0 on `48-COMMAND-EVIDENCE.md` and `48-DISPOSITION-LEDGER.md`; the broad scan of added lines finds 0 (see T-48-04); the ledger carries IDs, commits, file names and reasons only | closed |
| T-48-SC (01) | Tampering | package installs | low | accept | AR-48-01; the five scripts import only standard-library modules, checked with `sys.stdlib_module_names` (no network module; subprocess calls `git` only) | closed (accepted) |
| T-48-SC (02) | Tampering | package installs | low | accept | AR-48-01; `build.gradle.kts`, `settings.gradle.kts`, `gradle/` (incl. `libs.versions.toml`, wrapper) and `gradle.properties` have an empty diff since B0 | closed (accepted) |
| T-48-SC (03) | Tampering | package installs | low | accept | AR-48-01; same; no jar, class, zip or native binary in the change set | closed (accepted) |
| T-48-SC (04) | Tampering | package installs | low | accept | AR-48-01; documentation-only plan; build files unchanged | closed (accepted) |
| T-48-SC (05) | Tampering | package installs | low | accept | AR-48-01; build files unchanged since B0 | closed (accepted) |

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-48-01 | T-48-SC (01-05) | Nothing installed and no dependency change: `build.gradle.kts`, `settings.gradle.kts`, `gradle/` (incl. `libs.versions.toml` and the wrapper) and `gradle.properties` have an empty diff against B0 `92570854`; no binary artefact in the change set; the phase's Python scripts use only the standard library. | plan disposition (48-01..48-05); auditor | 2026-11-06 |

## Checks Asked For Beyond The Register

- **Review-fix commits.** Closed. `72928578` (WR-01) is test-only: it strengthens `followsTwoLiteralSegments` to strip `outer/` and count non-empty segments, adds 3 refused and 2 accepted cases, and leaves the lists and compare script untouched. Proven stricter: anything the new rule accepts, the old one accepted too. It was red 3 on the old rule, and is green 14/14 here. `7ae4284c` (WR-02) is a one-line FORK-DELTA item 14 rewording; the claim matches `ClickHouseWriter.java:623-626`, `:1092-1093`, `:1279-1309` and `:1574`, and it adds no planning identifier. `a968e486` (IN-01) changes Javadoc only in `DeclaredNullabilityFixtures`. All three are linear on `sync/v1.7.0` (0 merges since B0).
- **No secrets or record data logged.** Closed. The only production change deletes a constant. No logging statement was added in `src/main` or `src/test`. The existing log lines named in FORK-DELTA item 14 carry field type, column, column type, exception class, topic, partition and offset, never the value. New test messages are fixed constants. Report XMLs and all added lines read 0 for credential and record patterns.
- **No dependency change.** Closed under AR-48-01.
- **Nothing outward-facing.** Closed under T-48-05 and T-48-25. Origin has no `sync/v1.7.0`, `pre-v1.7.0_gg8` or `v1.7.0_gg8`. The rollback tag `pre-v1.7.0_gg8` (`baa404cd`, an ancestor of HEAD) is local only.
- **Release path unedited except as the plans allow.** Closed. `.github` (incl. `compare_confluent_archives.py`, md5 = E-01), `.planning/scripts` (5 allow-lists, `fork-anchors.txt`), `VERSION`, `CHANGELOG.md`, `src/integrationTest` and `src/testFixtures` have an empty diff since B0. The gg6 and gg7 lists equal their tags. The gg7 note equals its tag. The only `docs/` edits are the plan-approved gg6, gg1 and FORK-DELTA changes (U-6, U-7, U-10, U-11) plus the review qualification `7ae4284c`.

## Informational (not threats)

- `DeclaredNullability.ESCAPED_NEWLINE` was `public`. Removing it is a source-compatibility change for any external code that referenced it. Nothing in the repository reads it outside `src/test`. The removal was approved by the user (U-4) and handed to the Phase 49 CHANGELOG (ledger hand-off).
- Ledger rows `45-IN-04` and `46-WR-05` and hand-off items 3 and 4 still say "two ERROR lines", which is stale after WR-02 (verifier note 3). FORK-DELTA item 14 is the authoritative text. Pointer integrity is not affected.
- `/tmp/gsd48` still holds scratch from research and execution: plain repo copies `m1..m4` and `tip` with no `.git` and not registered as worktrees, the 14-row draft, and archive and XML snapshots. All of it is outside the repository and on no classpath.
- The working tree has uncommitted `.planning/config.json` (`auto_advance`, `skip_discuss`) and `.planning/state.json` changes. Both predate this audit and are not on the release path.

## Unregistered Flags

None. All five SUMMARY `## Threat Flags` sections read "None". The new HTTP stub (48-02) binds `127.0.0.1` on an ephemeral port and is stopped after each test (T-48-11). Open review items IN-02 (a duplicated MBean test, kept because tests are not removed) and IN-03 (the `.planning/phases/45..47` paths move at the milestone close, ledger hand-off) are advisory and add no attack surface.

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-06 | 31 | 31 | 0 | gsd-security-auditor |

## Sign-Off

- [x] All threats have a disposition
- [x] Accepted risks documented
- [x] `threats_open: 0` confirmed
- [x] `status: verified`

**Approval:** verified 2026-11-06
