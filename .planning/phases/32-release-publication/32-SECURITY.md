---
phase: 32
slug: release-publication
status: verified
verdict: SECURED
asvs_level: 1
block_on: high
threats_total: 38
threats_closed: 38
threats_open: 0
threats_open_non_blocking: 0
audited: 2026-10-27
auditor: orchestrator (grep-depth, plan-time register)
---

# Phase 32 Security Verification

**Verdict: SECURED.** The register was authored at plan time (31 numbered threats T-32-01 to T-32-31, all `mitigate`, plus a `T-32-SC` supply-chain row in each of the seven plans, `accept`). All mitigations were checked against recorded evidence and live commands at grep depth; no auditor agent was spawned (ASVS 1, nothing open). `threats_open: 0`.

## Threat Verification

| Threat ID | Disposition | Status | Key evidence |
|-----------|-------------|--------|--------------|
| T-32-01 | mitigate | CLOSED | Secret-pattern scan (private key, AWS key, GitHub token) of `32-COMMAND-EVIDENCE.md`, docs and CHANGELOG at HEAD: 0 hits |
| T-32-02 | mitigate | CLOSED | `VERSION` and its anchor row changed in one commit (`a6bfd78f`); anchors mirror `rows=131 bad=0`; gate E-61 `result=PASS` |
| T-32-03 | mitigate | CLOSED | Every GitHub read checks the rate limit first and requires the expected body shape (E-61, E-80) |
| T-32-04 | mitigate | CLOSED | The allow-list holds exactly the four release-identity rows with exact paths, decided by the user (32-AMENDMENT-01) after a javap proof of the inlined constant (E-03); compare `changed 4` |
| T-32-05 | mitigate | CLOSED | The rollback ran as written on a dry-run merge of the tip and of the published merge (E-11, E-33, E-70, E-72) |
| T-32-06 | mitigate | CLOSED | The note names no host, credential or production system (round 1 and 2 confirmations) |
| T-32-07 | mitigate | CLOSED | Three review rounds checked each note claim against evidence; 15 findings dispositioned, open 0 |
| T-32-08 | mitigate | CLOSED | `pre-v1.7.0_gg5` was a local tag until after `custom_build` and its green run (E-73) |
| T-32-09 | mitigate | CLOSED | `git diff 8dcbbfc1 HEAD -- CHANGELOG.md` has 41 added lines and 0 removed lines |
| T-32-10 | mitigate | CLOSED | FORK-DELTA has one CI workflows group with every workflow row (`FORK-DELTA-CI-GROUP-OK`) |
| T-32-11 | mitigate | CLOSED | Raw numbers recomputed by `RAW-NUMBERS-GG5-OK` and again on the tag (`RECORDS-AND-TAIL-OK`) |
| T-32-12 | mitigate | CLOSED | Each of the three rounds was a separately spawned fresh `gsd-code-reviewer` (32-04 SUMMARY) |
| T-32-13 | mitigate | CLOSED | Reviewer fix text was checked and corrected before applying (round 1 WR-02/WR-03/IN-01, round 2 WR-05, round 3 IN-10) |
| T-32-14 | mitigate | CLOSED | `REVIEW-32-ROUND-OK` and `DISPOSITION-32-OK` print: 15 headings, unique IDs, one row each |
| T-32-15 | mitigate | CLOSED | `DOCS-FROZEN-OK`: only the four expected non-.planning files changed; `src`, build files and `.github` unchanged since `8dcbbfc1` |
| T-32-16 | mitigate | CLOSED | `--rerun`, deleted result dirs, XML newer than the last commit, equal digests (`SUITE-TWICE-GREEN`) |
| T-32-17 | mitigate | CLOSED | Skips compared by name to the 18 named; ci guard counts 58, 8, 21, 39, 9; no file under `src/` changed |
| T-32-18 | mitigate | CLOSED | `FREEZE-OK` and `PUSH-READY` re-fetched: origin `custom_build` stayed `21c51598` until the merge |
| T-32-19 | mitigate | CLOSED | Pinned `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2` on every run; one server version 26.10.3.7 (E-60) |
| T-32-20 | mitigate | CLOSED | `PUSH-READY` secret scan of every file changed since `21c51598`: no match |
| T-32-21 | mitigate | CLOSED | The user's direct "publish" answer is recorded in E-71 before any push |
| T-32-22 | mitigate | CLOSED | `custom_build` moved `21c51598..3a3a15a9` by a non-force push of a two-parent merge |
| T-32-23 | mitigate | CLOSED | M's tree equals X0's; X0 and M each had one green `tests.yaml` run (E-71, E-72) |
| T-32-24 | mitigate | CLOSED | Origin tags gained exactly `pre-v1.7.0_gg5` and `v1.7.0_gg5`; non-Dependabot heads are `custom_build` and `main` only (`PUBLISHED-AND-TAGGED-OK`) |
| T-32-25 | mitigate | CLOSED | Tags were pushed after M's green run; run counts for X0 and M stayed 1 |
| T-32-26 | mitigate | CLOSED | No run was re-run or pushed again (E-71, E-72) |
| T-32-27 | mitigate | CLOSED (observation) | No agent dispatched a release workflow and no GitHub release exists. The user's account dispatched `release.yaml` run 40474419526 by mis-click and cancelled it during the archive build; 0 releases, tags unchanged (E-81, E-82) |
| T-32-28 | mitigate | CLOSED | No agent touched `NIGHTLY_ENABLED`; E-82 records the operator's report after E-81 was committed; scheduled run 40477788530 was skipped |
| T-32-29 | mitigate | CLOSED | The run was located by path, event, branch and creation time after `published_at`; `NIGHTLY-DISPATCH-RECORDED` compares id, SHA and conclusion |
| T-32-30 | mitigate | CLOSED | The first run was green, so no fix was made; no change under `src/`, build files or `.github/` since `8dcbbfc1` |
| T-32-31 | mitigate | CLOSED | The token route was not used |
| T-32-SC (plans 01-07) | accept | CLOSED (AR-01) | No package installed or upgraded; no Dependabot PR merged |

## Accepted Risks Log

| ID | Threats | Risk accepted | Rationale |
|----|---------|---------------|-----------|
| AR-01 | T-32-SC x7 | Package installs | None installed in this phase |

## Unregistered Flags

None. The footprint is the four release documents, `VERSION`, its anchor row, the Phase 32 allow-list and `.planning/` records. After publication Dependabot opened its first `github-actions` branch on origin; it is not part of the release and was not touched.

## Notes

1. The value of `NIGHTLY_ENABLED` cannot be read unauthenticated; E-82 rests on the operator's report.
2. The FORK-DELTA Dependabot row says no `github-actions` pull request has been observed; that is out of date after publication. Fixing it needs a new commit on `custom_build`; carried to the milestone close.
