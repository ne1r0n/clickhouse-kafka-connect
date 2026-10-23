---
phase: 24-release-publication
verified: 2026-10-20T19:10:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - ".planning/phases/24-release-publication/24-01-PLAN.md"
  - ".planning/phases/24-release-publication/24-01-SUMMARY.md"
  - ".planning/phases/24-release-publication/24-02-PLAN.md"
  - ".planning/phases/24-release-publication/24-02-SUMMARY.md"
  - ".planning/phases/24-release-publication/24-03-PLAN.md"
  - ".planning/phases/24-release-publication/24-03-SUMMARY.md"
  - ".planning/phases/24-release-publication/24-04-PLAN.md"
  - ".planning/phases/24-release-publication/24-04-SUMMARY.md"
  - ".planning/phases/24-release-publication/24-05-PLAN.md"
  - ".planning/phases/24-release-publication/24-05-SUMMARY.md"
  - ".planning/phases/24-release-publication/24-06-PLAN.md"
  - ".planning/phases/24-release-publication/24-06-SUMMARY.md"
  - ".planning/phases/24-release-publication/24-07-PLAN.md"
  - ".planning/phases/24-release-publication/24-07-SUMMARY.md"
  - ".planning/scripts/fork-anchors.txt"
  - "CHANGELOG.md"
  - "VERSION"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg3.md"
  - "src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java"
covered_digest: "v3:sha256:8048372f0b963eb7cf5261fcbb77ea863896f468b3883c38a7d7cd85ac7fa55f"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Decide what to do with the five Round 3 Warnings (WR-03..WR-07) and two Info items (IN-04, IN-05) that 24-REVIEW-DISPOSITION.md still records as `open`"
    expected: "Either accept them as a follow-up docs-only commit on custom_build after the milestone (tag v1.7.0_gg3 and M do not move), or fix them now. WR-03 (item 3 says 'every root that is not Nullable' is identical, and 4 of 14 differing roots are not Nullable), WR-05 (item 4 excludes exactlyOnce=false connectors from a rollback hazard that E-12 measured on exactlyOnce=false), and WR-06 (item 7 commits the rollback, which removes the note, before the in-flight check that item 4 says to run first) are statements or procedures an operator could act on wrongly."
    why_human: "The goal says 'reviewed rollout note'. ROLL-03 is met in the letter (a review was recorded before the gate, two rounds, 20-D-07). Whether a pre-gate review that an independent Round 3 pass later contradicted in five places counts as 'reviewed' for the milestone is a judgment for the maintainer, not something grep can decide."
  - test: "Confirm that the two approvals were your own, direct answers"
    expected: "You answered `push` (rehearsal, E-62) and `publish` (release, E-70) yourself, directly to the orchestrator, not through a relayed message."
    why_human: "The repository records the verbatim answers and the receiver, but an approval is a human act that no file can prove."
---

# Phase 24: Release & Publication Verification Report

**Phase Goal:** `v1.7.0_gg3` is published on `custom_build`. Operators have a reviewed rollout note for the changed writer error text, the suite is green twice on `latest` x V2, and the published SHA has one green CI run before the tag is pushed.
**Verified:** 2026-10-20T19:10:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

All four roadmap Success Criteria hold against the repository and the live remote. The status is `human_needed` and not `passed` because a post-publication independent review left five Warnings and two Info items `open` in a document that is already published, and whether that is acceptable is a maintainer decision (see Human Verification Required).

## Goal Achievement

### Observable Truths

| #   | Truth (roadmap SC) | Status | Evidence |
| --- | ------------------ | ------ | -------- |
| 1 | `docs/ROLLOUT-v1.7.0_gg3.md` tells operators the writer error text no longer carries the record value and what to grep for, covers the MBean `version=` segment, the client name and the lombok/spotless/slf4j-reload4j bumps; review recorded before the publication gate; rollback commands re-run on a dry-run merge of the tip | VERIFIED (with open Warnings, see below) | The note is 332 lines and byte-identical to the tag. Item 1 states the value-free text, the unchanged DLQ headers, the grep targets (`Failed to write column `, `Error writing value of`; alerts on `Invalid UUID string` / `For input string` stop matching) and the measured Map-key exception. Item 5 covers MBean `version=` and client name `clickhouse-kafka-connect/v1.7.0_gg3`. Item 6 covers lombok 1.18.46->1.18.48, spotless 8.10.1->8.10.4, slf4j-reload4j 2.0.17->2.0.20. Review: commit `91fa4137` (17:12Z) precedes the suite gate (17:17Z) and the first push (17:55Z); 24-REVIEW.md rounds 1 and 2 are marked done there. Rollback: E-61 (dry-run merge D of X onto `6473e24f`, `ROLLBACK-ON-TIP-OK`), E-62 on X0 and E-71 against M itself (`ROLLBACK-RECHECK-ON-M-OK`). |
| 2 | After `clean`, `VERSION` reads `v1.7.0_gg3` and the generated version agrees; CHANGELOG has a plain-language `1.7.0_gg3` section; FORK-DELTA has a row for every fork-side patch added in Phases 21-23 | VERIFIED | `git show v1.7.0_gg3:VERSION` = `v1.7.0_gg3`; `build/generated/.../Version.java` has `ARTIFACT_VERSION = "v1.7.0_gg3"`; the anchors file has `VERSION v1.7.0_gg3 1 BLD-07`. The tag's CHANGELOG opens with `# 1.7.0_gg3, 2026-10-20` (Important Changes, Known Issues, Bug Fixes, Build and CI) and no planning IDs. FORK-DELTA at the tag names DEPS-01, CI-18, CI-19, WRT-13, WRT-14, WRT-15, HARN-08, COLS-12, COLS-13, COLS-14, DEBT-10 and DEBT-11 (count >= 1 each), plus the gg3 raw numbers (130/22783/534, 33/2245/145) that E-72 reproduced on the tag. |
| 3 | Full suite passes twice on `latest` x V2, second run with `--rerun`; 0 failures, 0 errors, every skip named; `testUnit` passes with Docker stopped | VERIFIED | E-60: Docker-stopped `testUnit` 49 classes / 838 tests / 2 skipped / 0 failures / 0 errors; two `test --rerun` runs each 90 classes / 1281 tests / 0 / 0 / 18 skipped, server 26.9.19.9, run 2 timestamps after run 1; the 18 skips are named as a set and compared with the FORK-DELTA inventory. Independent check: the XML on disk now totals 90 files, 1281 tests, 0 failures, 0 errors, 18 skipped. `git diff --stat 3376a18f HEAD -- src build.gradle.kts VERSION docs CHANGELOG.md` is empty, so no source changed since. |
| 4 | After the user's own direct approval, `custom_build` on origin is a non-force `--no-ff` merge whose first parent is the pre-merge tip; that SHA has exactly one `tests.yaml` run and it is green; `v1.7.0_gg3` reaches origin only after that run is green; `pre-v1.7.0_gg3` points at the pre-merge tip | VERIFIED (approval is a human act, see below) | `git ls-remote origin`: `custom_build` = `3376a18f`, `refs/tags/v1.7.0_gg3` = `3376a18f`, `refs/tags/pre-v1.7.0_gg3` = `6473e24f`. `git log -1 --format=%P 3376a18f` = `6473e24f 8b1c318d`, so two parents and first parent is the pre-merge tip. GitHub API `actions/runs?head_sha=3376a18f`: `total_count` 1, run 39472696952, `.github/workflows/tests.yaml`, push on `custom_build`, completed/success, 18:12:17Z to 18:17:32Z. E-72 records the tag push at 18:20:56Z, after the green result at 18:18:32Z, with a late re-check that the run count stayed 1 before and after the tag. The rollback tag was pushed first (E-70, 18:10:45Z) and started no run. Origin's final heads and tags are exactly the expected set (no `sync/v1.7.0`, no stray tag). No force flag appears in any recorded push. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `docs/ROLLOUT-v1.7.0_gg3.md` | seven three-part items | VERIFIED | Present at the tag, 332 lines, seven items in the table of contents order, substantive and not a stub |
| `CHANGELOG.md` | `1.7.0_gg3` section | VERIFIED | Present at the tag |
| `docs/FORK-DELTA.md` | rows for Phase 21-23 IDs, Known costs, raw numbers | VERIFIED | All 12 IDs named; raw numbers match E-72 |
| `VERSION` / `.planning/scripts/fork-anchors.txt` | `v1.7.0_gg3` and its anchor row in step | VERIFIED | Both read `v1.7.0_gg3` |
| `src/test/.../JsonAsStringKeyResidualLiveTest.java` | live pin of the Jackson Map-key residual | VERIFIED | Included in the 1281-test, 90-class run (4 cases) |
| `24-COMMAND-EVIDENCE.md` E-60..E-62, E-70..E-72 | publication facts | VERIFIED | Cross-checked against `git ls-remote` and the public API; every recorded fact I could re-read matched |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `custom_build` (M) | pre-merge tip | first parent | WIRED | `3376a18f^1` = `6473e24f` |
| `pre-v1.7.0_gg3` | pre-merge tip | tag | WIRED | `6473e24f` on origin |
| `v1.7.0_gg3` | M | tag | WIRED | `3376a18f` on origin, lightweight commit tag |
| M | green CI | `tests.yaml` run 39472696952 | WIRED | One run, success |
| CHANGELOG / FORK-DELTA / note | each other | shared wording, consistency script | WIRED | E-30..E-33 and E-40 record the checks; not re-run here |

### Data-Flow Trace (Level 4)

Not applicable: the phase ships documents, a version file and a test. No dynamic rendering.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Remote refs as claimed | `git ls-remote origin` | custom_build, tag and rollback tag as above | PASS |
| Merge shape | `git log -1 --format='%H %P' 3376a18f` | two parents, first = `6473e24f` | PASS |
| Exactly one run on M | `GET /actions/runs?head_sha=3376a18f` | total_count 1, success | PASS |
| Suite totals on disk | sum of `build/test-results/test/*.xml` | 90 / 1281 / 0 / 0 / 18 | PASS |
| Generated version | grep `ARTIFACT_VERSION` | `v1.7.0_gg3` | PASS |
| Source unchanged since tag | `git diff --stat 3376a18f HEAD -- src build.gradle.kts VERSION docs CHANGELOG.md` | empty | PASS |

The full suite was not re-run: `./gradlew test` was UP-TO-DATE and the on-disk XML plus the empty source diff stand in for it. The anchors invariant (about 9 minutes) was not re-run; E-60 records `result=PASS`.

### Probe Execution

No phase-declared probes (`scripts/*/tests/probe-*.sh`). SKIPPED.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| ROLL-03 | 24-01..24-05 | Rollout note, reviewed before the publication gate | SATISFIED (see Warnings) | Note covers value-free error text, grep targets, MBean, client name and bumps. Review recorded pre-gate (inline, two rounds, accepted by the user at the gate per 24-06 SUMMARY). Round 3 found 5 Warnings and 2 Info afterwards. |
| PUB-09 | 24-01, 24-04 | VERSION, CHANGELOG section, FORK-DELTA rows | SATISFIED | See SC 2 |
| SUITE-06 | 24-06 | Two green full runs with `--rerun`, Docker-stopped `testUnit`, skips named | SATISFIED | See SC 3 |
| PUB-10 | 24-06, 24-07 | Non-force `--no-ff` merge after explicit approval, one green run, tag after green, rollback tag | SATISFIED (approval is human) | See SC 4 |

REQUIREMENTS.md maps exactly these four IDs to Phase 24 (traceability table and line 143), and every one is claimed by at least one plan. No orphaned requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| `CHANGELOG.md`, `docs/ROLLOUT-v1.7.0_gg3.md`, `VERSION`, the new test | n/a | `TBD`/`FIXME`/`XXX`/`TODO` | none found | No debt marker gate trips |
| `docs/ROLLOUT-v1.7.0_gg3.md` | 167 | Overclaim: "every root that is not `Nullable`" is identical (WR-03) | Warning | Could lead an operator to dismiss an `Array(Nullable(Tuple))` or `Tuple(x Nullable(Tuple))` table that needs the rollback clean stop |
| `docs/ROLLOUT-v1.7.0_gg3.md`, `CHANGELOG.md`, `docs/FORK-DELTA.md` | 115/121, 38-42, 173 | "every insert ... fails" for all `size` shapes while saying a shorter row might parse (WR-04) | Warning | Universal wording hides an unmeasured silent-wrong-value case |
| `docs/ROLLOUT-v1.7.0_gg3.md` | 239 | `exactlyOnce=false` excluded from the rollback hazard E-12 measured on `exactlyOnce=false` (WR-05) | Warning | Those operators get no instruction |
| `docs/ROLLOUT-v1.7.0_gg3.md` | 256-257, 315-323 | Item 7 commits the rollback (which removes the note) before the in-flight check of item 4 (WR-06) | Warning | Order is confusing; partly mitigated by the note's own `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg3.md` line |
| `docs/ROLLOUT-v1.7.0_gg3.md` | 246-256 | State-table query has room for two topics and the empty-result sentence says it is safe (WR-07) | Warning | Third and later topics are not checked. The gap is inherited from the gg2 note |
| `docs/ROLLOUT-v1.7.0_gg3.md`, `CHANGELOG.md` | 188, 59 | Wording (IN-04, IN-05) | Info | Cosmetic |

I read lines 167, 239 and 315-323 of the note and confirmed that WR-03, WR-05 and WR-06 describe what the text says. No Critical finding exists in 24-REVIEW.md, and no command in the note is destructive (no TRUNCATE, DELETE, DROP or ALTER).

### Human Verification Required

#### 1. Disposition of the open Round 3 findings

**Test:** Read 24-REVIEW.md Round 3 (WR-03..WR-07, IN-04, IN-05) and 24-REVIEW-DISPOSITION.md (`open: 7`).
**Expected:** A decision: follow-up docs-only commit on `custom_build` (no tag or merge-commit move), or accept as known and carry in the milestone close record.
**Why human:** ROLL-03 says "reviewed before the publication gate", and that is true. The review was an inline reading by the executing agent (24-05 "Skill adaptation"), disclosed at the gate and accepted by the user's `push`. The independent pass came after publication and found three statements or procedures that could mislead (WR-03, WR-05, WR-06). I conclude that the requirement is met as written and the goal's "reviewed rollout note" is met in the sense of "a review was done and recorded in time", but I cannot say that the note is free of operator-misleading text. That is for the maintainer to weigh. I did not treat it as FAILED: the writer error-text content the goal names is correct and measured, and nothing is Critical or destructive. I did not treat it as clean either.

#### 2. User approvals were direct

**Test:** Confirm that `push` (E-62) and `publish` (E-70) were your own answers.
**Expected:** Yes.
**Why human:** The evidence records them verbatim with the receiver, but approval cannot be proven from files.

### Gaps Summary

No gap blocks the phase goal. Everything the goal names exists and is wired: the tag, the merge shape, the single green CI run before the tag, the rollback tag, the two green suite runs and the reviewed note. Two things stay for the maintainer. The first is the seven open Round 3 findings above. The second is minor bookkeeping: `.planning/ROADMAP.md` line 81 still shows `- [ ] **Phase 24**` although the requirement checkboxes are ticked and the detail section reads 7/7. It is probably left for milestone close.

---

_Verified: 2026-10-20T19:10:00Z_
_Verifier: Claude (gsd-verifier)_
