---
phase: "16"
slug: "early-docs-publication"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-10"
---

# Phase 16 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Operator doc -> production ClickHouse | Operators run the doc's SQL and shell commands against shared production state | Recovery SQL against the shared KeeperMap state table (high: shared exactly-once state) |
| Evidence file -> approval gate | The user decides publication partly on recorded command output | Command outputs and verdicts (integrity) |
| Throwaway worktree -> repository refs | Dry-run merges and reverts must not move a published or protected ref | Git refs (integrity) |
| Local planning history -> public origin | The merge publishes every .planning commit since f328fd21 for the first time | ~126 files of planning prose (confidentiality: secrets, hostnames, identifiers) |
| Review record -> approval gate | The user approves on the strength of the review and its dispositions | Finding dispositions and fix SHAs (integrity) |
| Bundle -> human decision | A cited, unmeasured bundle item could approve a different tree than the one checked | Approval bundle measurements (integrity) |
| User -> orchestrator -> commands | Only a direct user answer authorises the merge and push | Approval decision (authorisation) |
| Approved evidence -> published artifact | What the user approved (X0) must be what is published (M's tree) | Git tree (integrity) |
| Local custom_build -> public fork | A non-force push to a protected, published branch is only revertible forward | custom_build ref (integrity) |
| Local refs -> origin | The local repo holds upstream and milestone tags that must not reach origin | Local-only tags and sync/v1.7.0 (confidentiality / integrity) |
| GitHub REST API -> CI evidence | Unauthenticated and rate-limited; a rate-limit body must not read as a result | CI run status (integrity) |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-16-01 | Tampering | rollout item 2 recovery SQL (shared KeeperMap state table) | high | mitigate | SQL-UNCHANGED-NO-TRUNCATE-OK + ROLLOUT-FINDINGS-OK (16-COMMAND-EVIDENCE E-08, E-11). Re-checked on origin/custom_build 537beb1f: SQL blocks identical to 505e4981; only truncate lines are ROLLOUT:111 "Do not `TRUNCATE`" and FORK-DELTA:126 Debezium event | closed |
| T-16-02 | Repudiation | 16-COMMAND-EVIDENCE.md | medium | mitigate | EVIDENCE-COMPLETE printed (16-01-SUMMARY). E-01..E-14 each carry one `Matches text:` verdict: 13 `yes`, 1 `n/a (gate check)` (E-04), none `no` | closed |
| T-16-03 | Tampering | gate dry run and revert demonstration worktrees | medium | mitigate | GATE-DRY-RUN-OK (E-04) with custom_build = f328fd21 after; no push in 16-01. `git worktree list` now shows only the main tree | closed |
| T-16-04 | Information Disclosure | changed doc text | low | mitigate | Placeholders only; 16-REVIEW check (d) found no URL, host, address or credential added; SECRET-SCAN-CLEAN in 16-02 | closed |
| T-16-05 | Spoofing | 15/16 disposition records claiming fixed | medium | mitigate | D10-CLOSED and DISPOSITIONS-COMPLETE printed (16-02-SUMMARY); IN-07..IN-10 resolve to fad6f7f9, 65582a31, 8c1f0bc1 | closed |
| T-16-06 | Information Disclosure | files first published by the merge | high | mitigate | SECRET-SCAN-CLEAN over 126 files in f328fd21..X0; 5 assignment hits classified as prose (16-02-SUMMARY). Re-run on X before the push (16-03-SUMMARY). Re-checked on the published diff f328fd21..537beb1f: no private-key, AWS or GitHub token pattern | closed |
| T-16-07 | Tampering | a non-docs change riding the docs-only merge | high | mitigate | ALLOW-LIST-OK on X0 (16-02) and on X/M (16-03). Re-checked: f328fd21..537beb1f changes nothing outside `.planning/`, `docs/`, `CHANGELOG.md`; docs diff is exactly the three files | closed |
| T-16-08 | Spoofing | approval bundle items | medium | mitigate | Approval bundle in 16-02-SUMMARY is command output measured on X0 = 427a2f9d; 16-03 Task 1 re-measured it on X = 1e0eefde | closed |
| T-16-09 | Tampering | review fix silently weakening the WR-01 KeeperMap fix | high | mitigate | 16-REVIEW Confirmation 1 = yes; after the IN-01 fix ROLLOUT-FINDINGS-OK and SQL-UNCHANGED-NO-TRUNCATE-OK re-ran; no SQL changed, so no D-07 live run needed | closed |
| T-16-10 | Elevation of Privilege | publication without, or on a relayed, user approval | high | mitigate | Task 1 returned a blocking-human checkpoint, with no commits and no push. The user's verbatim `publish` was given directly to the orchestrator on 2026-10-10; Tasks 2-3 ran inline (16-03-SUMMARY) | closed |
| T-16-11 | Tampering | a fast-forward or force push replacing the reviewable merge commit | high | mitigate | MERGE-SHAPE-OK before the push; the push command is recorded with no force flag. Re-checked: 537beb1f has parents f328fd21 and 1e0eefde | closed |
| T-16-12 | Tampering | publishing a tree other than the reviewed and approved one, or a non-docs change | high | mitigate | Merged X by hash; MERGE-SHAPE-OK and MERGED-TREE-NO-TRUNCATE-INSTRUCTION ran on M before the push. Re-checked: tree(537beb1f) = tree(1e0eefde) = 3f11a84d | closed |
| T-16-13 | Information Disclosure | upstream or local-only tags, or sync/v1.7.0, reaching origin | high | mitigate | Single refspec refs/heads/custom_build; CUSTOM-BUILD-PUBLISHED-ONLY + FINAL-REFS-OK. Re-checked with `git ls-remote origin`: heads custom_build and main only; tags pre-v1.6.0-sync, pre-v1.7.0-sync, v1.6.0_gg1, v1.7.0_gg1 only | closed |
| T-16-14 | Spoofing | CI evidence from a rate-limit body, another run, or a cancelled sibling | medium | mitigate | PUBLISHED-SHA-RUN-GREEN (run 38074050935 on 537beb1f, success) + CI-STILL-ONE-RUN at 18:10:44Z, more than 120 s later | closed |
| T-16-15 | Repudiation | a red run re-run or pushed over until green, hiding the cause | medium | mitigate | 16-03-SUMMARY: no D-11 deviation, no red run, no re-run | closed |
| T-16-16 | Denial of Service | publishing on the Ubuntu 26 runner, muddying the attribution of a red run | medium | mitigate | Published 2026-10-10, before 2026-10-19; run on `ubuntu-latest` (same runner image as the f328fd21 baseline); no D-12 deviation | closed |
| T-16-SC | Tampering | npm/pip/cargo installs | low | accept | See AR-16-01 | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-16-01 | T-16-SC | No package is installed or changed in plans 16-01..16-03 (accepted at plan time). Confirmed: f328fd21..537beb1f changes no build, gradle, catalog or CI file (ALLOW-LIST-OK) | plan-time threat model (16-01/02/03-PLAN) | 2026-10-10 |

*Accepted risks do not resurface in future audit runs.*

---

## Out-of-Register Notes

- **WR-01 (16-REVIEW, warning, `open`).** On the published tip 537beb1f, rollout item 8 step 1 (`git revert -m 1 <merge-commit>`) stops with conflicts. This is an operability defect in the rollback procedure, not a registered threat. It does not affect any mitigation above. It is tracked as human-needed item 1 in 16-UAT.md and 16-VERIFICATION.md, and this audit does not decide it.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-10 | 17 | 17 | 0 | /gsd-secure-phase (orchestrator, L1 short-circuit: register authored at plan time, ASVS 1) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-10
