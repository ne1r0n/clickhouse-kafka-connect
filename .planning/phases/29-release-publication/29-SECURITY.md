---
phase: "29"
slug: release-publication
status: verified
threats_open: 0
asvs_level: 1
created: "2026-10-24"
---

# Phase 29 — Security

> Threat register of plans 29-01..29-08 with the evidence that closes each. Dependency-install threats (T-29-SC, one per plan) are accepted: no package was installed or upgraded in this phase.

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Evidence | Status |
|-----------|----------|-----------|----------|-------------|----------|--------|
| T-29-01 | Tampering | VERSION bumped without its anchor row | medium | mitigate | VERSION-GG4-OK and ANCHORS-MIRROR-OK in 29-01 (E-01, E-02); anchors gate PASS rows=94 in E-61 | closed |
| T-29-02 | Tampering | a broken archive passing as green (Shadow 9 nested jars) | high | mitigate | ARCHIVE-COMPARE-OK, 0 nested jars (E-03); rollback-tree hazard measured, 25 nested jars (E-04); item 7 warns | closed |
| T-29-03 | Repudiation | advisory statements that misstate a source | medium | mitigate | RECHECKS-RECORDED (E-07); no exposure statement in item 6 (ITEMS-5-6-OK) | closed |
| T-29-04 | Information Disclosure | record data, a host or a token in the probe output or evidence | medium | mitigate | constants-only probe output; no token created (E-01..E-09) | closed |
| T-29-05 | Tampering | extracted or downloaded content executed | low | mitigate | archives read with python3 -I zipfile; downloads isolated (E-03, E-07) | closed |
| T-29-06 | Tampering | fixture DDL, inserts and landed rows | low | mitigate | probe ran only in Gradle test runs on Testcontainers (E-10..E-25) | closed |
| T-29-07 | Repudiation | a rollback or upgrade claim worded from Phase 26 E-11 instead of a measurement | medium | mitigate | D994-INFLIGHT-OK (E-11, E-12) | closed |
| T-29-08 | Tampering | an operator query or grep that writes or deletes | high | mitigate | D994-QUERIES-AND-GREPS-OK; NOTE-FRAME-OK rejects non-SELECT blocks | closed |
| T-29-09 | Information Disclosure | record data in captured WARN lines or evidence | low | mitigate | constants only in WARN lines (E-13..E-15) | closed |
| T-29-10 | Tampering | a corrected example that deletes other connectors' state | high | mitigate | ROLL05-OK incl. survival of other-topic and look-alike keys (E-25) | closed |
| T-29-11 | Repudiation | the key rule claimed as measured | medium | mitigate | rule sentence still labelled read from code (ROLL05-OK) | closed |
| T-29-12 | Tampering | probe DDL or DELETE reaching a real server | low | mitigate | Testcontainers only (E-20..E-25) | closed |
| T-29-13 | Information Disclosure | record data in names, labels or evidence | low | mitigate | fixed name entries only (D995-NAMES-OK) | closed |
| T-29-14 | Tampering | a note command that destroys the shared KeeperMap state | high | mitigate | NOTE-FRAME-OK and NOTE-FINAL-OK; DOCS-FROZEN-OK scans docs for TRUNCATE; round-4 confirmation (a) | closed |
| T-29-15 | Tampering | a rollback that deploys a broken archive | high | mitigate | ITEMS-1-7-OK; E-30 rollback dry run; ROLLBACK-ON-TIP-OK (E-61) and on M (E-71) | closed |
| T-29-16 | Repudiation | overclaims (measured, safe, all) and exposure statements about advisories | medium | mitigate | ITEMS-5-6-OK; four review rounds (29-REVIEW.md) | closed |
| T-29-17 | Information Disclosure | hosts, addresses, credentials or record values in the note | medium | mitigate | NOTE-FINAL-OK secret/address scan (server version masked) | closed |
| T-29-18 | Repudiation | the three documents contradicting each other (Phase 20 WR-02) | medium | mitigate | consistency.py in CHANGELOG-GG4-OK and RAW-NUMBERS-GG4-OK | closed |
| T-29-19 | Tampering | released CHANGELOG sections rewritten | medium | mitigate | CHANGELOG-GG4-OK tail byte-identical | closed |
| T-29-20 | Repudiation | findings closed without a reason, owner or trigger, or marked fixed without a change | medium | mitigate | CLOSE-RECORD-29-OK, DISPOSITIONS-25-28-OK | closed |
| T-29-21 | Information Disclosure | secrets or record values in the close record | low | mitigate | secret scan in CLOSE-RECORD-29-OK | closed |
| T-29-22 | Repudiation | a review that is not independent (run inside an executor) | high | mitigate | four orchestrator-spawned gsd-code-reviewer rounds (29-06-SUMMARY) | closed |
| T-29-23 | Tampering | a reviewer-proposed fix that is wrong (Phase 24 WR-10) | high | mitigate | six reviewer Fix texts corrected against code/evidence (29-06-SUMMARY); next round reviewed fixes | closed |
| T-29-24 | Repudiation | findings silently dropped by a wrong heading level, or IDs reused | medium | mitigate | REVIEW-29-ROUND-OK, REVIEW-ROUNDS-OK, DISPOSITION-29-OK (20 headings, 20 rows) | closed |
| T-29-25 | Tampering | a fix widening into code, tests, build or CI, or reversing a locked decision | high | mitigate | DOCS-FROZEN-OK scope and protected paths; no out-of-scope fix needed | closed |
| T-29-26 | Elevation of Privilege | a push on a relayed or absent approval | high | mitigate | user answered "push" directly; Task 4 ran inline (29-07-SUMMARY, E-62) | closed |
| T-29-27 | Information Disclosure | local tags or other branches reaching origin | high | mitigate | single refspec; REHEARSAL-PUSHED-ONLY compared heads and tags (E-62) | closed |
| T-29-28 | Repudiation | a gate result read from Gradle's cache or from run 1 | medium | mitigate | two --rerun runs with digests equal to E-05 (E-60) | closed |
| T-29-29 | Spoofing | CI evidence from a rate-limit body, another run or a cancelled sibling | medium | mitigate | head_sha+path locator, exactly one run, zero cancelled (REHEARSAL-RUN-GREEN) | closed |
| T-29-30 | Tampering | a test weakened or skipped to pass the gate | high | mitigate | skip set = 18 names; digest equal to base (E-60) | closed |
| T-29-31 | Tampering | publishing on a base that moved (a slipped Dependabot merge) | high | mitigate | FREEZE-OK/PUSH-READY/PUBLISH-PRECONDITIONS-OK: custom_build = 69cd680b at every gate | closed |
| T-29-32 | Information Disclosure | a CI-log token printed or stored in files | medium | mitigate | no token created or used | closed |
| T-29-33 | Information Disclosure | secrets in the published history | high | mitigate | PUSH-READY secret-pattern grep over files changed since 69cd680b | closed |
| T-29-34 | Elevation of Privilege | publication on a relayed or absent approval | high | mitigate | user answered "publish" directly; Task 3 ran inline (29-08-SUMMARY) | closed |
| T-29-35 | Tampering | a fast-forward or force push replacing the reviewable merge | high | mitigate | --no-ff merge, 2 parents, M^1=69cd680b, no force flag (MERGE-CHECKS-ON-M-OK, E-71) | closed |
| T-29-36 | Tampering | publishing a tree other than the rehearsed and approved one | high | mitigate | tree(M)=tree(X), X0..X planning-only, dry run D identical (E-70, E-71) | closed |
| T-29-37 | Information Disclosure | local upstream, milestone or baseline tags, or extra branches, reaching origin | high | mitigate | PUBLISHED-AND-TAGGED-OK: exactly two new tags, heads custom_build and main only (E-72) | closed |
| T-29-38 | Denial of Service | a tag push cancelling the release run, or a tag naming an untested SHA | medium | mitigate | tags pushed after green run + re-check; run counts unchanged (E-72) | closed |
| T-29-39 | Tampering | a base that moved before the merge (OPS-05) | high | mitigate | origin custom_build re-read before merge and push (E-70, E-71) | closed |
| T-29-40 | Repudiation | a red run re-run until green | medium | mitigate | single green run, no re-run (E-71) | closed |
| T-29-41 | Tampering | Dependabot PRs #4 and #10 merged or ignored during or after the release by an agent | medium | mitigate | no PR touched; reminders only (29-MILESTONE-CLOSE-RECORD.md) | closed |
| T-29-42 | Repudiation | a phase base measured on another server or client than latest x V2 (an inherited `CLICKHOUSE_VERSION` or `CLICKHOUSE_CLUSTER_NAME`, or `CLIENT_VERSION=V1`), which the 29-07 gate would then match | medium | mitigate | PHASE-BASE-OK, all Version: lines latest (E-05) | closed |
| T-29-43 | Repudiation | a green gate run on another server or client than latest x V2 (an inherited `CLICKHOUSE_VERSION` or `CLICKHOUSE_CLUSTER_NAME`, or `CLIENT_VERSION=V1`) | medium | mitigate | SUITE-TWICE-GREEN, Version: lines latest, one server version (E-60) | closed |
| T-29-44 | Repudiation | a release gate silently narrower than AGENTS.md's pre-PR commands (`clean integrationTest` not run) | low | mitigate | integration_test: excluded line with reason (E-60); compileIntegrationTestJava green in gate and CI | closed |
| T-29-SC | Tampering | dependency installs (all eight plans) | low | accept | no dependency changed; `git diff c1dc6e59 HEAD -- build.gradle.kts gradle` empty (DOCS-FROZEN-OK) | closed |

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-29-01 | T-29-SC | no package installed or upgraded | plan-time register | 2026-10-23 |

## Security Audit Trail

Written by the orchestrator from the plan-time registers and the recorded check markers; no auditor agent was spawned (threats_open 0, ASVS level 1, same shape as Phases 26-28).

