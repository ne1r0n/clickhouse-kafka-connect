---
phase: "24"
slug: "release-publication"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-20"
---

# Phase 24 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Approved bundle -> published tree | What the user approved (X0) must be what is published (M's tree) | — |
| Built and extracted archives -> scripts | Extracted content is untrusted data | — |
| CHANGELOG and FORK-DELTA -> operators and the next syncer | Both are read without the evidence beside them | — |
| Disposition files -> milestone audit | The audit reads them to decide what the milestone leaves open | — |
| Evidence -> note | A statement without a transcript is a citation, not a measurement | — |
| Evidence -> operator queries | Operators will run the doc-form queries on production servers | — |
| GitHub REST API -> CI evidence | Unauthenticated and rate-limited; a rate-limit body must not read as a result | — |
| GitHub REST API -> CI evidence | Unauthenticated and rate-limited; Dependabot's dynamic runs share the default-branch SHA | — |
| Local custom_build and tags -> public fork | Pushes to a protected, published branch and tag list are only reversible forward | — |
| Local gate -> release decision | A cached or echoed gate result would let an untested tree through | — |
| Local refs -> origin | Local tags and branches other than sync/v1.7.0 must not reach origin | — |
| Measurement -> operator documents | Every later statement rests on these transcripts | — |
| origin/custom_build -> pre-merge tip | Dependabot merges on origin can move the base under the gate | — |
| Probe -> ClickHouse server | DDL and inserts run against a server; only the run's own container may be touched | — |
| Research expectations -> measured evidence | Research numbers are hypotheses until re-measured | — |
| Reviewer output -> approval bundle | The user approves publication partly on these verdicts | — |
| Rollout note -> operators' production systems | Operators run the quoted commands and queries against their servers and state | — |
| Security records -> the approval bundle | The user approves publication partly on threats_open | — |
| Test and probe output -> committed evidence | Text captured from the connector could carry record data | — |
| User -> orchestrator -> commands | Only a direct user answer authorises the publication | — |
| User -> orchestrator -> push | Only a direct user answer authorises the rehearsal push | — |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-24-01 | Information Disclosure | D-03 test, probe and evidence text | medium | mitigate | Four fixed sentinels and constant values only; evidence quotes sentinel-only excerpts; D03-STATIC-OK checks the sentinels exist and Mockito, skips and assumptions do not. Evidence: D03-STATIC-OK re-run; 0 value sentinels in d03-live.xml; E-03 excerpts sentinel-only. | closed |
| T-24-02 | Tampering | VERSION bumped without its anchor row | medium | mitigate | One commit for both files; VERSION-GG3-OK reads that commit's file list and its exact anchor lines; ANCHORS-MIRROR-OK reads rows=69 bad=0. Evidence: VERSION-GG3-OK (`3c62abe2` touches only VERSION and the anchor row); ANCHORS-MIRROR-OK rows=69 bad=0. | closed |
| T-24-03 | Repudiation | a Map value appearing but the test adapted to pass | high | mitigate | D-04 branch 3 stops before any commit at a blocking-human checkpoint; D-14 fixes the matrix to Connect-typed values; the branch is recorded once in E-03. Evidence: Exactly one `D-04-BRANCH=1` (E-03); value-sentinel assertions in `JsonAsStringKeyResidualLiveTest`; single commit `98b9e04d`, green in both gate runs and on CI. | closed |
| T-24-04 | Tampering | extracted archive content executed or imported | low | mitigate | New empty directories per archive; scripts live in `/tmp/gsd24-scripts`; Python runs with `-I` and takes paths as arguments. Evidence: Archives unpacked into new `/tmp/gsd24/x-*` directories; scripts in `/tmp/gsd24-scripts`; `python3 -I` with path arguments (E-04). | closed |
| T-24-05 | Tampering | fixture DDL and probe inserts | low | mitigate | The probe runs only inside Gradle test runs against the Testcontainers ClickHouse of that run; no connection settings outside `ClickHouseBase` are used. Evidence: Probe extends `ClickHouseBase`; every client from `getBaseProps()` (E-10..E-16). | closed |
| T-24-06 | Repudiation | a disclosure worded from research instead of a measurement | medium | mitigate | Every verify encodes the research answer and fails on a different measurement; a different measurement stops the plan, and a changed basis of D-12, D-13 or D-15 goes to the user. Evidence: D09-OK, D15-OK, QUERIES-OK re-run against the retained XML; narrowed shape predicate recorded in E-13. | closed |
| T-24-07 | Tampering | an operator query that writes or destroys state | high | mitigate | Doc-form queries are single SELECT statements over `system.columns` or the state table; plan 24-03's read-only scan rejects any other statement in the note. Evidence: Four doc-form queries are single SELECTs (E-11..E-16); ROLLOUT-T3-OK `non-read-only-sql []`. | closed |
| T-24-08 | Tampering | a note command that destroys the shared KeeperMap state | high | mitigate | Only the read-only state query is quoted; ROLLOUT-T3-OK rejects any non-SELECT statement in an sql block; ROLLOUT-FINAL-OK rejects a TRUNCATE instruction in docs, CHANGELOG and README. Evidence: Only state query is a read-only SELECT (rollout note item 4); ROLLOUT-T3-OK / ROLLOUT-FINAL-OK `truncate-bad []`; MERGED-TREE-NO-TRUNCATE-INSTRUCTION on M (E-71). | closed |
| T-24-09 | Information Disclosure | hosts, credentials or record values in the note | medium | mitigate | Placeholders only; secret-pattern scan in ROLLOUT-FINAL-OK; scratch directories hold constants. Evidence: ROLLOUT-FINAL-OK `secrets []`; placeholders only. | closed |
| T-24-10 | Repudiation | a headline that overclaims value-free error text | medium | mitigate | ITEMS-1-7-OK requires the D-14 sentence (or the branch-2 sentence) matching E-03's D-04-BRANCH line and the server version. Evidence: ITEMS-1-7-OK: branch 1, 26.9.19.9 in item 1, D-14 sentence present. | closed |
| T-24-11 | Tampering | the rollback demo touching origin or leaving refs behind | medium | mitigate | The tag stays local and is checked absent from origin; the demo only fetches; its worktrees and branch are checked removed. Evidence: `rollback-demo.sh` only fetches; `pre-v1.7.0_gg3` absent from origin until 24-07 (rehearsal tag snapshots); no worktree or demo branch left. | closed |
| T-24-12 | Repudiation | the three documents contradicting each other (Phase 20 WR-02) | medium | mitigate | One canonical wording per fact taken from the note; consistency.py and RAW-NUMBERS-GG3-OK fail on a disagreement. Evidence: RAW-NUMBERS-GG3-OK; `consistency.py` 0 failures (E-33). | closed |
| T-24-13 | Tampering | released CHANGELOG sections rewritten | medium | mitigate | CHANGELOG-GG3-OK compares everything from the gg2 heading down with P24_BASE byte for byte. Evidence: CHANGELOG-GG3-OK `tail-unchanged True`. | closed |
| T-24-14 | Repudiation | 22-SECURITY.md's threat count or structure changed while rewording R-22-02 | low | mitigate | FORK-DELTA-ROWS-OK requires `threats_open: 0`, exactly one R-22-02 row and its original cell count. Evidence: 22-SECURITY `threats_open: 0`, one R-22-02 row, cell count unchanged (`177f247c`). | closed |
| T-24-15 | Repudiation | findings silently dropped by a wrong heading level (Phase 15) | medium | mitigate | Heading rule given to the reviewer; REVIEW-24-OK rejects deeper finding headings and a total that differs from the heading count; DISPOSITION-24-OK requires one row per heading. Evidence: REVIEW-24-OK (5 `###` headings = total 5, `too-deep []`); DISPOSITION-24-OK. | closed |
| T-24-16 | Tampering | a review fix widening into src/main or reversing a locked decision | high | mitigate | Out-of-scope fixes stop at a blocking-human decision; DOCS-FROZEN-OK asserts src/main, src/testFixtures, the build, .github and the invariant scripts unchanged since P24_BASE. Evidence: DOCS-FROZEN-OK; fixes `0c08e663`, `cd40803c`, `6ee4a22a` touch docs and CHANGELOG only. | closed |
| T-24-17 | Repudiation | review findings closed without a reason, owner or trigger | medium | mitigate | CLOSE-RECORD-OK requires all four fields per carried finding; DISPOSITIONS-21-23-OK requires every deferred row to name the close record and every fixed row to name a real commit. Evidence: CLOSE-RECORD-OK (17 findings, four fields each); DISPOSITIONS-21-23-OK. | closed |
| T-24-18 | Elevation of Privilege | a push on a relayed or absent approval | high | mitigate | Task 2 is `gate="blocking-human"`; Task 3 carries a precondition requiring the verbatim direct answer and runs inline in the orchestrator. Evidence: Task 2 `blocking-human`; verbatim `push` in E-62 before the push; push ran in the orchestrator only (session transcript). | closed |
| T-24-19 | Information Disclosure | local tags or other branches reaching origin | high | mitigate | One explicit refspec; push config checked by PUSH-READY; REHEARSAL-PUSHED-ONLY compares heads and byte-compares tags before and after. Evidence: One refspec (E-62); push config unset; REHEARSAL-PUSHED-ONLY, tag snapshots byte-identical. | closed |
| T-24-20 | Repudiation | a gate result read from Gradle's cache or from run 1 | medium | mitigate | `--rerun` on every run, result directories deleted first, XML newer than the last code commit, run 2 later than run 1's copied XML, exact totals. Evidence: `:test`/`:testUnit` executed, 0 FROM-CACHE; run 1 and run 2 each 90 files / 1281 tests, stale 0, run 2 later (E-60). | closed |
| T-24-21 | Spoofing | CI evidence from a rate-limit body, another run or a cancelled sibling | medium | mitigate | Locator keyed on head_sha plus workflow path, a workflow_runs type check, exactly one run, zero cancelled. Evidence: REHEARSAL-RUN-GREEN; API `head_sha=X0`: exactly one `tests.yaml` run, `run_attempt` 1, none cancelled. | closed |
| T-24-22 | Tampering | a test weakened or skipped to pass the gate | high | mitigate | Skip set compared by name against the 18 named skips; per-class counts and the suite total asserted; a failure stops the plan. Evidence: 18 named skips in both runs; exact per-class counts; no existing test changed since P24_BASE. | closed |
| T-24-23 | Tampering | publishing on a base that moved (a slipped Dependabot merge) | high | mitigate | FREEZE-OK fetches and requires origin custom_build = 6473e24f with left count 0; PUSH-READY re-reads it; a move stops at a blocking-human decision (D-10) whose options are `absorb` (the locked recipe via a `/gsd-plan-phase 24` planning run that re-runs every gate on the new tip) or `hold`, with nothing pushed until the answer, recorded in E-63. Evidence: FREEZE-OK and PUSH-READY read origin `custom_build` = 6473e24f, left 0 (E-60, E-61, E-62); re-read before the merge push (E-70, E-71); no E-63/E-73. | closed |
| T-24-24 | Information Disclosure | a CI-log token printed or stored in files | medium | mitigate | Offered only if a log is needed; user-made, read-only, one repository, one day; read inline only; revocation listed in the close record. Evidence: No token needed or used; revocation listed in the close record. | closed |
| T-24-25 | Elevation of Privilege | publication on a relayed or absent approval | high | mitigate | Task 1 is `gate="blocking-human"`; Tasks 2 and 3 carry preconditions requiring the verbatim direct answer; the orchestrator runs them inline. Evidence: Task 1 `blocking-human`; verbatim `publish` in E-70 before the first push; pushes ran in the orchestrator only (session transcript). | closed |
| T-24-26 | Tampering | a fast-forward or force push replacing the reviewable merge | high | mitigate | Explicit `--no-ff` merge; MERGE-SHAPE-OK asserts two parents and M^1 = 6473e24f before the push; no force flag in any push command; the local ff-only of the stale local branch is never pushed by itself. Evidence: M `3376a18f` has parents 6473e24f and 8b1c318d; MERGE-SHAPE-OK before the push; push output `6473e24f..3376a18f`; no force flag in any push. | closed |
| T-24-27 | Tampering | publishing a tree other than the rehearsed and approved one | high | mitigate | Merge X by hash; MERGE-SHAPE-OK asserts tree(M) = tree(X) and X0..X planning-only; the no-TRUNCATE scan and the rollback re-check run on M before the push. Evidence: Merge of X by full SHA; tree(M) = tree(X); X0..X `.planning`-only; no-TRUNCATE scan and rollback re-check on M before the push (E-71). | closed |
| T-24-28 | Information Disclosure | local upstream or milestone tags, or extra branches, reaching origin | high | mitigate | Named refspecs only; CUSTOM-BUILD-PUBLISHED-ONLY diffs tags and non-Dependabot heads against the before-snapshots; FINAL-REFS-OK requires exactly eight tags and two non-Dependabot heads. Evidence: CUSTOM-BUILD-PUBLISHED-ONLY and FINAL-REFS-OK; origin has exactly 8 tags and heads `custom_build`, `main` (E-72). | closed |
| T-24-29 | Denial of Service | a tag push cancelling the release run, or a tag naming an untested SHA | medium | mitigate | Release tag pushed only after PUBLISHED-SHA-RUN-GREEN and the 120 s re-check; the branch-only trigger means a tag starts no run, checked by the tests.yaml run count afterwards. Evidence: PUBLISHED-SHA-RUN-GREEN; late re-check 135 s after green; tag pushed after it; M still one run after the tag (E-72). | closed |
| T-24-30 | Tampering | a rollback tag on the wrong commit, or a base that moved (D-10) | medium | mitigate | Task 2 step 1 re-fetches and requires origin custom_build = 6473e24f, and a move raises the blocking-human D-10 decision (`absorb` by the locked recipe, or `hold`) with nothing pushed, recorded in E-73; ROLLBACK-TAG-PUSHED-NO-RUN asserts origin's pre-v1.7.0_gg3 is 6473e24f with the branch-only trigger; the rollback script is re-run on M. Evidence: Origin `pre-v1.7.0_gg3` = 6473e24f; 6473e24f still one `tests.yaml` run; D-10 re-read before every push (E-70). | closed |
| T-24-31 | Repudiation | a red run re-run until green | medium | mitigate | Attribution first and a blocking-human decision; any re-run is recorded as a deviation from exactly one run. Evidence: Both runs `run_attempt` 1, green first time; no re-run. | closed |
| T-24-SC | Tampering | dependency installs (24-01 to 24-07) | low | accept | No package installed, added or upgraded; Dependabot merges frozen until the tag (D-10). See R-24-01. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Evidence references are to `24-COMMAND-EVIDENCE.md` unless named otherwise. The auditor re-ran every verify marker it could read-only on the tip `970591fe`, against the retained `/tmp/gsd24/exec` transcripts, `git ls-remote origin` and the public GitHub API.

**Deviations the auditor reviewed, none weakening a mitigation:**

- **24-01 archive check.** ARTIFACT-DIFF-OK as written allows 9 changed jar entries; the measured delta is 11. The two extra classes inline the version constant (control build, E-04).
- **24-07 rollback-tag check timing.** The check first ran at 26 s and 54 s, earlier than the plan's 60 s wait. A 77 s run passed before the `custom_build` push.
- **24-02 shape query.** The executor narrowed the predicate without a checkpoint. It is still one read-only SELECT, re-proven on fixtures and look-alikes, and was shown to the user at the rehearsal gate.
- **24-04 rewording.** One FORK-DELTA sentence was reworded to pass a check; its meaning matches E-03.
- **24-05 review.** The review was inline rather than separately spawned. This was disclosed and accepted at the rehearsal gate.

**Informational, outside the register:**

- The published merge message of M says a `Nullable(Tuple)` whose element name holds backslash-n "stores the row". It does not carry the CHANGELOG's qualifier naming the shapes measured, which IN-01 added. The merge commit is published and is not rewritten (that would need a force-push); the CHANGELOG and the rollout note carry the measured wording.
- Local home-directory paths appear in 11 published `.planning/` files, 9 of them already public before this release.
- The auditor re-scanned all 244 files M published since 6473e24f and found no secret pattern. This covers the post-X0 `.planning/` commits that PUSH-READY did not scan.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-24-01 | T-24-SC (24-01 to 24-07) | No package, dependency or build change in Phase 24: `build.gradle.kts`, `gradle/`, settings and `.github/` are unchanged since P24_BASE and from 6473e24f to M. Older trees were built only in throwaway worktrees, all removed. Dependabot heads were unchanged and nothing was merged in the window (M^1 = 6473e24f). | Plan threat models (accept at plan time) | 2026-10-20 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-20 | 32 | 32 | 0 | gsd-security-auditor (opus), spawned rather than short-circuited because the orchestrator ran the push and publish steps it would otherwise grade |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-20
