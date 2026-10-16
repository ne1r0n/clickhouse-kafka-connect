---
phase: 20-release-publication
verified: 2026-10-14T19:30:00Z
status: passed
score: 8/8 must-haves verified
covered_files:
  - ".planning/phases/20-release-publication/20-01-PLAN.md"
  - ".planning/phases/20-release-publication/20-01-SUMMARY.md"
  - ".planning/phases/20-release-publication/20-02-PLAN.md"
  - ".planning/phases/20-release-publication/20-02-SUMMARY.md"
  - ".planning/phases/20-release-publication/20-03-PLAN.md"
  - ".planning/phases/20-release-publication/20-03-SUMMARY.md"
  - ".planning/phases/20-release-publication/20-04-PLAN.md"
  - ".planning/phases/20-release-publication/20-04-SUMMARY.md"
  - ".planning/phases/20-release-publication/20-05-PLAN.md"
  - ".planning/phases/20-release-publication/20-05-SUMMARY.md"
  - ".planning/phases/20-release-publication/20-06-PLAN.md"
  - ".planning/phases/20-release-publication/20-06-SUMMARY.md"
  - ".planning/phases/20-release-publication/20-07-PLAN.md"
  - ".planning/phases/20-release-publication/20-07-SUMMARY.md"
  - ".planning/phases/20-release-publication/20-08-PLAN.md"
  - ".planning/phases/20-release-publication/20-08-SUMMARY.md"
  - "CHANGELOG.md"
  - "VERSION"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg2.md"
  - "src/test/java/com/clickhouse/kafka/connect/sink/DedupTokenRetryLiveTest.java"
covered_digest: "v3:sha256:bcc0e889aa9022fbf8a29337072357b98bccc3eb1c0589fbec97fdaf427948ca"
behavior_unverified: 0
overrides_applied: 0
re_verification: null
---

# Phase 20: Release & Publication Verification Report

**Phase Goal:** `v1.7.0_gg2` is published on `custom_build`. Operators have a reviewed rollout note for every behaviour change, the suite is green twice on `latest` x V2, and the published SHA has one green CI run before the tag is pushed.
**Verified:** 2026-10-14
**Status:** passed
**Re-verification:** No, initial verification

## Method

SUMMARY.md claims were treated as unproven. Each truth was checked against git objects on the release merge `M = a246ca01b073faae86e2e92bb0288c07dd7eb0d7` (read with `git show M:<path>`), against the live `origin` refs (`git ls-remote origin`, read only), and against the public GitHub Actions API (`curl`, unauthenticated, read only). The Keychain item was not touched and nothing was pushed. The full Gradle suite was not re-run; per the verification brief, SUITE-05 rests on the pinned gate evidence, and the check below shows that the non-planning tree at HEAD is byte-identical to the gated tree.

## Goal Achievement

### Observable Truths

| # | Truth (roadmap contract) | Status | Evidence |
|---|---|---|---|
| 1 | `docs/ROLLOUT-v1.7.0_gg2.md` gives operators the 999.1 value change (rows already stored wrongly not repaired), the dedup-token retry consequence as verified on `latest`, the empty-CDC skip change and the MBean `version=` change, each with a self-check; its review is recorded before the publication gate | VERIFIED | The note exists in `M` (357 lines). Item 1 is the dedup-token retry, measured on ClickHouse 26.9.18.11. Item 2 is the Nullable Enum/Tuple change and states "Rows stored by earlier releases are not repaired by the connector" (line 106) with L-38. Item 3 is empty Debezium records. Item 5 is the MBean `version=` and client name. Every item has an "Am I affected?" self-check. Item 6 is the rollback by `git read-tree -u --reset pre-v1.7.0_gg2`. The note has no TRUNCATE or unscoped DELETE instruction: the only grep hit for `truncate` is the CDC `op=t` event. `20-REVIEW.md` has 6 findings, all `fixed`, `open: 0` in `20-REVIEW-DISPOSITION.md` (recorded 13:47Z). The 8 D-07 confirmations are listed there. The review and the freeze were committed before the rehearsal push (E-60 FREEZE-OK: `review-open0 True`, `19-review-open0 True`, `threats0 True`). `19-SECURITY.md` has `status: verified`, `threats_open: 0`. The gg1 note is unchanged between `be6b8861` and `M` (0 diff lines). |
| 2 | After `clean`: `VERSION` reads `v1.7.0_gg2` and the generated version agrees | VERIFIED | `VERSION` = `v1.7.0_gg2` (read at `M` and at HEAD). `build.gradle.kts` line 48 sets `version` from `VERSION`; `generateVersionFile` emits `ARTIFACT_VERSION = "${project.version}"` (lines 435-447). The generated `Version.java` on disk reads `v1.7.0_gg2`. E-60 records `clean compileJava` followed by the grep for `ARTIFACT_VERSION = "v1.7.0_gg2"` passing. The `VERSION` anchor row in `.planning/scripts/fork-anchors.txt` moved with it (`VERSION	v1.7.0_gg2	1	BLD-07`). I re-ran `verify-merge-invariants.sh 54e63648 HEAD` at HEAD: `result=PASS` (anchor_rows=59). |
| 3 | `CHANGELOG.md` has a plain-language `1.7.0_gg2` section | VERIFIED | `M:CHANGELOG.md` line 1 is `# 1.7.0_gg2, 2026-10-14` with Important Changes and Bug Fixes. It covers the Nullable closure, the L-38 `DataException`, the removed startup WARN, the empty-CDC skip, the log-line changes, the dedup-token caution and the MBean/client-name change. Deletions below the gg2 section: 0 (`be6b8861` to `M`), so the released gg1 section is unchanged. |
| 4 | `docs/FORK-DELTA.md` has a row for every fork-side patch added in Phases 17-19 | VERIFIED | E-41 audit: CI-11..16, BLD-14, DEBT-06..08, JSONP-09, WRT-10..12, COLS-08 and COLS-10 are each pinned in a row's "Pinned by" cell; 17 anchor rows backed by a Phase 17-19 ID, all covered, 0 uncovered. The first run found DEBT-06 and DEBT-08 missing; they were added and the audit re-run. E-42: every `.planning/` path the file cites resolves. I re-ran the raw numbers on the tags: `git diff --shortstat v1.7.0 v1.7.0_gg2 -- src build.gradle.kts gradle .github` gives 119 files, +18802, -529; `-- src/main` gives 32 files, +2042, -143. Both match FORK-DELTA lines 113 and 116. |
| 5 | The full suite passes twice on ClickHouse `latest` x client V2, the second run with `--rerun`; 0 failures, 0 errors, every skip named; `testUnit` passes with Docker stopped | VERIFIED (pinned evidence) | E-60 and 20-06-SUMMARY: run 1 and run 2 each 85 classes, 988 tests, 0 failures, 0 errors, 18 skipped, `stale 0`, server 26.9.18.11; run 2's earliest XML timestamp (13:54:02Z) is later than run 1's latest (13:53:55Z); both used `--rerun`, after `rm -rf build/test-results/test`. The 18 skips are named as a `class#method` set and compared with the FORK-DELTA inventory. `testUnit` ran with colima stopped (`docker info` failed): 45 classes, 556 tests, 0 failures, 0 errors. I did not re-run the suite. I confirmed instead that the gate holds for what shipped: `git diff d316bc45 HEAD -- . ':(exclude).planning'` is empty, and `git diff M d316bc45 -- . ':(exclude).planning'` is empty, so the non-planning tree at the gate, at `M` and at HEAD is the same. |
| 6 | After the user's own explicit approval, `custom_build` on `origin` is a non-force `--no-ff` merge | VERIFIED | `M` has exactly two parents: `be6b8861` (the previous `custom_build` tip) and `3879b883` (the rehearsed `sync/v1.7.0` tip). `git rev-parse M^{tree}` equals `M^2^{tree}` (`56abcb0a...`), so the merged tree is the rehearsed tip. `git ls-remote origin` shows `custom_build` at `a246ca01`. The push was a fast-forward of `be6b8861..a246ca01` (E-71), no force. E-62 and E-70 record the user's verbatim answers `push` and `publish`, each given directly to the orchestrator at a blocking-human checkpoint, with the push run inline. `.gsd-ws-arg` is absent from `M`'s tree. `verify-merge-invariants.sh` is unchanged between `be6b8861` and `M`. |
| 7 | `M`'s SHA has exactly one green `tests.yaml` run; the tag reaches `origin` only after that run is green | VERIFIED | The GitHub API returns `total_count 1` for `head_sha=M`: run 38587062864, `push`, `custom_build`, `completed/success`, `.github/workflows/tests.yaml`, started 15:10:54Z. The rehearsal SHA `d316bc45` also has exactly one run (38578966373, `sync/v1.7.0`, success). E-72 records the tag push at 15:21Z, after the run finished at 15:17:42Z and after a late re-check (148 s later) still showed one green run. The tag started no run: the API still shows one run for `M`. `git ls-remote origin` shows `v1.7.0_gg2` at `a246ca01`, no `sync/v1.7.0` branch (deleted after the tag), and the expected tag set. |
| 8 | The rollback tag points at a commit that already carries the branch-only trigger | VERIFIED | `pre-v1.7.0_gg2` on `origin` is `be6b8861`. `git show be6b8861:.github/workflows/tests.yaml` has `on: push: branches: ['**']` (no `tags` key) and the comment citing CI-08. The tag was pushed before the merge and the run count on `be6b8861` stayed at 1 after a 62 s wait (E-70, ROLLBACK-TAG-PUSHED-NO-RUN). E-31 and E-71 prove the item-6 `read-tree` rollback on a dry-run merge and on `M` itself. `M` is a descendant of `be6b8861`. |

**Score:** 8/8 truths verified (0 present, behavior-unverified)

The one behavior-dependent truth is the dedup-token retry consequence in truth 1. It is not left at presence-only: `DedupTokenRetryLiveTest` (8 cases, committed, in the 988-test runs) exercises the retry through the server and through the real `ClickHouseSinkTask` in both exactly-once modes. E-04 records two seen-red mutations (token removed in `QueryIdentifier`; token removed in `Processing.doInsert` only), and the test is green again after both were reverted. So the claim in the rollout note is pinned by a test that fails without the connector's token.

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `VERSION` | `v1.7.0_gg2` | VERIFIED | Matches at `M`, at HEAD and in the generated class |
| `CHANGELOG.md` | `1.7.0_gg2` section, earlier sections untouched | VERIFIED | Section at the top; 0 deletions |
| `docs/ROLLOUT-v1.7.0_gg2.md` | Operator note, six items, self-checks, rollback | VERIFIED | 357 lines in `M`; no placeholder markers, no TBD/FIXME/XXX |
| `docs/FORK-DELTA.md` | Rows for Phase 17-19 patches, refreshed raw numbers | VERIFIED | Coverage audit clean; raw numbers re-measured by me and equal |
| `src/test/.../DedupTokenRetryLiveTest.java` | Live pins for the dedup outcomes | VERIFIED | 8 test cases; seen red without the connector's token (E-04) |
| `20-REVIEW.md` / `20-REVIEW-DISPOSITION.md` | Review recorded, `open: 0` | VERIFIED | 6 findings, all `fixed` with commit SHAs |
| `20-COMMAND-EVIDENCE.md` | E-10..E-72 | VERIFIED | Entries present; the ones I cross-checked against git and the API agree |
| `20-MILESTONE-CLOSE-RECORD.md` | Carried advisories, L-38, IN-04, three manual-check outcomes | VERIFIED | All three outcomes recorded 2026-10-14; none pending |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| Rehearsed tip `3879b883` | `M` tree | `--no-ff` merge | WIRED | Tree hashes equal |
| Green run on `M` | tag push | ordering | WIRED | Run finished 15:17:42Z; tag pushed 15:21Z; tag spawned no run |
| `pre-v1.7.0_gg2` | `be6b8861` | tag on `origin` | WIRED | Same SHA locally and on `origin` |
| `VERSION` | `Version.ARTIFACT_VERSION` | `generateVersionFile` | WIRED | `build.gradle.kts` lines 48, 435-468 |
| Rollout note item 1 | `DedupTokenRetryLiveTest` | measured outcomes | WIRED | The test's 8 cases cover the matrix the note cites |

### Data-Flow Trace (Level 4)

Not applicable. Phase 20 ships documents, a version string, a test and a publication; it adds no rendered dynamic data.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| `origin/custom_build` is the release merge | `git ls-remote origin` | `a246ca01...` | PASS |
| Merge shape | `git show -s --format=%P M`; tree hash compare | 2 parents; trees equal | PASS |
| Exactly one CI run on `M` | `curl api.github.com/.../actions/runs?head_sha=M` | `total 1`, success | PASS |
| Non-planning tree unchanged since the gate | `git diff d316bc45 HEAD -- . ':(exclude).planning'` | empty | PASS |
| Anchors invariant | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` | `result=PASS` | PASS |
| FORK-DELTA raw numbers | `git diff --shortstat v1.7.0 v1.7.0_gg2 ...` | 119/+18802/-529 and 32/+2042/-143 | PASS |

### Probe Execution

No phase-declared `probe-*.sh`. The only runnable check the phase names is the anchors invariant, run above. Not applicable otherwise.

### Prohibitions (must_haves.prohibitions)

All plan-level prohibitions are `status: resolved`. The ones that could be checked from the repository were checked:

| Prohibition | Result |
|---|---|
| gg1 note and released CHANGELOG sections not edited | Holds: 0 diff lines (`be6b8861` to `M`) |
| No TRUNCATE or unscoped DELETE for the shared KeeperMap table | Holds: not present in the note at `M` |
| No `.gsd-ws-arg`, no secret in the merged tree | Holds: not in `M`'s tree; no secret patterns in the note |
| `verify-merge-invariants.sh` not edited | Holds: 0 diff lines |
| Only `custom_build`, the rollback tag, `v1.7.0_gg2` and the scratch deletion pushed; no force, no `--tags` | Holds: final origin refs match the expected set (E-72, re-read by me via `ls-remote`) |
| No push before the user's direct answer | Recorded verbatim in E-62 and E-70; the orchestrator received both directly. Not independently checkable from git; judgment-tier, resolved in the evidence |
| No agent changed a repository setting | The close record says the user made the default-branch change in the web UI. Not independently checkable; judgment-tier |
| No token printed or committed | Not touched by this verification; no token string found in the phase files I read |

The two judgment-tier items that depend on session behaviour are carried as NON-AUTHORITATIVE, resolved by the recorded evidence. They are not blockers.

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|---|---|---|---|---|
| ROLL-02 | 20-01, 20-02, 20-03, 20-04, 20-05 | Rollout note with 999.1, dedup-token retry, empty-CDC and MBean items, reviewed before the gate | SATISFIED | Truth 1; REQUIREMENTS.md line 50 `[x]`, traceability row Complete |
| PUB-07 | 20-02, 20-04, 20-05 | `VERSION`, CHANGELOG section, FORK-DELTA rows | SATISFIED | Truths 2-4; REQUIREMENTS.md line 51 |
| SUITE-05 | 20-06 | Full suite green twice on `latest` x V2; `testUnit` green with Docker stopped | SATISFIED | Truth 5; REQUIREMENTS.md line 52 |
| PUB-08 | 20-06, 20-07, 20-08 | Non-force `--no-ff` merge after approval, one green run, tag after green, rollback tag with branch-only trigger | SATISFIED | Truths 6-8; REQUIREMENTS.md line 53 |

All four IDs from the phase are claimed by at least one PLAN's `requirements:` field (ROLL-02 in 20-01..20-05, PUB-07 in 20-02/04/05, SUITE-05 in 20-06, PUB-08 in 20-06/07/08). REQUIREMENTS.md maps no other ID to Phase 20, so there are no orphaned requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| `docs/ROLLOUT-v1.7.0_gg2.md`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, `DedupTokenRetryLiveTest.java` (at `M`) | n/a | TBD / FIXME / XXX grep | None | No hits: the debt-marker gate is clear |

### Deferred Items

None. The items carried to the next milestone (19-REVIEW WR-01, WR-02, IN-01..IN-04, the Dependabot `gradle` "Last checked" glance, backlog 999.3) are recorded with reasons and re-open triggers in `20-MILESTONE-CLOSE-RECORD.md`. They are not gaps against a Phase 20 must-have.

### Human Verification Required

None. The three post-publication manual checks (Dependabot version updates, default branch, `exclude-paths` validity) were answered by the user and recorded on 2026-10-14; none is pending. They were not part of the roadmap success criteria.

### Notes for the orchestrator (non-blocking)

1. `ROADMAP.md` still shows `20-08-PLAN.md` unchecked, "Plans: 7/8 plans executed", the progress-table row `7/8 In Progress` and Phase 20 unchecked in the milestone list, and `STATE.md` has `status: verifying`, `completed_phases: 4`. `20-08-SUMMARY.md` exists and `REQUIREMENTS.md` marks all four requirements complete. This is tracking lag that the phase-complete step normally resolves after verification; it is not a codebase gap.
2. Manual check 3 is recorded as "no configuration error seen" with the first Dependabot job still running, so the `gradle` entry's "Last checked" time was not yet visible. The close record carries that forward to the next milestone, and the roadmap criteria do not depend on it.
3. `20-MILESTONE-CLOSE-RECORD.md` housekeeping is still open for the user: revoke the read-only GitHub token and delete Keychain item `gsd17-gh-actions-read`. This verification did not touch it.
4. The untracked `.planning/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md` is the stale 2026-10-10 snapshot; `/gsd-audit-milestone` is expected to replace it (D-12).

### Gaps Summary

No gaps. Every roadmap success criterion for Phase 20 holds against git objects, live `origin` refs and the GitHub Actions API, and every requirement ID is accounted for. The suite result (truth 5) rests on the pinned gate evidence by agreement, supported by the proof that the shipped non-planning tree is byte-identical to the gated one.

---

_Verified: 2026-10-14T19:30:00Z_
_Verifier: Claude (gsd-verifier)_
