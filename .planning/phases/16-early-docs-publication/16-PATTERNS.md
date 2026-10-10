# Phase 16: Early Docs Publication - Pattern Map

**Mapped:** 2026-10-10
**Files analyzed:** 9 (4 edited, 1 archived edit, 4 new)
**Analogs found:** 9 / 9

All analog paths are git-tracked (`.planning/milestones/...` and `docs/` verified via `git ls-files`). `A15` below means `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout`. `A11` means `.planning/milestones/v1.6.0_gg1-sync-phases/11-full-suite-ci-publication`.

## File Classification

| File | Role | Data Flow | Closest Analog | Match |
|------|------|-----------|----------------|-------|
| `docs/ROLLOUT-v1.7.0_gg1.md` (edit) | operator doc | transform (text edits) | commit `505e4981` (same file, WR-01..03 fix) | exact |
| `docs/FORK-DELTA.md` (edit, lines 94, 154-155) | maintainer doc | transform | `505e4981` | exact |
| `CHANGELOG.md` (edit, `1.7.0_gg1` section only) | release notes | transform | existing section, lines 1-47 | exact |
| `A15/15-REVIEW-DISPOSITION.md` (edit) | review record | CRUD (flip rows) | same file, WR-01..03 rows | exact |
| `16-REVIEW-DISPOSITION.md` (new) | review record | CRUD | `A15/15-REVIEW-DISPOSITION.md` | exact |
| `16-COMMAND-EVIDENCE.md` (new) | evidence log | transform | `A15/15-07-PLAN.md` Task 3 measured-bundle style (no dedicated file exists) | role-match |
| approval bundle (new, in plan SUMMARY) | gate artifact | request-response | `A15/15-07-PLAN.md` lines 203-213 ("Approval bundle") | exact |
| publication plan (worktree merge, push, CI check) | plan | batch | `A15/15-08-PLAN.md` Task 2 | exact |
| merge message | commit msg | n/a | `A15/15-08-PLAN.md` step 3 | exact |

## Starting state of each target section

### `docs/ROLLOUT-v1.7.0_gg1.md`

Preface line 13 (D-02, rewrite to a relative link to `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md`):
```
References such as `L-20` are entries of the fork's Phase 13 behaviour ledger, kept in this repository's planning directory; this document restates everything an operator needs from them. ClickHouse versions are written as major.minor.
```
TOC lines 18 and 21 (anchors must change with the headings; only this TOC links to them):
```
2. [R-01 behaviour change](#item-2-r-01-behaviour-change)
5. [TYPE-08 and Array(Tuple)](#item-5-type-08-and-arraytuple)
```
Headings: line 77 `## Item 2: R-01 behaviour change`, line 198 `## Item 5: TYPE-08 and Array(Tuple)`.

Other bare IDs to replace in prose (grep `TYPE-08\|R-01\|WR-02\|13-REVIEW\|999.1`): line 126 region is `L-xx` (keep as trailing tag), line 163 `**Stricter nested nulls (WR-02 strictness, L-10; ...)`, line 167 `**Known limitation, not fixed in this release (backlog 999.1).**`. Also check lines 77-116 and 198-228 for `R-01` / `TYPE-08` in body text.

Item 2 "What to do", lines 93-115 (IN-07, D-04). Current opener and closer:
```
To recover the lost data, both steps are needed, in this order:

1. Stop the connector, then reset the consumer group offsets ...
2. Clear the KeeperMap state rows for those topic-partitions. Skipping this step makes the connector see the replayed ranges as already stored (state AFTER_PROCESSING) and drop them.
...
Do not `TRUNCATE` the table. It is shared by every `exactlyOnce` connector ... deduplication cannot catch it.

Then start the connector. Without step 2 the connector sees the replayed ranges as already stored (state `AFTER_PROCESSING`) and drops them. If you use neither exactly-once nor an explicit `insertFormat=json` with schema'd records, there is nothing to recover. Rolling back to `v1.6.0_gg1` re-introduces the silent loss (item 8).
```
Keep the `DELETE FROM ... WHERE key IN (...)` and `arrayStringConcat(arrayPopBack(splitByChar('-', key), '-')` examples and the "Do not `TRUNCATE`" paragraph intact (D-04, D-09). The SQL is unchanged so it is not re-run (D-07).

Item 6 self-check, around line 246 (D-06). Narrow the last alternative:
```sh
grep -rniE 'compress=true|setCompressionMethod"?[[:space:]]*[:=][[:space:]]*"?false|set_compression_method' <connector-config-directory>
```
Target alternative: `set_compression_method"?[[:space:]]*[:=][[:space:]]*"?false`. Run the new grep against a scratch file with positive and negative lines for the evidence file.

Item 8, lines 306-330 (IN-08, D-05). Current "What to do" ends with step 3 and the paragraph "Rolling back re-introduces the silent loss of item 2 ...". Add one line: going forward again after `git revert -m 1` needs the revert commit itself reverted first; merging `sync/v1.7.0` again without that brings nothing back.

### `docs/FORK-DELTA.md` (D-03, IN-09)

Line 94 (change "once the `v1.7.0_gg1` tag exists" to wording that the commands reproduce the figures against the tag):
```
... They can be refreshed by running the four commands shown beneath the table once the `v1.7.0_gg1` tag exists.
```
Lines 152-155 (the commands print per-file counts, not totals):
```text
grep -rc '@Disabled' src/test/java                -> 7 annotation sites, 7 test methods
grep -rc 'Assumptions.assumeTrue' src/test/java   -> 1 site, in a parameterized test with 11 cases
```
Replacement per D-03: `grep -rho '@Disabled' src/test/java | wc -l` and `grep -rho 'Assumptions.assumeTrue' src/test/java | wc -l`. Run both and make the arrow text match actual output (note `-o` also counts matches inside comments or strings; verify 7 and 1). Line 55 (dependency-pins row) is the lz4 wording source: "`at.yawk.lz4:lz4-java` 1.11.2 and 1.11.3 are still affected by three advisories, all fixed in 1.11.4 ... `1.12.0` was chosen after a live LZ4 round trip against client `0.9.6`". No other FORK-DELTA edits.

### `CHANGELOG.md` `# 1.7.0_gg1, 2026-10-10` (lines 1-47, D-01)

Edit in place, no correction note. Current text to change:
```
* R-01: with an explicit `insertFormat=json`, `Struct` and Debezium CDC records now land rows ...     (line 9)
* WR-02 strictness: a null in a nested non-nullable `Array` element, ...                              (line 16)
  metadata-free JSON route (13-REVIEW WR-02). Previously the raw number was read as seconds.          (Bug Fixes)
* Bumped `at.yawk.lz4:lz4-java` from `1.10.2` to `1.12.0` (no known advisories at the time of the check).
```
Replacements: drop `R-01:` and `WR-02 strictness:` prefixes (lead with the behaviour), drop `(13-REVIEW WR-02)`. Lz4 bullet becomes: "Bumped `at.yawk.lz4:lz4-java` from `1.10.2` to `1.12.0`, because `1.11.2` and `1.11.3` are affected by three published advisories (fixed in `1.11.4`); see FORK-DELTA." No advisory IDs. Add an Important Changes bullet: JMX MBean `version=` segment `v1.6.0_gg1` to `v1.7.0_gg1` and client name `clickhouse-kafka-connect/v1.7.0_gg1`, pointing at rollout item 7. Do not touch `# 1.7.0, 2026-09-30` (line 49) or below. Style: bullets `* `, wrapped at ~120 chars with 2-space continuation indent.

## Pattern Assignments

### `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/FORK-DELTA.md` (docs fix pattern)

**Analog:** commit `505e4981` (`git show 505e4981`). It is a docs-only commit with message `docs(15): scope rollout KeeperMap steps to the connector, qualify defaults (15-REVIEW WR-01..03)`; the body explains each finding as `WR-xx / T-15-xx: <what was wrong>. <what it says now>`. Copy that: one commit per concern, subject `docs(16): ... (15-REVIEW IN-07..IN-10)`, body naming each finding and the new text. Its SHA is what goes in the Source cells.

Edits for planning-file safety: slice to the target section before any scripted edit (`c1480f8f` truncation precedent); for the docs, use exact Edit replacements, then re-grep.

Post-edit assertions (copy the "AUTOMATED ... echo TOKEN" idiom from A15 plans):
- `grep -nE 'R-01|TYPE-08|WR-0[0-9]|13-REVIEW|999\.1' docs/ROLLOUT-v1.7.0_gg1.md CHANGELOG.md` prints nothing (FORK-DELTA is exempt per D-03).
- `grep -rn 'item-2-r-01\|item-5-type-08' . --include='*.md' --exclude-dir=.planning` prints nothing.
- New TOC anchors resolve to GitHub slugs of the new headings (lowercase, spaces to `-`, punctuation dropped).
- TRUNCATE check (D-09): `grep -rniE 'truncate' docs CHANGELOG.md` shows only the "Do not `TRUNCATE`" sentence.

### `A15/15-REVIEW-DISPOSITION.md` (flip rows, D-10)

Two parallel structures, both must change. Front matter:
```yaml
  - id: IN-07
    severity: info
    disposition: open
    title: "Item 2's recovery steps are not qualified by `exactlyOnce`, ..."
...
open: 4
total: 13
```
Table (existing fixed-row Source convention):
```
| WR-01 | warning | fixed | 505e4981 (docs only, local sync/v1.7.0; not yet published) |
| IN-07 | info | open | - |
```
Set `disposition: fixed`, `open: 0`, and each table row to `| IN-07 | info | fixed | <phase-16 sha> (docs only) |`. The Phase 16 SHA must be the docs commit, so this edit follows it (a separate small commit or a SHA-after-the-fact edit). The file's footer notes that `deferred` and recorded Source text are preserved on re-run; do not run the gate script that regenerates it unless it keeps `fixed`.

### `16-REVIEW-DISPOSITION.md` (new)

**Analog:** same file as above. Copy its front matter (`phase`, `review`, `titles`, `findings:` list with `id/severity/disposition/title`, `open`, `total`, `recorded`) and the table `| Finding | Severity | Disposition | Source |` plus the dispositions footer (`open`, `fixed`, `skipped`, `deferred`). Set `phase: 16`, `review: 16-REVIEW.md`. Every `gsd-code-reviewer` finding from D-09 goes in, including the two mandated confirmations (WR-01..03 still read correctly; no TRUNCATE instruction) as explicit rows or noted verdicts, before approval is asked.

### `16-COMMAND-EVIDENCE.md` (new, D-07, D-08)

No dedicated analog file exists. Closest: A15/15-07-PLAN.md Task 3 style, where every bundle item is "measured in this plan or read from a committed file by command". Structure per command: a heading, the exact command in a fenced block, the actual output, and `Matches text: yes/no`. Entries needed:
1. `grep -rho '@Disabled' src/test/java | wc -l` and the `assumeTrue` counterpart.
2. Item 6 grep against positive and negative scratch inputs.
3. The anchor/ID greps above.
4. SQL only if its text changed: none is expected, so state "unchanged, keeps 15-REVIEW verification (26.9.14.10), not re-run". If a SQL line does change, add `SELECT version()` output from Testcontainers `latest` on colima profile `vz` with `keeper_map_path_prefix` set (see memory note on the colima `vz` profile).
5. Gate checks from the publication plan (allow-list diff, tree equality, run query output).

### Publication plan (merge, push, CI) and approval bundle

**Analog:** `A15/15-08-PLAN.md` Task 2 (lines 149-176), with Phase 16 differences: no rehearsal push of `sync/v1.7.0`, no tags, no `origin/sync` deletion, no `verify-merge-invariants.sh`, first parent `f328fd21` (not `c61d90ce`).

**Ground truth and worktree** (15-08 lines 161-162, adapt):
```
git ls-remote --heads origin custom_build   # = f328fd21...
mkdir -p /tmp/gsd16 && git worktree add /tmp/gsd16/cb custom_build
git rev-list --left-right --count custom_build...sync/v1.7.0   # 0 on left => ff possible, so --no-ff required
```
**Merge** (15-08 line 164), X as full SHA of the `sync/v1.7.0` tip:
```
git -C /tmp/gsd16/cb merge --no-ff -F /tmp/gsd16/merge-msg.txt X
```
**Merge message** (15-08 line 163): subject like `Merge sync/v1.7.0: publish corrected v1.7.0_gg1 release docs`; body names the corrected docs, says docs-only, and states that `git revert -m 1 <this commit>` backs it out. Readable without opening `.planning/`.

**Shape check** (15-08 verify block 1), adapt parent to `f328fd21`:
```sh
M=$(git rev-parse custom_build) && P=$(git rev-list --parents -n 1 "$M") && test "$(printf '%s\n' "$P" | awk '{print NF}')" -eq 3 && test "$(git rev-parse "$M^1")" = <f328fd21 full sha> && test "$(git rev-parse "$M^2")" = "$X" && git diff --quiet "$M" "$X" && echo MERGE-SHAPE-OK
```
`git diff --quiet M X` is the tree-equality check (merged tree equals the `sync/v1.7.0` tip).

**Allow-list diff** (15-07 line 216 idiom):
```sh
D=$(git diff --name-only <f328fd21> "$X") && test -z "$(printf '%s\n' "$D" | grep -v -e '^\.planning/' -e '^docs/' -e '^CHANGELOG\.md$')"
```
Plus the Phase 16 addition: `git diff --quiet f328fd21 X -- . ':!.planning' ':!docs' ':!CHANGELOG.md'`, and `git diff --name-only f328fd21 X -- src build.gradle.kts settings.gradle.kts gradle .github VERSION` prints nothing. Also (15-07 line 216): `.planning/.gsd-ws-arg` absent from `git ls-tree -r --name-only X` and from `git log f328fd21..X -- .planning/.gsd-ws-arg`; re-check `.planning/state.json` is untracked.

**Push** (15-08 line 166), custom_build only:
```
git -C /tmp/gsd16/cb push origin refs/heads/custom_build:refs/heads/custom_build
```
Save `git ls-remote --tags origin` before and after and `cmp -s` them (15-08 verify block 2 prints `CUSTOM-BUILD-PUBLISHED-TAGS-UNCHANGED`). Confirm `push.followTags` is not true. Pre-push secret scan idiom: 15-07 line 196 (`git grep -nIE` over files changed in the published range; docs-only here).

**CI check** (15-08 verify, line 175), exactly one run, no cancelled sibling:
```sh
M=$(git rev-parse custom_build) && J=$(curl -sS --max-time 30 "https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=$M") && printf '%s' "$J" | jq -e --arg p .github/workflows/tests.yaml '(.workflow_runs | type == "array") and ([.workflow_runs[] | select(.path == $p)] as $r | ($r | length == 1) and ($r[0].head_branch == "custom_build") and ($r[0].event == "push") and ($r[0].status == "completed") and ($r[0].conclusion == "success"))' && echo PUBLISHED-SHA-RUN-GREEN
```
Also assert no run for M has `conclusion == "cancelled"` across all runs of M (the filter above only sees tests.yaml). `gh` is not installed; check `/rate_limit` first (15-08 line 161: at least 15).

**Red CI** (15-08 line 168 and 15-07 line 201): apply Phase 11 D-11. Read `/jobs` and annotations, compare the `latest` image date and server version with `f328fd21`'s green run, reproduce the failing class locally, report to user, and raise a `checkpoint:decision` (gate `blocking-human`). No re-run without the user's explicit OK; forward path is revert, never force-push.

**Approval bundle** (15-07 lines 203-213, measured not cited), trimmed for Phase 16:
- X and the commit count `git rev-list --count f328fd21..X`; number touching `.planning/`.
- `git diff --stat f328fd21 X -- CHANGELOG.md docs/` and a 3-5 line summary of the corrections.
- The allow-list and tree-equality outputs (tokens above).
- Link to `16-COMMAND-EVIDENCE.md` and `16-REVIEW-DISPOSITION.md` (all findings dispositioned).
- `.gsd-ws-arg` absent; no tag will be pushed; the push command text with no force flag.
- Environment: today's date and the 2026-10-19 `ubuntu-latest` move (D-12: stop and ask if the push cannot happen before then).
- Merge will be `--no-ff` by hash in a throwaway worktree, `custom_build` only.

**Approval gate** (15-08 lines 119-125): `checkpoint:decision`, `gate: blocking-human`, orchestrator-inline, with `<reversibility rating="one-way">` text: "A non-force push to the published, protected custom_build cannot be retracted, only reverted forward with `git revert -m 1`." The user's own explicit "publish" is required; a relayed approval is refused. Commit authorization also needs to be covered here, since `AGENTS.md` forbids commits without asking.

**Hard-constraint list** (copy format from 15-07 lines 54-59 and 15-08 lines 50-55): MUST NOT force-push, MUST NOT push any tag or `--tags/--follow-tags/--all/--mirror`, MUST NOT re-run red CI without user OK, MUST NOT "fix" `verify-merge-invariants.sh`, MUST NOT alter anything outside `.planning/`, `docs/`, `CHANGELOG.md`.

## Shared Patterns

### Verify blocks with named success tokens
**Source:** `A15/15-07-PLAN.md` line 216, `A15/15-08-PLAN.md` lines 170-176. Each gate is `<automated>cmd && echo TOKEN</automated>` plus `<fails_when>` stating what a missing token means. Apply to every gate in the Phase 16 plans.

### Throwaway worktree
**Source:** `A15/15-08-PLAN.md` line 105; Phase 11 used `/tmp/gsd11/cb`. Main tree stays on `sync/v1.7.0`; remove the worktree at the end (`git worktree remove /tmp/gsd16/cb`).

### Explicit single refspec, no tags
**Source:** `A15/15-07-PLAN.md` line 199, `A15/15-08-PLAN.md` line 166. Never `--tags`; compare origin tag list before and after.

### CI located by API, not `gh`
**Source:** `A15/15-07-PLAN.md` line 220. `head_sha` plus `.path == ".github/workflows/tests.yaml"`, explicit `.workflow_runs | type == "array"` guard so a rate-limit body cannot pass.

### Doc commit message style
**Source:** `git show 505e4981`. `docs(NN): <what> (<finding ids>)`; body per finding.

## No Analog Found

| File | Role | Reason |
|------|------|--------|
| `16-COMMAND-EVIDENCE.md` | evidence log | No committed transcript-style evidence file exists; use the structure above. Look at A15 SUMMARY files for tone only. |
| Docs-only allow-list diff gate | gate | 15-07 line 216 allows only `.planning/` and `CHANGELOG.md`; Phase 16 adds `docs/` and the tree-outside-allow-list equality assertion, written fresh. |

## Metadata

**Analog search scope:** `.planning/milestones/v1.7.0_gg1-sync-phases/15-*`, `.planning/milestones/v1.6.0_gg1-sync-phases/11-*`, `docs/`, `CHANGELOG.md`, commit `505e4981`
**Files scanned:** about 12 (targeted greps over the 15-07/15-08 plans)
**Not read in full:** `15-REVIEW.md` (IN-07..IN-10 text), Phase 11 05/06 plans; planner should read the 15-REVIEW "Incremental review: release documents" section before writing the docs plan.
**Pattern extraction date:** 2026-10-10
