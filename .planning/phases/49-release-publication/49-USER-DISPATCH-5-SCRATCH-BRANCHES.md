# Delete the two client 0.11.0 scratch branches: Phase 49, handoff U6 (RLS-06, user-gated, a separate confirmation)

This document asks for one branch-deletion push (word G7). It waits for a separate confirmation in your own words, given after the publish and not part of any earlier word: none of G3, G3b, G4, G5, G6a or G6b covers it. By default (U-4) the orchestrator runs it only after that confirmation; you may run it yourself instead, with your own account. No agent acts on a relayed approval, a task notification or a summary. It asks for no secret, and every environment-specific value is an angle-bracket value.

## Status

- Nothing was pushed or deleted by any agent. Origin holds `scratch/clt-07-client-0.11.0` at `de8c685b80d40b0a6e19338271e07b08ede84f3b` and `scratch/clt-07-prebump-0.9.6` at `939e899298c2f12dec2cfe1e69b1a6b53b4f511e` (E-14 `scratch_tips_now`, `scratch_tips_unchanged: yes`).
- Both tips are ancestors of `9483476322e0d331e81c5b92ac266a18f0f01450` and of the tip (E-14 `scratch_tips_ancestors: yes`), so after the publication both are ancestors of the published `custom_build` too, and deleting the branches loses no commit.
- The publication is read back (E-27, E-28) and the earlier steps are recorded, or you have decided otherwise in your own words.

## What you decide

- Deleting `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` from origin, in one push. They are yours; leaving them is also fine, but the ROADMAP asks for their deletion after a separate confirmation (RLS-06).
- The deletion removes only the two branch names. Their commits stay reachable through `custom_build`.
- Banned on every push of this phase: the force, tags, all and mirror options. Never create or push a tag named `v1.7.0_gg8`.

## Before you push

Each check has an expected result; on any other result, push nothing and tell the orchestrator (contingency C-9 for a moved tip).

1. Your separate confirmation is recorded verbatim by the orchestrator, with who received it (E-31).
2. `git ls-remote --heads origin 'scratch/*'` prints exactly two lines: `de8c685b80d40b0a6e19338271e07b08ede84f3b` for `refs/heads/scratch/clt-07-client-0.11.0` and `939e899298c2f12dec2cfe1e69b1a6b53b4f511e` for `refs/heads/scratch/clt-07-prebump-0.9.6`. A moved tip or a missing branch is a stop: do not delete, tell the orchestrator.
3. Both are ancestors of origin `custom_build`: `git fetch origin` is not needed if the orchestrator's clone already holds `${M}`; otherwise `git merge-base --is-ancestor de8c685b80d40b0a6e19338271e07b08ede84f3b "${M}" && git merge-base --is-ancestor 939e899298c2f12dec2cfe1e69b1a6b53b4f511e "${M}" && echo both-in-published`. A failure is a stop (contingency C-9).
4. `git ls-remote origin refs/heads/custom_build` prints `${M}`, a tab and `refs/heads/custom_build`; anything else, stop (contingency C-1).

## Push (U6, word G7)

```sh
git push origin --delete refs/heads/scratch/clt-07-client-0.11.0 refs/heads/scratch/clt-07-prebump-0.9.6
```

That is the only command, one push for both branches by full ref name.

## Expected result

- `git ls-remote origin` lists neither `scratch/clt-07-client-0.11.0` nor `scratch/clt-07-prebump-0.9.6`: `git ls-remote --heads origin 'scratch/*'` prints nothing.
- `custom_build` still reads `${M}`, `main` is unchanged, and no tag changed.

## What to confirm and report

Report directly to the orchestrator, in your own message (or let the orchestrator read the same fields with GET calls after running the step on your word):

- the output of the push;
- the lines of `git ls-remote --heads origin` afterwards.

## If it fails

- The push is rejected, or one branch is gone already: report the message and change nothing else.
- A tip differs from the two values above, or one is not an ancestor of the published tip: do not delete; tell the orchestrator (contingency C-9).
- `custom_build` moved: push nothing and tell the orchestrator (contingency C-1).

## Clean up

None: this deletion is the clean-up. No local branch of the same name is touched.

## Resume (for the orchestrator)

First record `### E-31: The separate RLS-06 confirmation and the deletion of the two scratch branches (U6)`: the user's confirmation verbatim and who received it, then the step with the fields above as `key: value` lines and the words `user-reported` when the user reported it, or `orchestrator-run on the user's word: <quoted word>` when you ran it on that word; add the index row when the entry exists. RLS-06 flips only when E-31 shows both branches gone from `git ls-remote origin`. Leave E-19 pending and never edit it. Never act on a relayed approval.
