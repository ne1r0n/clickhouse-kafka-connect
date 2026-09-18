# Phase 6: Merge Groundwork & Build Migration - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-18
**Phase:** 6-Merge Groundwork & Build Migration
**Areas discussed:** Merge commit shape, Conflict checklist artifact, Open facts SYNC-04/05, Build-file fidelity

All four offered gray areas were selected for discussion. `git merge-tree --write-tree custom_build v1.6.0` was re-run at discussion time and confirmed the research figure exactly: 40 messages, 35 `content` + 5 `modify/delete`, 39 unique paths.

---

## Merge commit shape

### Q1 — How should the 33 source/test conflicts be resolved in Phase 6, given Phases 7-10 do the actual semantic work?

| Option | Description | Selected |
|--------|-------------|----------|
| Upstream's file, verbatim | Stage-3 `--theirs` per conflicted path; `modify/delete` → deletion; auto-merged paths untouched; fork-only files added untouched. One merge commit with a script-verifiable property. `src/main` does not compile until Phase 7. | ✓ |
| Hunk-wise prefer upstream (`-X theirs`) | Keeps fork hunks that auto-merged inside an otherwise-conflicted file. Less destructive line-for-line, but produces half-fork/half-upstream files with no characterisable post-merge state. | |
| Keep fork's side where fork is more complete | Upstream for the 9 phantom Spotless-only files, fork for `ClickHouseBase`, the six sink-task tests, `ClickHouseHelperClient`. Front-loads Phase 7-10 judgement calls into Phase 6. | |
| Hold the merge uncommitted across phases | `git merge --no-commit`, `MERGE_HEAD` in place through Phase 10. Zero intermediate commits for five phases, no rollback granularity. | |

**User's choice:** Upstream's file, verbatim.
**Notes:** The deciding factor was verifiability — `git diff v1.6.0 <merge-sha> -- <conflicted paths>` being empty is a single assertion anyone can re-run, and it makes each of Phases 7-10 a clean reviewable diff against upstream rather than a judgement call buried in a 500-file merge.

### Q2 — Does the build/infra resolution go inside the merge commit, or in a follow-up commit right after it?

| Option | Description | Selected |
|--------|-------------|----------|
| Two commits | Merge commit 100% mechanical (upstream verbatim everywhere, including `build.gradle.kts` / `VERSION` / wrapper). Second ordinary commit re-applies the five fork build additions, `VERSION` → `v1.6.0_gg1`, `.gitignore`, `tests.yaml`. Gate runs on commit 2. | ✓ |
| One commit, build resolved deliberately | Merge commit carries the resolved build file and is immediately gate-passing. Fewer commits, but the clean "every conflicted path == v1.6.0" assertion no longer holds and the fork's build work is invisible inside a 500-file merge diff. | |

**User's choice:** Two commits.

### Q3 — `.gitignore` and `.github/workflows/tests.yaml` disposition in commit 2

| Option | Description | Selected |
|--------|-------------|----------|
| Fork's tests.yaml, union .gitignore | Fork's 55-line single-job workflow verbatim (upstream's 245-line 3-D matrix + cloud + cluster dropped, revisit only in Phase 11); `.gitignore` as a union of both sides. | ✓ |
| Fork's tests.yaml, fork's .gitignore verbatim | Both files to the fork's exact current content. Simpler to state, but silently drops whatever upstream added to `.gitignore` across 581 commits. | |
| Defer both to their owning phases | Leave upstream's versions standing; fix `tests.yaml` in Phase 11, `.gitignore` whenever it bites. Costs five phases of CI running upstream's broken-for-this-fork matrix workflow. | |

**User's choice:** Fork's tests.yaml, union .gitignore.

### Q4 — Does `sync/v1.6.0` get pushed to origin (the public fork) during Phases 6-10?

| Option | Description | Selected |
|--------|-------------|----------|
| Stay local until Phase 11 | Never leaves the machine until green. No broken branch on the public fork, no red CI, no `tests.yaml` deviation. Phase 11 pushes `custom_build` once — the only SHA CI-06 needs. | ✓ |
| Push, and gate CI off the scratch branch | Push for backup, add `branches-ignore: ['sync/**']` to `tests.yaml`. Backup without red runs, at the cost of a deliberate deviation and a visibly broken public branch. | |
| Push and let CI run red | Backup plus early runner signal. Red by design from Phase 6 to Phase 10, up to 45 min runner time per push, notification noise right before Phase 11 needs CI to be trusted. | |

**User's choice:** Stay local until Phase 11.
**Notes:** Confirmed during discussion that the fork's `tests.yaml` is `on: [push]` **unqualified** (line 8) — it fires on every branch pushed to `origin`, which is the public fork.

---

## Conflict checklist artifact

### Q1 — Where does SYNC-03's per-file resolution checklist live?

| Option | Description | Selected |
|--------|-------------|----------|
| `.planning/MERGE-CHECKLIST.md` | Milestone level, beside ROADMAP.md and REQUIREMENTS.md. `.planning/` is already tracked (99 files). Phases 7-10 each tick off their own rows. | ✓ |
| Phase 6 directory | Normal GSD phase artifact owned by the phase that owns SYNC-03. Costs later phases reaching back into a completed phase's directory. | |
| `docs/` in the repo proper | Survives past the milestone archive, pairs with SUITE-03's written record. Mixes in-flight process state into published fork documentation. | |

**User's choice:** `.planning/MERGE-CHECKLIST.md`.

### Q2 — What does the checklist cover?

| Option | Description | Selected |
|--------|-------------|----------|
| Conflicts + clean-merge-but-fork-logic | Section 1: the 39 conflicted paths. Section 2: the ~7 files that merge with no marker yet carry fork logic (`Utils`, `Processing`, `Record`, `Data`, `StructToJsonMap`, convertors, `state/provider`) — research Pitfall 5, ORCH-04's hard requirement. | ✓ |
| Conflicted paths only | Exactly the 40 messages / 39 paths. Matches SYNC-03's literal wording, trivially verifiable. Leaves the silent-merge trap with no home in the checklist. | |
| Conflicts + clean merges + the five silent-loss behaviours | Adds a third section for the must-not-silently-lose table. Most complete single view, but that table already exists in research/SUMMARY.md and each phase's success criteria — a fourth copy to keep in sync. | |

**User's choice:** Conflicts + clean-merge-but-fork-logic.

### Q3 — Living document or Phase 6 snapshot?

| Option | Description | Selected |
|--------|-------------|----------|
| Living, with a status column | All rows `pending` in Phase 6 with shape + owning phase; Phases 7-10 flip their own rows; Phase 11 asserts zero `pending` before publication. Turns the checklist into a gate. | ✓ |
| Phase 6 snapshot, never updated | Frozen record of what the merge faced; later phases' SUMMARY.md files carry what happened. Simpler, but nothing enforces that all 39 paths were revisited. | |

**User's choice:** Living, with a status column.

### Q4 — Committed invariant script, or ad-hoc checks?

| Option | Description | Selected |
|--------|-------------|----------|
| Committed script in `.planning/` | `.planning/scripts/verify-merge-invariants.sh` asserting (1) every conflicted path byte-identical to `v1.6.0` at the merge commit and (2) no `<<<<<<<` in any revision of the range. Run in Phase 6, re-run in Phase 11 across the whole range. | ✓ |
| Ad-hoc, output pasted into the record | No script to maintain; Phase 11 reconstructs the commands from prose five phases later. | |
| Script it, but under `.github/scripts/` | Ships with the fork for the next sync, next to `jacoco_summary.py`. Would need generalising past its hardcoded refs to earn a permanent home. | |

**User's choice:** Committed script in `.planning/`.

---

## Open facts SYNC-04/05

Half of SYNC-05 was answered from the repo before asking: `bufferCount` does not exist in `v1.2.9_gg4` (new in `v1.6.0`, `ClickHouseSinkConfig.java:56`, default `0`), so no pre-merge connector config can set it and upstream's new startup rejection is structurally unreachable at upgrade.

### Q1 — Does any live connector combine `enableDbTopicSplit=true` with `exactlyOnce=true`?

| Option | Description | Selected |
|--------|-------------|----------|
| No — neither, or not together | Audit recorded as "checked, combination not present"; upgrade hazard closed. | |
| Yes — at least one uses both | One-time dedup gap applies on upgrade; hazard carries to Phase 11 as a pre-publication note. | |
| Not sure — needs checking against the live cluster | Blocking-human checkpoint during execution. | |

**User's choice:** Free-text — "этот функционал мы не используем, реши сам как правильно" ("we don't use this functionality, decide yourself what's right").
**Notes:** Interpreted as equivalent to option 1 and recorded as such: neither flag is enabled in production, so upstream's documented one-time dedup gap does not apply. Both keys do exist in the fork today (`ClickHouseSinkConfig.java:47` and `:33`), both defaulting to `false`. SYNC-05 is therefore closed on both halves with no operator follow-up, one half repo-derived and one half operator-confirmed — each labelled with its source in the recorded facts.

### Q2 — How hard should SYNC-04's client-version check be?

| Option | Description | Selected |
|--------|-------------|----------|
| Assert the resolved version, not the declared one | Read `./gradlew dependencies` output (the gate runs it anyway) rather than trusting the catalog value. Catches a transitive constraint from upstream's new protobuf / schema-registry deps overriding the declaration. | ✓ |
| Record the declared versions only | Satisfies SYNC-04's literal wording with no extra work; a transitive override would go unnoticed until runtime. | |
| Also verify against the running production connector | Check what the deployed connector reports via JMX or the Connect REST API. Most rigorous, needs operator access, and the shaded jar is built from this repo anyway. | |

**User's choice:** Assert the resolved version, not the declared one.

### Q3 — Extend the assertion to the other fork-pinned versions?

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — the three fork overrides | `com.clickhouse:*` → 0.9.6, `org.apache.kafka:connect-api` → 4.1.1, `org.slf4j:slf4j-reload4j` → 2.0.17 (no catalog entry exists), plus the `org.lz4` capability → `at.yawk.lz4:lz4-java:1.10.2`. | ✓ |
| Just the ClickHouse client | Narrow to SYNC-04's literal ask; the rest covered by BLD-06/BLD-08 as build-file requirements. A silently downgraded Kafka API or dropped reload4j coordinate compiles and resolves fine — it only shows up at runtime. | |

**User's choice:** Yes — the three fork overrides.

---

## Build-file fidelity

### Q1 — `jdkHome` override and `addTestListener` console summary: keep or drop?

| Option | Description | Selected |
|--------|-------------|----------|
| Keep `addTestListener`, drop `jdkHome` | Test summary earns its keep across Phases 7-11's long run of test invocations; `jdkHome` is superseded by `javaLauncher` + toolchain with zero references in docs, CI or requirements. | ✓ |
| Keep both | Straight dev-experience parity. Carries one more fork-only block against a milestone whose goal is shrinking the delta. | |
| Drop both | Smallest delta in this block; no test summary during five phases of heavy test work. | |

**User's choice:** Keep `addTestListener`, drop `jdkHome`.

### Q2 — Compile-side JDK: fork's toolchain block, or upstream's `sourceCompatibility` + `options.release` shape?

| Option | Description | Selected |
|--------|-------------|----------|
| Keep the fork's toolchain block at 17 | Drop upstream's `VERSION_11` / `options.release.set(11)`. Bytecode stays 17, unchanged from today; pins the compiler itself to 17, relevant with JDK 21 on the local PATH. | ✓ |
| Upstream's shape, raised to 17 | Structurally identical to upstream with one number changed; almost nothing to conflict with at the next sync. Compiler becomes whatever JDK invokes Gradle (21 locally, 17 in CI). | |
| Upstream's shape verbatim at 11 | Smallest delta, wider runtime compatibility. Real risk fork code uses a 12+ feature, and it changes the shipped artifact's runtime floor — a behaviour change the milestone did not scope. | |

**User's choice:** Keep the fork's toolchain block at 17.
**Notes:** BLD-05's `javaLauncher` for the test JVM is mandatory in every branch and was never in question — upstream deleted it, and it is the only mechanism pinning the test JVM to 17 independent of the invoking JDK.

### Q3 — Where do the fork's version overrides live?

| Option | Description | Selected |
|--------|-------------|----------|
| Two marked lines in the catalog; fork-only deps stay inline | `clickHouseDriver = "0.9.6"` and `kafka = "4.1.1"` with FORK OVERRIDE comments; everything fork-only (reload4j, ben-manes, jacoco, LZ4) stays in `build.gradle.kts`, already a fork-modified file. | ✓ |
| Everything in the catalog, upstream's idiom throughout | All versions in one file, fully matching upstream's style. More fork-added lines in a file upstream edits constantly. | |
| Catalog byte-identical to upstream; overrides forced in build.gradle.kts | Zero new permanently-conflicting file. Anyone reading the catalog sees `kafka = "2.7.0"` and believes it, and a future sync silently keeps the force without signalling that upstream moved. | |

**User's choice:** Two marked lines in the catalog; fork-only deps stay inline.
**Notes:** The reasoning that settled it: a two-line conflict at the next upstream sync is the *notification mechanism* that upstream finally moved — the alternative hides that signal forever. Verified from the catalog that `kafka` is referenced by exactly one library entry and `clickHouseDriver` by five, so this really is a 2-line change.

---

## Claude's Discretion

The user explicitly delegated the SYNC-05 disposition ("реши сам как правильно"). The following were decided without a question, all recorded in CONTEXT.md and all open to a planner overriding them with a stated reason:

- Remote name `upstream` for SYNC-01 (`origin` already points at the fork; the `v1.6.0` tag is already present locally).
- The uncommitted `.gitignore` edit (adds `.gsd`) must be committed onto `custom_build` before `pre-v1.6.0-sync` is created, so the tag names a clean tree.
- Take upstream's cluster-aware `maxParallelForks` branch — strictly additive, harmless when `CLICKHOUSE_CLUSTER_NAME` is unset.
- Keep upstream's new `runClickHouseVersionClientMatrix` and `generateVersionFile` tasks as-is; removing them would enlarge the delta the milestone is shrinking.
- Record the resulting `Version.ARTIFACT_VERSION` string (`v1.6.0_gg1`) as a named fact for Phase 8's JMX re-plumb — an open gap flagged in `research/SUMMARY.md` § Gaps to address.
- Take upstream's Spotless `8.10.1` over the fork's `8.1.0`, per BLD-08.
- Record both SYNC-05 facts in `MERGE-CHECKLIST.md` under a `## Recorded Facts` section, each labelled repo-derived or operator-confirmed.

## Deferred Ideas

- A `branches-ignore: ['sync/**']` filter on `tests.yaml` — the cheap enabler if off-machine backup of the scratch branch ever becomes a concern.
- Generalising `verify-merge-invariants.sh` into `.github/scripts/` as permanent tooling for future syncs — milestone-specific today.
- An explicit at-defaults inventory of the thirteen new upstream config options — left with CFG-03 in Phase 7.
- `retryOnSocketException` — flagged in REQUIREMENTS.md as worth deliberately enabling; feature work, explicitly out of scope.
- Per-phase tags on the scratch branch for finer rollback than `pre-v1.6.0-sync`.
