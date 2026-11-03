# The push of the merge commit M to custom_build by SHA: Phase 44, handoff U3 (user-gated)

This document asks for one push to origin and it is yours. No agent pushes, tags, dispatches or comments unless your answer to question (1) of `44-APPROVAL-BUNDLE.md` lets the orchestrator run the pushes, and then only on your direct word for this step. It asks for no secret, every environment-specific value is an angle-bracket value, and you run every step with your own account.

**It starts only after the dry run (first handoff) was reported green and after your own direct "publish" message to the orchestrator.** An approval relayed by another agent does not count. The orchestrator then builds the merge commit M and tells you two SHAs in its own message: `X0`, the SHA you pushed to the scratch branch in the first handoff, and `M`. Wherever this document writes `<M>` or `<X0>`, use those two values.

## Status

- Nothing was pushed by any agent. Origin `custom_build` reads `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` until you push (E-27 `origin_custom_build`), and no `v1.7.0_gg7` tag exists.
- M is built by the orchestrator on a detached worktree at `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` with `git merge --no-ff -F` of the committed merge message (`44-MERGE-MESSAGE.txt`) and `X0`; the rehearsal of the same merge (E-27) had exactly two parents, the tip's tree, the agreed subject and no `.gsd-ws-arg`, and the note's rollback block ran on it and printed `ok`. The local `custom_build` ref is never moved or checked out; M is reachable through a private ref and is pushed by SHA; it exists only in the local repository the orchestrator built it in, so run the push from that same repository.
- What the dry run (first handoff) showed is recorded by the orchestrator (E-29 and E-30) before this step.
- Not proven by the local gate, as in the first handoff: the round 5 review fixes no fresh reviewer read, the deferred finding IN-03, the F-2 claim now measured (an insert streaming progress headers escapes the bound), only one integration test class re-run, and every GitHub behaviour named as an assumption.

## What you decide

- M is a non-force `--no-ff` merge with parents `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` and `<X0>`, the subject `Merge sync/v1.7.0: release v1.7.0_gg7` and the tree of `<X0>`. It differs from `<X0>` as a commit, so the two `tests.yaml` runs do not share a concurrency group.
- Pushing M moves `custom_build`, the default branch, and puts the whole `.planning` tree on it; the history it brings is the one you already pushed in the first handoff.
- A push starts `tests.yaml` once and starts no release run.
- The CHANGELOG heading of the release reads `2026-11-02` and the merge happens after the UTC date moved on. Your answer to question (5) of the bundle decides: keep the heading date as a recorded deviation (the pre-check prints a difference that you accepted), or hold for a date-fix commit, which re-opens the review of that diff, the allow-list, the Docker-free gates and the suites on the new head (about 2.5 hours) and voids this approval.
- Banned options on every push: the force, tags, all and mirror options. A rejected push (not a fast-forward) is a stop.
- Nobody merges a Dependabot pull request (#21, #22) on origin before this push, and no agent comments on any pull request.

## Before you push

Each check has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved, push nothing and tell the orchestrator (contingency C-1): it merges `origin/custom_build` into `sync/v1.7.0` (never a rebase), checks the landed lines against the anchors, runs the gates again, rebuilds M and asks you whether to absorb or hold. A move voids your approval.

1. `git ls-remote origin refs/heads/custom_build` still prints `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` and a tab and `refs/heads/custom_build`.
2. `git cat-file -p <M>` shows two `parent` lines, `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` and `<X0>`, and the subject `Merge sync/v1.7.0: release v1.7.0_gg7`.
3. `git diff --quiet <M> <X0> && echo tree-equal` prints `tree-equal`.
4. `git diff --quiet 5b25c18ce6137892383899f9f10663a22f498fe5 <X0> -- . ':(exclude).planning' && echo same-as-gated` prints `same-as-gated`, and `git diff --quiet 06a9980a7f7271efa1bae4c8b88ad149e51f50ee <X0> -- .planning/scripts && echo scripts-same-as-gated` prints `scripts-same-as-gated` (the same two checks as in the first handoff, now for `<X0>`).
5. `git ls-remote origin | grep <M>` prints nothing.
6. You need no checkout of `custom_build`, and no Dependabot pull request has been merged on origin since the first handoff (`git rev-list --count sync/v1.7.0..origin/custom_build` after a fetch is still `0`).
7. `git show <M>:CHANGELOG.md | head -1` prints `# 1.7.0_gg7, 2026-11-02`, and `date -u +%F` against it: report both values. A difference is the one you decided in question (5); if you held for a date-fix, stop.

## Push (U3)

```sh
git push origin "${M}":refs/heads/custom_build
```

Set `M` to the value the orchestrator gave you first. That is the only command. Then wait for the workflow run of `tests.yaml` on `<M>`: exactly one run, with the conclusion `success`, still exactly one two minutes later, and zero `release.yaml` runs on `<M>`:

```sh
gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<M>' --jq '[.workflow_runs[] | [.name, .event, .conclusion]]'
```

## Expected result

- The push is accepted as a fast-forward of `custom_build` from `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` to `<M>`.
- Exactly one run of `tests.yaml` on `<M>` (event `push`), concluded `success` in six to ten minutes, still one two minutes later; no run of `release.yaml` and no other workflow run is listed for `<M>`; if one is, name it in the report.
- `git ls-remote origin refs/heads/custom_build` prints `<M>`.

## What to confirm and report

Report directly to the orchestrator, in your own message:

- `<M>` and `<X0>` as you used them;
- the output of the push;
- the `tests.yaml` run id, URL and conclusion;
- the run count at once and the count two minutes later, and any other run on `<M>`;
- `git ls-remote origin refs/heads/custom_build` printing `<M>`;
- the two values of pre-check 7.

## If it fails

- A rejected push (not a fast-forward): `custom_build` moved. Push nothing else and tell the orchestrator (contingency C-1).
- A red `tests.yaml` run on `<M>`: do not dispatch the release, and report (contingency C-4). `custom_build` is then already at `<M>`; the way back is the rollback below, forward only, never a force-push, and the orchestrator and you decide.
- Two or more `tests.yaml` runs on `<M>`, or a `release.yaml` run: report them and change nothing.
- Never push a tag from this handoff, and never create or push `v1.7.0_gg7`.

Rollback, if you decide it after this push (the block of the Rollback section of `docs/ROLLOUT-v1.7.0_gg7.md`, which ran unchanged on the rehearsal merge and printed `ok`). Start from the published tip with nothing uncommitted, then:

```sh
git switch -c rollback-v1.7.0_gg7
git read-tree -u --reset pre-v1.7.0_gg7
git commit -m "Roll back v1.7.0_gg7"
git diff --quiet HEAD pre-v1.7.0_gg7 && echo ok
```

The rehearsal on this base showed a clean tree after the block (no ` M gradlew.bat` line); if a fresh worktree lists one anyway, run `git switch` and `git revert` with git's attributes switched off, as `git --attr-source="$(git hash-object -t tree /dev/null)" revert <rollback-commit>`. Publish the rollback commit as a new commit on `custom_build` (step 4 of the note's Rollback section), and redeploy the `v1.7.0_gg6` archive. It is forward only and never a force-push. The local tag `pre-v1.7.0_gg7` must exist in the clone you use (it is local until the last handoff pushes it).

## Clean up

None.

## Resume (for the orchestrator)

First record `### E-31: The user's own publish message and the merge commit M, built after it`: the user's message verbatim, who received it, M, X0 and the checks of the runbook (step O3). Then record the user's report as `### E-32: U3 push of M to custom_build and its single tests.yaml run, user-reported`, with the fields above as `key: value` lines and the words `user-reported`, and add the index rows. Read the runs for M's SHA with read-only calls and record the reading with it. Leave E-16 pending and never edit it. A red or not-exactly-one run stops the sequence: no dispatch, and the orchestrator asks the user again. Never act on a relayed approval.
