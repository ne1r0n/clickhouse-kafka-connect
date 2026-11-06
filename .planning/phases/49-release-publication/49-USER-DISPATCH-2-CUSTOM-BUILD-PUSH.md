# The push of the merge commit M' to custom_build by SHA: Phase 49, handoff U3 (user-gated)

This document asks for one push to origin (word G4). By default (U-4) the orchestrator runs it only after your direct word for this exact step, one step per word; you may run it yourself instead, with your own account, from the repository that built `M'`. No agent acts on a relayed approval, a task notification or a summary. It asks for no secret, and every environment-specific value is an angle-bracket value.

**It starts only after the dry run (first handoff) was reported green and after your own direct "publish" message to the orchestrator.** The orchestrator then builds the merge commit `M'` and tells you two SHAs in its own message: `X0`, the SHA pushed to the throwaway branch in the first handoff, and `M'`. Wherever this document writes `${M}` or `${X0}`, use those two values.

## Status

- Nothing was pushed by any agent. Origin `custom_build` reads `9483476322e0d331e81c5b92ac266a18f0f01450` until the push (E-14 `origin_custom_build`), and no `v1.7.0_gg8` tag exists.
- `M'` is built by the orchestrator on a detached worktree at `9483476322e0d331e81c5b92ac266a18f0f01450` with `git merge --no-ff -F` of the committed merge message (`49-MERGE-MESSAGE.txt`) and `X0`; the rehearsal of the same merge (E-14, the frozen script in both modes) had exactly two parents, the tip's tree, the agreed subject and no `.gsd-ws-arg`, and the note's rollback block ran on it and printed `ok`. The orchestrator re-runs that rollback rehearsal on `M'` itself (E-25). The local `custom_build` ref is never moved or checked out; `M'` is reachable through a private ref and is pushed by SHA; it exists only in the local repository that built it, so run the push from that same repository.
- What the dry run (first handoff) showed is recorded by the orchestrator (E-23 and E-24) before this step.
- Not proven by the local gate, as in the first handoff: the two last review fixes no fresh reviewer read, `integrationTest` not re-run, and every GitHub behaviour named as an assumption.

## What you decide

- `M'` is a non-force `--no-ff` merge with parents `9483476322e0d331e81c5b92ac266a18f0f01450` and `${X0}`, the subject `Merge sync/v1.7.0: release v1.7.0_gg8` and the tree of `${X0}`. It differs from `${X0}` as a commit, so the two `tests.yaml` runs do not share a concurrency group.
- Pushing `M'` moves `custom_build`, the default branch, and puts the whole `.planning` tree on it; the history it brings is the one pushed in the first handoff.
- A push starts `tests.yaml` once and starts no release run.
- The CHANGELOG heading of the release carries the push day (the orchestrator's date commit). If the merge slips to another UTC day, pre-check 7 differs and you stop: a second date commit and a new scratch push are needed (contingency C-7).
- Banned options on every push: the force, tags, all and mirror options. A rejected push (not a fast-forward) is a stop.
- Nobody merges a pull request on origin before this push, and no agent comments on any pull request.

## Before you push

Each check has an expected result; on any other result, push nothing and tell the orchestrator. If `custom_build` moved, push nothing and tell the orchestrator (contingency C-1): it merges `origin/custom_build` into `sync/v1.7.0` (never a rebase), checks the landed lines against the anchors, runs the gates again, rebuilds `M'` and asks you whether to absorb or hold. A move voids your approval.

1. `git ls-remote origin refs/heads/custom_build` still prints `9483476322e0d331e81c5b92ac266a18f0f01450`, a tab and `refs/heads/custom_build`.
2. `git cat-file -p "${M}"` shows two `parent` lines, `9483476322e0d331e81c5b92ac266a18f0f01450` and `${X0}`, and the subject `Merge sync/v1.7.0: release v1.7.0_gg8`.
3. `git diff --quiet "${M}" "${X0}" && echo tree-equal` prints `tree-equal`.
4. The same-as-gated, date-only and scripts checks of the first handoff, now for `${X0}`: `git diff --quiet 315f5de1c85fb61bd2a1e887d2be085bc62571ce "${X0}" -- . ':(exclude).planning' ':(exclude)CHANGELOG.md' && echo same-as-gated` prints `same-as-gated`; `git diff --numstat 315f5de1c85fb61bd2a1e887d2be085bc62571ce "${X0}" -- CHANGELOG.md` prints `1`, a TAB, `1`, a TAB and `CHANGELOG.md`; `git diff --quiet d9b10b9bf670b8236119c725e3639c53ad3e570b "${X0}" -- .planning/scripts && echo scripts-same-as-gated` prints `scripts-same-as-gated`.
5. `git ls-remote origin | grep "${M}"` prints nothing.
6. No pull request was merged on origin since the bundle: `gh api 'repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50' --jq 'length'` prints `0` (or the pull requests you know about), and check 1 still holds.
7. `git show "${M}:CHANGELOG.md" | head -1` prints `# 1.7.0_gg8, <the push day>` and `date -u +%F` prints the same date.
8. The rollback rehearsal on `M'` (`bash 49-rehearse.sh at "${M}" <new dir>`, run by the orchestrator, E-25) ended `rehearsal: PASS`.

## Push (U3, word G4)

```sh
git push origin "${M}:refs/heads/custom_build"
```

Set `M` to the value the orchestrator gave you first. That is the only command, and it is never forced. Then wait for the run of `tests.yaml` on `${M}`: exactly one run, with the conclusion `success`, still exactly one two minutes later, and zero `release.yaml` runs on `${M}`:

```sh
gh api "repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=${M}" --jq '[.workflow_runs[] | [.name, .path, .event, .conclusion]]'
```

## Expected result

- The push is accepted as a fast-forward of `custom_build` from `9483476322e0d331e81c5b92ac266a18f0f01450` to `${M}`.
- Exactly one run of `tests.yaml` on `${M}` (event `push`), concluded `success` in six to ten minutes, still one two minutes later; no run of `release.yaml` and no other workflow run is listed for `${M}`; if one is, name it in the report.
- `git ls-remote origin refs/heads/custom_build` prints `${M}`.

## What to confirm and report

Report directly to the orchestrator, in your own message (or let the orchestrator read the same fields with GET calls after running the step on your word):

- `${M}` and `${X0}` as used;
- the output of the push;
- the `tests.yaml` run id, URL and conclusion;
- the run count at once and the count two minutes later, and any other run on `${M}`;
- `git ls-remote origin refs/heads/custom_build` printing `${M}`;
- the two values of pre-check 7.

## If it fails

- A rejected push (not a fast-forward): `custom_build` moved. Push nothing else and tell the orchestrator (contingency C-1).
- A red `tests.yaml` run on `${M}`: do not dispatch the release, and report (contingency C-4). `custom_build` is then already at `${M}`; the way back is the rollback below, forward only, never a force-push, and the orchestrator and you decide.
- Two or more `tests.yaml` runs on `${M}`, or a `release.yaml` run: report them and change nothing.
- Never push a tag from this handoff, and never create or push `v1.7.0_gg8`.

Rollback, if you decide it after this push (the block of the Rollback section of `docs/ROLLOUT-v1.7.0_gg8.md`, which ran unchanged on the rehearsal merge and printed `ok`). Start from the published tip with nothing uncommitted, then:

```sh
git switch -c rollback-v1.7.0_gg8
git read-tree -u --reset pre-v1.7.0_gg8
git commit -m "Roll back v1.7.0_gg8"
git diff --quiet HEAD pre-v1.7.0_gg8 && echo ok
```

The rehearsal on this base showed a clean tree after the block (no ` M gradlew.bat` line); if a fresh worktree lists one anyway, run `git switch` and `git revert` with git's attributes switched off, as `git --attr-source="$(git hash-object -t tree /dev/null)" revert <rollback-commit>`. Publish the rollback commit as a new commit on `custom_build` (step 4 of the note's Rollback section) and redeploy the `v1.7.0_gg7` release asset. It is forward only and never a force-push. The local tag `pre-v1.7.0_gg8` must exist in the clone you use (it is local until handoff U5a pushes it).

## Clean up

None.

## Resume (for the orchestrator)

First record `### E-25: The user's own publish word and the merge commit M', built after it, with its rollback rehearsal`: the user's message verbatim, who received it, `M'`, `X0` and the checks of the runbook (step O2). Then record the push as `### E-26: U3 push of M' to custom_build and its single tests.yaml run`, with the fields above as `key: value` lines and the words `user-reported` when the user reported it, or `orchestrator-run on the user's word: <quoted word>` when you ran it on that word; read the runs for the SHA of `M'` with GET calls and record the reading with it. Add the index rows when the entries exist. Leave E-16 pending and never edit it. A red or not-exactly-one run stops the sequence: no dispatch, and the orchestrator asks the user again. Never act on a relayed approval.
