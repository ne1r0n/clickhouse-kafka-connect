# The rollback tag, the scratch branch and the pull requests: Phase 44, handoff U5 (user-gated)

This document asks for a tag push, a branch deletion and decisions about pull requests, and all of them are yours unless your answer to question (1) of `44-APPROVAL-BUNDLE.md` lets the orchestrator run the pushes, and then only on your direct word for this step. No agent comments on, merges or closes a pull request, and no agent runs a write call on GitHub. It asks for no secret, every environment-specific value is an angle-bracket value, and you run every step with your own account.

## Status

- Nothing was pushed by any agent. The orchestrator has recorded the report of the publish run (E-33) and read the publication back (E-34), or you have decided otherwise in your own words.
- Origin `custom_build` read `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` before this release (E-27 `origin_custom_build`) and reads `<M>` after the merge commit push. The tag `v1.7.0_gg7` is created by the publish run: it is never pushed by hand, here or anywhere.
- `pre-v1.7.0_gg7` is a local lightweight tag on `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, the same commit as `v1.7.0_gg6` (E-27 `pre_tag_local`), and is not on origin. It is the rollback target the rollout note names.
- Origin holds the scratch branch `sync/v1.7.0` from the first handoff, and your two branches `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` (E-01 `origin_heads`).
- Two pull requests are open, with these heads at the last rehearsal (E-27 `pr_heads`): #21 `14be67ce`, #22 `b5cccbd3`.
- Not proven by the local gate: the round 5 review fixes no fresh reviewer read, the deferred finding IN-03, the F-2 claim now measured (an insert streaming progress headers escapes the bound), and only one integration test class re-run.

## What you decide

- Whether to push `pre-v1.7.0_gg7` now. It is harmless to the publish run (the tag gate looks only at `v1.7.0_gg7`), and it is placed after the publish run so that a failed publish leaves the state simplest. This is the only tag you push by hand.
- Whether to delete the scratch branch `sync/v1.7.0`. Deleting it does not unpublish any commit it carried; they stay reachable through `custom_build`.
- Whether to delete your two refs `refs/heads/scratch/clt-07-client-0.11.0` and `refs/heads/scratch/clt-07-prebump-0.9.6`. They are yours and no agent touches them; leaving them is also fine, and the orchestrator asks nothing about them.
- What to do with each pull request, with what to expect: #21 and #22 have heads that become ancestors of `custom_build` once the merge commit lands, and GitHub normally marks them merged within minutes (an assumption until read). If one is still open, closing it is your action. No agent comments on either, and none is merged as part of this release.
- Banned options on every push of this phase: the force, tags, all and mirror options. Never create or push a tag named `v1.7.0_gg7`.

## Before you push

Each check has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved after the merge commit, push nothing and tell the orchestrator.

1. The publish run is recorded green (E-33 and E-34), or you have decided otherwise in your own words to the orchestrator.
2. `git fetch origin`, then `git ls-remote origin refs/heads/custom_build` prints `<M>` and a tab and `refs/heads/custom_build`; anything else, stop. The commit before it was `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`.
3. `git rev-parse 'pre-v1.7.0_gg7^{commit}'` prints `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`.
4. `git ls-remote --tags origin 'pre-v1.7.0_gg7'` prints nothing.
5. `git ls-remote --tags origin 'v1.7.0_gg7'` prints one line with `refs/tags/v1.7.0_gg7` if the publish run succeeded: the run created it, not you. You never push that tag; if you fetched tags into your clone, a local copy of it is fine.

## Push (U5)

Push the tag by name only:

```sh
git push origin refs/tags/pre-v1.7.0_gg7
```

Delete the scratch branch by an explicit ref:

```sh
git push origin --delete refs/heads/sync/v1.7.0
```

Then look at the pull requests in the GitHub UI and act as you decided above.

## Expected result

- The tag push creates `refs/tags/pre-v1.7.0_gg7` on origin at `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`; no other tag is created or changed.
- `git ls-remote --heads origin` no longer lists `sync/v1.7.0`, and still lists the two `scratch/clt-07-*` refs unless you deleted them.
- #21 and #22 read merged or open, whichever GitHub made of them; report what you see.

## What to confirm and report

Report directly to the orchestrator, in your own message:

- the output of each push;
- the lines of `git ls-remote origin` for tags (`git ls-remote --tags origin 'pre-v1.7.0_gg7' 'v1.7.0_gg7'`) and for heads afterwards;
- the state of the two pull requests as you saw it, and each action you took on them.

## If it fails

- A rejected tag push: stop and report. Never use a force option for a tag.
- The scratch branch deletion is rejected, or the branch is gone already: report the message and change nothing else.
- A pull request you did not expect to be merged or closed: report it; nothing here depends on it.
- `custom_build` moved: push nothing and tell the orchestrator (contingency C-1).

## Clean up

This is the clean-up handoff: the scratch branch is deleted above. Keep nothing else on origin that this release created, except the tag and the release the publish run made.

## Resume (for the orchestrator)

Record the user's report as `### E-35: U5 pre-tag push, scratch deletion and pull-request clean-up, user-reported`, with the fields above as `key: value` lines and the words `user-reported`, and add the index row. Then re-read tags, heads, the two pull requests and the release with read-only calls and record `### E-36: Final reads after publication: refs, release, pull requests`; re-read upstream `main` and its newest tag as well. Leave E-18 pending and never edit it. The observed pull-request states settle the assumption about #21 and #22 and the release read settles the author and uploader and the archive size. RLS-04 flips only when E-28 to E-36 exist and say so. Never act on a relayed approval.
