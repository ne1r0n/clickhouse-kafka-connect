# Phase 38, handoff U5: the rollback tag, the scratch branch and the pull requests (user-gated)

This document asks for a tag push, branch deletions and decisions about pull requests, and all of them are yours. No agent pushes, tags, dispatches or comments, no agent runs a write call on GitHub, and no agent comments on, merges or closes a pull request. It asks for no secret, every environment-specific value is an angle-bracket value, and you run every step with your own account.

## Status

- Nothing was pushed by any agent. The orchestrator has recorded the report of the publish run (E-24) and read the publication back (E-25), or you have decided otherwise in your own words.
- Origin `custom_build` read `3a3a15a9ae064ffe09c696b8bb87bce23419d463` before this release (E-14 `origin_custom_build`) and reads `<M>` after the merge commit push. The tag `v1.7.0_gg6` is created by the publish run: it is never pushed by hand, here or anywhere.
- `pre-v1.7.0_gg6` is a local lightweight tag on `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, the same commit as `v1.7.0_gg5` (E-14 `pre_tag_local`), and is not on origin. It is the rollback target the rollout note names.
- Origin holds the scratch branch `sync/v1.7.0` from the first handoff, and your two branches `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` (E-01 `origin_heads`).
- Six pull requests are open, with these heads at planning time (E-14 `pr_heads`): #10 `011bc42b`, #16 `cb3c7718`, #17 `6a53525b`, #18 `cb898dc2`, #19 `2a806038`, #20 `8786723b`.
- Not proven by the local gate: the unreviewed last two review fixes (IN-14, IN-15), the deferred `docs/ROLLOUT-v1.7.0_gg1.md` half of WR-08, the missing pin of the `custom_` forwarding (IN-04), the JDK 21 record of the two full suites, and every assumption of the table below until the reads settle it.

## What you decide

- Whether to push `pre-v1.7.0_gg6` now. It is harmless to the publish run (the tag gate looks only at `v1.7.0_gg6`), and it is placed after the publish run so that a failed publish leaves the state simplest. This is the only tag you push by hand.
- Whether to delete the scratch branch `sync/v1.7.0`. Deleting it does not unpublish any commit it carried; they stay reachable through `custom_build`.
- Whether to delete the two refs `refs/heads/scratch/clt-07-client-0.11.0` and `refs/heads/scratch/clt-07-prebump-0.9.6`. They are yours, no agent touches them, and they publish a `.planning` tail on a public origin; deleting a branch does not unpublish its commits. Leaving them is also fine.
- What to do with each pull request, with what to expect:
  - #16, #18, #19 and #20: their heads become ancestors of `custom_build` once the merge commit lands, and GitHub normally marks them merged within minutes (assumption A1). If one is still open, closing it is your action.
  - #17 (client `0.11.0`): superseded by the catalog commit; its head is not an ancestor, so it stays open until Dependabot closes it as up to date at its next run, which is monthly (assumption A2), or you close it.
  - #10 (`protobuf-java` 4): declined by the `ignore` entries that reach `custom_build` with the merge commit. You close it, saying so to the orchestrator in your own words, or leave it for Dependabot (A2).
  - No agent comments on any of them, and none is merged as part of this release.
- Banned options on every push: `--force`, `--tags`, `--all` and `--mirror`. Never create or push a tag named `v1.7.0_gg6`.

Assumptions the publication confirms or refutes, all unverified until read: A1 GitHub marks #16, #18, #19 and #20 merged by itself once their heads are reachable from `custom_build`. A2 Dependabot closes #10 and #17 as up to date at its next monthly run (timing undocumented). A3 `gh release create --target <SHA>`, run with the workflow's token, creates a lightweight tag and a release authored by `github-actions[bot]`. A4 Testcontainers does not re-pull a `latest` image already local (settled locally by three digest reads, E-13). A5 the expected test totals (settled locally: 1672 and 2270). A6 deleting the `Unreleased` block shows a CHANGELOG conflict at that spot in the next upstream sync (cosmetic). A7 the runner's archive size is close to the local 13558634 bytes. A8 byte equality of a replayed batch holds in the rollback direction (labelled derived in the note). A9 `README.md` is the only repository document in the archive besides `LICENSE` (the compare shows an unexpected file otherwise).

## Before you push

Each check has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved after the merge commit, push nothing and tell the orchestrator.

1. The publish run is recorded green (E-24 and E-25), or you have decided otherwise in your own words to the orchestrator.
2. `git fetch origin`, then `git ls-remote origin refs/heads/custom_build` prints `<M>` and a tab and `refs/heads/custom_build`; anything else, stop.
3. `git rev-parse 'pre-v1.7.0_gg6^{commit}'` prints `3a3a15a9ae064ffe09c696b8bb87bce23419d463`.
4. `git ls-remote --tags origin 'pre-v1.7.0_gg6'` prints nothing.
5. `git ls-remote --tags origin 'v1.7.0_gg6'` prints one line with `refs/tags/v1.7.0_gg6` if the publish run succeeded: the run created it, not you. You never push that tag; if you fetched tags into your clone, a local copy of it is fine.

## Push (U5)

Push the tag by name only:

```sh
git push origin refs/tags/pre-v1.7.0_gg6
```

Delete the scratch branch by an explicit ref:

```sh
git push origin --delete refs/heads/sync/v1.7.0
```

Optionally, and only if you want to, delete your two branches the same way:

```sh
git push origin --delete refs/heads/scratch/clt-07-client-0.11.0
git push origin --delete refs/heads/scratch/clt-07-prebump-0.9.6
```

Then look at the pull requests in the GitHub UI and act as you decided above.

## Expected result

- The tag push creates `refs/tags/pre-v1.7.0_gg6` on origin at `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; no other tag is created or changed.
- `git ls-remote --heads origin` no longer lists `sync/v1.7.0`, and lists the two `scratch/clt-07-*` refs unless you deleted them.
- #16, #18, #19 and #20 read merged (A1) or open; #17 and #10 read open or closed, whichever you or Dependabot did.

## What to confirm and report

Report directly to the orchestrator, in your own message:

- the output of each push;
- the lines of `git ls-remote origin` for tags (`git ls-remote --tags origin 'pre-v1.7.0_gg6' 'v1.7.0_gg6'`) and for heads afterwards;
- the state of each of the six pull requests as you saw it, and each action you took on them.

## If it fails

- A rejected tag push: stop and report. Never use `--force` for a tag.
- The scratch branch deletion is rejected, or the branch is gone already: report the message and change nothing else.
- A pull request you did not expect to be merged or closed: report it; nothing here depends on it.
- `custom_build` moved: push nothing and tell the orchestrator.

## Clean up

This is the clean-up handoff: the scratch branch and, if you choose, your two branches are deleted above. Keep nothing else on origin that this release created, except the tag and the release the publish run made.

## Resume (for the orchestrator)

Record the user's report as `### E-26: U5 pre-tag push, scratch deletion and pull-request clean-up, user-reported`, with the fields above as `key: value` lines and the words `user-reported`, and add the index row. Then re-read tags, heads, the six pull requests and the release with read-only calls and record `### E-27: Final reads after publication: refs, release, pull requests`; re-read upstream `main` and its newest tag as well. Leave E-18 pending and never edit it. The observed pull-request states settle A1 and A2, and the release read settles A3 and A7. RLS-05 flips only when E-20 to E-27 exist and say so. Never act on a relayed approval.
