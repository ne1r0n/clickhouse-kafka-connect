---
phase: 38-release-publication
verified: 2026-11-01T12:30:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - .planning/phases/38-release-publication/38-01-PLAN.md
  - .planning/phases/38-release-publication/38-01-SUMMARY.md
  - .planning/phases/38-release-publication/38-02-PLAN.md
  - .planning/phases/38-release-publication/38-02-SUMMARY.md
  - .planning/phases/38-release-publication/38-03-PLAN.md
  - .planning/phases/38-release-publication/38-03-SUMMARY.md
  - .planning/phases/38-release-publication/38-04-PLAN.md
  - .planning/phases/38-release-publication/38-04-SUMMARY.md
  - .planning/phases/38-release-publication/38-05-PLAN.md
  - .planning/phases/38-release-publication/38-05-SUMMARY.md
  - CHANGELOG.md
  - VERSION
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg5.md
  - docs/ROLLOUT-v1.7.0_gg6.md
covered_digest: "v4:sha256:1e3dcc401c2c5a905484018709da179f619df0f6eb3f5594d119a06273331424"
behavior_unverified: 0
overrides_applied: 0
re_verification: false
---

# Phase 38: Release & Publication Verification Report

**Phase Goal:** `v1.7.0_gg6` is published on `custom_build`. Operators get an independently reviewed rollout note that labels every operator-visible effect with its evidence. The gg6 allow-list compare passes on the final tree. The suite is green twice on one pinned `latest` build, and the published SHA has one green `tests.yaml` run before the user's publish run, which creates `v1.7.0_gg6`.
**Verified:** 2026-11-01 (UTC)
**Status:** passed
**Re-verification:** No, initial verification

The method was goal-backward. The publication facts were re-read from git and GitHub with read-only calls (`git ls-remote`, `git cat-file`, `gh api` GETs, `gh run view`). The documents were read from the published tree (`git show 72e8ccff:...`). The test-suite evidence in `38-COMMAND-EVIDENCE.md` (E-12, E-13) cannot be re-run here, so it is cross-checked against the code state (the gated head and the published tree differ only in phase-38 planning files) and against the CI runs of the same tree.

## Goal Achievement

### Observable Truths (ROADMAP criteria 1 to 5)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | After `clean`, `VERSION` reads `v1.7.0_gg6` and the generated version agrees; the anchor row changes in the same commit; the CHANGELOG has a complete `1.7.0_gg6` section absorbing upstream's `Unreleased`; FORK-DELTA is final with refreshed numbers (RLS-01) | VERIFIED | `git show 72e8ccff:VERSION` = `v1.7.0_gg6`. `fork-anchors.txt` at M has the row `VERSION v1.7.0_gg6 1 BLD-07`. Commit `70bf8108` touches exactly `VERSION` and `.planning/scripts/fork-anchors.txt`. E-02 read the generated constant `ARTIFACT_VERSION = "v1.7.0_gg6"` after `clean generateVersionFile`. CHANGELOG at M starts with `# 1.7.0_gg6, 2026-10-31` and has no `# Unreleased` heading (it was at line 346 of the gg5 tip file, `additionalRetriableErrorCodes` entry now inside the section). The released sections are intact. FORK-DELTA was refreshed (E-04, commit `21697379`). No TBD/FIXME/XXX/TODO in the added lines of the five documents. |
| 2 | `docs/ROLLOUT-v1.7.0_gg6.md` labels each operator-visible effect with one of four defined labels, covering `ssl`/`sslmode`, 159 cadence and 502/503, `User-Agent`, socket buffers and zstd, `additionalRetriableErrorCodes` with its `errors.tolerance` bypass, the clean-stop requirement, and the buffered offset-commit change (RLS-02) | VERIFIED | I ran `38-note-check.py` (read-only) on the note extracted from M: `rows=29 measured=19 code=18 derived=3 research=9`, `labels: 4 of 4 defined`, `forbidden: 0 lines`. Its one `claims` hit is the SHA-256 of the gg5 release asset, which I confirmed is real (`gh api releases/tags/v1.7.0_gg5`, digest `4c69b6fb...6767`) and which the plan allows. Rows: B1 to B8 (buffered offsets, BUF-07), C1 to C3 (`additionalRetriableErrorCodes`, bypass), D1 to D14 (`ssl`/`sslmode` D1, 159 D2, 502/503 D3, `User-Agent` D5, zstd D9, socket buffers D10, clean stop D11), E1 to E4. Client 0.11.0 shipped, so CLT-09's fallback did not fire. |
| 3 | A fresh-context `gsd-code-reviewer` checked the note, CHANGELOG and FORK-DELTA before the push gate; every finding is dispositioned (RLS-04); the gg6 allow-list was generated last and the compare passes (RLS-03) | VERIFIED (with human-judgment items below) | `38-REVIEW.md`: 12 rounds, 43 findings (28 warning, 15 info, 0 critical). `38-REVIEW-DISPOSITION.md`: `open: 0`, 42 fixed with commit SHAs, 1 deferred with reason, owner and trigger (WR-08, gg1 half). The allow-list `confluent-archive-allowlist-v1.7.0_gg6.txt` exists in M. The compare ran on a runner twice against the real archive: dry run 41257794165 and publish run 41298074815, both `exit 0, 0 unallowed, 0 stale rows`, baseline `v1.7.0_gg5`, report `RESULT: PASS` (steps `Compare the archive with the previous release` success). That is runner evidence for RLS-03, independent of the local gate. |
| 4 | On the release tip, two full `--rerun` suites pass on one pinned `latest` build with client V2, `SELECT version()` recorded, the named skips; `testUnit` passes with Docker unreachable; anchors gate passes (RLS-06) | VERIFIED (JDK 21 record accepted by the user) | E-13: image digest `sha256:739e099a...b1ce` and `SELECT version()` = 26.10.3.7 read before run 1, after run 1 and after run 2 (unchanged). Suite 1 and suite 2: 2270 tests, 0 failures, 0 errors, 18 skips (`named=7 state_mismatch_cases=11 unexpected=0`). E-12: `testUnit` with `DOCKER_HOST` at a nonexistent socket passed 1672 tests; anchors mirror `rows=172 bad=0`. Cross-check: `git diff --name-only ea613ec3 a46be37f` outside `.planning/phases/38` is empty, so the gated head and the published tree differ only in phase-38 planning files. M's tree equals a46be37f's, and the CI run `tests.yaml` 41297502026 on M is green. Both full suites ran on JDK 21 after two OpenJ9 JDK 17 crashes; see the disposition section. |
| 5 | The release dry run passed; divergence check measured; after the user's approval `custom_build` is a non-force `--no-ff` merge with tree equal to the rehearsed tip; the published SHA has exactly one `tests.yaml` run, green before the publish run, which created the tag and release; only `pre-v1.7.0_gg6` was hand-pushed; scratch branch deleted (RLS-05) | VERIFIED | See the publication table below. |

**Score:** 5/5 truths verified. No behavior-dependent truth was left unexercised (`behavior_unverified: 0`).

### Publication facts, re-read independently (RLS-05)

| Claim | Re-read result | Status |
|-------|----------------|--------|
| `origin/custom_build` = `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` | `git ls-remote origin` returns that SHA for `refs/heads/custom_build` and `HEAD` | OK |
| M is a `--no-ff` merge of `a46be37f` into `3a3a15a9` | `git rev-list --parents -n1 72e8ccff` = M, `3a3a15a9ae06...`, `a46be37f4971...` (two parents). `3a3a15a9` is an ancestor of M. | OK |
| M's tree equals the rehearsed tip | `72e8ccff^{tree}` = `a46be37f^{tree}` = `c9b652c2040ee4b7b3c146cd0e9fd01dc848b7d3` | OK |
| Non-force push | `3a3a15a9` is an ancestor of M, so the push was a fast-forward (E-23 records `3a3a15a9..72e8ccff fast-forward`) | OK |
| Tag `v1.7.0_gg6` on origin at M, created by the publish run | `git ls-remote origin refs/tags/v1.7.0_gg6` = `72e8ccff`. `gh api git/ref/tags/v1.7.0_gg6` object type `commit` (lightweight). Release id 430511927, author `github-actions[bot]`, not draft, not prerelease, `target_commitish` = M. Publish run 41298074815: `workflow_dispatch`, head `72e8ccff`, success, step `Publish release` success, the four dry-run steps skipped. | OK |
| Release asset | `clickhouse-kafka-connect-v1.7.0_gg6.zip`, 13558692 bytes, digest `sha256:e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af`, uploader `github-actions[bot]`. Same size and digest as the dry-run archive (E-21). | OK |
| Exactly one successful `tests.yaml` run on M, before the publish run | `gh api actions/runs?head_sha=72e8ccff` returns 6 runs; the only one from `tests.yaml` is 41297502026 (push, `custom_build`, success, created 10:26:35Z, finished 10:33:32Z). The others are the publish run, Dependabot dynamic runs and a skipped scheduled nightly. The publish run was created at 10:34:19Z, after the green run finished. The publish workflow also has a `Check tests.yaml is green on this SHA` step, which passed. | OK |
| `pre-v1.7.0_gg6` on origin at `3a3a15a9` | `git ls-remote origin refs/tags/pre-v1.7.0_gg6` = `3a3a15a9ae064ffe09c696b8bb87bce23419d463` (equals `v1.7.0_gg5`) | OK |
| Scratch branch `sync/v1.7.0` deleted from origin | `git ls-remote origin refs/heads/sync/v1.7.0` returns nothing | OK |
| Dry run green | Run 41257794165: `Release sync/v1.7.0 (dry run)`, `workflow_dispatch`, head `a46be37f`, success. The scratch push also had one green `tests.yaml` run (41257363463). | OK |
| Divergence check expected empty | E-20: `precheck_left_count: 0` before the scratch push, and `origin/custom_build` = `3a3a15a9` at the merge base. Consistent with M having `3a3a15a9` as first parent. | OK |
| No agent hand-pushed the release tag | The tag's release author is `github-actions[bot]`. E-26 lists only `pre-v1.7.0_gg6` pushed by name. | OK |
| Release object of gg5 unchanged | `gh api releases/tags/v1.7.0_gg5`: id 426690002, author `ne1r0n` | OK |
| Pull-request states (A1) | #16, #18, #19, #20 closed and merged; #10 and #17 closed, not merged | OK (informational) |

Authorisation note: E-20 to E-27 record that the orchestrator executed the pushes and dispatches after the user's direct messages («делай» and the answer to the date question). The tag and release still came from the user-authorised `release.yaml` dispatch and not from a hand push, which is what the ROADMAP contract requires.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `VERSION` | `v1.7.0_gg6` | VERIFIED | Read at M |
| `.planning/scripts/fork-anchors.txt` | VERSION row at gg6 | VERIFIED | Row 41, same commit as VERSION |
| `CHANGELOG.md` | complete gg6 section, Unreleased absorbed | VERIFIED | Heading present, no Unreleased heading, retained entry text present |
| `docs/FORK-DELTA.md` | final, refreshed | VERIFIED | Present at M; reviewed in the 12 rounds |
| `docs/ROLLOUT-v1.7.0_gg6.md` | 29 labelled rows | VERIFIED | 274 lines at M; note check OK |
| `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt` | generated last | VERIFIED | Present at M; compare PASS on two runner runs |
| `38-COMMAND-EVIDENCE.md`, `38-RELEASE-RECORD.md`, `38-REVIEW.md`, `38-REVIEW-DISPOSITION.md` | evidence | VERIFIED | Content matches the independent re-reads above |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| CHANGELOG 1.7.0_gg6 section | `docs/ROLLOUT-v1.7.0_gg6.md` | link in the section's first paragraph and `Important Changes` | WIRED (target exists at M) |
| `release.yaml` publish | tag and release | workflow token (`Publish release` step) | WIRED (E-25 and my re-read) |
| `tests.yaml` green on M | publish run | `Check tests.yaml is green on this SHA` step | WIRED (step success) |

### Scope check (Phase 29 D-05 standing rule)

Files changed by Phase 38 outside `.planning/`, from `git diff --name-only 0a9599f6^ a46be37f`: `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg5.md`, `docs/ROLLOUT-v1.7.0_gg6.md`. No `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file was touched, as the phase note requires.

### Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|----------|
| RLS-01 | VERSION with anchor in one commit, CHANGELOG, FORK-DELTA | SATISFIED | Criterion 1 |
| RLS-02 | rollout note with four labels for each effect | SATISFIED | Criterion 2 |
| RLS-03 | allow-list generated last, compare passes | SATISFIED | Criterion 3; runner compares on the dry run and the publish run |
| RLS-04 | fresh reviewer before the push gate | SATISFIED (human-judgment items below) | `38-REVIEW.md`, `38-REVIEW-DISPOSITION.md` |
| RLS-05 | dry run, divergence check, `--no-ff` merge, one green `tests.yaml`, publish run creates the tag, only the pre-tag pushed by hand | SATISFIED | Publication table |
| RLS-06 | two suites on one pinned build, `testUnit` without Docker | SATISFIED (JDK 21 record accepted) | Criterion 4 |

No orphaned requirement: REQUIREMENTS.md maps exactly RLS-01 to RLS-06 to Phase 38, and all six are covered by the five plans.

### Behavioral Spot-Checks and Probes

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Rollout note structure (labels, rows, forbidden text) | `python3 -I 38-note-check.py all <note at M>` | OK on rows, labels, forbidden; the single hex string is the real gg5 asset digest, allowed by the plan | PASS |
| Full test suites | Not re-run here. They need Docker and several minutes, and Step 7b forbids running a full suite for this check. | Evidence from E-12 and E-13, plus the green `tests.yaml` run 41297502026 on M | SKIPPED, covered by recorded runs |

### Anti-Patterns Found

None. The added lines of the five changed documents hold no TBD, FIXME, XXX or TODO marker.

### Data-Flow Trace

Not applicable: the phase changes no runnable code.

## Human-judgment disposition

These items were decided by the user before this verification. They are recorded here and are not gaps.

| Item | Decision / state | Owner and re-open trigger |
|------|------------------|---------------------------|
| JDK 21 record of the two full suites | Both suites ran with `-PjavaVersion=21` after two crashes of the default OpenJ9 JDK 17 in the Mockito inline mock maker (traceassert and gpf dumps in `retransformClasses0`). The user accepted criterion 4 as met on JDK 21. `testUnit` without Docker ran on the default JDK 17. | Fork maintainer; a full suite that must run on JDK 17, or a host JDK change |
| IN-14 and IN-15 fixed without a thirteenth review round | The review loop ended on the user's decision («Исправить два info и закрыть ревью»), not on a round that found nothing. The two fixes (`fd8b8575`, `927ac07c`) were not read by a fresh reviewer; the owning checks were re-run after both. | Fork maintainer; the next edit of the first-start subsection of the note |
| WR-08, gg1 half deferred | The `docs/ROLLOUT-v1.7.0_gg1.md` edit is outside this phase's editable set. The FORK-DELTA half was fixed in `f1e32844`. | Fork maintainer; the next edit of that note, or the milestone close |
| IN-04 pin gap | The wording of row D7 is fixed. No Docker-free test pins the forwarding of a `custom_` key on client 0.11.0; adding one is outside this phase's editable set. | Fork maintainer; the next client bump or the next edit of `createClientV2` |
| CHANGELOG date deviation (E-22) | The heading reads `2026-10-31`, the merge and publication happened on 2026-11-01 (UTC). The user chose to keep it. | None; recorded in `38-RELEASE-RECORD.md` |

Other items the phase records as carried to the milestone close (FORK-DELTA open items 12 to 23, the integration suite not re-run, the nightly acceptance on the client-bump scratch tip, the two `scratch/clt-07-*` branches left on origin) are owned, triggered and documented in `38-LOCAL-CLOSE-RECORD.md`. They are not Phase 38 must-haves.

## Housekeeping for the orchestrator

These are bookkeeping items in files this verifier does not edit, and they are not gaps:

- `.planning/REQUIREMENTS.md` still lists RLS-01 to RLS-06 as `Pending` (checkboxes and traceability rows), and the Phase 38 entry in `.planning/ROADMAP.md` is still unchecked. Item 7 of `38-LOCAL-CLOSE-RECORD.md` held RLS-05 back until the publication; with E-20 to E-27 now recorded and verified, all six can flip to Complete.
- Outstanding wording changes 8 to 11 of `38-LOCAL-CLOSE-RECORD.md` (SWP-03 wording, the Phase 38 list line, the Phase 33 note about the pre-tag, the progress row) are still the orchestrator's.
- The two branches `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` remain on origin, and the new Dependabot PRs #21 and #22 are open. Both were left to the user and are out of scope.

## Gaps Summary

No gaps. Every ROADMAP criterion is backed by an artifact I re-read: the origin refs, the merge commit and its parents and tree, the tag and release written by `github-actions[bot]`, the single green `tests.yaml` run that finished before the publish run started, the green dry run, the deleted scratch branch, and the documents at the published SHA. The accepted human-judgment items do not change the status.

---

_Verified: 2026-11-01_
_Verifier: Claude (gsd-verifier)_
