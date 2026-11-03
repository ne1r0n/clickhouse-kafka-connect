---
phase: 44-release-publication
verified: 2026-11-04T09:00:00Z
status: passed
score: 5/5 must-haves verified (3 with user-accepted deviations carried as overrides)
covered_files:
  - .planning/phases/44-release-publication/44-01-PLAN.md
  - .planning/phases/44-release-publication/44-01-SUMMARY.md
  - .planning/phases/44-release-publication/44-02-PLAN.md
  - .planning/phases/44-release-publication/44-02-SUMMARY.md
  - .planning/phases/44-release-publication/44-03-PLAN.md
  - .planning/phases/44-release-publication/44-03-SUMMARY.md
  - .planning/phases/44-release-publication/44-04-PLAN.md
  - .planning/phases/44-release-publication/44-04-SUMMARY.md
  - .planning/phases/44-release-publication/44-05-PLAN.md
  - .planning/phases/44-release-publication/44-05-SUMMARY.md
  - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
  - .planning/scripts/fork-anchors.txt
  - CHANGELOG.md
  - VERSION
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg7.md
covered_digest: "v4:sha256:91bbfcea069b2007128b57061fde7bd6d58cf2ee36f3a18db21c4ee760f3058d"
behavior_unverified: 0
overrides_applied: 3
overrides:
  - must_have: "Rounds repeated until one found nothing (at most three unless the user authorises more); a fresh gsd-code-reviewer reviewed the note, CHANGELOG, FORK-DELTA and the Phase 43 published notes"
    reason: "Five fresh-reviewer rounds ran (14, 2, 2, 2, 5 findings) and none came back empty. The user authorised round 4 (G1 after round 3), asked for round 5 (G3, measure T6 first) and declined a round 6 ('Accept without round 6 (Recommended)', E-28 review_end_state). The last fixes (IN-13, IN-14 and WR-07, WR-08, IN-15..IN-17) were not read by a fresh reviewer."
    accepted_by: "the user, directly to the orchestrator through its question tool (E-09 round3_decision, E-28)"
    accepted_at: "2026-11-03T13:30:00Z"
  - must_have: "The user launches release.yaml and the user gives a direct approval in their own words (the user runs the origin writes)"
    reason: "The orchestrator ran the three origin writes (push of sync/v1.7.0, push of M, the pre-tag and scratch deletion) and dispatched the publish run, each on the user's own message ('делай сам по документу то что нужно', 'publish', 'push m', 'dispatch the publish run', 'run U5'). U4 was dispatched on an explicit override of the runbook rule that U4 stays the user's (E-33). GitHub shows triggering_actor ne1r0n for the publish run (the user's credentials)."
    accepted_by: "the user, directly to the orchestrator (E-28 carried answer 'You run them (Recommended)', E-29, E-31, E-32, E-33, E-35)"
    accepted_at: "2026-11-03T19:40:00Z"
  - must_have: "The CHANGELOG heading carries the merge date in UTC"
    reason: "The heading reads 2026-11-02 (the freeze date); the merge and publish happened on UTC 2026-11-03. The user chose 'Keep the date (Recommended)'."
    accepted_by: "the user, through the orchestrator's question tool (E-28)"
    accepted_at: "2026-11-03T13:30:00Z"
gaps: []
human_verification: []
---

# Phase 44: Release & Publication Verification Report

**Phase Goal:** `v1.7.0_gg7` is published on `custom_build`. Operators get an independently reviewed rollout note that states what F-1, F-2 and the four RSK items change for them, each claim labelled by its evidence. The gg7 allow-list compare passes on the final tree, the suite is green twice, the published SHA has one green `tests.yaml` run before the publish run creates the tag, and every push, the merge and the publish run happen only on the user's direct word.
**Verified:** 2026-11-04
**Status:** passed (with three user-accepted deviations and the human-judgment items listed below)
**Re-verification:** No, initial verification

## Independent spot-checks (read-only, run by the verifier)

| Check | Command | Result |
|-------|---------|--------|
| origin `custom_build` equals M | `git ls-remote origin`; `gh api .../branches/custom_build` | both read `9483476322e0d331e81c5b92ac266a18f0f01450`; `git rev-list --left-right --count M...origin/custom_build` is `0 0` |
| M is a two-parent `--no-ff` merge | `git log --format='%H %P' -1 M` | parents `72e8ccff...` (previous `custom_build`, gg6) and `fc570937...` (X0) |
| M's tree equals X0's tree | `git rev-parse M^{tree} fc570937^{tree}` | both `ebe6c13ae266a9250d6d15ee0933cf5d4da6cc53` |
| X0 differs from the gated head `06a9980a` only under `.planning` | `git diff --name-only 06a9980a fc570937 -- . ':(exclude).planning'`; same for M; `.planning/scripts` | empty, empty, empty |
| Phase 44 edited no `src`, test, gradle or `.github` file | `git diff --name-only 9a5209c1 M -- . ':(exclude).planning'` | only `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg7.md` |
| Tag ref | `gh api .../git/ref/tags/v1.7.0_gg7`; `git ls-remote --tags origin` | `refs/tags/v1.7.0_gg7`, object type `commit`, SHA `9483476322e0d331e81c5b92ac266a18f0f01450` (= M, lightweight tag) |
| Pre-tag | `git ls-remote --tags origin` | `pre-v1.7.0_gg7` = `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` (= previous `custom_build`, `v1.7.0_gg6`) |
| Release | `gh api .../releases/433237218` | tag `v1.7.0_gg7`, `draft=false`, `prerelease=false`, author `github-actions[bot]`, `target_commitish` = M |
| Release asset | same call | exactly one: `clickhouse-kafka-connect-v1.7.0_gg7.zip`, 13564238 bytes, digest `sha256:3dbdcf03e5825073b7b0535718c9998a5e5754c0ae05a17502bbf4c6d69472af`, uploader `github-actions[bot]`. Size and digest equal the dry run's (E-30). |
| One `tests.yaml` run on M | `gh api ".../actions/runs?head_sha=M"` | one `ClickHouse Kafka Connect Tests CI` run (41704437509, event `push`, success; job `ClickHouse latest Client version V2 tests` success), plus the publish run |
| Publish run | `gh api .../actions/runs/41721771615`; release.yaml run list | `Release custom_build (publish)`, `workflow_dispatch`, branch `custom_build`, head SHA M, success; created 21:26:10Z, after the green push run finished (19:39:14Z). It is the only release.yaml run on `custom_build` for gg7 (earlier runs: 41688323407 dry run on `sync/v1.7.0`, 41298074815 is gg6). |
| Scratch branch gone | `git ls-remote --heads origin` | `sync/v1.7.0` absent; heads are `custom_build`, `main`, two `scratch/clt-07-*` (left alone, as E-35 says) |
| PRs #21, #22 | `gh api .../pulls/21`, `/22` | closed, `merged=true`, `merged_at 2026-11-03T19:33:58Z`, base `custom_build` |
| `VERSION` at M | `git show M:VERSION` | `v1.7.0_gg7`; bump commit `1d7383b9` changes only `VERSION` and `.planning/scripts/fork-anchors.txt` (1 line each) |
| CHANGELOG | `git show M:CHANGELOG.md` | line 1 `# 1.7.0_gg7, 2026-11-02`; links to `docs/ROLLOUT-v1.7.0_gg7.md` |
| Allow-list | `git ls-files .planning/scripts` | `confluent-archive-allowlist-v1.7.0_gg7.txt` exists in M's tree: 18 rows, three gson rows (`com/google/gson/*`, `META-INF/maven/com.google.code.gson/gson/*`, `META-INF/versions/9/module-info.class`) and 14 exact connector class paths plus `MANIFEST.MF`; last changed 2026-11-03 (402b6e2e), after the docs froze |
| Rollout note at M | `git show M:docs/ROLLOUT-v1.7.0_gg7.md` run through `44-note-check.py all` | `rows=33 measured=24 code=21 derived=4 mechanism=1`, `labels 4 of 4`, `forbidden 0`, `fut OK`; `claims` fails only for one hex string that the checker accepts with `--allow e745e7d5...` (the same value E-24 passes). The T6 row is 5 cells (WR-08 fix present), and the FORK-DELTA no longer says "default settings" (WR-07 fix present). |
| Evidence ids E-01..E-36 | script over `44-COMMAND-EVIDENCE.md` | every id has exactly one `### E-NN` heading (E-12 and E-24 also appear in later headings as cross-references) and exactly one index row; E-15..E-18 index rows and entries read `pending (user-gated)` |
| E-15..E-18 unedited | hashed the E-15..E-19 block over every revision of the file | one revision only differs, by a single blank line when E-19 was appended (`d2f56ad4` to `5d6bd098`); content unchanged |

## Goal Achievement

### Roadmap success criteria

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| 1 | `VERSION` reads `v1.7.0_gg7` via `bump_version.py`, generated version agrees after `clean`; CHANGELOG `1.7.0_gg7` section; FORK-DELTA final with raw numbers refreshed once; every `fork-anchors.txt` row moves with its count | VERIFIED | E-02 (bump stdout `v1.7.0_gg7`, `version_clean_exit: 0`, generated `ARTIFACT_VERSION = "v1.7.0_gg7"`, bump commit `1d7383b9` touches `VERSION` plus the anchors row, mirror `rows=190 bad=0`, baseline `v1.7.0_gg6`); E-03 (CHANGELOG section, released sections byte-identical); E-04 (FORK-DELTA recounts and raw numbers, `numbers_check` OK); the invariant script ended `result=PASS` on the gated head (E-24, E-25 `invariants_line`). Re-read independently: `VERSION` and CHANGELOG at M. CHANGELOG heading date: see override 3. |
| 2 | gg7 allow-list generated last, from the final tree, exact/literal-prefix rows and the gson rows; dry-run compare `0 unallowed, 0 stale rows` | VERIFIED | E-11 and E-23 (regenerated last after the T6 documents; 18 rows cover all 120 differences, `list_cover ... uncovered=0 stale_rows=0`, mutation `STALE-ROW` turns it red, `list_compare_exit: 0`). Corroborated on GitHub, not by the executor: dry run E-30 and the publish run E-34 both printed `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6`. |
| 3 | *[record]* Rollout note covers F-1, F-2, RSK-01..04 with labelled claims, F-2 content as specified; a fresh `gsd-code-reviewer` reviewed the note with CHANGELOG, FORK-DELTA and the Phase 43 notes; rounds repeated until one found nothing; every finding dispositioned | PASSED (override 1) | Note and checker: E-05..E-07, re-run at M above (33 rows, labels 4 of 4, `FUT-05 stays open.` once, F-2 rows T1..T15 with `socket_timeout`, `exactlyOnce=false`, scope and unbounded cases). Review: `44-review-check.py agents` reads `rounds=5 distinct=5` ids. All 25 findings dispositioned (24 fixed, 1 deferred IN-03, 0 open), each fixed row names its commit (`44-REVIEW-DISPOSITION.md`). The literal clause "until one found nothing" is NOT met: round 4 found 2, round 5 found 5, and the fixes after rounds 4 and 5 were not re-read by a fresh reviewer. The user accepted that twice (override 1). The verifier re-read the round-5 fixes WR-07 and WR-08 in M's tree (present) but is not an independent fresh reviewer of the rest. |
| 4 | *[live]* Two full `--rerun` suites on one pinned `latest` with client V2, `SELECT version()` and named skips recorded; `testUnit` passes with Docker unreachable; anchors gate passes | VERIFIED (recorded evidence; not re-run by the verifier) | E-24: `testUnit` exit 0, 1826 tests, 0 failures, 2 skipped, with `DOCKER_HOST=unix:///nonexistent/docker.sock` (proof `exit=1 failed to connect`), anchors mirror `rows=190 bad=0`. E-25: two suites of 2425 tests, 0 failures, 0 errors, 18 skipped (`named=7 state_mismatch_cases=11 unexpected=0`), `SELECT version()` `26.10.3.7`, one digest `sha256:739e099a...` before and after both runs, `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`. The decision `no-go` of E-24/E-25 was a hygiene hit in a planning file only, turned `go` by E-26. Independent corroboration: M's `tests.yaml` run (latest, client V2) is green. Caveats: the verifier ran no Gradle or Docker; the suites ran with `-PjavaVersion=21` because the default OpenJ9 17 test JVM could not load class 65 (recorded, user answered "JDK 21 is enough", E-28); only one integration class was re-run (E-22, 55 passed), which the criterion does not require. |
| 5 | *[operator, CI]* User's direct approval; non-force `--no-ff` merge whose tree equals the rehearsed tip; exactly one `tests.yaml` run on the published SHA, green before the user launches `release.yaml`; the publish run creates tag and release | PASSED (override 2) | Independent reads above: M has two parents and tree `ebe6c13a...` = X0's tree; push `72e8ccff..94834763` was a fast-forward (E-32), `custom_build` still equals M; one `tests.yaml` run, success, finished 19:39Z, publish dispatched 21:26Z; the publish run's tag-absent notice (E-34 `tag_absent_notice`) shows the tag did not exist before it, and the tag ref points at M with release 433237218 made by `github-actions[bot]`. Approvals: the user's own words are recorded in E-28 (question-tool answers), E-29, E-31 ("publish", a second time), E-32 ("push m"), E-33 ("dispatch the publish run"), E-35 ("run U5"). The user did not type the origin writes themselves (override 2). |

### Requirements

| Requirement | Status | Evidence |
|-------------|--------|----------|
| RLS-01 `VERSION=v1.7.0_gg7` via `bump_version.py`; CHANGELOG, FORK-DELTA and `fork-anchors.txt` updated with the counts they change | SATISFIED | E-02, E-03, E-04; `git show --stat 1d7383b9`; `VERSION` and CHANGELOG read at M; invariant script PASS (E-24, E-25) |
| RLS-02 allow-list generated from the final tree with gson rows; dry run `0 unallowed, 0 stale rows` | SATISFIED | E-11, E-23, E-30, E-34; file present at M |
| RLS-03 `docs/ROLLOUT-v1.7.0_gg7.md` covers F-1, F-2 (duplicate risk with `exactlyOnce=false`), RSK-01..04, each claim labelled; passed an independent review by a fresh `gsd-code-reviewer` | SATISFIED with a user-accepted gap (override 1) | E-05..E-10, E-21; `44-REVIEW.md`, `44-REVIEW-DISPOSITION.md`; note re-checked at M. The review ended without a clean round and without a fresh reader for the last fixes. |
| RLS-04 two full runs on `latest` x V2 green; `--no-ff` merge only after the user's direct approval; one green `tests.yaml` on the published SHA; tag created by the publish run | SATISFIED with a user-accepted process deviation (override 2) | E-13, E-24..E-26 (suites, Docker-free), E-28..E-36; independent GitHub and git reads above |

REQUIREMENTS.md still shows RLS-01, RLS-03 and RLS-04 as `Pending` (RLS-02 `Complete`); the orchestrator updates it.

## Human-judgment items and deviations

1. **Review loop ended without a clean round.** Five fresh reviewers; findings 14, 2, 2, 2, 5; none empty. IN-13/IN-14 (after round 4) and WR-07, WR-08, IN-15, IN-16, IN-17 (after round 5, commit `5b25c18c`) were fixed and never re-read by a fresh `gsd-code-reviewer`. The user accepted this ("Accept without round 6", E-28). Residual risk: a defect introduced by the round-5 fixes in the T6 text, the FORK-DELTA item 14 title or the CHANGELOG line would be unreviewed in a published operator document. The verifier confirmed only that the WR-07 and WR-08 fixes are present at M (T6 row is 5 cells, "default settings" removed, rows=33, labels OK); IN-15..IN-17 and the accuracy of the new T6 wording were not independently re-checked.
2. **CHANGELOG heading date `2026-11-02`, merge and publish on UTC `2026-11-03`.** The user kept the date (E-28). The roadmap note says the heading carries the merge date in UTC, so the published heading is one day off by design.
3. **Origin writes were run by the orchestrator, not the user.** Each on the user's direct message. E-33 records U4 as an explicit user override of the runbook rule that U4 stays the user's. The ROADMAP wording "the user launches `release.yaml`" is met only in that sense. The E-28 "go" and the earlier answers were given through the orchestrator's question tool, with the recommended option selected; the verifier cannot read the user's messages and relies on the orchestrator's quotations in E-28..E-35.
4. **IN-03 deferred.** The planning identifier "Phase 39" that Phase 43 added to the published `docs/ROLLOUT-v1.7.0_gg6.md` (row D7) stays; owner the fork maintainer, re-open trigger the next edit of that note (`44-REVIEW-DISPOSITION.md`).
5. **Only one integration class re-run** (`ClickHouseSinkConnectorIntegrationTest`, 55 passed, E-22, on a docs-only change after the last `src` change). The full `integrationTest` task was never re-run in Phase 44; the criterion asks for the two `test` suites only.
6. **Suites ran on JDK 21 (`-PjavaVersion=21`), not the default JDK 17 line**, for the gated head (E-25), because the default OpenJ9 test JVM cannot load class version 65 on the machine. E-13 (older head `b265f5a4`) ran on the default line. The user accepted JDK 21 (E-28).
7. **Gated head moved once.** The first gate was voided by the T6 re-open (round 5, user's G3 answer "Measure the T6 progress-header claim first"); the final gates are on `06a9980a`, and X0 differs from it only under `.planning`.
8. **Evidence quality.** The UTC stamps of E-32, E-33 and E-34 (18:50Z, 19:40Z, 19:45Z) are earlier than the facts they record: GitHub shows the push run created 19:33:59Z and the publish run created 21:26:10Z. E-31 (18:20Z) is likewise earlier than its commit (19:26Z). The ids, SHAs, digests and results were all confirmed independently, so this does not change a verdict, but the stamps in the evidence file are not reliable. Per the file's own rule they are not edited here; a future entry should note it.
9. **E-12, E-24, E-25 keep verdict `fail`** (a hygiene hit only); E-20 and E-26 supersede their decisions without editing them, as the file's rule requires. Intentional.
10. **Upstream `main` moved during the phase** (spotless 8.10.4, commons-lang3 3.21.0, cloud-test secrets in `tests.yaml`; E-14, E-36 newest tag still `v1.7.0`, no tag past it). Recorded as information for the next sync, not a gate.
11. **`44-LOCAL-CLOSE-RECORD.md` is stale** (written before publication, says publication pending) and says so; `44-RELEASE-RECORD.md` supersedes it. Not edited.
12. **Not independently re-run by the verifier:** Gradle suites, `testUnit`, the invariant script, the rehearsal, the archive build, since Gradle and Docker were out of scope. These rest on E-11..E-14, E-22..E-27 and the green GitHub runs (dry run, push run, publish run).

## Anti-patterns

None blocking. No `src`, test, build or `.github` change in Phase 44. The published CHANGELOG, FORK-DELTA and rollout note contain no TBD/FIXME/XXX markers that were checked by the hygiene scans (E-20, E-26; zero hits on default and strict patterns).

## Gaps Summary

No gaps. Every must-have resolves to VERIFIED or PASSED (user-accepted override). The deviations above are decisions the user took and the evidence records; they are listed so the milestone close does not read them as clean passes.

---

_Verified: 2026-11-04_
_Verifier: Claude (gsd-verifier)_
