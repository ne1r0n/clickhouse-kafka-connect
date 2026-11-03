# Phase 44 re-open handoff (orchestrator, 2026-11-03)

## What the user answered at the G3 approval bundle (verbatim option labels, given directly via AskUserQuestion)

- U1+U2 go: "Go for U1 and U2"
- Who runs origin writes: "You run them (Recommended)"  (the user runs every push/dispatch from the 44-USER-DISPATCH-* files)
- CHANGELOG heading date: "Keep the date (Recommended)"  (heading `2026-11-02` stays, read as the date the documents were frozen; record as a deviation in the release record)
- Extras BEFORE the release: "Re-run integrationTest first" AND "Measure the T6 progress-header claim first"

## Consequence (why nothing was pushed or handed over)

Measuring T6 changes `docs/ROLLOUT-v1.7.0_gg7.md` (and possibly CHANGELOG / FORK-DELTA wording), which is outside `.planning`. By the Freeze rule that voids the bundle and the "go" for gated head `b265f5a4` (the go was given for that head). So the sequence below must run first; U1/U2 are NOT authorised for the new head until the user answers the rewritten bundle again.

Nothing has been pushed, merged, tagged or dispatched. Origin `custom_build` = `72e8ccff`; no `v1.7.0_gg7` tag; no `sync/*` head on origin.

## Sequence to run (fresh session: `/gsd-autonomous --from 44`, or `/gsd-resume-work`)

1. Measure the T6 claim against a real ClickHouse (pinned `latest` 26.10.3.7, digest 739e099a…): does an insert during which the server keeps sending progress headers reset the per-insert socket timer? (INSERT ... SELECT through the connector's V2 path with `send_progress_in_http_headers=1`, the connector default; earlier probes: body insert bounded; INSERT ... SELECT via the query path NOT bounded by a 1000 ms `socket_timeout`.) Record the outcome as `measured`; update note row T6, FORK-DELTA Known cost 14 and the CHANGELOG wording to the measurement, honestly (if it escapes the bound, state it as an unbounded case).
2. Re-run `integrationTest` (at least `ClickHouseSinkConnectorIntegrationTest`, as Phase 39's recorded run; whole task ~33 min; the nightly upstream workflow is red by inheritance, see memory) and record the result as evidence.
3. Re-open plan 44-03: a fresh orchestrator-spawned `gsd-code-reviewer` round over the changed text (round 5, new agentId, appended to `44-REVIEW.md`, IDs continue WR-07, IN-15), fixes, disposition, new frozen-documents evidence entry. G1 stop rule: more than the rounds already run needs the user's direct word again.
4. Re-run plan 44-04 Tasks 1-3 on the new head: allow-list regenerated last (archives unaffected by docs, but the freeze rule says re-run), `testUnit` with Docker unreachable, two full `--rerun` suites on one pinned `latest`, invariants script once on the new explicit SHA (~40-50 min, do not overlap with Gradle).
5. Re-run plan 44-05 Task 1 (rehearsal on the new tip, new merge message check, rewrite `44-APPROVAL-BUNDLE.md` for the new gated head). Keep the user's decisions above as recorded answers; ask again the questions the Freeze rule voids (the go for U1/U2 at least).
6. Then the publication tail only on the user's direct word, orchestrator-inline: U1/U2 (user pushes scratch branch + dry run), G4 "publish" -> orchestrator builds M detached, U3 (user pushes custom_build), G5 -> U4 (user dispatches release.yaml), U5 (pre-tag push, scratch delete). Evidence numbers E-21..E-29 (E-20 is the hygiene re-run).
7. After publication: Phase 44 verification (gsd-verifier), `phase.complete`, then the milestone lifecycle: `gsd-audit-milestone` -> `gsd-complete-milestone v1.7.0_gg7` -> `gsd-cleanup`.

## Facts at this point

- Tip `45cbc9db5bba2e5f367eff4bb7043251587e44e2` (then later `.planning` commits); gated head `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd`; last non-.planning commit `a43ff32f39647dfc3afb9849f6729b251f2619d2`; allow-list commit `402b6e2ea99a65e80c42f710e2b93277c6f78c64`.
- Phases 39-43 verified `passed`; Phase 44 plans 44-01..44-05 executed (local); RLS-01/02/03 satisfied locally, RLS-04 pending publication.
- Upstream main moved (9bed6f79 -> d92a1830: tests.yaml cloud secrets + two dependency bumps); no tag past v1.7.0; PRs #861/#855/#857 unchanged. Release is built on 9bed6f79 as the CHANGELOG states.
- Deferred to the milestone close: IN-03 (planning identifier "Phase 39" in the published gg6 note; owner user), 28 IN-05(b), four Phase 37 src/main rows, 16 carried items (see 44-LOCAL-CLOSE-RECORD.md).
- Hygiene helper quirk: it flags the plain word "stand" (pattern `\bstand\b`) in reviewer text; E-12 stays `fail` by design and E-20 supersedes it.
