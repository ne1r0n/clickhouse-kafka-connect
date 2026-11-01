# Phase 38, handoff U3: the push of the merge commit M to custom_build by SHA (user-gated)

This document asks for one push to origin and it is yours. No agent pushes, tags, dispatches or comments, and no agent runs a write call on GitHub. It asks for no secret, every environment-specific value is an angle-bracket value, and you run every step with your own account.

**It starts only after the dry run (first handoff) was reported green and after your own direct "publish" message to the orchestrator.** An approval relayed by another agent does not count. The orchestrator then builds the merge commit M and tells you two SHAs in its own message: `X0`, the SHA you pushed to the scratch branch in the first handoff, and `M`. Wherever this document writes `<M>` or `<X0>`, use those two values.

## Status

- Nothing was pushed by any agent. Origin `custom_build` reads `3a3a15a9ae064ffe09c696b8bb87bce23419d463` until you push (E-14 `origin_custom_build`), and no `v1.7.0_gg6` tag exists.
- M is built by the orchestrator on a detached worktree at `3a3a15a9ae064ffe09c696b8bb87bce23419d463` with `git merge --no-ff -F` of the committed merge message and `X0`; the rehearsal of the same merge (E-14) had exactly two parents, the tip's tree, the agreed subject and no `.gsd-ws-arg`, and the note's rollback block ran on it. The local `custom_build` ref is never moved or checked out; M is reachable through a private ref and is pushed by SHA; it exists only in the local repository the orchestrator built it in, so run the push from that same repository.
- What the dry run (first handoff) showed is recorded as E-20 and E-21 before this step.
- Not proven by the local gate, as in the first handoff: the unreviewed last two review fixes (IN-14, IN-15), the deferred `docs/ROLLOUT-v1.7.0_gg1.md` half of WR-08, the missing pin of the `custom_` forwarding (IN-04), the JDK 21 record of the two full suites (the Docker-free `testUnit` ran on the default JDK 17), and every GitHub behaviour named as an assumption (A1 to A9).

## What you decide

- M is a non-force `--no-ff` merge with parents `3a3a15a9ae064ffe09c696b8bb87bce23419d463` and `<X0>`, the subject `Merge sync/v1.7.0: release v1.7.0_gg6` and the tree of `<X0>`. It differs from `<X0>` as a commit, so the two `tests.yaml` runs do not share a concurrency group.
- Pushing M moves `custom_build`, the default branch, and puts the whole `.planning` tree on it; the history it brings is the one you already pushed in the first handoff.
- A push starts `tests.yaml` once and starts no release run.
- The CHANGELOG heading of the release reads `2026-10-31`. M must be pushed on that UTC date; a different date is a stop (pre-check 7), and the orchestrator asks you directly whether to keep the heading date as a recorded deviation or to hold for a date-fix commit, which needs a new review of that diff, the allow-list compare and the Docker-free gates.
- Banned options on every push: `--force`, `--tags`, `--all` and `--mirror`. A rejected push (not a fast-forward) is a stop.
- Nobody merges a Dependabot pull request on origin before this push, and no agent comments on any pull request.

## Before you push

Each check has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved, push nothing and tell the orchestrator: it merges `origin/custom_build` into `sync/v1.7.0` (never a rebase), checks the landed lines against the anchors, runs the gates again, rebuilds M and asks you whether to absorb or hold. A move voids your approval.

1. `git ls-remote origin refs/heads/custom_build` still prints `3a3a15a9ae064ffe09c696b8bb87bce23419d463` and a tab and `refs/heads/custom_build`.
2. `git cat-file -p <M>` shows two `parent` lines, `3a3a15a9ae064ffe09c696b8bb87bce23419d463` and `<X0>`, and the subject `Merge sync/v1.7.0: release v1.7.0_gg6`.
3. `git diff --quiet <M> <X0> && echo tree-equal` prints `tree-equal`.
4. `git diff --quiet 927ac07c30274c46b2330d597d417f9f1eda8f8b <X0> -- . ':(exclude).planning' && echo same-as-gated` prints `same-as-gated`, and `git diff --quiet ea613ec35ebade7a2182cac4e6ecd5d86d099def <X0> -- .planning/scripts && echo scripts-same-as-gated` prints `scripts-same-as-gated` (the same two checks as in the first handoff, now for `<X0>`).
5. `git ls-remote origin | grep <M>` prints nothing.
6. You need no checkout of `custom_build`, and no Dependabot pull request has been merged on origin since the first handoff (`git rev-list --count sync/v1.7.0..origin/custom_build` after a fetch is still `0`).
7. `git show <M>:CHANGELOG.md | head -1` prints `# 1.7.0_gg6, 2026-10-31` and `date -u +%F` prints `2026-10-31`. A different date is a stop.

## Push (U3)

```sh
git push origin <M>:refs/heads/custom_build
```

That is the only command. Then wait for the workflow run of `tests.yaml` on `<M>`: exactly one run, with the conclusion `success`, still exactly one two minutes later, and zero `release.yaml` runs on `<M>`:

```sh
gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<M>' --jq '[.workflow_runs[] | [.name, .event, .conclusion]]'
```

## Expected result

- The push is accepted as a fast-forward of `custom_build` from `3a3a15a9ae064ffe09c696b8bb87bce23419d463` to `<M>`.
- Exactly one run of `tests.yaml` on `<M>` (event `push`), concluded `success` in six to ten minutes, still one two minutes later; no run of `release.yaml` and no other workflow run is listed for `<M>`; if one is, name it in the report.
- `git ls-remote origin refs/heads/custom_build` prints `<M>`.

## What to confirm and report

Report directly to the orchestrator, in your own message:

- `<M>` and `<X0>` as you used them;
- the output of the push;
- the `tests.yaml` run id, URL and conclusion;
- the run count at once and the count two minutes later, and any other run on `<M>`;
- `git ls-remote origin refs/heads/custom_build` printing `<M>`.

## If it fails

- A rejected push (not a fast-forward): `custom_build` moved. Push nothing else and tell the orchestrator.
- A red `tests.yaml` run on `<M>`: do not dispatch the release, and report. `custom_build` is then already at `<M>`; the way back is the rollback below, forward only, never a force-push, and the orchestrator and you decide.
- Two or more `tests.yaml` runs on `<M>`, or a `release.yaml` run: report them and change nothing.
- Never push a tag from this handoff, and never create or push `v1.7.0_gg6`.

Rollback, if you decide it after this push (the block of the Rollback section of `docs/ROLLOUT-v1.7.0_gg6.md`, which ran unchanged on the rehearsal merge and printed `ok`). Start from the published tip with nothing uncommitted, then:

```sh
git switch -c rollback-v1.7.0_gg6
git read-tree -u --reset pre-v1.7.0_gg6
git commit -m "Roll back v1.7.0_gg6"
git diff --quiet HEAD pre-v1.7.0_gg6 && echo ok
```

Afterwards a fresh worktree lists ` M gradlew.bat` without any change of yours (the tag's file has Windows line endings and `.gitattributes` asks for normalisation); run `git switch` and `git revert` with git's attributes switched off, as `git --attr-source="$(git hash-object -t tree /dev/null)" revert <rollback-commit>`. Publish the rollback commit as a new commit on `custom_build` (step 4 of the note's Rollback section), and redeploy the `v1.7.0_gg5` archive. It is forward only and never a force-push. The local tag `pre-v1.7.0_gg6` must exist in the clone you use (it is local until the last handoff pushes it, or run `git fetch --tags origin` afterwards).

## Clean up

None.

## Resume (for the orchestrator)

First record `### E-22: The user's own approval and the merge commit M, built after it`: the user's message verbatim, who received it, M, X0 and the checks of the runbook (step O3). Then record the user's report as `### E-23: U3 push of M to custom_build and its single tests.yaml run, user-reported`, with the fields above as `key: value` lines and the words `user-reported`, and add the index rows. Read the runs for M's SHA with read-only calls and record the reading with it. Leave E-16 pending and never edit it. A red or not-exactly-one run stops the sequence: no dispatch, and the orchestrator asks the user again. Never act on a relayed approval.
