# Phase 38: Release & Publication - Research

**Researched:** 2026-10-31
**Domain:** fork release engineering: version bump, operator documents, archive allow-list compare, gates, and a user-run publication through `release.yaml`
**Confidence:** HIGH for repository, origin and archive facts (read or measured this session); MEDIUM for GitHub behaviours that no run has exercised yet (the publish step, PR auto-close)

Evidence tags used below: `[VERIFIED: path:lines]` (file opened this session, value quoted verbatim), `[VERIFIED: command]` (command run this session, output given), `[CITED: file]` (a committed planning record, not re-run), `[ASSUMED]` (needs confirmation, listed in the Assumptions Log). Scratch work is in `/tmp/gsd38/` (exports, two archive builds, compare outputs, a candidate allow-list); nothing in the repository was edited except this file.

<user_constraints>
## User Constraints (no CONTEXT.md; `skip_discuss` is true, so the ROADMAP goal and criteria are the locked scope)

### Locked Decisions
- Scope is ROADMAP Phase 38 as written: goal, success criteria 1 to 5, requirements RLS-01..RLS-06. `[CITED: .planning/ROADMAP.md, Phase 38 block]`
- (a) The tag `v1.7.0_gg6` and the GitHub release come from a REAL `release.yaml` `workflow_dispatch` run from `custom_build` that the USER launches after the green `tests.yaml` run on `custom_build`. It also exercises the publish path and closes the ADV-03 caveat. It is a user-gated handoff and no agent ever runs it. (user decision, 2026-10-31)
- (b) Only LOCAL preparation is authorized now. Nothing is pushed, merged on origin, tagged or deleted on origin. No PR action, no `gh workflow run`, no mutating `gh` call by an agent. Every origin-touching step (scratch push, `custom_build` merge and push, tags, release dispatch, scratch deletion) is a user-run handoff with explicit pre-checks, as in the Phase 36 and 37 `USER-DISPATCH` documents. (user decision, 2026-10-31)
- Phase 38 edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file (Phase 29 D-05, restated in the ROADMAP Note). A defect found here goes back to its phase as a gap, or is deferred with a reason, an owner and a re-open trigger in the milestone close record. `[CITED: ROADMAP Phase 38 Note]`
- Project rules: CLAUDE.md and AGENTS.md apply (no commits without being asked, no dependency upgrades without asking, operator documents get an independent review before the push gate).

### Claude's Discretion
- Plan and wave split; wording and structure of the rollout note, CHANGELOG section and FORK-DELTA edits; which evidence helper scripts to copy; scratch branch name; layout of the handoff documents.

### Deferred Ideas (OUT OF SCOPE)
- None were given. Future requirements stay out: FUT-01 (tagged upstream sync; upstream `main` is still `9bed6f79` and its newest tag is `v1.7.0`, read 2026-10-31), FUT-03 (protobuf 4), FUT-04 (retire V1), FUT-05 (retry budget; only its rollout warning is in scope).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RLS-01 | `VERSION=v1.7.0_gg6` changes in the same commit as its anchor row, with CHANGELOG (absorbing upstream `Unreleased`) and `docs/FORK-DELTA.md` updated | Section 2: `bump_version.py gg` measured on a scratch copy; version-agreement check; CHANGELOG and FORK-DELTA edit lists; numbers already measured |
| RLS-02 | `docs/ROLLOUT-v1.7.0_gg6.md` states each operator-visible effect with a "measured" or "read from the code" label | Section 3: the full effect inventory with source pointers and labels, and the label-vocabulary problem (Open Question 1) |
| RLS-03 | The gg6 allow-list is generated last from the final tree and the compare passes against it | Section 4: measured compare, 31-row candidate list that exits 0 with no stale row, regeneration recipe |
| RLS-04 | A freshly spawned reviewer checks the operator documents before the push gate | Section 3.5: review mechanism, scope files, heading rule, round cap |
| RLS-05 | Release dry run passes, divergence checked, `--no-ff` merge after human approval, one green `tests.yaml` run on the published SHA before the tag | Sections 1 and 6: measured origin state, order of operations, user handoffs, what a green dry run proves |
| RLS-06 | The suite passes twice on one pinned `latest` build with client V2, and `testUnit` passes with Docker stopped | Section 5: pinned image digest measured, gate commands, JDK notes |
</phase_requirements>

## Summary

The phase is a rehearsal-and-publish recipe that has run three times (gg3 to gg5), with three differences this time. First, the tag `v1.7.0_gg6` and the release object are created by the workflow, not hand-pushed: `release.yaml` ends its publish step with `gh release create "$RELEASE_VERSION" "$ARCHIVE" --target "$GITHUB_SHA" --title "$RELEASE_VERSION" --notes ""`, which creates the tag server-side at the dispatched SHA. Only `pre-v1.7.0_gg6` is pushed by hand, and `v1.7.0_gg6` must not exist beforehand because the step `Check the release tag is absent` refuses to publish over it. Second, the compare chain from Phase 37 has never run on a GitHub runner, so the scratch-branch dry run is its first exercise and no phase may fix a `.github/` defect it shows. Third, divergence is already zero: `git rev-list --count sync/v1.7.0..origin/custom_build` prints 0 (see below), contrary to the ROADMAP Note.

Everything else is prepared: `VERSION` is `v1.7.0_gg5`; the bump script moves the anchors row in the same write; the carried rollout items are in three handoff files (35, 36 and the Phase 34 and 37 records); the gg6 archive differs from the gg5 archive in exactly 242 entries, all of which one 31-row allow-list names with no overlap and no stale row (measured here on a rebuild of tag `v1.7.0_gg5` against the current tree with `VERSION` set to `v1.7.0_gg6` in a scratch export). The long poles are the independent review rounds on the note (up to three, orchestrator-spawned), the two `--rerun` suites (about 3 minutes each when the JVM does not crash; the host is swapping heavily now), and the user-run origin steps.

**Primary recommendation:** Plan five local waves (version bump commit; documents; reviewer rounds; allow-list generated last plus gates; approval bundle with a dry-run merge and rollback rehearsal), then four user handoff files (scratch push and dry-run dispatch; `custom_build` push by merge SHA; publish dispatch from `custom_build`; tag, scratch deletion and PR clean-up), with every origin write run by the user and recorded verbatim by the orchestrator.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Version bump, CHANGELOG, FORK-DELTA, rollout note | Local repo (agent, `sync/v1.7.0`) | none | Plain files; one commit for `VERSION` plus its anchors row |
| Independent review of operator documents | Orchestrator (spawns `gsd-code-reviewer`) | none | Executors have no Agent tool; an inline review does not count `[CITED: memory gsd-phase15-review-gate-gotchas]` |
| Allow-list generation and compare proof | Local repo (agent, builds in `/tmp`) | GitHub runner (repeats it in the dry run) | The runner rebuilds the baseline from the tag; the local recipe is the same steps |
| Two full suites, `testUnit` Docker-free, anchors gates | Local repo (agent, Docker on colima `vz`) | none | Needs Docker and the pinned `latest` image |
| Scratch push, `custom_build` push, tag push, deletions | Origin refs (USER) | none | Locked decision (b) |
| Release dry run and real publish | GitHub runner (USER dispatches) | Origin (tag and release created there) | Locked decision (a) |
| Merge commit M construction | Local repo, throwaway worktree (orchestrator inline) | Origin (user pushes by SHA) | `custom_build` is GSD's protected base branch; M is built detached and pushed by SHA |
| Approval | User directly to the orchestrator | none | Executors refuse relayed approvals |

## Standard Stack

No library is added and nothing is installed. The phase uses what is already in the repository.

### Core
| Tool | Version | Purpose | Source |
|------|---------|---------|--------|
| `.github/scripts/bump_version.py` | in tree | `VERSION` and the anchors `VERSION` row in one write | `[VERIFIED: .github/scripts/bump_version.py:1-30]` |
| `.github/scripts/baseline_tag.py` | in tree | baseline tag = greatest release tag below `VERSION` | `[VERIFIED: .github/scripts/baseline_tag.py:1-14]` |
| `.github/scripts/compare_confluent_archives.py` | in tree | entry-by-entry archive compare under an allow-list | `[VERIFIED: .github/scripts/compare_confluent_archives.py:1-45]` |
| `.planning/scripts/verify-merge-invariants.sh` | in tree | anchors gate (tail form and cumulative form) | `[VERIFIED: .planning/scripts/verify-merge-invariants.sh:1-30]` |
| Gradle wrapper 9.8.2, JDK 17 toolchain | in tree | `createConfluentArchive`, `testUnit`, `test` | `[VERIFIED: /tmp/gsd38/build-tip.log, "BUILD SUCCESSFUL in 11s"]` |
| Python 3.14.8, `gh` 2.102.0 (authenticated as the repository owner; agents use GET only), git, Docker 27.4.0 on colima `vz` | host | scripts, read-only API reads, suites | `[VERIFIED: commands run 2026-10-31]` |
| Phase 37 helpers `37-evidence-check.py`, `37-hygiene-check.py`, `37-ledger-check.py` | in `.planning/phases/37-ci-hardening-advisory-debt/` | JUnit totals, skips, anchors mirror, secrets and hygiene scans | `[VERIFIED: directory listing]`; copy into `38-` names as Phases 35 to 37 did |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Real `release.yaml` dispatch (locked) | Hand-pushed tags (Phase 29 and 32 recipe) | Rejected by the user: it would leave the publish path unexercised (ADV-03) |
| Scratch name `sync/v1.7.0` | `scratch/<name>` | Either works; any non-`custom_build` branch makes `release.yaml` a dry run. `sync/v1.7.0` follows the Phase 24/29/32 precedent and the merge subject; no `scratch/cih-04-dry-run` exists on origin today |

**Version verification:** not applicable (no package). **Installation:** none.

## Package Legitimacy Audit

No external package is installed or recommended in this phase. The gate does not apply. `zstd-jni` 1.5.7-23 and client 0.11.0 entered earlier (Phase 36) and are documented, not added, here.

## Architecture Patterns

### System Architecture Diagram

```
LOCAL (agent, sync/v1.7.0, nothing pushed)                       ORIGIN / GITHUB (user-run steps only)
------------------------------------------                       --------------------------------------
W1 bump_version.py gg ──► commit {VERSION, fork-anchors row}
W2 CHANGELOG 1.7.0_gg6, FORK-DELTA, ROLLOUT-v1.7.0_gg6.md
W3 orchestrator spawns gsd-code-reviewer (<=3 rounds) ─► fixes ─► REVIEW-DISPOSITION
W4 generate allow-list LAST (build baseline from tag v1.7.0_gg5,
   build tip, compare) ─► exit 0, no STALE-ROW
   gates on tip X0: testUnit (Docker unreachable) | 2x test --rerun
   on one pinned latest | anchors mirror + tail gate + cumulative gate
W5 dry-run merge D (detached) + rollback rehearsal + approval bundle
        │ user says "publish" directly to the orchestrator
        ▼
U1 ────────────────────────────────────────────────────────►  push X0 to scratch branch ──► tests.yaml run #1 (informational, must be green)
U2 ────────────────────────────────────────────────────────►  dispatch release.yaml from scratch (forced dry run)
                                                               build archive, build baseline from tag, COMPARE, draft+delete
U3 (M = merge --no-ff X0 onto 3a3a15a9, built detached) ────►  push M:refs/heads/custom_build (non-force) ──► tests.yaml run on M (exactly 1, green)
U4 ────────────────────────────────────────────────────────►  dispatch release.yaml from custom_build, dry_run NOT ticked (publish)
                                                               compare ─► tests.yaml gate ─► tag-absent gate ─► gh release create v1.7.0_gg6 --target M
U5 ────────────────────────────────────────────────────────►  push tag pre-v1.7.0_gg6 (3a3a15a9); delete scratch branches; PR clean-up
A  read-only GETs: release author, asset uploader, run + steps  ─► record ADV-03 closed (local tail)
```

### Recommended file layout (all under `.planning/phases/38-release-publication/` unless noted)
```
38-COMMAND-EVIDENCE.md          # E-nn entries, as in Phases 35-37
38-REVIEW.md, 38-REVIEW-DISPOSITION.md   # reviewer rounds, heading rule below
38-USER-DISPATCH-*.md           # handoffs U1..U5 (copy the Phase 37 dry-run handoff shape)
38-RELEASE-RECORD.md            # ADV-03 closure, final refs, PR re-read (local tail, written after U4)
38-evidence-check.py, 38-hygiene-check.py   # copies of the Phase 37 helpers
docs/ROLLOUT-v1.7.0_gg6.md      # repo root docs (published)
.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt   # written last
```

### Pattern 1: Publish by merge SHA, not by branch checkout
**What:** Build M in a detached throwaway worktree on `3a3a15a9`, never moving the local `custom_build` ref; the user pushes `git push origin <M>:refs/heads/custom_build`. M has parents `3a3a15a9` and X0, and `tree(M) == tree(X0)`.
**When:** whenever `origin/custom_build` is still `3a3a15a9` (pre-check). If it moved, STOP (OPS-05 contingency below).
**Evidence for the shape:** the gg5 merge `3a3a15a9` has parents `21c51598` and `41e7d546` and subject `Merge sync/v1.7.0: release v1.7.0_gg5` `[VERIFIED: git log -1 3a3a15a9]`. Because `3a3a15a9` is already an ancestor of the tip, `--no-ff` still creates a real merge commit with the tip's tree and a SHA different from X0, which keeps `tests.yaml`'s SHA-keyed concurrency group from collapsing the two runs `[VERIFIED: .github/workflows/tests.yaml:31-33, "group: ${{ github.workflow }}-... || github.sha", "cancel-in-progress: true"]`.

### Pattern 2: Allow-list as a derived artifact (see Section 4)
### Pattern 3: User handoff document shape
Status; What you decide (what the push publishes, with counts); Before you push (numbered pre-checks, each with an expected value; stop on any other); the exact push command; the dispatch (UI wording and CLI); expected result; what to confirm and report (run URL and id, `headSha`, event, branch, conclusion, per-step conclusions, notices, artifact, no draft left); if it fails earlier; clean-up; "Resume (for the orchestrator)" with the evidence-entry rule: record the user's report verbatim with the words `user-reported`, never act on a relayed approval. `[CITED: 37-USER-DISPATCH-DRYRUN.md, whole file]`

### Anti-Patterns to Avoid
- **Hand-pushing `v1.7.0_gg6`.** It makes the release job's tag gate refuse. Push only `pre-v1.7.0_gg6`.
- **Writing the allow-list before the tree is final.** Any later change to `src/`, `build.gradle.kts`, `gradle/` or `README.md` changes the archive (`outer/doc/README.md` is in it), so the list goes stale or fails.
- **Treating a green dry run as proof of the publish step.** The dry run skips `Publish release` (measured on run 40693904004: `Publish release: skipped`).
- **Re-running or re-pushing to turn a red run green** without the user's explicit answer (Phase 32 prohibition, kept).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Version bump with anchors row | `sed` on `VERSION` and the anchors file | `python3 -I .github/scripts/bump_version.py gg` | Stages both writes, refuses a mismatched row, restores on failure `[VERIFIED: bump_version.py docstring]` |
| Baseline tag choice | A hard-coded `v1.7.0_gg5` | `git tag --merged HEAD --list 'v*' \| python3 -I .github/scripts/baseline_tag.py v1.7.0_gg6` printed `v1.7.0_gg5` `[VERIFIED: command]` | Same code the workflow runs |
| Archive compare | `diff`, `unzip` trees | `compare_confluent_archives.py` | Reads in memory, never extracts, escapes log-forging names `[VERIFIED: docstring lines 30-40]` |
| Allow-list entries | `*` or a leading-wildcard row | Anchored literal-prefix globs with one reason each | A row starting with `*`, `?` or `[` is refused (exit 2) `[VERIFIED: compare_confluent_archives.py:149-155]` |
| Anchors gate | Manual grep | `verify-merge-invariants.sh` (tail form and cumulative form) | Per-commit anchor evaluation with SHA-pinned exemptions |
| Evidence scans | Ad hoc greps | Copies of the Phase 37 helpers | Hygiene patterns are assembled from fragments so the helper itself carries no forbidden name |

## Runtime State Inventory

Not a rename or migration phase (a version bump has no runtime state outside the repository and origin refs). The places the version string lives are in Section 2.

## 1. The gg4 and gg5 publication recipes, adapted to gg6, and the current origin state

### What the recipes did (read from the records)

| Step | gg5 (Phase 32) | gg4 (Phase 29) | gg6 adaptation |
|------|----------------|----------------|----------------|
| Rehearsal | Plan 32-06 task 1: dry-run `--no-ff` merge of the tip H onto `PRE=21c51598` in a detached worktree, checks (two parents, `tree(D)==tree(H)`, subject, no `.gsd-ws-arg`, no operator document tells anyone to empty a table, rollback script prints ok), `SELECT version()` recorded, approval bundle of eight items `[CITED: 32-06-PLAN.md]` | same shape, E-70 `[CITED: 29-COMMAND-EVIDENCE.md E-70..E-72]` | Same, with `PRE = 3a3a15a9` |
| Approval | one blocking-human gate, user's own "publish" to the orchestrator, recorded verbatim (E-71) | same | same; plus the user runs each origin step |
| Scratch push | `git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0` at X0, one green `tests.yaml` run | same | user-run (U1) |
| Merge | throwaway worktree on `custom_build`, `git merge --no-ff -F msg X0` | same | M built detached; user pushes by SHA (U3) |
| Push | `git push origin refs/heads/custom_build:refs/heads/custom_build`, non-force; M has exactly one green `tests.yaml` run, still one 120 s later | same | user-run |
| Tags | both tags lightweight, pushed by name after the green run: `v1.7.0_gg5` at M, `pre-v1.7.0_gg5` at `21c51598` | same (`pre-v1.7.0_gg4` = `69cd680b`) | `v1.7.0_gg6` created by the release run (U4); `pre-v1.7.0_gg6` pushed by name (U5) |
| Scratch deletion | `git push origin --delete refs/heads/sync/v1.7.0` | same | user-run |
| Release object | none (D-01 of Phase 32) | none | created by `release.yaml` |

Merge commit shape, gg5: parents `21c51598b06b6f5af58430765d57b1e5f6747c5a` and `41e7d5465e8353abb43a5f56dcf9f697c96b537a`, subject `Merge sync/v1.7.0: release v1.7.0_gg5`, a body that is readable without `.planning/` (version, "src/main identical", rollout note pointer, one line per shipped change, "This merge also publishes the .planning/ history since <pre>", the rollback sentence) and a `Co-Authored-By` trailer as its last line (`git merge -F` adds none) `[VERIFIED: git log -1 --format=%B 3a3a15a9]`. gg4: parents `69cd680b` and `3d847009`, subject `Merge sync/v1.7.0: release v1.7.0_gg4` `[VERIFIED: git log -1 21c51598]`. Use the subject `Merge sync/v1.7.0: release v1.7.0_gg6` and the trailer from the session's attribution reminder.

`pre-v1.7.0_gg6` already exists locally as a lightweight tag on the origin tip `3a3a15a9ae064ffe09c696b8bb87bce23419d463` and is not on origin `[VERIFIED: git rev-parse --verify pre-v1.7.0_gg6; git ls-remote origin tag list]`. It equals `v1.7.0_gg5`. Do not create a local `v1.7.0_gg6`.

### Divergence (RLS-05), measured

```
git ls-remote origin refs/heads/custom_build   -> 3a3a15a9ae064ffe09c696b8bb87bce23419d463
git rev-list --left-right --count sync/v1.7.0...origin/custom_build   -> 234   0
git rev-list --count sync/v1.7.0..origin/custom_build                 -> 0
git merge-base --is-ancestor 3a3a15a9 sync/v1.7.0                     -> true
```
`[VERIFIED: commands, 2026-10-31, HEAD 10e79599]`. **The ROADMAP Note says the left side "is expected to hold the gg4 and gg5 release merges, so its count is not 0". That is stale: `sync/v1.7.0` already contains `3a3a15a9`, so the expected count is 0, and anything above 0 means origin moved.** The 234 is the number of commits M would add (it grows with every Phase 38 commit). 71 of them are not on any origin ref yet (`git rev-list --count HEAD --not --remotes=origin`), the other 163 were published by the user's `scratch/clt-07-*` pushes `[VERIFIED: command]`.

If the count is above 0 at any pre-check: push nothing; merge `origin/custom_build` into `sync/v1.7.0` (never rebase), check for anchored lines in whatever landed (`dependabot-merges-on-custom-build` memory), re-gate; ask the user directly whether to absorb or hold (OPS-05 shape `[CITED: 32-06-PLAN.md "OPS-05 contingency"]`).

### Current origin state (read-only: `git ls-remote origin`, `gh api` GET)

| Item | Value | Tag |
|------|-------|-----|
| `custom_build` | `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, `protected: false` on GitHub (GSD's "protected base branch" is a GSD rule, not a GitHub rule); default branch `custom_build` | `[VERIFIED: ls-remote; gh api repos/ne1r0n/clickhouse-kafka-connect/branches/custom_build]` |
| Other heads | `main` `bec167d8`; six `dependabot/*` branches; `scratch/clt-07-client-0.11.0` `de8c685b`, `scratch/clt-07-prebump-0.9.6` `939e8992` (user-owned); no `sync/v1.7.0`, no `scratch/cih-04-dry-run` | `[VERIFIED: ls-remote]` |
| Tags | `pre-v1.7.0_gg2..gg5`, `v1.7.0_gg1..gg5`, `pre-v1.6.0-sync`, `pre-v1.7.0-sync`, `v1.6.0_gg1`; none for gg6; the local tags `pre-v1.7.0-tail*`, `pre-v1.7.0-deps` are not on origin | `[VERIFIED: ls-remote]` |
| Releases | exactly one: id 426690002, tag `v1.7.0_gg5`, author `ne1r0n`, asset `clickhouse-kafka-connect-v1.7.0_gg5.zip` 12453556 bytes uploaded by `ne1r0n` | `[VERIFIED: gh api releases]` |
| Workflows | `release.yaml` 384641583 active, `pre-release.yaml` active, `increment-version.yml` active, `nightly-integration-tests.yaml` active, `integrationTests.yaml` `disabled_manually`, `tests.yaml` active | `[VERIFIED: gh api actions/workflows]` |
| Repository variables | `total_count: 0`, so `NIGHTLY_ENABLED` is not set; the scheduled nightly runs of 2026-10-30 and 2026-10-31 on `custom_build` ended `skipped` | `[VERIFIED: gh api actions/variables; nightly runs list]` |
| Open PRs | #10 (protobuf-java 4.36.2, head `011bc42b`), #16 (setup-java, `cb3c7718`), #17 (client 0.11.0, `6a53525b`), #18 (okhttp, `cb898dc2`), #19 (lz4-java, `2a806038`), #20 (Gradle wrapper, `8786723b`). PR #4 is closed unmerged. Every head equals the one Phase 33 and 36 recorded | `[VERIFIED: gh api pulls; ls-remote]` |
| Absorbed heads | #16, #18, #19, #20 heads are ancestors of the tip (second parents of `79f56a5e`, `e075b2cc`, `6bf1c43b`, `e4f49dca`). #17 and #10 heads are NOT ancestors | `[VERIFIED: git merge-base --is-ancestor]` |
| Upstream | `main` `9bed6f79`; newest tag `v1.7.0`; issue #777 open; PR #786 closed, not merged | `[VERIFIED: git ls-remote upstream; gh api]` |

### What the user must do about the PRs at publication
- **#16, #18, #19, #20:** their heads become ancestors of `custom_build` when M lands. GitHub normally marks such a PR merged by itself `[ASSUMED, A1]`; if one is still open a few minutes later, closing it is the user's action. Record the observed state, do not act.
- **#17 (client 0.11.0):** superseded by the Phase 36 catalog commit (`clickHouseDriver = "0.11.0"`). Its head is not absorbed, so it stays open; once `custom_build` carries the same value Dependabot should close it as up to date at its next run (monthly) `[CITED: 33-RESEARCH.md, dependabot-core `close_pull_request(reason: :up_to_date)`]`, or the user closes it. No agent comments on it.
- **#10 (protobuf-java 4.x):** declined by the `ignore` entries that reach `custom_build` with M; recorded as closing at publication. The user closes it, confirmed in their own words, or it is recorded as pending Dependabot's next run. The REQUIREMENTS wording of SWP-03 is slightly stricter than the ROADMAP's (flagged by Phase 33, still for the orchestrator to align) `[CITED: 33-03-SUMMARY.md]`.
- **Until M is pushed the user must not merge any of them on origin** (a merge moves `custom_build` and diverges X0).
- After publication the orchestrator re-reads all six PRs read-only (SWP-03) and records the result on the local tail.

## 2. RLS-01: what changes for `VERSION=v1.7.0_gg6`

### The bump (measured on a scratch copy, nothing in the repo touched)
```
cp VERSION /tmp/gsd38/bump/VERSION; cp .planning/scripts/fork-anchors.txt /tmp/gsd38/bump/fork-anchors.txt
python3 -I .github/scripts/bump_version.py gg --version-file /tmp/gsd38/bump/VERSION --anchors-file /tmp/gsd38/bump/fork-anchors.txt
-> v1.7.0_gg6
diff: 41c41  < VERSION<TAB>v1.7.0_gg5<TAB>1<TAB>BLD-07   >  VERSION<TAB>v1.7.0_gg6<TAB>1<TAB>BLD-07
```
`[VERIFIED: commands]`. The real call is `python3 -I .github/scripts/bump_version.py gg` from the repository root, then one commit holding exactly `VERSION` and `.planning/scripts/fork-anchors.txt`. The anchors row is `VERSION\tv1.7.0_gg5\t1\tBLD-07` at line 41 of `fork-anchors.txt` `[VERIFIED: .planning/scripts/fork-anchors.txt:41]`; the file has 201 lines and the mirror helper reported `rows=172` at the Phase 37 gate `[CITED: 37-COMMAND-EVIDENCE E-18]`. The bump leaves the row count at 172. Tests `BumpVersionScriptUnitTest` and `BaselineTagScriptUnitTest` use `v1.7.0_gg5` only as a literal fixture, so they do not read the real `VERSION` and cannot trip `[VERIFIED: grep, src/test/.../BumpVersionScriptUnitTest.java:47, BaselineTagScriptUnitTest.java:41]`.

### Generated version agrees (criterion 1)
`generateVersionFile` writes `Version.java` from `project.version`, which is `file("VERSION").readText().trim()` `[VERIFIED: build.gradle.kts:48, 504-520]`. The generated file is not an input of the task, so a `clean` build is required (FORK-DELTA records this trap). Measured on a scratch export with `VERSION` set to `v1.7.0_gg6`: the generated file reads `public static final String ARTIFACT_VERSION = "v1.7.0_gg6";` `[VERIFIED: /tmp/gsd38/tip/build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java]`, and the compare reports `MANIFEST.MF: differs only in Implementation-Version` against the gg5 archive. The check to run in the repo: `./gradlew clean generateVersionFile` then read the generated file (or `createConfluentArchive` and read the inner manifest).

### Where else the version appears and what trips
| Place | Content | Action |
|-------|---------|--------|
| `VERSION` | `v1.7.0_gg5` | bump |
| `.planning/scripts/fork-anchors.txt:41` | VERSION row | bump (same commit); only anchors row mentioning `VERSION`, `CHANGELOG`, `FORK-DELTA` or `docs/` `[VERIFIED: awk over the file]` |
| `config/archive/manifest.json` | no version literal; the archive's `manifest.json` gets `"version" : "v1.7.0_gg6"` at build time `[VERIFIED: unzip of /tmp/gsd38/tip.zip]` | none |
| `CHANGELOG.md` | heading `# 1.7.0_gg5, 2026-10-26` at line 1 | add `# 1.7.0_gg6, <date>` above it |
| `docs/FORK-DELTA.md` | lines 5, 7, 35, 65, 82-84, 90, 139-173, 242, 256 mention gg5 | refresh (below) |
| `docs/ROLLOUT-v1.7.0_gg5.md` | historical; line 111 says "no release asset exists for any tag" which has been false since the user's manual release of 2026-10-28 | optional one-line correction riding the same merge (Open Question 5) |
| README, other docs | no version string found by grep outside the files above `[VERIFIED: grep]` | none |
| Tests | no guard reads CHANGELOG, FORK-DELTA or the rollout note (Phase 33 research and a grep of `src/test` for `CHANGELOG` found none) | none |
| `.github/workflows/*` | `RELEASE_VERSION` is read from `VERSION` at run time | none |

### CHANGELOG `1.7.0_gg6` section
- Upstream's block is the only `# Unreleased` heading, at lines 346-354 (between the `1.7.0_gg1` section and `# 1.7.0, 2026-09-30`): `additionalRetriableErrorCodes` entry with the issue links `[VERIFIED: CHANGELOG.md:346-354]`. Move that entry verbatim into the gg6 section under `## Improvements` and delete the `# Unreleased` heading, so the tag carries no `Unreleased`. Tell the next syncer in FORK-DELTA that upstream's block was absorbed (Phase 34 record: it was "present, unedited, one heading, for RLS-01 to absorb" `[CITED: 34-COMMAND-EVIDENCE.md E-10]`). Effect on a later upstream sync conflict: `[ASSUMED, A6]`.
- Section skeleton (mirror gg5's headings `Important Changes`, `Known Issues`, `Build and CI`): the intro line names the upstream base `1.7.0` plus untagged tail `9bed6f79` (gg4 and gg5 said `a6f1df83`). Important Changes: buffered offset commit (BUF-07), client 0.11.0 and its operator-visible shifts, the new `additionalRetriableErrorCodes` setting and its bypass of `errors.tolerance`, zstd-jni natives, lz4-java 1.12.1 (security release GHSA-48mj-8f7c-7qfm, archive-verified), MBean `version=` and client name read `v1.7.0_gg6`. Known Issues: F-1..F-3 (Section 3.4). Build and CI: release workflows compare the archive with the previous release under a `VERSION`-keyed allow-list; `increment-version.yml` bumps through the script and no longer writes `vnull` (the gg5 section's "Known Issues" bullet about `vnull` is historical and stays); Dependabot `ignore` for protobuf 4; Gradle 9.8.2, okhttp 5.5.1 (tests only), `setup-java` 6.1.0.
- Heading date: PT-14 from Phase 32 requires the CHANGELOG heading date to equal the UTC date at the merge, or the user's direct answer on the mismatch is recorded `[CITED: 32-06-PLAN.md PT-14]`. Today is 2026-10-31; write the date at the last pre-gate commit and re-check at U3.

### FORK-DELTA final pass ("raw numbers refreshed once")
Numbers already measured on HEAD `10e79599` (scopes contain no file Phase 38 may edit, so doc-only commits cannot move them; `.github`/`src` edits are prohibited):

| Scope | Command | Result |
|-------|---------|--------|
| wider | `git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github` | 331 files, 39665 insertions, 1161 deletions |
| narrower | `git diff --shortstat v1.7.0 HEAD -- src/main` | 40 files, 2989 insertions, 272 deletions |
| gg5 to tip, wider | `git diff --shortstat v1.7.0_gg5 HEAD -- src build.gradle.kts gradle .github` | 79 files, 7939 insertions, 478 deletions |
| gg5 to tip, `src/main` | `git diff --shortstat v1.7.0_gg5 HEAD -- src/main` | 7 files, 112 insertions, 31 deletions |

`[VERIFIED: commands]`. The table rows in FORK-DELTA say `v1.7.0` to `v1.7.0_gg5`; add "after gg6: `v1.7.0` to `v1.7.0_gg6`" rows and the two command lines. The tag does not exist before publication, so measure against the release tip and say so (gg5 did the same on `sync/v1.7.0`). Re-run both commands on the final tip and compare; they must be equal.

Other FORK-DELTA edits to do in the same pass (each found by reading the file):
1. Intro paragraph (line 5) and the "Anyone moving a running connector" paragraph (line 7): add `v1.7.0_gg6` and a pointer to `docs/ROLLOUT-v1.7.0_gg6.md`.
2. "For the next syncer": add the gg6 paragraph (published from `sync/v1.7.0` by a non-force merge into `custom_build`, rollback tag `pre-v1.7.0_gg6` on `3a3a15a9`, tail-2 sync, client 0.11.0, BUF fix fork-only until upstream #777 resolves, tag and release created by `release.yaml`); add `MERGE-CHECKLIST-v1.7.0-tail-2.md` to the checklist reading-order sentence and the `Utils.java` import-order row `[CITED: 34-COMMAND-EVIDENCE.md E-10 note]`.
3. Open item 8 (line 208): it still names client `0.10.0` as a future requirement; shipped client is 0.11.0 `[CITED: 36-ROLLOUT-ITEMS.md Handoffs]`.
4. Forward references that go stale: line 73 ("Phase 38 re-reads it and the user closes it"), line 216 ("recorded as closing once the `ignore` reaches `custom_build` at the `v1.7.0_gg6` publication"), line 90 (the open PR list "when Phase 33 started"), and the 13 citations of `33-COMMAND-EVIDENCE.md`-style files. The Phase 33 review (IN) asked that forward references become outcomes or conditions `[CITED: 33-REVIEW.md:96-97]`. State PR #10's outcome as a condition, because the outcome is unknown when the merge is made.
5. Open items for the new carried findings (Section 3.4) with owner and re-open trigger.
6. The Dependabot row sentence "no `github-actions` pull request observed" is stale (PR #16 exists) `[CITED: 32-VERIFICATION.md "Anti-Patterns Found"]`.

## 3. RLS-02 and RLS-04: operator-visible effects for `docs/ROLLOUT-v1.7.0_gg6.md`

### 3.0 Label vocabulary problem (Open Question 1)
RLS-02 and the ROADMAP name two labels, "measured" and "read from the code". The three handoff files use more: Phase 35 adds `derived from Kafka consumer-group semantics (not measured)` `[CITED: 35-ROLLOUT-ITEM.md lines 7-13]`; Phase 36 adds `measured in research (not re-run)` for items 2 (cadence), 5 (tail), 6, 7 (rejection), 8, 9 (worker behaviour) `[CITED: 36-ROLLOUT-ITEMS.md lines 7-13 and table]`. Writing those rows as "measured" would overclaim, and Phase 15, 24 and 32 each paid for overclaims found after publication. Recommendation: keep the honest third and fourth labels, define all four in the note's preamble, and make the reviewer check every label against its evidence. Alternative: re-measure the "in research" rows locally (needs throwaway clones of both client lines and a worker image for the zstd rows; large). The user decides.

### 3.1 Inventory (source files are the handoffs; evidence ids are theirs)

Sources: `S35` = `.planning/phases/35-.../35-ROLLOUT-ITEM.md`; `S36` = `36-.../36-ROLLOUT-ITEMS.md`; `S34` = `34-.../34-03-SUMMARY.md` and `34-COMMAND-EVIDENCE.md` E-09, E-10; `S37` = `37-.../37-RELEASE-RECORD.md`.

| # | Effect (operator-visible) | Label | Pointer |
|---|---------------------------|-------|---------|
| B1 | With `bufferCount > 0` and a topic-changing SMT, offsets are now committed under the original topic and partition at last flushed offset + 1 (before: keyed by the renamed topic, so nothing was committed) | measured | S35 "What changes"; E-02, E-05 |
| B2 | Connect commits only offsets of assigned partitions, so the old keys committed nothing | derived (not measured) | S35 |
| B3 | First start after upgrade resumes at the group's existing committed offset, else at `auto.offset.reset`; with `earliest` and no committed offset the retained topic is reread once (full duplicate load for tables without deduplication); later restarts and rebalances stop rereading | derived (not measured) | S35 "First start after the upgrade"; E-08 |
| B4 | A group empty longer than the broker's `offsets.retention.minutes` loses even a stale committed offset | derived | S35 |
| B5 | Not affected: unbuffered connectors, buffered connectors without renamed topics, `bufferCount`/`bufferFlushTime` contract; table routing follows the renamed topic; revoked partitions drop buffered records | measured (controls in E-02, E-05); unbuffered path also read from the code (`DirectDeliveryStrategy.preCommit`) | S35 |
| B6 | Minimum Connect runtime: `SinkRecord.originalTopic()`, `originalKafkaPartition()`, `originalKafkaOffset()` exist in `connect-api-7.9.7-ccs.jar` and in connect-api 4.1.1 and 4.4.0; an older runtime fails with `NoSuchMethodError` (exactly-once on first `put`, at-least-once as a redelivered duplicate) | presence measured (E-03 `javap`, BLD-16 re-bless); failure modes derived from the code | S35 "Minimum Connect runtime" |
| B7 | Fork-only: upstream issue #777 open, PR #786 closed unmerged. Re-read today: #777 `open`, #786 closed and `merged: false` | measured (re-read 2026-10-31, `[VERIFIED: gh api]`); re-read again at publication | S35; FORK-DELTA row |
| B8 | Not fixed, documented limits: Direct path with `reportInsertedOffsets=true` under a rename commits nothing (OI-35-1); many-to-one rename shares one state record and can drop or reject records (OI-35-2); exactly-once buffering is claimed only for an injective rename (OI-35-3); `onPartitionRemoved` keying (review IN-04) | measured (OI-35-1, -2: E-06 P1..P7); OI-35-3 and IN-04 read from the code | `35-BUF03-RECORD.md` |
| C1 | New setting `additionalRetriableErrorCodes` (`ADDITIONAL_RETRIABLE_ERROR_CODES = "additionalRetriableErrorCodes"`), default empty; parsing trims, de-duplicates, rejects non-integers and values <= 0; unset changes nothing; the built-in set holds 15 codes including 159 | measured (Docker-free pins, `AdditionalRetriableErrorCodesPinUnitTest`, E-09) | `[VERIFIED: ClickHouseSinkConfig.java:45]`; S34 |
| C2 | A code listed there raises `RetriableException`, which Connect redelivers with no retry budget and outside `errors.tolerance` and the DLQ; only transient codes belong there (FUT-05) | read from the code / upstream's own description | S34 E-10; ROADMAP Phase 34 Note |
| C3 | Two upstream behaviours are not asserted by any test and are retriable today: a `DataException` wrapping a retriable server error, and one whose root cause is a socket timeout or unknown host under `errors.tolerance=all`. A user decision on changing them is still pending | read from the code (E-09 observation) | `34-03-SUMMARY.md` "Interpretation flag" |
| D1 | `ssl` and `sslmode` in `jdbcConnectionProperties` never changed V2's transport (V2 TLS follows the `https` scheme that `ssl` selects); client 0.9.6 only warned; client 0.11.0 fails at start without the fix with `Unknown and unmapped config properties: [ssl, sslmode]`; with the fix both are ignored with a WARN | measured | S36 item 1; E-03, E-05, E-06. Code: `ClickHouseHelperClient.java:200-203` `clientBuilder.setOption(IGNORE_UNKNOWN_CONFIG_KEY, "true");` `[VERIFIED]` |
| D2 | Error 159 stays a `RetriableException`, but the client no longer retries it itself: 4 attempts on 0.9.6, 1 on 0.11.0; about 4025 ms vs 1006 ms against a 1 s `max_execution_time` | attempts measured; cadence measured in research (not re-run) | S36 item 2 |
| D3 | 502/503 from a proxy: 0.9.6 `ClientException` / code-0 `ServerException`, one attempt, never retriable; 0.11.0 `java.net.ConnectException` after 4 attempts, retriable only with `retryOnSocketException=true` (default `false`, `[VERIFIED: ClickHouseSinkConfig.java:284]`) | measured | S36 item 3 |
| D4 | Unexpected status (418): code-0 `ServerException` on 0.9.6, `ClientException` "Unexpected result status 418" on 0.11.0; non-retriable on both | measured | S36 item 4 |
| D5 | `User-Agent` becomes `clickhouse-kafka-connect/<VERSION> clickhouse-java-v2/0.11.0 (lv:java/<jvm>; os:<os>; hc:ahc_<version>)` (was `... clickhouse-java-v2/0.9.6 (<os>; jvm:<jvm>) Apache-HttpClient/<version>`) | prefix measured; tail measured in research (not re-run) | S36 item 5 |
| D6 | Log text changes (`Insert failed (Attempt: ...)` becomes `Insert request failed (attempt: ..., duration: ..., queryId: <id>)`; ` (queryId= <id>)` appended to server messages; non-ClickHouse HTTP error text) | measured in research (not re-run); not pinned | S36 item 6 |
| D7 | `custom_*` keys in `jdbcConnectionProperties` are forwarded to the server on V2 by 0.11.0 with the percent-encoding doubled; the server rejects them with `Code: 115 ... UNKNOWN_SETTING` (not retriable, task fails); 0.9.6 ignored them. Search every `jdbcConnectionProperties` for `custom_` | forwarding measured (E-11 stub probe on both lines); rejection measured in research (not re-run) | S36 item 7 and "custom_ keys" section |
| D8 | `decompress=true`: LZ4 blocks on 0.9.6, ZSTD on 0.11.0 and a 1,000,000-byte incompressible body fails client-side `Destination buffer is too small`; the connector never sets `decompress` | measured in research (not re-run) | S36 item 8 |
| D9 | `zstd-jni` 1.5.7-23 (`cloud` classifier) is new in the archive with natives for linux/amd64, linux/aarch64, darwin/aarch64 only; other platforms fail with `UnsatisfiedLinkError: Unsupported OS/arch`; `noexec` `/tmp` fails with `failed to map segment from shared object` unless `-DZstdTempFolder=<dir>` or `-Djava.io.tmpdir=<dir>`; the default configuration never loads the native | archive and natives measured (E-09); worker behaviour measured in research (not re-run); loader behaviour read from the library | S36 item 9 and "zstd-jni" section |
| D10 | The client no longer sets 804800-byte socket buffers; `socket_rcvbuf` and `socket_sndbuf` stay accepted through `jdbcConnectionProperties`; throughput effect not measured | read from the code | S36 item 10 |
| D11 | Exactly-once replay across the upgrade: a `BEFORE_PROCESSING` batch is byte-identical on the wire for the 48-case matrix (both variants), same `insert_deduplication_token`, only the User-Agent differs; **no clean stop is required** for that matrix; a type outside it was not compared. The golden was blessed on 0.9.6 and is green on 0.11.0, so the bytes also match in the rollback direction (the direction itself is a logical consequence, not a separate measurement `[ASSUMED, A8]`) | golden measured (E-04, E-08); token read from the code (`QueryIdentifier.getDeduplicationToken`) | S36 item 11 and replay section |
| D12 | Archive grows: 7801 inner entries (gg5) to 7933 (gg6), +132 (+1 from the tail's new `ErrorCodeListValidator`, +131 from the bump); 7399 to 7513 files in the compare; 12453556 to 13558634 bytes in the local rebuild; guava override redundant on 0.11.0, httpclient5 and lz4 pins still win | measured (this session, `/tmp/gsd38`; E-09 for the bump alone) | S36 item 12; Section 4 |
| D13 | D-17 default stays (`setCompressionMethod` true, `compress=false`): V1 cannot decode ZSTD on either line, V2 of 0.11.0 can with `setCompressionMethod=false` and `compress=true` | measured | S36 item 13 |
| D14 | `Read timed out after` / `Write timed out after` prefixes unchanged and still retriable | measured | S36 item 14 |
| E1 | lz4-java `1.12.0` to `1.12.1` (GHSA-48mj-8f7c-7qfm) is in the shipped archive | measured: compare prints `VERSION at.yawk.lz4:lz4-java 1.12.0 -> 1.12.1` `[VERIFIED: /tmp/gsd38/cmp-empty.out]`; Phase 33 `dependencyInsight` | `33-` records; FORK-DELTA lz4 row |
| E2 | okhttp 5.5.1 (integration tests only), Gradle 9.8.2 (build only), `setup-java` 6.1.0 (CI only); protobuf-java stays 3.x | read from the code (catalog and workflow pins) | Phase 33 records |
| E3 | Release identity: JMX `version=` segment and the client name read `v1.7.0_gg6` (`version=v1.7.0_gg6`, `clickhouse-kafka-connect/v1.7.0_gg6`); update matchers that spell the old value | read from the code (`Version.ARTIFACT_VERSION` inlined into `ClickHouseHelperClient` and `SinkTaskStatistics`; both appear as CHANGED in the compare); gg5's note measured it with a probe, repeat or relabel | `docs/ROLLOUT-v1.7.0_gg5.md` section 3 for the shape |
| E4 | Rollback: tag `pre-v1.7.0_gg6` = `3a3a15a9` = tag `v1.7.0_gg5`; redeploy the gg5 archive (a release asset exists since 2026-10-28, SHA-256 `4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767`, uploaded by the user; a rebuild from the tag has the same size but a different digest and compares PASS) `[CITED: 37-RELEASE-RECORD.md]`; rolling back reverses B1 (offsets stop being committed again) and D1..D10 | measured (rebuild, compare); reversal derived | rollback section of the gg5 note for the command shape |

The note must contain no host, connector, cluster or production-system name; every environment-specific value is an angle-bracket value (as `35-ROLLOUT-ITEM.md` and `ROLLOUT-v1.7.0_gg5.md` do).

### 3.2 Items that need a decision before the note can say them
- **`custom_` restore (D7).** One more client option set before the property loop in `createClientV2` would restore 0.9.6 behaviour; Phase 36 left it as a user decision and did not add it `[CITED: 36-ROLLOUT-ITEMS.md "custom_ keys"]`. Phase 38 cannot edit `src/main`. Either the note ships D7 as an upgrade-breaking item (recommended: it is the one operator-breaking effect and it is documented with a search recipe), or the user opens a gap phase first.
- **gg5 note line 111 correction** (Open Question 5).

### 3.3 What the note states about the new release object
Do not state facts about `v1.7.0_gg6`'s release (asset digest, tag SHA) in a document that is published before the release exists. State what `release.yaml` does ("creates the release and its tag at the merged SHA and attaches the archive") and keep the measured release facts in a local-tail record (`38-RELEASE-RECORD.md`) after U4. Cost of getting this wrong: a docs-only republish, as in quick task 261020-vcr `[CITED: memory gsd-phase15-review-gate-gotchas]`.

### 3.4 Carried open items (for FORK-DELTA "Known costs and open items", the CHANGELOG Known Issues, and the milestone close record)

| Item | Disposition now | Owner and trigger | Source |
|------|-----------------|-------------------|--------|
| F-1 `ClickHouseWriter.stop()` never closes its `ClickHouseHelperClient` (one V2 client leaked per task start) | carried, not fixed; both client lines | user; growing connections on workers that restart tasks often, or next edit of `stop` | `36-FOLLOWUPS.md`; `37-RELEASE-RECORD.md` table |
| F-2 4-minute insert timeout has no effect with `async=false`; `socket_timeout` 0 | carried | user; insert blocking on a stalled server | same |
| F-3 harness Datagen completion counting spans all connectors outside `generateData` | carried | user; nightly red traced to it | same |
| UF-36-1 two `scratch/clt-07-*` branches on a public origin (published `.planning` tail) | user deletes when ready; deleting does not unpublish | user | `36-SECURITY.md:95` |
| UF-36-4 no `gradle/verification-metadata.xml`; no `zstd-jni` row in the compare VERSION table | not added; natives are caught as UNALLOWED | user; any native-bearing bump | `37-RELEASE-RECORD.md` |
| UF-36-5, 36 review IN-02..IN-06 | open, test hygiene only | user | `36-SECURITY.md:99,108` |
| OI-35-1, OI-35-2, OI-35-3, 35 IN-04 | open, user decision (state key change stops for the user) | milestone close | `35-BUF03-RECORD.md:29-48` |
| 34 DataException wrapped cases (C3) and FUT-05 | open, behaviour change is the user's call | user | `34-03-SUMMARY.md` |
| 37: `increment-version.yml` header names the wrong guard class | open; Phase 38 cannot edit `.github/` | user; next edit of that workflow | `37-RELEASE-RECORD.md` "Open item" |
| 37 ledger rows 26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01 (`src/main`) and 28 IN-05(b) | dispositioned, need the user's approval if wanted | user | `37-ADVISORY-LEDGER.md:7` |
| 30 IN-04 (gate accepts a green run of any branch on the SHA) | owner is Phase 38: record that M's only `tests.yaml` run is the `custom_build` push | Phase 38 | `37-ADVISORY-LEDGER.md:19` |
| ADV-03 caveat "no run exercised the publish path" | closes only with the U4 run and its record | Phase 38 | `37-RELEASE-RECORD.md` Handoffs 4 |
| SWP-03 PR #10 and PR #17 states | re-read after U3 | Phase 38 | ROADMAP Phase 38 Note |
| Phase 36 CLT-07: one green nightly (run 41152289179, scratch `de8c685b`) | accepted by the user; see Section 5.3 | n/a | `36-VERIFICATION.md` disposition |

### 3.5 RLS-04: the independent review
- **Who:** the orchestrator spawns a fresh-context `gsd-code-reviewer` through its Agent tool before the push gate. An executor's inline reading does not count (recurred in Phases 15 and 24) `[CITED: memory gsd-phase15-review-gate-gotchas; 32-04-PLAN.md truths]`.
- **Scope (round 1 brief names these):** `docs/ROLLOUT-v1.7.0_gg6.md`; the `1.7.0_gg6` section of `CHANGELOG.md`; `docs/FORK-DELTA.md`; `docs/ROLLOUT-v1.7.0_gg1.md` (Phase 36 edited its item 6 wording: `git diff 3a3a15a9 HEAD -- docs/ROLLOUT-v1.7.0_gg1.md` shows 4 changed lines `[VERIFIED: command]`) and `docs/ROLLOUT-v1.7.0_gg5.md` if line 111 is corrected; diff base `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; `38-COMMAND-EVIDENCE.md` as the evidence map; the three handoff files as the completeness checklist (the reviewer must check that every row of `S35`, `S36` and the `S34` FUT-05 item appears in the note with the same label strength, and that no row claims more than its evidence).
- **Rounds:** repeat until one finds nothing, at most three unless the user authorises another; reviewer-proposed fix text is itself checked against the files and evidence before it is applied (the Phase 24 WR-10 fix text was wrong); the next round reviews the fix commits; finding IDs continue per prefix and are never reused `[CITED: 32-04-PLAN.md truths]`.
- **Heading and count rules (a recurring failure):** every finding sits under `### <CR|BL|WR|IN>-NN: <title>`; each round ends with `round: <n> findings: <k>`; frontmatter totals equal the number of finding headings; when a reviewer appends, spell the rule out in the brief, or the disposition step silently records fewer rows `[CITED: memory gsd-phase15-review-gate-gotchas, gsd-code-review-fix-gotchas]`. Dispositions go to `38-REVIEW-DISPOSITION.md` through the `code-review-disposition.md` step extracted to a script and run with `bash`.
- **Round 3 outcome precedent:** Phase 32 hit the cap with two Info findings; the user chose "fix, accept unreviewed" and the verifier recorded an override `[CITED: 32-VERIFICATION.md frontmatter]`. Expect to ask the same question if round 3 is not empty.
- **Hygiene:** also run the hygiene helper over the three documents and over every added or modified text file since `3a3a15a9` before the push gate (Section 7).

## 4. RLS-03: generating the gg6 allow-list last

### Measured result (this session)
Both archives were built exactly as `release.yaml` builds them: the baseline from an export of tag `v1.7.0_gg5` with the tag's own wrapper, the candidate from an export of HEAD with `VERSION` set to `v1.7.0_gg6` in the scratch copy.

| Archive | Gradle | verify line | size | SHA-256 |
|---------|--------|-------------|------|---------|
| baseline gg5 (`/tmp/gsd38/base.zip`) | 9.8.1 | `verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class` | 12453556 | `efa6cca4f30d182eea52565f331581f2c4ccee30c99e91ce0f053b341629e0fe` (equals the Phase 37 rebuild; differs from the published asset `4c69b6fb...`, and compares PASS against it per Phase 37) |
| candidate gg6 (`/tmp/gsd38/tip.zip`) | 9.8.2 | `verifyConfluentArchive: 7933 inner entries, no nested jar, no Kafka API class` | 13558634 | `a1b9108859fc73b84ad6ac6931c387bd10800dde9fec8b6a488e8bb5b402f1cf` |

`[VERIFIED: /tmp/gsd38/build-base.log, build-tip.log, shasum]`. Compare with a comment-only list: exit 1, `totals files-a 7399 files-b 7513 only-a 4 only-b 118 changed 120 same 7275`, 242 `UNALLOWED` lines (the 238 of Phase 37's E-07 run B plus the 4 release-identity rows that the gg5 list had allowed), `nested jars in b: 0`, `kafka classes in b: 0`, and `MANIFEST.MF: differs only in Implementation-Version`. The `VERSION` table changes in exactly one line: `at.yawk.lz4:lz4-java 1.12.0 -> 1.12.1` `[VERIFIED: /tmp/gsd38/cmp-empty.out]`.

### Expected content: the 242 differences by owner
| Owner | Entries | Notes |
|-------|---------|-------|
| client-v2 and clickhouse-client 0.9.6 to 0.11.0, classes under `com/clickhouse/client/` | 143 (80 CHANGED, 60 ONLY-B, 3 ONLY-A); of these 129 are `com/clickhouse/client/api/`, 14 are V1 `ClickHouseDnsResolver`, `ClickHouseRequest`, `ClickHouseRequestManager` (+ inner), `config/`, `http/` | one prefix row, or split into `.../client/api/*`, `.../client/config/*`, `.../client/http/*` and `com/clickhouse/client/ClickHouse*` for reviewability |
| `com/clickhouse/data/*` | 10 CHANGED | clickhouse-data |
| `META-INF/versions/11/com/clickhouse/client/http/*` (2) and `META-INF/versions/11/module-info.class` (1) | 3 | the descriptor's owner is `clickhouse-http-client-0.11.0.jar` (sha256 match in the Gradle cache `[VERIFIED: /tmp/gsd38/findjar.py]`) |
| `clickhouse-client-version.properties` (CHANGED), `client-v2-version.properties` (ONLY-A) | 2 | exact rows |
| zstd-jni 1.5.7-23: `com/github/luben/zstd/*` 37, `META-INF/versions/22/com/github/luben/zstd/*` 17, three natives `darwin/aarch64/libzstd-jni-1.5.7-23.dylib`, `linux/aarch64/libzstd-jni-1.5.7-23.so`, `linux/amd64/libzstd-jni-1.5.7-23.so` | 57 | all ONLY-B; exact rows for the natives |
| lz4-java 1.12.0 to 1.12.1: `net/jpountz/lz4/*` 7, `META-INF/maven/at.yawk.lz4/lz4-java/*` 2 | 9 | CHANGED |
| connector classes under `com/clickhouse/kafka/connect/`, exact paths | 17 | `AtLeastOnceBufferStrategy`, `ChunkFlusher`, `ExactlyOnceBufferStrategy` (BUF); `ClickHouseSinkConfig` and 7 inner classes (one ONLY-B: `$ErrorCodeListValidator`; the others change through the `InnerClasses` attribute); `ProxySinkTask`, `Utils` (TAL); `ClickHouseHelperClient` and `$ClickHouseClientBuilder` (client fixes and identity); `Version`, `SinkTaskStatistics` (identity) |
| `META-INF/MANIFEST.MF` | 1 | `Implementation-Version` only |

The seven changed `src/main` files since gg5 map to those classes: `AtLeastOnceBufferStrategy`, `ChunkFlusher`, `ClickHouseSinkConfig`, `ExactlyOnceBufferStrategy`, `ProxySinkTask`, `ClickHouseHelperClient`, `Utils` `[VERIFIED: git diff --name-only v1.7.0_gg5 HEAD -- src/main]`.

### Candidate list (31 rows, glob then reason; the real file needs a header like the gg4 and gg5 lists)
Proved: `python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd38/base.zip /tmp/gsd38/tip.zip /tmp/gsd38/allow-gg6.txt` exits 0, `RESULT: PASS`, zero `UNALLOWED`, zero `STALE-ROW`; every one of the 242 differences matches exactly one row (no overlap) `[VERIFIED: /tmp/gsd38/cmp-gg6.out]`. Rows (reasons shortened here; the file in `/tmp/gsd38/allow-gg6.txt` has them in full):

```
com/clickhouse/client/*                                   clickhouse-client and client-v2 0.9.6 -> 0.11.0
com/clickhouse/data/*                                     clickhouse-data 0.9.6 -> 0.11.0
META-INF/versions/11/com/clickhouse/client/http/*         clickhouse-http-client multi-release classes
META-INF/versions/11/module-info.class                    clickhouse-http-client module descriptor
clickhouse-client-version.properties                      0.9.6 -> 0.11.0
client-v2-version.properties                              shipped by 0.9.6, not by 0.11.0
com/github/luben/zstd/*                                   zstd-jni 1.5.7-23 (new)
META-INF/versions/22/com/github/luben/zstd/*              zstd-jni 1.5.7-23 (new, multi-release)
darwin/aarch64/libzstd-jni-1.5.7-23.dylib                 native (new)
linux/aarch64/libzstd-jni-1.5.7-23.so                     native (new)
linux/amd64/libzstd-jni-1.5.7-23.so                       native (new)
net/jpountz/lz4/*                                         lz4-java 1.12.0 -> 1.12.1
META-INF/maven/at.yawk.lz4/lz4-java/*                     lz4-java pom files
17 exact com/clickhouse/kafka/connect/... .class rows      one reason each (BUF / TAL / client fixes / identity)
META-INF/MANIFEST.MF                                      Implementation-Version v1.7.0_gg5 -> v1.7.0_gg6
```

Rules the list must meet (all `[VERIFIED: compare_confluent_archives.py:149-155 and CompareConfluentArchivesScriptUnitTest.java:511-526]`): `glob<TAB>reason`, a non-empty reason, not only stars, the first character not `*`, `?` or `[` (WR-02 glob rule), at least one row (the shipped-lists test requires every list to load and to report `STALE-ROW` against identical synthetic archives), file name `confluent-archive-allowlist-v1.7.0_gg6.txt` under `.planning/scripts/`, because `release.yaml` builds the path as `allowlist=".planning/scripts/confluent-archive-allowlist-${RELEASE_VERSION}.txt"` `[VERIFIED: .github/workflows/release.yaml:97]` and fails if it is missing. Convention from gg4: third-party rows may be globs; a `com/clickhouse/kafka/connect/` row is an exact path whose source changed, or the release identity `[VERIFIED: allowlist-v1.7.0_gg4.txt header]`. Never copy the gg5 list: it names four identity rows and nothing else.

### Regeneration recipe (agent-doable locally, no Docker, no origin)
1. On the final tip (all non-`.planning` edits committed): export the tip and tag `v1.7.0_gg5` with `git archive <ref> | tar -x -C /tmp/gsd38/<name>` (or `git worktree add --detach` in `/tmp`); the tip's `VERSION` is already `v1.7.0_gg6` by then.
2. Build each with its own wrapper: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew createConfluentArchive --console=plain` (about 12 s each when the wrapper is cached); the line `verifyConfluentArchive: <n> inner entries ...` must appear. Copy `build/confluent/clickhouse-kafka-connect-<VERSION>.zip` out.
3. Run the compare with a comment-only list to get the full UNALLOWED set; derive rows from it (group by owner as above); write the file; re-run: exit 0, `RESULT: PASS`, no `STALE-ROW`.
4. Stale-row check: append one row that names nothing and expect exit 1 with `STALE-ROW <glob>`; delete one real row and expect `UNALLOWED` lines (the gg5 handoff did the same, run C of E-07 `[CITED: 37-COMMAND-EVIDENCE.md E-07]`).
5. Mutation guard on the final file: `grep -c` rows, and check that no row starts with `*`, `?` or `[`.
6. Commit the file as the LAST non-gate commit; then run the gates. Any later edit of `src/`, `build.gradle.kts`, `gradle/` or `README.md` (the archive carries `doc/README.md` and `doc/LICENSE`, `manifest.json` is generated from `config/archive/manifest.json` `[VERIFIED: unzip -l of /tmp/gsd38/tip.zip; build.gradle.kts:424]`) invalidates it.

Reproducibility note: the local gg5 rebuild digest equals Phase 37's, and the runner's gg5 archive differed from the published asset in size by 105 bytes (12453661 vs 12453556, `[CITED: 37-RELEASE-RECORD.md]`), so never compare a runner archive's digest with a local one; the compare tool, not a digest, is the check.

## 5. RLS-06: gates

### 5.1 Pinned `latest`
- Local image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, created 2026-10-26T00:06:54Z; Docker Hub's `latest` index digest read today is the same value, last updated 2026-10-26T00:08:26Z `[VERIFIED: docker image inspect; curl of hub.docker.com tags/latest]`. It is the digest of Phases 34 to 37 (server `26.10.3.7`).
- "Pinned" means: do not `docker pull` between or during the runs; record `docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest` before run 1 and after run 2 (equal), and `docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'` at each gate (`26.10.3.7` expected). That Testcontainers does not re-pull a present local `latest` is `[ASSUMED, A4]`; the two digest reads are what proves it.
- If Docker Hub's `latest` moves before U1, the runner's `tests.yaml` will test a different build than the local pin. That is not a reason to re-pin: record both digests (the runner's is not recorded by any log today).

### 5.2 Commands (Phase 36 E-12 and Phase 37 E-18 forms, `[CITED]`)
```
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd38/exec/gate-testunit.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd38/exec/suite1.log 2>&1   # run twice, rm -rf build/test-results/test between
python3 -I <38-evidence-check.py> junit|skips|failed|anchors ...
python3 -I <38-evidence-check.py> anchors .planning/scripts/fork-anchors.txt                      # expect rows=172 bad=0
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 <tip>   # tail gate
bash .planning/scripts/verify-merge-invariants.sh 54e63648 <tip>                                            # cumulative gate, about 17-23 min, run in background
```
`[CITED: 36-COMMAND-EVIDENCE.md E-12 and 37-COMMAND-EVIDENCE.md E-18 command blocks]`. The Docker-free run uses an unreachable `DOCKER_HOST`, the measured Phase 37 way to run `testUnit` "with Docker stopped" without stopping colima; stopping colima (`colima stop -p vz`, not the bare default profile `[CITED: memory colima-vz-profile]`) is the stricter alternative.

Expected values to check against (derived from the last gates, `[ASSUMED, A5]` until measured): `testUnit` 1672 tests, 0 failures, 2 skipped (the Phase 37 verifier's re-run at `a165f8f6` `[CITED: 37-VERIFICATION.md:111,164]`; plus nothing from Phase 38 since it adds no test); full suite = 1672 + 598 Docker-only = 2270 tests with 18 skips (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`; the 598 delta held in Phases 36 and 37). Anchors mirror `rows=172 bad=0`; the tail gate ended `anchor_rows=172 ... result=PASS` at Phase 37; the cumulative gate form is the Phase 29 D-01 / Phase 36 form with exemptions.

### 5.3 JDK and crash notes
- Two suites on one JVM line. Phase 36's suite 2 crashed twice on the default JVM (exit 255, OpenJ9 Mockito inline-mocking fault, `javacore` stack in `retransformClasses0`) and went green on `-PjavaVersion=21`, then was re-run on the default JDK 17 in `36-JDK17-RERUN.md` (2072 tests, green, same digest) `[CITED]`. The property exists: `val javaVersion: Int = (project.findProperty("javaVersion") as String? ?: defaultJdkVersion.toString()).toInt()` `[VERIFIED: build.gradle.kts:234]`.
- Today the host swap is 14.7 of 16 GB used (`vm.swapusage`) `[VERIFIED: sysctl]`, the condition under which the crash recurred. Run `./gradlew --stop`, close heavy processes, check swap before each suite. On exit 255 read the stack from `javacore.*.txt`, delete the core dump (about 8 GB) and `javacore`, `Snap`, `jitdump` files by name (never `git clean`), re-run once, and record both runs honestly `[CITED: memory openj9-mockito-test-jvm-crash]`. Preferred record: both runs on the default JDK 17; if one needs `-PjavaVersion=21`, run both on 21 and say so.
- Use `--rerun` on every gate run (a bare second run echoes the first run's results) `[CITED: STATE.md decisions]`. Do not run `spotlessApply` or `spotlessCheck` as a gate: 46 violations pre-exist in the tree and Phase 38 edits no Java `[CITED: memory gsd-code-review-fix-gotchas]`.
- The gradle `gradlew.bat` CRLF line shows as an unstaged diff in fresh worktrees; every clean check excludes it, as does the dirty `.planning/config.json` and `state.json` pair `[CITED: STATE.md, git status]`.

### 5.4 Coverage of CLT-07 and of "tests not re-gated" at the release tip
- **CLT-07 nightly:** one green run on client 0.11.0, run 41152289179, `workflow_dispatch`, scratch SHA `de8c685b`, 63 tests, 0 failures, 9 skipped (cloud tests), job 33m41s `[CITED: 36-COMMAND-EVIDENCE.md E-17]`. `de8c685b` is an ancestor of HEAD, and `git diff de8c685b HEAD -- src/main build.gradle.kts gradle VERSION` is empty `[VERIFIED: command]`. The 26 non-`.planning` commits since touch only `.github` (workflows, scripts, `dependabot.yml`), CI guard tests, `docs/FORK-DELTA.md`, `.gitattributes`, `.gitignore` and four test files `[VERIFIED: git diff --stat]`. So the nightly's tree is the release tree for everything the nightly exercises. The user accepted this on 2026-10-31 (36 disposition item 1). Optional extra, user's choice only: a second nightly dispatch on the scratch tip (about 34 min, one more push-visible run). No scheduled nightly will follow publication because `NIGHTLY_ENABLED` is unset (Section 1).
- **Test-only commits not re-gated by a full suite since Phase 36 E-12** (`0bb894a4`, `7e717d2d`, `bdd2e592`, `195fe556`): covered by Phase 37's E-18 full suite at `b2b2d265` (2231 tests, green) `[CITED]`, and again by the two Phase 38 suites on the release tip.
- **Commits since Phase 37 E-18** (the review fixes WR-01..WR-04, UF-37-1, UF-37-2 and the test-only `5ba9b84f`): `.github` files and Docker-free tests only; the Docker-backed suite and the merge gates were not re-run after E-18 `[CITED: 37-VERIFICATION.md:164]`. The two Phase 38 suites and the gates close this. Record in the evidence that `git diff --name-only 5ba9b84f <tip> -- . ':(exclude).planning'` lists only the Phase 38 files outside `.planning` (`VERSION`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg6.md`, optionally `docs/ROLLOUT-v1.7.0_gg5.md`), so a gated tip equals the pushed tip outside docs.

## 6. The release dry run, the single `tests.yaml` run, and the order of operations

### 6.1 What `release.yaml` does today (read in full this session)
- Triggers only on `workflow_dispatch` with input `dry_run` (boolean, default false). Any ref other than `refs/heads/custom_build` is a dry run regardless of the input `[VERIFIED: .github/workflows/release.yaml:14,27-28]`: `run-name: Release ${{ github.ref_name }} (${{ (inputs.dry_run || github.ref != 'refs/heads/custom_build') && 'dry run' || 'publish' }})`.
- Steps in order: checkout with `fetch-depth: 0` (line 49), JDK 17, Gradle, read `VERSION` (must match `^v[0-9]+\.[0-9]+\.[0-9]+(_gg[0-9]+)?$`), build and verify the archive, check archive, `Select the baseline tag` (line 91-104: `git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py "$RELEASE_VERSION"`, and the allow-list file must exist), `Build the baseline archive from the previous tag` (a `git worktree add --detach` of the tag, checks its `VERSION` equals the tag, builds with the tag's own wrapper), `Compare the archive with the previous release` (no `if:`, no `continue-on-error`; exit code propagates), `Upload the archive compare report` (`if: always()`, artifact `archive-compare-report`, 14 days), `Check tests.yaml is green on this SHA` (requires at least one successful `tests.yaml` run with `head_sha=${GITHUB_SHA}` when publishing; advisory warning on a dry run), `Check the release tag is absent` (refuses to publish when `refs/tags/${RELEASE_VERSION}` exists; advisory on a dry run), then either `Publish release` (line 177-182, publish only) or the draft sequence (create draft, check it holds exactly the archive with `draft=true`, check no tag exists for the draft, delete it, always).
- Publish command, verbatim: `gh release create "$RELEASE_VERSION" "$ARCHIVE" --target "$GITHUB_SHA" --title "$RELEASE_VERSION" --notes ""` `[VERIFIED: release.yaml:182]`. The tag is created by this call at the dispatched SHA; no step tags or pushes from the runner `[VERIFIED: release.yaml header comment]`. The release body is empty by design; editing it afterwards is a user action (optional).
- Publish concurrency group is `release-publish`, `cancel-in-progress: false`; a dry run uses a per-run group `[VERIFIED: release.yaml:27-30]`. Job limit 30 minutes.
- `pre-release.yaml` is the twin, with `--prerelease` and its own names `[VERIFIED: diff]`. Do not use it.

### 6.2 What a green dry run now means, and what it does not
- With `VERSION=v1.7.0_gg6`, the baseline resolves to `v1.7.0_gg5` and the list to `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt`. A green run means: the full-history checkout works on the runner, the baseline tag worktree builds with the tag's wrapper, the compare passes strictly (no `if:`, no `continue-on-error`: `[CITED: 37-RELEASE-RECORD.md Handoff 6]`), the report artifact uploads, a draft with exactly the archive is created, checked and deleted, and no tag was created for the draft. Expect the compare notice `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg5, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt` and the artifact's last line `RESULT: PASS`.
- It does not prove the publish step (`Publish release` is skipped on a dry run; run 40693904004 shows `Publish release: skipped`, `Create draft release (dry run): success` `[VERIFIED: gh api actions/runs/40693904004/jobs]`), nor tag creation by `GITHUB_TOKEN`. A past dry run proves the token can create and delete draft releases. Those two facts are proven only by the U4 publish run; that is exactly the ADV-03 caveat.
- The compare chain has never run on a runner. The previous `release.yaml` runs (40693904004, 40474419526, 40296481765) predate the chain `[VERIFIED: runs list]`. If the dry run fails before the compare step (checkout, worktree, baseline build, upload), that is a CI-only defect: STOP, record the step and annotation, do not re-dispatch, and return it to Phase 37 as a gap (Phase 38 may not edit `.github/`); the user decides whether to authorise a `.github/` exception. See Open Question 3.
- Timing: the earlier dry run took 1m40s with one archive build `[VERIFIED: run 40693904004 started 13:09:45Z, completed 13:11:25Z]`; this one builds twice. A `tests.yaml` run takes 6 to 10 minutes `[VERIFIED: runs list]`.

### 6.3 The single `tests.yaml` run
`tests.yaml` triggers on pushes of any branch only (`on: push: branches: ['**']`, `[VERIFIED: tests.yaml:27-29]`), so a tag creation or a release dispatch starts none. Count by `actions/runs?head_sha=<sha>` filtered to `path == .github/workflows/tests.yaml`. Required counts: X0 on the scratch branch: 1, green (informational but required by the Phase 32 precedent); M on `custom_build`: exactly 1, green, still 1 at least 120 seconds later; zero `release.yaml` runs on M before U4. Never push two branches at one SHA (the SHA-keyed group cancels one). Poll every 30 to 60 s until the run registers (5 min), then every 90 s (60 min) `[CITED: 32-06-PLAN.md "GitHub REST"]`; `gh` is authenticated now, so the old 60 requests per hour limit and the "gh not installed" note in STATE.md are outdated (read-only GETs only).

### 6.4 Order of operations (A = agent, local; U = user-run origin step; O = orchestrator inline)

| # | Who | Step |
|---|-----|------|
| 0 | A | Pre-flight (read-only): origin `custom_build` still `3a3a15a9`; left count 0; PR heads unchanged; no `sync/v1.7.0`, no gg6 tags on origin; upstream `main` still `9bed6f79`; Docker and the pinned image present |
| 1 | A | `bump_version.py gg`; commit `VERSION` plus anchors row; clean-build version agreement |
| 2 | A | CHANGELOG, FORK-DELTA, rollout note, with evidence entries for every claim; hygiene helper on them |
| 3 | O | Reviewer rounds (<=3), fixes, dispositions |
| 4 | A | Allow-list generated last and proven; commit |
| 5 | A | Gates on the tip: `testUnit` Docker-free; two `--rerun` suites on the pinned image; anchors mirror, tail gate, cumulative gate; scope facts (no `src`, test, build or `.github` change since the phase start) |
| 6 | O | Approval bundle (the Phase 32 eight-item shape): dry-run merge D in a detached worktree on `3a3a15a9` with the real merge message; checks (two parents, `tree(D)==tree(tip)`, subject, operator-document check that no document tells anyone to empty a table, rollback commands from the note run on D); `SELECT version()` and digest; then ask the user |
| 7 | U | **U1**: pre-checks, push X0 to the scratch branch, wait for its one green `tests.yaml` run |
| 8 | U | **U2**: dispatch `release.yaml` from the scratch branch (dry run), report |
| 9 | O | The user's own "publish" message to the orchestrator; build M detached on `3a3a15a9`, `tree(M)==tree(X0)`; re-check origin is unchanged |
| 10 | U | **U3**: `git push origin <M>:refs/heads/custom_build` (non-force); wait for M's single green run |
| 11 | U | **U4**: dispatch `release.yaml` from `custom_build` with `dry_run` NOT ticked; report; the run's `run-name` reads `Release custom_build (publish)`, and the user can cancel before the `Publish release` step if it reads `dry run` |
| 12 | A | Read-only GETs after U4: release `v1.7.0_gg6`, author, asset and uploader, tag ref and object type, the run and its steps; record ADV-03 closed in `38-RELEASE-RECORD.md` on the local tail |
| 13 | U | **U5**: `git push origin refs/tags/pre-v1.7.0_gg6`; delete the scratch branch (and optionally the two `scratch/clt-07-*`); close or leave PRs |
| 14 | A | Final reads: tags, heads, PR states (SWP-03), release; STATE and evidence on the local tail; do not push |

Why the pre tag goes after U3 (not U4): the ROADMAP asks for tags after the green `custom_build` run. Pushing `pre-v1.7.0_gg6` is harmless to the publish run (the tag gate looks only at `v1.7.0_gg6`), so U5 may precede or follow U4; keep it after U4 so a failed publish leaves the state simplest.

### 6.5 User handoff pre-checks (to put in the U-files, each with an expected value)
- U1: `git ls-remote origin refs/heads/custom_build` = `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; `SHA=$(git rev-parse sync/v1.7.0)` equals the gated tip; `git diff --quiet <gated sha> "$SHA" -- . ':(exclude).planning'` (same outside `.planning`); `git ls-remote origin | grep "$SHA"` prints nothing; `git ls-remote --heads origin sync/v1.7.0` prints nothing; the count of commits the push publishes: `git rev-list --count 3a3a15a9..$SHA` and `git rev-list --count $SHA --not --remotes=origin`. Push: `git push origin "$SHA":refs/heads/sync/v1.7.0`.
- U2 dispatch: UI `Release` workflow, "Use workflow from" the scratch branch, never `custom_build`; or `gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref sync/v1.7.0 -f dry_run=true`. Report: run id and URL, `headSha` equal to the pushed SHA, event `workflow_dispatch`, `headBranch`, conclusion `success`, every step conclusion, the `Select the baseline tag` notice naming `v1.7.0_gg5` and the gg6 list, the compare notice, artifact `archive-compare-report` last line `RESULT: PASS`, the archive annotation (name, size near 13.5 MB, SHA-256), no draft or tag left.
- U3: origin `custom_build` still `3a3a15a9`; `git cat-file -p <M>` shows parents `3a3a15a9` and X0 and the agreed subject; `git diff --quiet <M> X0`; push `git push origin <M>:refs/heads/custom_build` with no `--force`, `--tags` or `--all`.
- U4: M's `tests.yaml` run count is 1 and `success`; `v1.7.0_gg6` absent on origin (`git ls-remote --tags origin v1.7.0_gg6` empty); dispatch from `custom_build`, `dry_run` unticked, via UI, or `gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref custom_build`. Report the same fields plus the `Publish release` step conclusion and the release URL.
- U5: tag push by name only; deletions by explicit ref; the PR actions the user chooses.
- OPS-05 contingency at every U pre-check: if `custom_build` moved, push nothing and tell the orchestrator.

## Common Pitfalls

### Pitfall 1: A hand-pushed `v1.7.0_gg6` blocks the publish
**What goes wrong:** `Check the release tag is absent` fails the publish run. **Why:** the ROADMAP text and the gg4/gg5 recipes push both tags by hand. **Avoid:** the U-files say "do not push `v1.7.0_gg6`"; a local `v1.7.0_gg6` must not be created either. **Warning sign:** `git tag -l 'v1.7.0_gg6'` prints anything before U4 ends.

### Pitfall 2: Accidental publish
**What goes wrong:** a dispatch from `custom_build` with `dry_run` unticked publishes. It happened once (run 40474419526, cancelled by the user during the archive step) `[CITED: 37-RELEASE-RECORD.md]`. **Avoid:** U2 names the scratch branch in capitals, U4 is a separate file, and both state the `run-name` text to expect.

### Pitfall 3: The compare chain fails only on the runner
**What goes wrong:** a CI-only defect with no phase allowed to fix it. **Avoid:** it is why U2 happens before any `custom_build` push. If the user wants earlier warning, the pending Phase 37 handoff (`37-USER-DISPATCH-DRYRUN.md`, expected RED at gg5) can still be run now, at the cost of one more push of the `.planning` tail; it is optional (Open Question 3).

### Pitfall 4: Allow-list generated before the tree is final, or edited by hand to green
**What goes wrong:** stale or hand-fitted rows. **Avoid:** Section 4 order; the stale-row mutation; no row starting with a wildcard.

### Pitfall 5: Overclaiming labels in the note
**What goes wrong:** "measured" on a research-only row. **Avoid:** Section 3.0; reviewer checks labels; keep the evidence map.

### Pitfall 6: A note that asserts facts about a release that does not exist yet
See Section 3.3.

### Pitfall 7: `latest` drift and JVM crashes read as test failures
See Sections 5.1 and 5.3. A crashed fork writes no results, so a JUnit total below the expected count with 0 failures is a crash, not a pass.

### Pitfall 8: Same SHA on two branches
The SHA-keyed `tests.yaml` group cancels one run. M is a real merge (different SHA from X0). Never push the scratch branch and `custom_build` at one SHA (`--no-ff` guarantees it).

### Pitfall 9: `custom_build` moves under you
The user merging a Dependabot PR on origin diverges X0 (it happened before gg4 and gg5). Pre-check at every U step and the OPS-05 contingency.

### Pitfall 10: Publishing exposes the local tail
Pushing X0 publishes 71 commits not on any origin ref now (234 since `3a3a15a9` including the 163 already on the `scratch/clt-07-*` branches), and Phase 38's own commits; outside `.planning` that is 85 files, 8089 insertions, 607 deletions against the published tip `[VERIFIED: git diff --shortstat 3a3a15a9 HEAD -- . ':(exclude).planning']`. `.planning` carries 215 changed files, 43987 insertions since `3a3a15a9`, of which 51 files and 7385 insertions since `de8c685b` are new to origin `[VERIFIED: command]`. Deleting a branch afterwards does not unpublish commits (UF-36-1). Merging to `custom_build` puts the whole `.planning` tree on the default branch (it has been there since earlier releases). Uncommitted `.planning/config.json` and `state.json` changes are not published.

### Pitfall 11: Relayed approvals and the protected base
Executors refuse approvals relayed by another agent; the gates run orchestrator-inline; bookkeeping commits stay on the local `sync/v1.7.0` tail; `custom_build` is not checked out and its local ref is not moved by agents `[CITED: memory gsd-phase11-orchestration-gotchas]`.

## Code Examples

### Baseline choice and bump (as run)
```bash
git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg6   # -> v1.7.0_gg5
python3 -I .github/scripts/bump_version.py gg                                              # real run, from the repo root
```
### Compare proof
```bash
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd38/base.zip /tmp/gsd38/tip.zip \
  .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt        # exit 0, RESULT: PASS, no STALE-ROW
```
### Merge M (orchestrator, detached, after the user's direct approval; never pushed by an agent)
```bash
git worktree add --detach /tmp/gsd38/cb 3a3a15a9ae064ffe09c696b8bb87bce23419d463
git -C /tmp/gsd38/cb merge --no-ff -F /tmp/gsd38/merge-msg.txt <X0>      # subject: Merge sync/v1.7.0: release v1.7.0_gg6
git -C /tmp/gsd38/cb rev-parse HEAD^1 HEAD^2 && git diff --quiet HEAD <X0> && echo tree-equal
```
### ADV-03 close (read-only GETs, after U4)
```bash
gh api repos/ne1r0n/clickhouse-kafka-connect/releases --jq '.[] | {id, tag_name, author: .author.login, assets: [.assets[]|{name,size,uploader: .uploader.login}]}'
gh api repos/ne1r0n/clickhouse-kafka-connect/git/ref/tags/v1.7.0_gg6
gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/release.yaml/runs?per_page=5'
gh api repos/ne1r0n/clickhouse-kafka-connect/actions/runs/<run id>/jobs   # Publish release: success
```
Expected author and uploader login `github-actions[bot]` instead of `ne1r0n` `[ASSUMED, A3]`.

## State of the Art

| Old | Current | Impact |
|-----|---------|--------|
| Hand-pushed tags, no release object (gg3 to gg5) | `release.yaml` creates the release and the tag at the merged SHA behind a green-run gate and an archive compare | Only `pre-v1.7.0_gg6` is hand-pushed |
| Allow-list `-25/-29/-32` names | `confluent-archive-allowlist-<VERSION>.txt`, keyed by `VERSION` (Phase 37 renamed 29 and 32 to `v1.7.0_gg4` and `v1.7.0_gg5`; `-25` stays) | The gg6 file must carry exactly that name |
| Manual compare as a release-time step | compare as a strict workflow step on every release run | A wrong list fails the run before anything is created |

## Assumptions Log

| # | Claim | Section | Risk if wrong |
|---|-------|---------|---------------|
| A1 | GitHub marks PRs #16, #18, #19, #20 merged automatically once their heads are reachable from `custom_build` | 1 | The user closes four PRs by hand; harmless |
| A2 | Dependabot closes PR #10 and PR #17 as up to date on its next monthly run (documented for ignored versions in dependabot-core per Phase 33 research; timing undocumented) | 1 | PRs stay open until the user closes them |
| A3 | `gh release create ... --target <SHA>` run with `GITHUB_TOKEN` (`contents: write` at job level) creates a lightweight tag and a release authored by `github-actions[bot]`; the draft path is proven, tag creation is not | 6, Code Examples | The publish run fails at the last step with the tag absent; the user falls back to a manual release (the ADV-03 caveat then stays open) |
| A4 | Testcontainers does not re-pull a `latest` image that is already local | 5.1 | A suite runs on a newer build; the before/after digest reads would show it |
| A5 | Expected totals: `testUnit` 1672 and full suite 2270 (1672 plus the stable Docker-only 598) | 5.2 | Only a sanity check; measure and record |
| A6 | Deleting the `# Unreleased` block means the next upstream sync shows a CHANGELOG conflict at that spot | 2 | Cosmetic merge friction |
| A7 | The runner's gg6 archive size is close to the local 13558634 bytes (gg5 differed by 105 bytes) | 6.5 | The user reports a number that differs; compare tool, not size, decides |
| A8 | Byte equality of the replay holds in the rollback direction because the same golden is green on both clients | 3.1 D11 | The note would overstate; label it "derived" |
| A9 | The `README.md` file is the only repository document inside the archive besides `LICENSE` | 4 | An unexpected `outer/` diff; the compare shows it |

## Open Questions

1. **Label vocabulary for the note (needs the user).**
   - Known: RLS-02 names two labels; the handoffs use `derived ... (not measured)` and `measured in research (not re-run)`.
   - Unclear: whether the user accepts four labels, or wants the "in research" rows re-measured first.
   - Recommendation: four defined labels, reviewed.
2. **`custom_` restore (needs the user).** Ship D7 as a documented upgrade-breaking item, or open a gap to add the one client option. Recommendation: document it; the search recipe is in the note.
3. **Early CI-only de-risking (needs the user).** Run the pending Phase 37 dry-run handoff now (expected RED at gg5), or accept that the Phase 38 scratch dry run is the first runner exercise and any defect it shows returns to Phase 37 as a gap. Recommendation: accept; the local replay of the run blocks passed in Phase 37 and the compare itself is now proven on the real archives.
4. **ROADMAP and REQUIREMENTS wording (orchestrator).** Criterion 5 and RLS-05 say both tags are pushed after the green run; under decision (a) `v1.7.0_gg6` is created by the release run. The Note's "left side is not 0" is stale (measured 0). SWP-03 wording is stricter than the ROADMAP's. Amend or record deviations.
5. **gg5 note line 111** says "no release asset exists for any tag". It is false since 2026-10-28. Correct it in the same merge (a docs change inside the review scope), or leave it as history. Recommendation: correct it with one sentence, because operators read that note when rolling back to gg5.
6. **Release notes body.** The workflow publishes `--notes ""`. Pasting the CHANGELOG section into the release is an optional user action after U4.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | suites | yes (running) | server 27.4.0 | none; start with `colima start -p vz` |
| ClickHouse `latest` image | suites | yes, local | `739e099a...`, server `26.10.3.7` per earlier gates | none |
| JDK 17 toolchain (Gradle) | build, suites | yes (toolchain; `~/.gradle/jdks` holds adoptium 11 and 17); PATH java is 21.0.12 | 17 | `-PjavaVersion=21` |
| Gradle wrappers 9.8.1 (gg5 tag) and 9.8.2 (tip) | archive builds | yes, both ran this session | 9.8.1, 9.8.2 | none |
| python3 | scripts | yes | 3.14.8 | none |
| `gh` (authenticated, owner) | read-only GETs | yes | 2.102.0 | `curl` unauthenticated, 60 req/h |
| Network to Maven Central and Docker Hub | wrapper and dependency resolution, digest read | yes | n/a | none |
| Host memory | JVM stability | swap 14.7 of 16 GB used | n/a | free memory first; `-PjavaVersion=21` |

**Missing dependencies with no fallback:** none. Origin, GitHub Actions and the user's approval are inputs, not tools.

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json` `[VERIFIED: config.json:24 "nyquist_validation": false]`. The gate commands and expected totals are in Section 5.

## Security Domain

`security_enforcement` is on, ASVS level 1, block on high `[VERIFIED: config.json]`.

### Applicable ASVS Categories
| Category | Applies | Standard control |
|----------|---------|------------------|
| V2, V3 Authentication, session | no | none in scope |
| V4 Access control | yes (process) | only the user runs origin writes; executors refuse relayed approvals; `GITHUB_TOKEN` `contents: write` only on the release job |
| V5 Input validation | yes | the compare script escapes archive entry names before logging (log forging, UF-37-1); the allow-list loader refuses catch-all and leading-wildcard rows; `dry_run` is a typed boolean input |
| V6 Cryptography | no new use; archive digests are SHA-256 | recorded, not computed by hand |
| V10/V14 Supply chain and configuration | yes | all actions pinned to full SHAs; no package added; the archive gains native `zstd-jni` libraries (documented); no secret in any document |

### Known Threat Patterns
| Pattern | STRIDE | Mitigation |
|---------|--------|------------|
| Secrets or environment names in published `.planning` and docs | Information disclosure | run the Phase 37 hygiene and secrets helpers over every text file added or modified since `3a3a15a9` before U1; angle-bracket values only |
| Tampered archive or unexpected jar content | Tampering | strict compare against the tag-rebuilt baseline plus `verifyConfluentArchive`; the natives are named rows in the list |
| Accidental publish or wrong ref | Tampering | `run-name`, `dry_run` wording, separate U-files, tag-absent gate, tests gate |
| Force push or `--tags` | Tampering | the U-files give exact refspecs; the Phase 32 prohibitions are kept |
| Operator-document advice that damages state (the gg3 `TRUNCATE` finding) | Tampering | reviewer check; the dry-run bundle's "no document tells anyone to empty a table" check |

## Sources

### Primary (HIGH confidence, opened or run this session)
- `.planning/ROADMAP.md` Phase 33 to 38 blocks; `.planning/REQUIREMENTS.md` RLS-01..06, FUT-01..05; `.planning/STATE.md`; `AGENTS.md`; `CLAUDE.md`; `.planning/config.json`
- Handoffs: `35-ROLLOUT-ITEM.md`, `35-BUF03-RECORD.md`, `36-ROLLOUT-ITEMS.md`, `36-FOLLOWUPS.md`, `36-VERIFICATION.md`, `36-SECURITY.md`, `36-JDK17-RERUN.md`, `34-03-SUMMARY.md`, `34-COMMAND-EVIDENCE.md` E-09..E-11, `37-RELEASE-RECORD.md`, `37-ADVISORY-LEDGER.md`, `37-USER-DISPATCH-DRYRUN.md`, `37-VERIFICATION.md`, `37-COMMAND-EVIDENCE.md` E-07, E-18
- Recipes: `.planning/milestones/v1.7.0_gg5-ci-phases/32-release-publication/` (`32-CONTEXT.md`, `32-06-PLAN.md`, `32-VERIFICATION.md`, `32-COMMAND-EVIDENCE.md` E-11), `.planning/milestones/v1.7.0_gg4-hardening-phases/29-release-publication/29-CONTEXT.md`
- Code: `.github/workflows/release.yaml`, `pre-release.yaml` (diff), `tests.yaml`, `dependabot.yml`; `.github/scripts/{bump_version,baseline_tag,compare_confluent_archives}.py`; `.planning/scripts/{fork-anchors.txt,verify-merge-invariants.sh,confluent-archive-allowlist-*.txt}`; `build.gradle.kts` lines 48, 234, 424, 504-520; `ClickHouseSinkConfig.java:45,284`; `ClickHouseHelperClient.java:196-206`; `CompareConfluentArchivesScriptUnitTest.java:505-526`
- Documents: `docs/FORK-DELTA.md`, `CHANGELOG.md`, `docs/ROLLOUT-v1.7.0_gg5.md` and gg4 headings
- Commands: `git ls-remote origin` and `upstream`, `git rev-list`, `git merge-base`, `git diff --shortstat`, `gh api` GETs (repo, branch, releases, workflows, variables, pulls, runs, jobs, upstream issue and PR), `docker image inspect`, Docker Hub tags API, `colima status -p vz`, `sysctl vm.swapusage`, two `createConfluentArchive` builds and four compare runs under `/tmp/gsd38/`
- Auto-memory notes: `gsd-phase11-orchestration-gotchas`, `gsd-phase15-review-gate-gotchas`, `gsd-code-review-fix-gotchas`, `openj9-mockito-test-jvm-crash`, `colima-vz-profile`, `dependabot-merges-on-custom-build`

### Secondary and tertiary
- None. No web documentation was needed; nothing here depends on a library API.

## Metadata

**Confidence breakdown:**
- Standard stack and scripts: HIGH, all in the tree and run.
- Architecture and order of operations: HIGH for the local half, MEDIUM for the runner half (compare chain and publish step unexercised).
- Pitfalls: HIGH, each from a measured state or a recorded earlier incident.
- Operator-effect inventory: HIGH for completeness against the handoffs, label strength inherited from them.

**Research date:** 2026-10-31
**Valid until:** about 7 days, or sooner if origin `custom_build` moves, Docker Hub `latest` changes, or any PR head changes (re-run the Section 6.4 step 0 reads).
