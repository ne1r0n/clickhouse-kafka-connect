---
phase: 38
review: 38-REVIEW.md
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
  - id: WR-02
    severity: warning
    disposition: fixed
  - id: WR-03
    severity: warning
    disposition: fixed
  - id: WR-04
    severity: warning
    disposition: fixed
  - id: WR-05
    severity: warning
    disposition: fixed
  - id: WR-06
    severity: warning
    disposition: fixed
  - id: WR-07
    severity: warning
    disposition: fixed
  - id: WR-08
    severity: warning
    disposition: deferred
  - id: WR-09
    severity: warning
    disposition: fixed
  - id: IN-01
    severity: info
    disposition: fixed
  - id: IN-02
    severity: info
    disposition: fixed
  - id: IN-03
    severity: info
    disposition: fixed
  - id: IN-04
    severity: info
    disposition: fixed
  - id: IN-05
    severity: info
    disposition: fixed
  - id: IN-06
    severity: info
    disposition: fixed
  - id: IN-07
    severity: info
    disposition: fixed
  - id: WR-10
    severity: warning
    disposition: fixed
  - id: WR-11
    severity: warning
    disposition: fixed
  - id: WR-12
    severity: warning
    disposition: fixed
  - id: IN-08
    severity: info
    disposition: fixed
  - id: WR-13
    severity: warning
    disposition: fixed
  - id: IN-09
    severity: info
    disposition: fixed
  - id: WR-14
    severity: warning
    disposition: fixed
  - id: WR-15
    severity: warning
    disposition: fixed
  - id: WR-16
    severity: warning
    disposition: fixed
  - id: WR-17
    severity: warning
    disposition: fixed
  - id: WR-18
    severity: warning
    disposition: fixed
  - id: IN-10
    severity: info
    disposition: fixed
  - id: IN-11
    severity: info
    disposition: fixed
  - id: WR-19
    severity: warning
    disposition: fixed
  - id: WR-20
    severity: warning
    disposition: fixed
  - id: WR-21
    severity: warning
    disposition: fixed
  - id: WR-22
    severity: warning
    disposition: fixed
  - id: IN-12
    severity: info
    disposition: fixed
  - id: WR-23
    severity: warning
    disposition: fixed
  - id: WR-24
    severity: warning
    disposition: fixed
  - id: WR-25
    severity: warning
    disposition: fixed
  - id: WR-26
    severity: warning
    disposition: fixed
  - id: IN-13
    severity: info
    disposition: fixed
  - id: WR-27
    severity: warning
    disposition: fixed
  - id: WR-28
    severity: warning
    disposition: fixed
  - id: IN-14
    severity: info
    disposition: fixed
  - id: IN-15
    severity: info
    disposition: fixed
open: 0
total: 43
recorded: 2026-10-31T17:25:16Z
---

# Phase 38 review disposition

Twelve review rounds ran, each by a fresh `gsd-code-reviewer` spawned by the orchestrator over the previous round's fix commits (round 1 over the five release documents). Findings per round: 1:16; 2:4; 3:2; 4:2; 5:2; 6:2; 7:1; 8:2; 9:3; 10:5; 11:2; 12:2; total 43 (28 warnings, 15 info, 0 critical). Every fixed row names the commit that holds the fix, and each fix was re-run through its owning checks before the next round. The plan's cap of three rounds was exceeded to twelve by the user's decisions, quoted below.

round3_decision: "Исправить два info и закрыть ревью (Recommended)" (the user's answer after round 12, relayed by the orchestrator from its AskUserQuestion; the executor cannot verify it). IN-14 and IN-15, the two findings of round 12, were fixed in fd8b8575 and 927ac07c without a thirteenth review round, a recorded deviation from the plan; the owning checks were re-run after both fixes. The same decision was asked after every round from round 3 on, and each answer was relayed by the orchestrator in the same way:
  - after round 3: «Исправить и чётвёртый раунд (Recommended)»
  - after round 4: «Исправить и пятый раунд»
  - after round 5: «Исправить и шестой раунд»
  - after round 6: «Исправить и седьмой раунд»
  - after round 7: «Исправить и восьмой раунд»
  - after round 8: «Девятый раунд (Recommended)»
  - after round 9: «Десятый раунд по разделу B (Recommended)»
  - after round 10: «Одиннадцатый раунд (Recommended)»
  - after round 11: «Двенадцатый раунд (Recommended)»
  - after round 12: «Исправить два info и закрыть ревью (Recommended)»

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | Warning | fixed | 63d51d56 (Phase 38) |
| WR-02 | Warning | fixed | ccdd14df (Phase 38) |
| WR-03 | Warning | fixed | a8d05b43 (Phase 38) |
| WR-04 | Warning | fixed | cc3f119d (Phase 38) |
| WR-05 | Warning | fixed | 0d984e53 (Phase 38) |
| WR-06 | Warning | fixed | c01224a8 (Phase 38) |
| WR-07 | Warning | fixed | 59afabb1 (Phase 38) |
| WR-08 | Warning | deferred | Reason: the docs/ROLLOUT-v1.7.0_gg1.md half is outside this phase's editable set (the FORK-DELTA half was fixed in f1e32844 (Phase 38)); owner: the user as fork maintainer; re-open trigger: the next edit of docs/ROLLOUT-v1.7.0_gg1.md or the milestone close |
| WR-09 | Warning | fixed | 00ed58ff (Phase 38) |
| IN-01 | Info | fixed | 51dcf7cf (Phase 38) |
| IN-02 | Info | fixed | 08acd104 (Phase 38) |
| IN-03 | Info | fixed | 8185099d (Phase 38) |
| IN-04 | Info | fixed | 299a6fed (Phase 38): the wording of row D7 is fixed; the optional Docker-free pin of the forwarding was not added because this phase adds no test |
| IN-05 | Info | fixed | a987e97c (Phase 38) |
| IN-06 | Info | fixed | e9d7796e (Phase 38) |
| IN-07 | Info | fixed | 21d70b07 (Phase 38) |
| WR-10 | Warning | fixed | ed11c7ff (Phase 38) |
| WR-11 | Warning | fixed | b0edab5a (Phase 38) |
| WR-12 | Warning | fixed | 292c82d3 (Phase 38) |
| IN-08 | Info | fixed | 266d157e (Phase 38) |
| WR-13 | Warning | fixed | 3d55172c (Phase 38) |
| IN-09 | Info | fixed | 45defb53 (Phase 38) |
| WR-14 | Warning | fixed | 93db97f8 (Phase 38) |
| WR-15 | Warning | fixed | e00026fe (Phase 38) |
| WR-16 | Warning | fixed | 2ff65bb2 (Phase 38) |
| WR-17 | Warning | fixed | 6595c5e9 (Phase 38) |
| WR-18 | Warning | fixed | 3e27f23c (Phase 38) |
| IN-10 | Info | fixed | 667cfe21 (Phase 38) |
| IN-11 | Info | fixed | 26056ca1 (Phase 38) |
| WR-19 | Warning | fixed | b4550087 (Phase 38) |
| WR-20 | Warning | fixed | 4c765423 (Phase 38) |
| WR-21 | Warning | fixed | 9e5afc41 (Phase 38) |
| WR-22 | Warning | fixed | 58fdadc3 (Phase 38) |
| IN-12 | Info | fixed | 3d664af2 (Phase 38) |
| WR-23 | Warning | fixed | 97be60ba (Phase 38) |
| WR-24 | Warning | fixed | 3330b185 (Phase 38) |
| WR-25 | Warning | fixed | 50104166 (Phase 38) |
| WR-26 | Warning | fixed | 73ef2e6f (Phase 38) |
| IN-13 | Info | fixed | cd7183b1 (Phase 38) |
| WR-27 | Warning | fixed | dfbbe5b4 (Phase 38) |
| WR-28 | Warning | fixed | 99757d2d (Phase 38) |
| IN-14 | Info | fixed | fd8b8575 (Phase 38) |
| IN-15 | Info | fixed | 927ac07c (Phase 38) |

Notes on single rows. Sweep commits that carried no finding ID (wording found while fixing a finding, for example the definitions on first use and the scope sweeps of rounds 9 to 11) are fix commits of the phase too; they are listed in E-09 and are not separate findings. IN-04 has a second half, a Docker-free test that pins the forwarding of a `custom_` key on client 0.11.0; the reviewer worded it as "consider", and a test is outside what this phase may change, so it is not applied and is not carried as a deferred item; the note says that the probe behind row D7 is not in the repository. IN-14 and IN-15 were fixed after round 12 and were not reviewed by a further fresh reviewer (the user's decision above).

## Mandated confirmations (RLS-04)

These restate the verdicts of round 12, the last review round, over the round-11 fixes; the fixes of IN-14 and IN-15 that followed were not reviewed again.

- 29 rows against the three handoffs and the labels against the evidence: confirmed by the reviewer for the four labels verbatim and for every row's label against its How cell; the note check printed `OK rows=29 measured=19 code=18 derived=3 research=9` and `OK labels: 4 of 4 defined`. Row B3's split label is earned (Kafka part derived, connector part read from the code, `State MISMATCH` measured in research and not re-run). The same check prints the same counts after the IN-14 and IN-15 fixes.
- Commands, numbers and versions against the evidence: confirmed; `git diff v1.7.0_gg5 HEAD -- src/main` is 7 files, 112 insertions, 31 deletions, re-run by the reviewer and again after the last fix.
- Rollback and its target `pre-v1.7.0_gg6`: confirmed; `pre-v1.7.0_gg6` and `v1.7.0_gg5` both resolve to `3a3a15a9`; the rollback block was not changed in round 12's range.
- No fact about the `release object` of `v1.7.0_gg6`: confirmed; the note and the CHANGELOG state no digest, tag commit or URL of it, and FORK-DELTA states the allow-list as a plan with its condition.
- No advice that empties or destroys data: confirmed; the offset reset advice stays flagged as try-first and now says the outcome depends on where the first chunk lies.
- The `custom_` search recipe and item (D7): confirmed against `src/main` and the evidence; a `custom_` key fails the start under every `errors.tolerance` on client V2 only, with the forwarding and the doubled encoding labelled `measured in research (not re-run)`.
- `additionalRetriableErrorCodes` and its bypass of `errors.tolerance`: confirmed, described in rows C1 to C3 and the CHANGELOG as before.
- The rows still labelled `measured in research (not re-run)`: confirmed, none re-run and none relabelled stronger.
- FORK-DELTA stale forward references, numbers and open items: confirmed; the open items 12 to 23 each carry an owner and a re-open trigger or condition, and row 32 agrees with the note.
- Released CHANGELOG sections byte-identical: confirmed by the reviewer (`cmp` of the file below the new section against `git show v1.7.0_gg5:CHANGELOG.md`) and by the changelog check after the last fix.
- No host, connector or planning identifier in published text: confirmed; the strict hygiene check prints 0 hits, and no finding identifier or `round:` text occurs in the note or the CHANGELOG section.

## Carried to the milestone close

### 38-REVIEW WR-08: The `ROLLOUT-v1.7.0_gg1.md` correction and one FORK-DELTA sentence state more than rows D9 and D13 support

- **Reason:** the `docs/ROLLOUT-v1.7.0_gg1.md` half of the finding (item 6 states "measured" for the `zstd-jni` native and the executable temp folder, which the gg6 note labels `measured in research (not re-run)`, and says client V1 "of any line" where only two lines were observed) needs an edit of `docs/ROLLOUT-v1.7.0_gg1.md`, which is outside this phase's editable set; the FORK-DELTA half was fixed in `f1e32844`.
- **Owner:** the user as fork maintainer.
- **Re-open trigger:** the next edit of `docs/ROLLOUT-v1.7.0_gg1.md`, or the milestone close, whichever comes first.
