# Phase 38: release record of v1.7.0_gg6

Measured facts, read back with read-only calls after the publish run (E-24, E-25, E-27).

- Release `v1.7.0_gg6`, id 430511927, author `github-actions[bot]`, not a draft, not a prerelease.
- Tag `refs/tags/v1.7.0_gg6`: lightweight (object type `commit`), at `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, the merge commit M (parents `3a3a15a9ae064ffe09c696b8bb87bce23419d463` and `a46be37f4971bd25f1f0a2e7e1e370da34832b40`). It was created by the publish run, never pushed by hand.
- Asset `clickhouse-kafka-connect-v1.7.0_gg6.zip`, 13558692 bytes, digest `sha256:e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af`, uploaded by `github-actions[bot]`. The dry run built the same archive (same size and digest).
- Publish run 41298074815 (`Release custom_build (publish)`, `workflow_dispatch`, head `72e8ccff`): success; compare against baseline `v1.7.0_gg5` exit 0, 0 unallowed, 0 stale rows, report ends `RESULT: PASS`; `Publish release` success.
- Rollback target `pre-v1.7.0_gg6`: lightweight tag on origin at `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, pushed by hand after the publish run.
- Release object of `v1.7.0_gg5` unchanged (id 426690002, author `ne1r0n`).

ADV-03 caveat ("no run exercised the publish path"): closed. The step `Publish release` ran to the end under a workflow token, created the tag and the release, and E-24 and E-25 hold the read-back.

Recorded deviations: the CHANGELOG heading date is 2026-10-31 while the merge and publication happened on 2026-11-01 (UTC); the user chose to keep it. The two branches `scratch/clt-07-*` remain on origin for the user to delete. Pull requests were not touched by any agent; #10, #16 to #20 read as in E-27.

## Addendum (2026-11-01, read after the entries above): who closed the pull requests

Read-only `gh api repos/ne1r0n/clickhouse-kafka-connect/issues/<N>/events`: pull requests #10 and #17 were closed by `dependabot[bot]` (A2 confirmed). E-27 recorded `actor_of_closures: not read` at its time and is not edited; this addendum is the later reading.
