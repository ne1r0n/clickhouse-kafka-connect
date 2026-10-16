---
phase: "20"
slug: "release-publication"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-15"
---

# Phase 20 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Test JVM -> ClickHouse container | Synthetic fixture data only; results become the evidence operators will read | Fixture rows, dedup outcomes |
| Executor -> src/main | The seen-red mutation edits production code temporarily and must never be committed | Temporary source edits |
| Throwaway worktrees -> repository | The probe and the gg1 tree must never reach a commit on `sync/v1.7.0` | Probe source, detached trees |
| Evidence -> operator documentation | Queries proven in evidence are quoted to operators, who run them on production servers | SQL/sh command text |
| Operator note -> production ClickHouse and git | Operators run the quoted queries and commands against their own servers and clones | SELECT-only queries, rollback commands |
| Repository docs -> operators and the next syncer | Published CHANGELOG and FORK-DELTA text is acted on without the planning context | Release claims, diff figures |
| Reviewer and auditor output -> approval bundle | The user approves publication partly on these verdicts | Findings, dispositions, verdicts |
| User -> orchestrator -> push / publication commands | Only a direct user answer authorises the rehearsal push and the publication | Verbatim approval answers |
| Approved bundle -> published tree | What the user approved (X0) must be what is published (M's tree) | Tree hashes |
| Local refs -> origin / public fork | Local tags and branches other than the named refspecs must not reach origin; pushes to the published branch are only reversible forward | Branch heads, tags |
| GitHub REST API -> CI evidence | Unauthenticated and rate-limited; a rate-limit body must not read as a result | Workflow run JSON |
| Local gate -> release decision | A cached or echoed gate result would let an untested tree through | Test result XML |
| User -> repository settings | Only the user reads or changes settings; agents only record what the user reports | Settings outcomes (verbatim) |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-20-01 | Tampering | `QueryIdentifier.java` via the seen-red mutation | high | mitigate | Edit reverted with `git checkout`; plan verify asserted `git diff --quiet -- src/main` and no src/main commit since P20_BASE; re-confirmed at audit: `src/main`, build, gradle/ and .github unchanged since d7384880 | closed |
| T-20-02 | Repudiation | test pinning an assumed outcome instead of a measured one | medium | mitigate | Probe-before-pin procedure ran; seen-red run proved the connector token is the cause; `DedupTokenRetryLiveTest` pins measured E-01/E-03 outcomes (20-01-SUMMARY) | closed |
| T-20-03 | Information Disclosure | new log lines of `DedupTokenRetryLiveTest` | low | mitigate | Audit grep of the test's LOGGER calls: only server version, window settings, case keys and row counts are logged; no record values or credentials | closed |
| T-20-04 | Denial of Service | shared-container interference between test classes | low | mitigate | Unique table, replication path, zkPath and zkDatabase per test; tables and state tables dropped in `finally` (20-01 verify passed) | closed |
| T-20-05 | Tampering | the main tree, via the probe class or a worktree commit | medium | mitigate | Probe source lived in /tmp; worktrees detached and removed; 20-02 verify asserted no tracked/untracked probe file, no worktree left, `src` unchanged since P20_BASE (20-02-SUMMARY: "the probe and both worktrees were removed") | closed |
| T-20-06 | Tampering | operator data, via a quoted query that modifies tables | high | mitigate | Doc-form queries are SELECT-only; audit re-scan of the gg2 note's sql blocks found no INSERT/DELETE/TRUNCATE/DROP/ALTER/UPDATE statement | closed |
| T-20-07 | Repudiation | detection queries proven only on gg2 rows | medium | mitigate | Each query required to flag gg1-written rows and not gg2-written rows from the same records; 20-02 verify parsed both XMLs (verification passed 8/8) | closed |
| T-20-08 | Tampering | the release identity, via a split VERSION and anchor-row commit | medium | mitigate | One commit for both files, checked by file list; anchors invariant ran over every commit since 54e63648 (20-02 verify) | closed |
| T-20-09 | Tampering | shared KeeperMap state table, via a TRUNCATE or unscoped delete in the note | high | mitigate | Note has no delete statement; ROLLOUT-FINAL-OK scanned docs, CHANGELOG and README; audit re-scan: no TRUNCATE instruction in operator docs, sql blocks read-only | closed |
| T-20-10 | Tampering | an operator's branch, via a wrong rollback procedure | high | mitigate | Item 6's commands run verbatim by a script on a dry-run merge of the release tip (E-31); plan 20-07 re-ran it on the real merge M before the push | closed |
| T-20-11 | Repudiation | a dedup claim the server does not produce | medium | mitigate | Item 1 states only E-01/E-03 measured outcomes and the measured server version (ITEM1-CONTENT-OK); 20-05 review confirmed the claims against the test | closed |
| T-20-12 | Information Disclosure | hostnames, credentials or record values in the note | medium | mitigate | Placeholder-only rule; ROLLOUT-FINAL-OK secret/password-assignment scan; audit re-scan of the gg2 note found no credential assignment or token pattern | closed |
| T-20-13 | Tampering | the published gg1 note | medium | mitigate | `git diff --quiet P20_BASE HEAD -- docs/ROLLOUT-v1.7.0_gg1.md` in every task's verify; re-confirmed unchanged at audit | closed |
| T-20-14 | Elevation of Privilege | pushing the rollback tag before approval | medium | mitigate | Tag created locally only during 20-03; ROLLOUT-FINAL-OK asserted absence from origin pre-approval; pushed only in 20-07 after the direct `publish` answer | closed |
| T-20-15 | Tampering | the released 1.7.0_gg1 CHANGELOG section | medium | mitigate | CHANGELOG-GG2-OK required byte-for-byte equality from the gg1 heading on; re-confirmed at audit against `d7384880:CHANGELOG.md` | closed |
| T-20-16 | Repudiation | FORK-DELTA figures or claims the tree does not support | medium | mitigate | Raw numbers and src/main counts re-derived from `git diff --shortstat` inside the verify; coverage audit and path resolution recorded (E-40..E-42) | closed |
| T-20-17 | Repudiation | an advisory marked fixed without a change; fulfilled L-38 confused with the deferred WR-02 advisory | low | mitigate | DISPOSITION check resolved WR-03's SHA (3e39867d, touches only docs/FORK-DELTA.md); five advisories deferred with reasons; separate fulfilled L-38 entry with disambiguating WR-02 Source text (20-04-SUMMARY) | closed |
| T-20-18 | Information Disclosure | the Keychain token named in the close record | low | accept | Only the Keychain item name and the revoke command are written, never the token; the record exists to get it revoked; audit token-pattern scan of phase records found nothing | closed |
| T-20-19 | Repudiation | findings silently dropped by a wrong heading level | medium | mitigate | Heading rule given to the reviewer; REVIEW-20-OK rejected deeper finding headings and a total differing from the heading count; DISPOSITION-20-OK required one row per heading | closed |
| T-20-20 | Elevation of Privilege | an auditor verdict closing a threat whose mitigation is wrong | medium | mitigate | 20-05 Task 1 cross-checked every closed Phase 19 threat against 19-REVIEW findings; 19-SECURITY.md committed with `threats_open: 0` (SECURITY-19-OK) | closed |
| T-20-21 | Tampering | a review fix widening into src/main or reversing a locked decision | high | mitigate | Out-of-scope fixes stop at a blocking-human decision; DOCS-FROZEN-OK and SECURITY-19-OK asserted src/main, the build and .github unchanged since P20_BASE; re-confirmed at audit | closed |
| T-20-22 | Elevation of Privilege | a rehearsal push on a relayed or absent approval | high | mitigate | 20-06 Task 2 was `gate="blocking-human"`; Task 3 required the verbatim direct answer and ran inline in the orchestrator (20-06-SUMMARY: answer "given directly to the orchestrator and recorded verbatim") | closed |
| T-20-23 | Information Disclosure | local tags or other branches reaching origin at rehearsal | high | mitigate | One explicit refspec; PUSH-READY checked push config; REHEARSAL-PUSHED-ONLY compared heads and byte-compared tags before and after | closed |
| T-20-24 | Repudiation | a gate result read from Gradle's cache or from run 1 | medium | mitigate | `--rerun` on both runs, result directories deleted first, XML newer than the last code commit, run 2 later than run 1's copied XML (20-06 verify passed) | closed |
| T-20-25 | Spoofing | CI evidence from a rate-limit body, another run or a cancelled sibling | medium | mitigate | Locator keyed on head_sha plus workflow path, workflow_runs type check, exactly one run, zero cancelled | closed |
| T-20-26 | Repudiation | a red rehearsal run re-run until green | medium | mitigate | Attribution first and a blocking-human decision; a re-run recorded as a deviation; no re-run deviation appears in 20-06-SUMMARY | closed |
| T-20-27 | Tampering | a test weakened or skipped to pass the gate | high | mitigate | Skip set compared by name against the 18 named skips; expected classes and case counts asserted; a failure stops the plan (20-06 verify passed) | closed |
| T-20-28 | Elevation of Privilege | publication on a relayed or absent approval | high | mitigate | 20-07 Task 1 `gate="blocking-human"`; verbatim `publish` answer received directly by the orchestrator, recorded in E-70 before any command (20-07-SUMMARY) | closed |
| T-20-29 | Tampering | a fast-forward or force push replacing the reviewable merge | high | mitigate | Explicit `--no-ff` merge; MERGE-SHAPE-OK asserted two parents and M^1 = be6b8861; re-confirmed at audit: a246ca01 has two parents, first parent be6b8861 | closed |
| T-20-30 | Tampering | publishing a tree other than the rehearsed and approved one | high | mitigate | Merge X by hash; MERGE-SHAPE-OK asserted tree(M) = tree(X) and X0..X planning-only; no-TRUNCATE scan and rollback re-check ran on M before the push | closed |
| T-20-31 | Information Disclosure | local upstream or milestone tags, or extra branches, reaching origin | high | mitigate | Named refspecs only; CUSTOM-BUILD-PUBLISHED-ONLY diffed tags and heads against before-snapshots; FINAL-REFS-OK required exactly six tags and two heads at publication time. Audit: origin carries the six expected tags; the five extra `dependabot/*` heads were created server-side by GitHub after the user enabled Dependabot (plan 20-08), not pushed from local | closed |
| T-20-32 | Denial of Service | a tag push cancelling the release run, or a tag naming an untested SHA | medium | mitigate | Release tag pushed only after PUBLISHED-SHA-RUN-GREEN and the 120 s re-check; branch-only trigger means a tag starts no run, checked by the run count afterwards | closed |
| T-20-33 | Tampering | a rollback tag on the wrong commit | medium | mitigate | ROLLBACK-TAG-PUSHED-NO-RUN asserted origin's pre-v1.7.0_gg2 is be6b8861 with the branch-only trigger; re-confirmed at audit: origin tag points at be6b8861 | closed |
| T-20-34 | Repudiation | a red publication run re-run until green | medium | mitigate | Attribution first and a blocking-human decision; any re-run recorded as a deviation from exactly one run; no such deviation in 20-07-SUMMARY | closed |
| T-20-35 | Elevation of Privilege | an agent changing repository settings (default branch, Dependabot) | high | mitigate | 20-08 checks were a blocking-human `checkpoint:human-action`; prohibition forbids setting changes; CLOSE-STEPS-RECORDED compared origin's refs before and after; outcomes record the user as the actor ("default branch changed to custom_build by the user") | closed |
| T-20-36 | Repudiation | an outcome recorded that the user did not give | medium | mitigate | The three D-11 outcomes recorded verbatim with UTC date and "given directly to the orchestrator"; relayed answers refused (20-08-SUMMARY "The three answers, verbatim") | closed |
| T-20-37 | Information Disclosure | the read-only GitHub token leaking into the record | medium | mitigate | The record names only the Keychain item; CLOSE-STEPS-RECORDED scanned the record for token patterns; audit re-scan of phase 20 planning records found no `gh*_` token pattern | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-20-01 | T-20-18 | The milestone close record must name the Keychain token item so the user can revoke it; only the item name and the revoke command are written, never the token value. Audit token-pattern scan confirms no token in any phase 20 record. | plan 20-04 (disposition: accept, authored at plan time) | 2026-10-15 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-15 | 37 | 37 | 0 | gsd-secure-phase (L1 short-circuit: register authored at plan time, all mitigations evidenced) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-15
