---
phase: 29-release-publication
verified: 2026-10-24T00:00:00Z
status: human_needed
score: 3/4 roadmap success criteria verified (SC3 literal wording not met; one recorded, user-authorised deviation awaits acceptance)
covered_files:
  - ".planning/phases/29-release-publication/29-01-PLAN.md"
  - ".planning/phases/29-release-publication/29-01-SUMMARY.md"
  - ".planning/phases/29-release-publication/29-02-PLAN.md"
  - ".planning/phases/29-release-publication/29-02-SUMMARY.md"
  - ".planning/phases/29-release-publication/29-03-PLAN.md"
  - ".planning/phases/29-release-publication/29-03-SUMMARY.md"
  - ".planning/phases/29-release-publication/29-04-PLAN.md"
  - ".planning/phases/29-release-publication/29-04-SUMMARY.md"
  - ".planning/phases/29-release-publication/29-05-PLAN.md"
  - ".planning/phases/29-release-publication/29-05-SUMMARY.md"
  - ".planning/phases/29-release-publication/29-06-PLAN.md"
  - ".planning/phases/29-release-publication/29-06-SUMMARY.md"
  - ".planning/phases/29-release-publication/29-07-PLAN.md"
  - ".planning/phases/29-release-publication/29-07-SUMMARY.md"
  - ".planning/phases/29-release-publication/29-08-PLAN.md"
  - ".planning/phases/29-release-publication/29-08-SUMMARY.md"
  - ".planning/scripts/confluent-archive-allowlist-29.txt"
  - ".planning/scripts/fork-anchors.txt"
  - "CHANGELOG.md"
  - "VERSION"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg1.md"
  - "docs/ROLLOUT-v1.7.0_gg2.md"
  - "docs/ROLLOUT-v1.7.0_gg3.md"
  - "docs/ROLLOUT-v1.7.0_gg4.md"
covered_digest: "v3:sha256:c72674f67925f7f96324d66c7413be8251806870184a219e28e5aaeb723ead0e"
behavior_unverified: 0
overrides_applied: 0
re_verification: false
gaps: []
human_verification:
  - test: "Decide whether to accept the PUB-12 / SC3 review-round deviation"
    expected: "Either accept it as an override (suggested block below) or reject it. The review ran four rounds, not at most three. The last round (round 4) found one Info finding (IN-09), and no round found nothing. IN-09 was fixed by the orchestrator in be5a339f and b3035d25 (CHANGELOG wording only) and was not reviewed by a further round."
    why_human: "The roadmap contract says 'Rounds repeated until one found nothing, at most three.' Neither half holds literally. The user authorised round 4 (a fourth round, not a fifth) through AskUserQuestion, but nobody has recorded acceptance of a non-clean final round. Only the user can waive a roadmap criterion."
---

# Phase 29: Release & Publication Verification Report

**Phase Goal:** `v1.7.0_gg4` is published on `custom_build`. Operators have an independently reviewed rollout note for every behaviour change, with each claim backed by a measurement. The suite is green twice on `latest` x V2, and the published SHA has exactly one green CI run before the tags are pushed.
**Verified:** 2026-10-24
**Status:** human_needed
**Re-verification:** No, initial verification

I did not trust the SUMMARY files. I read git state, `git ls-remote origin`, the shipped documents, the evidence file and the GitHub API directly. As instructed I did not re-run the Gradle suite: Phase 29 changed no source, and the two full runs in E-60 are the regression gate.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | After `clean`, `VERSION` reads `v1.7.0_gg4` and the generated version agrees; the anchor row changes in the same commit; CHANGELOG has a complete `1.7.0_gg4` section; FORK-DELTA rows and raw numbers refreshed once | VERIFIED | `VERSION` and `git show v1.7.0_gg4:VERSION` both read `v1.7.0_gg4`. `build/generated/.../Version.java` has `ARTIFACT_VERSION = "v1.7.0_gg4"`. Commit `112fbdb5` changes exactly `VERSION` and `fork-anchors.txt` line 41 (`VERSION v1.7.0_gg4 1 BLD-07`). E-01 records the clean build, with `ANCHORS-MIRROR-OK rows=94 bad=0`. `CHANGELOG.md` starts `# 1.7.0_gg4, 2026-10-23` with Important Changes and Known Issues. I re-measured FORK-DELTA's raw numbers: `git diff --shortstat v1.7.0 v1.7.0_gg4` gives 277 files, 29679 insertions, 643 deletions over the wide scope and 37, 2877, 241 over `src/main`. Both equal the FORK-DELTA table. |
| 2 | `docs/ROLLOUT-v1.7.0_gg4.md` covers 999.4, 999.5, the Map-key removal, connect-api 4.4.0, Shadow 9 / Gradle 9.8.1, guava and httpclient5 advisories by source, and the MBean `version=` segment; names `cp-kafka-connect-base:7.9.7` and the oldest verified worker; states the KIP-1161 / Kafka 4.2 scope; quantifiers match measurements and commands were run live; gg1 and gg2 carry the corrected key rule and state-table query | VERIFIED | The note has seven items: names/Enum labels (999.5), size-named Tuple elements (999.4), writer error text, in-flight batches, JMX/client name, dependencies/build/worker, rollback. Lines 298-322 name the worker, Kafka 3.9, the oldest verified worker and KIP-1161 from 4.2. Quantifiers spot-checked against the evidence file: "47 fixture tables / exactly 25 differ / 22 identical" matches E-10 (`D994-DIFFERING` list of 25, 19 look-alikes plus `s4_o01`, `n05`, `x08` identical). "7 of the 25 inserted; six failed with code 32 or 33 and stored no row; `r01` stored a shifted row" matches the E-11 `PROBE-INSERT` lines. gg1/gg2 carry the `<db><dbTopicSplitChar><topic>` key rule and the trailing-split-char form, and the gg2 query is the same `arrayStringConcat` query as gg3 (E-25, E-50). 34 `Matches text` verdicts, none `pending` or `no`. Evidence is the recorded E-entries; I did not re-run the probes. |
| 3 | Before the publication gate an orchestrator-spawned fresh-context `gsd-code-reviewer` reviewed every operator document; rounds repeated until one found nothing, at most three; every finding's disposition recorded; no finding ID reused | UNCERTAIN (WARNING, human decision) | Met: 10 files reviewed (gg1-gg4 notes, CHANGELOG, FORK-DELTA, VERSION, both anchor/allow-list files, close record). The 20 findings (CR-01, WR-01..10, IN-01..09) have unique IDs (`uniq -d` is empty) and all 20 are `fixed` in `29-REVIEW-DISPOSITION.md` (`open: 0`). **Not met literally:** there were 4 rounds, not at most 3. Round 4 found IN-09, so no round found nothing. IN-09 was fixed in `be5a339f` and `b3035d25` and was never re-reviewed. The disposition file records this as a deviation, and the user answered the round-3 AskUserQuestion with "Fix them, then round 4". The current CHANGELOG text matches round 4's "alternative" fix wording. Suggested override below. |
| 4 | On the release tip: two full `--rerun` runs green on `latest` x V2, anchors gate passes, origin tip unchanged since the DEPS-02 merge; after the user's direct approval: `--no-ff` merge with the rehearsed tree, exactly one green `tests.yaml` run on the published SHA, tags only after it, scratch branch deleted | VERIFIED | **Suite** (E-60, gated head `153cb710`): run 1 and run 2 each BUILD SUCCESSFUL, 1865 tests, 0 failed, 18 skipped. Class-count digests equal the phase base, server 26.9.19.9, `CLICKHOUSE_VERSION=latest`, `CLIENT_VERSION=V2`. `git diff --name-only 153cb710 3d847009` has no non-`.planning` path, so the gated tree is the published tree. **Anchors gate** (E-61): `anchor_rows=94 ... result=PASS`, self-test no failures. **Freeze:** origin `custom_build` was `69cd680b` at every read (E-60, E-61, E-70, E-71). **Live origin** (`git ls-remote origin`): `custom_build` = `21c51598b06b6f5af58430765d57b1e5f6747c5a`; tags `v1.7.0_gg4` -> `21c51598` and `pre-v1.7.0_gg4` -> `69cd680b`; no `sync/v1.7.0` head. **Merge:** `git cat-file -p 21c51598` shows two parents, `69cd680b` and `3d847009`. `rev-parse 21c51598^{tree}` equals `3d847009^{tree}` (`a02f7e19...`), so the tree equals the rehearsed tip. It is not a fast-forward and `69cd680b` is an ancestor. **CI** (GitHub API, queried by me): `actions/runs?head_sha=21c51598...` returns `total_count: 1`, run 40022281005, `tests.yaml`, push, `custom_build`, completed, success. E-72 shows the tags were pushed at 20:59:44Z, after the run finished at 20:56:40Z. The approvals "push" and "publish" are recorded as given directly by the user (E-62, E-71). |

**Score:** 3/4 verified; SC3 is a literal-wording deviation (see Human Verification).

### Requirements Coverage

Every ID appears in the plans' `requirements` frontmatter and is marked `[x]` and "Phase 29 / Complete" in REQUIREMENTS.md. There are no orphaned IDs: REQUIREMENTS.md maps exactly these seven to Phase 29.

| Requirement | Source Plan(s) | Status | Evidence |
|-------------|----------------|--------|----------|
| PUB-11 | 29-01, 29-05 | SATISFIED | SC1 above. |
| ROLL-04 | 29-01, 29-02, 29-03, 29-04 | SATISFIED | SC2 above. |
| ROLL-05 | 29-03 | SATISFIED | gg1, gg2 and gg3 notes carry the corrected `enableDbTopicSplit=true` key rule and the state-table query. E-25 and E-50 show the other-topic and look-alike keys surviving the `DELETE` examples. |
| OPS-06 | 29-01, 29-04 | SATISFIED | Note lines 298-322 name `cp-kafka-connect-base:7.9.7` (Kafka 3.9), the oldest verified worker, and KIP-1161 from Kafka 4.2. |
| PUB-12 | 29-06 | NEEDS HUMAN | Four orchestrator-spawned rounds ran with dispositions and unique IDs, but the "at most three, until one finds nothing" wording is not met (SC3). |
| OPS-05 | 29-07, 29-08 | SATISFIED | `custom_build` was `69cd680b` at four gate reads and the final read (E-60, E-61, E-70, E-71). The OPS-05 contingency never fired. |
| PUB-13 | 29-07, 29-08 | SATISFIED | SC4 above, including live origin refs and the GitHub run count. |

### Key Link / Artifact Checks

| Artifact | Status | Details |
|----------|--------|---------|
| `docs/ROLLOUT-v1.7.0_gg4.md` (363 lines) | VERIFIED | Substantive and present in the tag. gg1, gg2 and gg3 headers point to the gg4 correction. |
| `29-COMMAND-EVIDENCE.md` (E-01 to E-72) | VERIFIED | 34 verdict lines, none pending or `no`. Entries E-60 to E-72 carry the gate, rehearsal and publication facts, and they agree with live git and the GitHub API. |
| `29-REVIEW.md` / `29-REVIEW-DISPOSITION.md` | VERIFIED with deviation | 20 findings, 4 rounds, `open: 0`, disposition table complete. Deviation is SC3. |
| `29-MILESTONE-CLOSE-RECORD.md` | VERIFIED | Carries 16 deferred findings from the Phase 25-28 reviews with reason, owner and re-open trigger. |
| `29-SECURITY.md` | VERIFIED | `status: verified`, `threats_open: 0`. Every threat row cites a recorded marker. |
| `.planning/scripts/fork-anchors.txt` VERSION row | VERIFIED | Matches `VERSION` at the tag. |

### Anti-Patterns

`grep` over `docs/ROLLOUT-v1.7.0_gg4.md` and `CHANGELOG.md` for TBD/FIXME/XXX finds nothing. Phase 29 changed no `src/`, `build.gradle.kts`, `gradle/` or `.github/` file since `P29_BASE` (E-60 scope check and the diff above).

### Informational Notes (non-blocking)

1. `ROADMAP.md` line 101 still shows the phase-level checkbox `- [ ] **Phase 29**`, while the plan checkboxes are `[x]` and the progress line says 8/8. This is bookkeeping for the phase-complete step.
2. The working tree has ` M gradlew.bat` (the known CRLF/`.gitattributes` artefact, E-30/E-70) and untracked `.planning/milestone.lock`. Neither is part of the phase.
3. The release-day CHANGELOG date `2026-10-23` matches the merge date (E-70 noted the dependency; the merge landed 2026-10-23T20:50Z).
4. OPS-04's "label check pending the next monthly Dependabot run" is carried as a reminder in the close record. It belongs to Phase 25 and is not a Phase 29 requirement.
5. The integration-test source set is compile-only at the gate. This is stated in E-60 (`integration_test: excluded (PT-16)`) and is an accepted prior position (SYNC-10).

## Human Verification Required

### 1. Accept or reject the PUB-12 / SC3 review-round deviation

**Test:** Read `29-REVIEW-DISPOSITION.md` (the "Round 4 reported one Info finding (IN-09)..." paragraph) and `29-REVIEW.md` Round 4.
**Expected:** Decide whether four rounds with an unreviewed Info-level wording fix (IN-09, CHANGELOG only, matching the reviewer's own alternative fix text) satisfies "rounds repeated until one found nothing, at most three".
**Why human:** This is a roadmap contract waiver. Only the user can grant it. The release is already published, so rejecting it cannot be fixed by a gap-closure plan. It could only be answered by a fifth review round on the published documents, or by accepting the deviation.

If you accept it, add this to the frontmatter:

```yaml
overrides:
  - must_have: "Rounds repeated until one found nothing, at most three"
    reason: "Four rounds ran by the user's direct decision after round 3. Round 4 found one Info finding (IN-09), fixed by the orchestrator in be5a339f/b3035d25 using the reviewer's own wording, CHANGELOG only, not re-reviewed. All 20 findings are fixed with unique IDs."
    accepted_by: "{name}"
    accepted_at: "{ISO timestamp}"
```

## Gaps Summary

No gaps. Every observable truth other than SC3 is verified against live state. SC3 is the one item left: the review process was thorough (20 findings, all fixed, IDs unique, fresh-context reviewer each round), but its stopping rule differs from the roadmap's literal wording, and that needs the user's acceptance.

---

_Verified: 2026-10-24_
_Verifier: Claude (gsd-verifier)_
