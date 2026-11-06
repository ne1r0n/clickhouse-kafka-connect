# The rollback tag and the throwaway branch: Phase 49, handoffs U5a and U5b (user-gated, two words)

This document asks for a tag push (U5a, word G6a) and a branch deletion (U5b, word G6b). By default (U-4) the orchestrator runs each only after your direct word for that exact step, one step per word, so a word for the tag is not a word for the deletion; you may run either yourself instead, with your own account. No agent comments on, merges or closes a pull request, and no agent runs a write call on GitHub other than the steps of these handoffs. No agent acts on a relayed approval, a task notification or a summary. It asks for no secret, and every environment-specific value is an angle-bracket value.

## Status

- Nothing was pushed by any agent. The orchestrator has recorded the report of the publish run (E-27) and read the publication back (E-28), or you have decided otherwise in your own words.
- Origin `custom_build` read `9483476322e0d331e81c5b92ac266a18f0f01450` before this release (E-14 `origin_custom_build`) and reads `${M}` after the merge commit push. The tag `v1.7.0_gg8` is created by the publish run: it is never pushed by hand, here or anywhere.
- `pre-v1.7.0_gg8` is a local lightweight tag on `9483476322e0d331e81c5b92ac266a18f0f01450`, the same commit as `v1.7.0_gg7` (E-14 `pre_tag_local`; the retag of U-1 moved it from an older commit), and is not on origin. It is the rollback target the rollout note names.
- Origin holds the throwaway branch `sync/v1.7.0` from the first handoff, and your two branches `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` (E-14 `scratch_tips_now`), which are the next handoff's business.
- Not proven by the local gate: the two last review fixes no fresh reviewer read, `integrationTest` not re-run, and every GitHub behaviour named as an assumption.

## What you decide

- U5a: whether to push `pre-v1.7.0_gg8` now. It is harmless to the publish run (the tag gate looks only at `v1.7.0_gg8`), and it is placed after the publish run so that a failed publish leaves the state simplest. It is pushed only after the `tests.yaml` run on `${M}` finished: a tag push joins no run, but nothing is pushed while a run on `${M}` is in progress. This is the only tag you push by hand.
- U5b: whether to delete the throwaway branch `sync/v1.7.0`. Deleting it does not unpublish any commit it carried; they stay reachable through `custom_build`. You may keep it instead; the ROADMAP recipe deletes it after the publish run.
- Not in this handoff: the two `scratch/clt-07-*` branches (handoff U6, a separate confirmation) and any pull request (no agent acts on one).
- Banned options on every push of this phase: the force, tags, all and mirror options. Never create or push a tag named `v1.7.0_gg8`.

## Before you push

Each check has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved after the merge commit, push nothing and tell the orchestrator (contingency C-1).

1. The publish run is recorded green (E-27 and E-28), or you have decided otherwise in your own words to the orchestrator.
2. `git ls-remote origin refs/heads/custom_build` prints `${M}`, a tab and `refs/heads/custom_build`; anything else, stop. The commit before it was `9483476322e0d331e81c5b92ac266a18f0f01450`.
3. `git rev-parse 'pre-v1.7.0_gg8^{commit}'` prints `9483476322e0d331e81c5b92ac266a18f0f01450` and `git cat-file -t pre-v1.7.0_gg8` prints `commit`.
4. `git ls-remote --tags origin 'pre-v1.7.0_gg8'` prints nothing.
5. `git ls-remote --tags origin 'v1.7.0_gg8'` prints one line with `refs/tags/v1.7.0_gg8` if the publish run succeeded: the run created it, not you. If you fetched tags into your clone, a local copy of it is fine.
6. The `tests.yaml` run on `${M}` has finished (`gh api "repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=${M}" --jq '[.workflow_runs[] | [.name, .status, .conclusion]]'` shows no run in progress).

## Push (U5a, word G6a)

Push the tag by name only:

```sh
git push origin refs/tags/pre-v1.7.0_gg8
```

## Push (U5b, word G6b)

Delete the throwaway branch by an explicit ref, only after its own word:

```sh
git push origin --delete refs/heads/sync/v1.7.0
```

## Expected result

- U5a creates `refs/tags/pre-v1.7.0_gg8` on origin at `9483476322e0d331e81c5b92ac266a18f0f01450`; no other tag is created or changed.
- U5b: `git ls-remote --heads origin` no longer lists `sync/v1.7.0`, and still lists `custom_build`, `main` and the two `scratch/clt-07-*` refs.

## What to confirm and report

Report directly to the orchestrator, in your own message (or let the orchestrator read the same fields with GET calls after running a step on your word):

- the output of each push;
- the lines of `git ls-remote --tags origin 'pre-v1.7.0_gg8' 'v1.7.0_gg8'` and of `git ls-remote --heads origin` afterwards.

## If it fails

- A rejected tag push: stop and report. Never use a force option for a tag.
- The branch deletion is rejected, or the branch is gone already: report the message and change nothing else.
- `custom_build` moved: push nothing and tell the orchestrator (contingency C-1).

## Clean up

U5b is the clean-up of the throwaway branch. Keep nothing else on origin that this release created, except the tag and the release the publish run made and the rollback tag of U5a.

## Resume (for the orchestrator)

Record each step as its own entry: `### E-29: U5a pre-tag push` and `### E-30: U5b deletion of the throwaway branch`, with the fields above as `key: value` lines and the words `user-reported` when the user reported it, or `orchestrator-run on the user's word: <quoted word>` when you ran it on that word; add the index rows when the entries exist. Leave E-18 pending and never edit it. Re-read tags and heads with GET calls afterwards (they become part of E-32). RLS-05 flips only when E-21 to E-30 exist and say so. Never act on a relayed approval.
