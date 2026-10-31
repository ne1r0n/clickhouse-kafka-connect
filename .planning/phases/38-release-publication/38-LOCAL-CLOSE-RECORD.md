# Phase 38: local close record

Phase 38 (Release and Publication) closes locally. Everything an agent may do is done and recorded in `38-COMMAND-EVIDENCE.md`; the one open requirement, RLS-05, needs five steps that only the user may run. This record states what is satisfied, what is pending, the wording changes the orchestrator still applies, and what is carried to the milestone close. It never counts a pending user step as passed.

## Status

- The phase is local-complete: plans 38-01 to 38-05 are executed and gated at the head named in E-12 and E-13 (`gated_head` `ea613ec35ebade7a2182cac4e6ecd5d86d099def`; the last commit outside `.planning` is `927ac07c30274c46b2330d597d417f9f1eda8f8b`; the allow-list commit is `cc928221b7f2a19ccad3fa2f160f970b35c671e9`). The gate decision of E-13 is `go`.
- The publication is pending and human-gated. Nothing was pushed, no tag was created, no workflow was dispatched and no pull request was touched. Origin `custom_build` reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; no `sync/v1.7.0` ref and no `v1.7.0_gg6` tag exists on origin or locally; `pre-v1.7.0_gg6` is a local tag on `3a3a15a9` and is not on origin.
- The verdict is pass for the local gate only. The publication will be done by the user's runs, from the four handoff files, with the orchestrator's builds and read-only readings from `38-ORCHESTRATOR-RUNBOOK.md`.

## Requirement status

| Requirement | Status | Evidence |
|-------------|--------|----------|
| RLS-01 | satisfied | E-01 to E-04: `VERSION` `v1.7.0_gg6` with its anchor row in one commit, the generated version agrees after `clean`, the CHANGELOG section absorbs `Unreleased`, FORK-DELTA final with refreshed numbers |
| RLS-02 | satisfied | E-05 to E-07: the rollout note holds 29 rows with four defined labels; the rollback block ran unchanged on a rehearsal merge |
| RLS-03 | satisfied | E-11: the gg6 allow-list was generated last from the final tree; 31 rows cover all 242 differences; the compare exits 0 with `RESULT: PASS`; three mutations turn it red |
| RLS-04 | satisfied | E-08 to E-10: twelve fresh-context review rounds, 43 findings, every one dispositioned; one is deferred and two Info fixes were not reviewed again (see the human-judgment items below) |
| RLS-05 | pending, human-gated | E-14 is the rehearsal and the approval bundle; E-15 to E-18 are the pending handoffs; the user steps are U1 to U5; it is satisfied only when E-20 to E-27 are recorded and show: the dry run green, `custom_build` a non-force `--no-ff` merge whose tree equals the rehearsed tip, exactly one green `tests.yaml` run on it, the publish run green with the release and the tag created by it, `pre-v1.7.0_gg6` pushed and the scratch branch deleted |
| RLS-06 | satisfied | E-12 and E-13: `testUnit` with Docker unreachable passes 1672 tests; two full suites pass 2270 tests each on one pinned `latest` image with client V2 |

The five ROADMAP criteria, with the same statuses:

| Criterion | Status | Evidence |
|-----------|--------|----------|
| 1. `VERSION`, CHANGELOG, FORK-DELTA | satisfied | E-02, E-03, E-04 |
| 2. the rollout note with four labels | satisfied | E-05, E-06, E-07 |
| 3. the independent review and the allow-list | satisfied, with the human-judgment items below | E-08 to E-11 |
| 4. two full suites and `testUnit` without Docker | satisfied, with the JDK note below | E-12, E-13 |
| 5. dry run, merge, one `tests.yaml` run, publish run, tag, scratch deletion | pending, human-gated | E-14 to E-18 pending; the remaining gate is U1 to U5 |

### Not proven (human-judgment items, not passed gates)

- The review loop: twelve rounds ran, the plan allowed three, and each further round was authorised by the user's answer relayed by the orchestrator (the executor cannot verify the relay). The loop ended on the user's decision to fix two Info findings (IN-14, IN-15) and close, not on a round that found nothing, so the criterion's wording "until one found nothing" is met by that decision. The two last fixes were not read by a fresh reviewer.
- The `docs/ROLLOUT-v1.7.0_gg1.md` half of WR-08 is deferred (outside this phase's editable set).
- The pin gap of IN-04: no Docker-free test pins the forwarding of a `custom_` key on client `0.11.0`; the wording half is fixed.
- The JDK line: both full suites ran on JDK 21 (`-PjavaVersion=21`) because the default JDK 17 (an OpenJ9 build) crashed twice in the Mockito inline mock maker; the Docker-free `testUnit` ran on the default JDK 17. Criterion 4 is met on JDK 21; whether that satisfies the user is the user's call.
- The integration suite was not re-run; the nightly run on the client-bump scratch tip, accepted by the user, is the record (E-12 `integration_test`).
- Every GitHub behaviour the publication relies on is an assumption until read (A1, A2, A3, A7 below). The CI wiring of the Phase 37 compare chain has never run on a runner; a CI-only defect goes back to Phase 37 as a gap, because Phase 38 cannot edit `.github/`.

## Why this is the honest shape

No agent may push, tag or dispatch (decisions O-1 and O-2), so the part of the ROADMAP that publishes can only be prepared and rehearsed here. The ROADMAP says the phase publishes, so the phase checkbox stays unchecked and RLS-05 stays Pending until E-27 exists. Verification of this phase should read `human_needed` for criterion 5 and RLS-05 and pass for the rest.

## Wording changes the orchestrator applies

The orchestrator already applied items 1 to 6 in commit `0d4763ee`; they are listed so the record is complete, and each is checked against the current ROADMAP.md and REQUIREMENTS.md. Items 7 to 11 are still outstanding. This phase edits neither file.

1. ROADMAP Phase 38 Goal, last sentence: "the published SHA has one green `tests.yaml` run before the tags are pushed" becomes "before the publish run creates the tag". Applied (the Goal now reads "before the user's publish run, which creates `v1.7.0_gg6`").
2. ROADMAP Phase 38 criterion 5: the single-run bullet reads "green before the user launches the publish run", the tag `v1.7.0_gg6` is created by that `release.yaml` run, and only `pre-v1.7.0_gg6` is pushed by hand afterwards; the first line says the dry run is a `release.yaml` run from the scratch branch that the user launches. Applied.
3. ROADMAP Phase 38 Note: step 4 reads "the user launches the publish run ... then push `pre-v1.7.0_gg6` by name", and the stale sentence about the divergence check's left side is replaced by "measures 0; a count above 0 means origin moved". Applied.
4. ROADMAP Phase 38 Research flag resolved by the user's decision of 2026-10-31, and `**Plans**` lists the five plans (38-01 to 38-05). Applied.
5. REQUIREMENTS RLS-05: "one `tests.yaml` run on the published SHA is green before the user's publish run, which creates the tag `v1.7.0_gg6`; only `pre-v1.7.0_gg6` is pushed by hand", with "(expected empty)" after the `git log sync/v1.7.0..origin/custom_build` check. Applied.
6. REQUIREMENTS RLS-02 and ROADMAP criterion 2 (from plan 38-02): the four labels are named. Applied.
7. Closure shape: leave the Phase 38 checkbox in ROADMAP.md unchecked and RLS-05 `Pending` in the traceability table until E-20 to E-27 are recorded; RLS-01 to RLS-04 and RLS-06 may flip to Complete (checkbox and traceability row) when the phase verification passes. Outstanding.
8. Flagged, not edited by this phase: REQUIREMENTS SWP-03 ("PR #10 ... is closed") is stricter than the ROADMAP's wording about PR #10 (recorded as closing at publication); the orchestrator aligns it, and the SWP-03 wording is never edited by a phase plan. Outstanding.
9. ROADMAP phase list line of Phase 38 (the `- [ ] **Phase 38: ...**` entry): "has one green CI run before the tags" becomes "has one green CI run before the publish run creates the tag". Outstanding.
10. ROADMAP Phase 33 note that reads "Phase 38 pushes it only after the published SHA's run is green" (about `pre-v1.7.0_gg6`): the user pushes it after the publish run, in handoff U5. Outstanding.
11. ROADMAP progress row of Phase 38 (`0/TBD`, Not started): the plan counts and status come from `roadmap update-plan-progress`; the plan checklist lines for 38-01 to 38-05 are the orchestrator's to add. Outstanding. Flagged, not Phase 38's: the ROADMAP wording of Phase 37 criterion 5 differs from what Phase 37 measured.

## What the publication still needs from the user

Five steps, each from its own file in `.planning/phases/38-release-publication/`, in this order; each starts with the check that `custom_build` still reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, and a move voids the approval:

1. U1 and U2, `38-USER-DISPATCH-1-SCRATCH-DRYRUN.md`: push the tip to the scratch branch `sync/v1.7.0`, wait for its one green `tests.yaml` run, then dispatch `release.yaml` from that branch as a dry run.
2. After the user's own direct "publish" message: the orchestrator builds the merge commit M (runbook step O3).
3. U3, `38-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md`: push M to `custom_build` by SHA and wait for its single green `tests.yaml` run.
4. U4, `38-USER-DISPATCH-3-PUBLISH.md`: dispatch `release.yaml` from `custom_build` with `dry_run` not ticked; the run creates the release and the tag `v1.7.0_gg6`. Never push that tag.
5. U5, `38-USER-DISPATCH-4-PRETAG-CLEANUP.md`: push `pre-v1.7.0_gg6` by name, delete the scratch branch (and, at the user's choice, the two `scratch/clt-07-*` branches), and close or leave the pull requests #10 and #16 to #20.

The user's reports become evidence from E-20 on; the CHANGELOG date `2026-10-31` must match the UTC date of the merge (the orchestrator asks the user directly if it differs); `NIGHTLY_ENABLED` stays unset.

## Carried to the milestone close

| Item | Disposition | Owner | Re-open trigger |
|------|-------------|-------|-----------------|
| The writer's `stop()` never closes its client (F-1; FORK-DELTA item 13) | carried, published knowingly open in FORK-DELTA, the CHANGELOG Known Issues and the note's known limits | fork maintainer (the user) | a task-restart leak report from an operator, or the next edit of `ClickHouseWriter.stop()` |
| The four-minute insert timeout has no effect while `async=false` (F-2; FORK-DELTA item 14) | carried, as above | fork maintainer (the user) | an operator report of an insert that hangs on a stalled server, or the next edit of the client V2 insert methods |
| The harness Datagen completion counting spans all connectors (F-3; FORK-DELTA item 15) | carried, in FORK-DELTA and the CHANGELOG Known Issues only (not operator-visible) | fork maintainer (the user) | a red nightly traced to it, or the next edit of a `ConfluentPlatform` helper that counts across `getConnectors()` |
| Two `scratch/clt-07-*` branches on a public origin (UF-36-1) | pending the user's choice in U5; deleting a branch does not unpublish its commits | fork maintainer (the user) | the user's answer in U5, or the milestone close |
| No `gradle/verification-metadata.xml`, and no `zstd-jni` row in the compare's version table (UF-36-4; FORK-DELTA item 18) | carried | fork maintainer (the user) | a dependency-substitution incident, a new dependency that bundles natives, or the next edit of the compare script's version table |
| Phase 36 review notes IN-02 to IN-06 and UF-36-5 (test hygiene) | carried | fork maintainer (the user) | the next edit of the Phase 36 pin test classes, or the milestone close |
| The documented limits of the buffered fix (OI-35-1 to OI-35-3 and IN-04 of the Phase 35 review; FORK-DELTA item 16, row B8 of the note) | carried, stated in FORK-DELTA, the CHANGELOG Known Issues and the note | fork maintainer (the user) | an operator who runs the Direct path with `reportInsertedOffsets=true` under a renaming SMT, a many-to-one rename, a rename onto an input topic, or upstream resolving issue 777 |
| `DataException` wrapped retriable cases and the missing retry budget (FUT-05; FORK-DELTA item 17; rows C2 and C3 of the note) | carried, with its rollout warning in the note | fork maintainer (the user) | a decision on FUT-05, or an operator report of a redelivery loop with `additionalRetriableErrorCodes` |
| `increment-version.yml` header names the wrong guard class (FORK-DELTA item 19) | carried (this phase cannot edit `.github/`) | fork maintainer (the user) | the next edit of that workflow |
| Phase 37 ledger rows that need the user's approval: four `src/main` rows and one null assertion (FORK-DELTA item 20) | carried, pointing at the Phase 37 advisory ledger | fork maintainer (the user) | the user's approval to edit `src/main`, or the next edit of the files named in the ledger |
| The `tests.yaml` gate accepts a green run of any branch on the SHA (30 IN-04; FORK-DELTA item 21) | condition: the publication records that the merged SHA's only run is the `custom_build` push (E-23) | fork maintainer (the user) | a publish whose only green run came from a branch whose workflow differs from `custom_build`'s |
| The earlier caveat that no workflow run exercised the publish path (ADV-03; FORK-DELTA item 22) | pending the publication; stays open until the publish run succeeds and E-24 and E-25 are recorded | Phase 38 publication | E-24 and E-25, or a failed or cancelled publish run |
| SWP-03: the states of pull requests #10 and #17 (FORK-DELTA item 23) | condition, stated without an outcome; re-read after the merge and recorded in E-27 | Phase 38 publication | E-27, or Dependabot's next monthly run |
| CLT-07 nightly: one green run on the scratch tip, accepted by the user (run 41152289179, 63 tests, 0 failures) | carried; the release tree has the same `src/main`, build and `gradle` files as that run | fork maintainer (the user) | any change of those files before publication, or the first nightly after `NIGHTLY_ENABLED` is set |
| Review finding WR-08, the `docs/ROLLOUT-v1.7.0_gg1.md` half: item 6 of that note says "measured" for the `zstd-jni` native and the temp folder, and says client V1 "of any line" where two lines were observed | deferred (outside this phase's editable set) | fork maintainer (the user) | the next edit of `docs/ROLLOUT-v1.7.0_gg1.md`, or the milestone close |
| Review finding IN-04, the second half: a Docker-free pin of the `custom_` forwarding on client `0.11.0` | not applied (a test is outside this phase's editable set) | fork maintainer (the user) | the next client bump, or the next edit of `createClientV2` |
| Review findings IN-14 and IN-15, fixed after round 12 and not reviewed by a fresh reviewer, on the user's decision | carried as a human-judgment item | fork maintainer (the user) | the next edit of the first-start subsection of `docs/ROLLOUT-v1.7.0_gg6.md`, or an operator question about it |
| The JDK 21 record of the two full suites (the default JDK 17 on the gating host crashes in the Mockito inline mock maker) | carried as a human-judgment item | fork maintainer (the user) | a full suite that must run on the default JDK 17, or a change of the host's JDK |

## Assumptions the publication confirms or refutes

- A1: GitHub marks pull requests #16, #18, #19 and #20 merged by itself once their heads are reachable from `custom_build`. Read the four states after U3 and U5 (E-27).
- A2: Dependabot closes #10 and #17 as up to date at its next monthly run (the timing is undocumented). Read both states in E-27 and, later, at the next Dependabot run.
- A3: `gh release create --target <SHA>`, run with the workflow's own token, creates a lightweight tag and a release authored by `github-actions[bot]`. Read the tag ref, the release author and the asset uploader after U4 (E-25).
- A7: the runner's archive size is close to the local 13558634 bytes (an earlier runner build differed by about 100 bytes). Read the `Check archive` notice of the dry run and of the publish run; the compare, not a size, decides.

## Not done by this phase

No push, no tag, no workflow dispatch, no pull-request action. No change to `src/`, a test, the build, `gradle/` or `.github/`. No edit of ROADMAP.md, REQUIREMENTS.md or STATE.md. The five released documents and the allow-list are frozen as of E-10 and E-11. Nothing in this record says the release exists: it will exist when the user's runs say so.
