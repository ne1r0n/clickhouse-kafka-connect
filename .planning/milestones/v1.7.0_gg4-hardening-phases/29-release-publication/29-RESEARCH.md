# Phase 29: Release & Publication - Research

**Researched:** 2026-10-23
**Domain:** fork release engineering. Operator rollout note for `v1.7.0_gg4`, gg1/gg2 note corrections, CHANGELOG and FORK-DELTA completion, an archive compare, two suite runs, the cumulative anchors gate, an orchestrator-spawned document review, and the rehearse, approve, publish recipe of Phase 24.
**Confidence:** HIGH for everything measured or read this session (ClickHouse `26.9.19.9`, image `sha256:bdb5c911a23d`, created 2026-10-14T18:05:21Z; REST reads of 2026-10-23); MEDIUM for the mechanics re-used from Phase 24 and not re-run here (rehearsal, merge, push, tag).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Anchors gate**
- **D-01:** The release-tip gate uses the cumulative form: `PRE_TAG=pre-v1.7.0-sync` with SHA-pinned exemption rows (the gg2/gg3 form). `pre-v1.7.0-tail` is not the gate; it stays a Phase 25 rollback tag. `pre-v1.7.0_gg4` is reserved for the release rollback tag.

**gg1/gg2 note corrections**
- **D-02:** The corrected `enableDbTopicSplit=true` key rule and state-table query in `ROLLOUT-v1.7.0_gg1.md` and `ROLLOUT-v1.7.0_gg2.md` ride the release merge, in the same rehearsed tip. There is no docs-only merge. The reviewer prompt names both notes.

**Rollback and clean stop**
- **D-03:** Rollback redeploys the published `v1.7.0_gg3` artifact. The branch rollback restores the `pre-v1.7.0_gg4` tree (Phase 24 D-07 `read-tree` plus commit form). The note lists what rollback brings back.
- **D-04:** In-flight batches are measured live per direction (Phase 24 D-09, `DedupTokenRetryLiveTest` rule). Planned wording: upgrade needs no clean stop for 999.4 because failing inserts stored nothing; rollback needs a clean stop; 999.5 needs the rename as precondition. Research confirms or corrects each claim; no blanket clean stop.

**Review carry-forward and post-tag**
- **D-05:** No code or test edit. Only doc-side findings are fixed. Every other open review finding from Phases 25-28 gets a written reason, owner and re-open trigger in a milestone close record (`20-MILESTONE-CLOSE-RECORD.md` shape). Disposition rows are flipped where they live.
- **D-06:** After the tag, a reminder only. PRs #4 (client 0.10.0) and #10 (protobuf-java 4.36.2) stay open, unmerged, not ignored. The close record lists the user's web-UI steps; nothing waits on answers. The pre-gate bundle re-checks OPS-04 (`dependencies` label) and OPS-05 (origin freeze).

**Carried from the roadmap (already locked)**
- **D-07:** Recipe: push `sync/v1.7.0` once as a scratch branch and wait for green; `--no-ff` merge from a throwaway worktree so the merged tree equals the rehearsed tip; push `custom_build` only and wait for its single run; push tags by name after that run is green; delete the scratch branch.
- **D-08:** Approval gates run orchestrator-inline (executors refuse relayed approvals; `custom_build` is the protected base). Record `SELECT version()` at each gate.
- **D-09:** The note labels each claim "measured", "read from connector code" or "inferred, to be pinned live". Budget two to three review rounds (max three), reviewer fix text is itself reviewed, finding IDs are never reused.
- **D-10:** Before the note states them, re-check the Kafka 4.4.0 upgrade notes and the guava advisory identity.

### Claude's Discretion
- Plan split and wave order across docs, measurements, review rounds and the publication gates.
- Exact wording and structure of the note, within D-09 labelling.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope. PRs #4 and #10 remain open by earlier decision (2026-10-21).

Phase boundary (CONTEXT `<domain>`): publish `v1.7.0_gg4` on `custom_build`. Deliver `docs/ROLLOUT-v1.7.0_gg4.md` (every claim measured or labelled), the gg1/gg2 note corrections, fresh-context review of all operator documents, two green `--rerun` suite runs, and the rehearse, approve, publish recipe from Phase 24. No `src/main` edit and no edit of an existing test.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PUB-11 | `VERSION=v1.7.0_gg4` with its anchor row in one commit, `clean` build, CHANGELOG `1.7.0_gg4` section, FORK-DELTA rows and raw numbers refreshed once | R1 (starting values), R8 (what to refresh, measured preview numbers), Pitfalls 12, 15 |
| ROLL-04 | `docs/ROLLOUT-v1.7.0_gg4.md`: 999.4, 999.5, removed Map-key exception, `connect-api` 4.4.0, Shadow 9 / Gradle 9.8.1, guava and httpclient5 advisories, MBean `version=` | R2 (re-checks), R3 (ledger to item map), R4 (clean-stop claims), R5 (archive and rollback tree), R6 (queries) |
| ROLL-05 | gg1 and gg2 notes carry the corrected `enableDbTopicSplit=true` key rule and state-table query | R7 |
| OPS-06 | Note names `cp-kafka-connect-base:7.9.7` (Kafka 3.9), states the oldest verified worker, says KIP-1161 strictness applies only from Kafka 4.2 workers | R2.3, R3 (Item E), Open Question 6 |
| PUB-12 | Orchestrator-spawned fresh-context `gsd-code-reviewer` over every operator document, at most three rounds, dispositions recorded, IDs not reused | R9, Pitfall 1 |
| OPS-05 | No merge on `origin/custom_build` between the DEPS-02 merge and publication; re-fetch at the gate | R1, R10 |
| PUB-13 | Two `--rerun` suite runs green on `latest` x V2 on the release tip, anchors gate, rehearsed tree equals published tree, `--no-ff` merge after approval, one green run on the published SHA, tags after it, scratch branch deleted | R10, Code Examples, Pitfalls 2-8 |
</phase_requirements>

## Summary

Phase 29 changes no connector behaviour. It documents, measures and publishes what Phases 25-28 delivered, in the Phase 24 shape. The research found **eight findings that change what the plan may say or must do**. Four of them correct a planned sentence, and one is a hazard in the locked rollback target.

1. **The tree of `pre-v1.7.0_gg4` (`69cd680b`) builds a green but broken archive.** `createConfluentArchive` at that commit finishes `BUILD SUCCESSFUL` and the inner jar holds 135 file entries with 25 nested `*.jar` entries (Shadow 9.6.1 with the old two-argument `from(...)`). Phase 24 could say "a build of `pre-v1.7.0_gg3` has the same jar"; Phase 29 cannot say that about `pre-v1.7.0_gg4`. D-03 already sends operators to the artifact built at tag `v1.7.0_gg3`; the note must also say that the rolled-back branch tip must not be used to build an archive. [VERIFIED: ran `createConfluentArchive` on `git archive 69cd680b` this session]
2. **The existing archive compare works for the third-party half and fails for exactly the fork's own classes.** Compared with a gg3 build, the Phase 25 allow-list passes every third-party difference and reports 20 `UNALLOWED` lines, all `com/clickhouse/...` (18 changed or added fork classes from Phases 26-28). The release compare therefore needs per-class rows for the fork classes (a Phase 29 allow-list file), not a script edit. [VERIFIED: ran `compare-confluent-archives.py` on archives built from `git archive v1.7.0_gg3` and `git archive HEAD`]
3. **The 999.5 "rename" precondition is wider than "stop cleanly, then rename".** The behaviour is per record: a record that still carries the earlier name is mishandled by gg4 (root column stores its DEFAULT, Tuple element fails). Records already in the topic keep the names they were produced with, so the precondition is that nothing produced under the earlier names is still unread when gg4 starts, and in the rollback direction that nothing produced under the raw names is unread when gg3 starts. Phase 28's rollout line 4 says only "stop cleanly, rename the fields, start the new version". [VERIFIED: 28-BEHAVIOUR-LEDGER L-62 and the handoff record; the backlog consequence is inference, A2]
4. **The 999.4 rollback claim is right in direction and wrong in mechanism.** Phase 26 E-11 fact 2 says the gg3 rebuild "defeats block deduplication". Measured at SQL level this session: a short block under the token of a landed full block fails with code 33 `CANNOT_READ_ALL_DATA` before any dedup decision, and a byte-identical retry is dropped. The failure is a parse failure, exactly the Phase 24 E-12 mechanism. The plan still has to pin it through the connector on a gg3 tree (A1). [VERIFIED: `clickhouse-local` on 26.9.19.9]
5. **The 999.4 upgrade claim needs one qualifier.** 24 of the 25 affected shapes stored nothing on gg3 (rows=0), but the 25th (`r01`) stored a shifted value. For that shape a gg3-left in-flight batch retried by gg4 behaves by the table's dedup window (earlier content wins, or a duplicate without a window), as in the gg2 note. [VERIFIED: 26-COMMAND-EVIDENCE E-08]
6. **The gg3 "unchanged" 999.5 detection query misses two cases that gg4 changes.** Run on 26.9.19.9: it lists root names with a backslash, line feed, tab or slash and element names with an escape or slash, but not a root column whose name holds a carriage return and not any name holding a double quote. Requirement ROLL-04 says "unchanged detection query". Keep it unchanged and add a proven companion, or state the two gaps (Open Question 2). [VERIFIED: `clickhouse-local` this session]
7. **The D-10 re-checks are done.** Kafka 4.4.0 is a signed ASF release (archive dir dated 2026-10-06, tag `4.4.0`, Maven Central) whose upgrade notes exist in the source tree at tag `4.4.0` although `kafka.apache.org/44/...` still returns 404; its only Connect-facing items are worker-side. The guava advisory identity is unchanged from Phase 25 E-22, with one new fact: `GHSA-xxph-c9ww-hj94` is a repository advisory of `google/guava` and is absent from GitHub's global advisory database, while the httpclient5 GHSA is present there. [VERIFIED/CITED: R2]
8. **Live state matches the plan's assumptions.** `origin/custom_build` is still `69cd680b` with one green `tests.yaml` run; `git rev-list --left-right --count origin/custom_build...HEAD` prints `0 204`; origin heads and tags are as expected; OPS-04 stays "pending" (no Dependabot PR after the label); OPS-01 holds. [VERIFIED: R1]

**Primary recommendation:** follow the Phase 24 plan shape (probe and evidence, measurements, documents, close record, orchestrator-run review rounds, suite gate and rehearsal, publication), with three changes: (a) the review rounds run as orchestrator steps, never inside an executor; (b) the archive compare gets a Phase 29 allow-list file with one row per changed fork class; (c) the note words the rollback tree hazard (finding 1), the backlog-wide rename precondition (finding 3) and the parse-failure mechanism (finding 4) from measurements.

## Project Constraints (from CLAUDE.md)

`./CLAUDE.md` includes `AGENTS.md`; `./.claude/CLAUDE.md` adds the GSD rule. Directives that bind this phase:

- Start edits through a GSD command; no direct repo edits outside a GSD workflow.
- Pre-PR obligations that this phase owns: `VERSION` bumped and `CHANGELOG.md` updated (PUB-11); `spotlessApply` or `spotlessCheck` (no Java changes here, so the baseline must stay equal).
- "Do NOT make commits to the branch without explicitly asking the user": GSD plan commits are the user-started workflow; every push, merge and tag still needs the user's own explicit approval (D-08).
- Tests: only add, never delete or weaken; no existing-test edit in this phase (CONTEXT). Read-back assertions for anything written to ClickHouse.
- Security: do not log secrets or record data. Evidence holds sentinels only. Do not upgrade dependencies without asking (PRs #4 and #10 stay open, D-06).
- Test target is ClickHouse `latest` x client V2 only (`.claude/CLAUDE.md`). The "Gradle 7.4.2" line in `.claude/CLAUDE.md` is stale: the wrapper is `gradle-9.8.1-bin.zip` [VERIFIED: gradle/wrapper/gradle-wrapper.properties `distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.1-bin.zip`].
- Memory-derived rules: every Gradle call as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz` (never a bare `colima start`); `gh` is not installed (REST only); an exit 255 of the test JVM is an OpenJ9 crash, not a test failure; ROADMAP.md repeats `**Wave N**` headings (slice to `### Phase 29:` before any scripted edit); appended review rounds keep `### <CR|BL|WR|IN>-NN: title` headings; GSD verbs use the space form first (`state advance-plan`), the commit verb adds no `Co-Authored-By` trailer, and `echo "$VAR"` in zsh mangles JSON (redirect to a file).

## Architectural Responsibility Map

The "tiers" are the places a claim or an action can live, not web tiers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Operator-facing statements (what changed, how to self-check, rollback) | `docs/ROLLOUT-v1.7.0_gg4.md` in the merged tree | `CHANGELOG.md`, `docs/FORK-DELTA.md`, corrected gg1/gg2 notes | Operators read the note; the CHANGELOG is plain language; FORK-DELTA is for the next syncer. All ship inside the merge. |
| Proof of every quoted command and number | `29-COMMAND-EVIDENCE.md` in the phase directory | throwaway worktrees and scratch probe tests under `/tmp`, committed phase 25-28 evidence | The note quotes only what a transcript shows (20-D-03). |
| Byte-level behaviour (what a release writes) | the two class trees (`v1.7.0_gg3` tag, release tip) run on one server | `clickhouse-local` for SQL-level mechanism checks | The writer is unchanged in this phase; the proof is a run on both trees. |
| Artifact identity | `createConfluentArchive` zips compared entry by entry | `verifyConfluentArchive`, `ConnectApiReferencesUnitTest` | The shipped artifact is the Confluent archive, not the branch. |
| Green on `latest` | two local `--rerun` suite runs, then one CI run per published SHA | `testUnit` with Docker stopped | Local runs gate the push; CI is the proof on the published SHA. |
| Publication (push, merge, tag) | orchestrator, inline, after the user's own approval | none | Executors refuse relayed approvals; `custom_build` is the protected base branch. |
| Independent document review | orchestrator-spawned `gsd-code-reviewer` | executors apply fixes between rounds | An executor cannot spawn the reviewer, and an inline review does not count (Phase 24). |
| Close bookkeeping (carried findings, reminders) | `29-MILESTONE-CLOSE-RECORD.md` and the four `*-REVIEW-DISPOSITION.md` files | PROJECT.md and STATE.md at transition | D-05, D-06. |

## Standard Stack

No package is installed, added or upgraded. The tools below are what the plans run.

### Core
| Tool | Version | Purpose | Source |
|------|---------|---------|--------|
| Gradle wrapper | 9.8.1 | `createConfluentArchive`, `testUnit`, `test`, `clean` | [VERIFIED: gradle/wrapper/gradle-wrapper.properties] |
| Shadow plugin | 9.6.1 | `confluentJar` (repaired in Phase 25) | [VERIFIED: gradle/libs.versions.toml `shadow = "9.6.1"`] |
| JDK toolchain | 17 (Gradle toolchain: `~/.gradle/jdks/adoptium-17-aarch64-hotspot-mac`; local Semeru 17.0.9 OpenJ9 x3; system `java` is Homebrew 21.0.12.1) | build and test JVM | [VERIFIED: `ls ~/.gradle/jdks`, `/usr/libexec/java_home -V`] |
| ClickHouse server | `clickhouse/clickhouse-server:latest` = 26.9.19.9, image `sha256:bdb5c911a23d`, created 2026-10-14T18:05:21Z | live tests and probes | [VERIFIED: `docker image inspect`, `clickhouse-local --query "select version()"`] |
| ClickHouse client | `client-v2` 0.9.6 | path under test | [VERIFIED: gradle/libs.versions.toml `clickHouseDriver = "0.9.6"`] |
| git 2.50.1, Python 3.14.8 (`python3 -I` for anything that reads built output), curl 8.7.1, Docker 29.8.2, colima profile `vz` (Running) | | gate, evidence, REST reads | [VERIFIED: `--version` calls, `colima list`] |
| Scripts in the repo | `.planning/scripts/verify-merge-invariants.sh`, `verify-merge-invariants-selftest.sh`, `compare-confluent-archives.py`, `confluent-archive-allowlist-25.txt`, `fork-anchors.txt`, `fork-anchor-exemptions.txt` | gate and compare | [VERIFIED: files listed in `.planning/scripts`] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| A Phase 29 allow-list file with one row per changed fork class | A hand-checked list of `UNALLOWED` lines | The allow-list gives both directions (unexplained difference fails, unused row fails as `STALE-ROW`); a hand check does not. |
| A live Connect worker smoke on `cp-kafka-connect-base:7.9.7` | The static bytecode check of Phase 25 (E-12, E-13) | A smoke needs a broker and a plugin directory (heavy). The static check is what exists; label it as such (Open Question 6). |
| Build in `git worktree add` | `git archive <sha> -- . ':(exclude).planning' \| tar -x -C <new dir>` then build there | Worktrees add metadata to the repository; `git archive` leaves git state untouched. The research used the second form; either is fine in execution. |

**Package legitimacy:** not applicable. No `npm`, `pip` or `cargo` install and no new Gradle coordinate. External reads were GitHub REST, NVD, OSV, Maven Central metadata and `apache/kafka` source files.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| none | - | - | - | - | - | No external package is installed by this phase |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 Wave 1  identity + probes (29-COMMAND-EVIDENCE.md)
   VERSION + fork-anchors.txt:41 in ONE commit --> clean build --> Version.java reads v1.7.0_gg4
        |--> MBean / client-name probe: gg3 tag tree vs release tree (scratch test copied in, not committed)
        |--> archive: gg3 zip vs release zip --> compare script + Phase 29 allow-list (fork-class rows)
        |--> hazard probe: archive built at 69cd680b (pre-v1.7.0_gg4) --> nested-jar count
        |--> re-checks: Kafka 4.4.0 notes, guava + httpclient5 advisory identity, OPS-04, OPS-01, OPS-05
 Wave 2  rollout measurements (same server, both trees)
        |--> 999.4: suspects query on the 47-table catalog; WARN greps; in-flight both directions (gg3 tree retry of landed rows)
        |--> 999.5: detection query (+ companion), record-level old-name behaviour, backlog consequence, enum-label query
        |--> Map-key: config grep; alert-rule greps on scratch dirs; accepted residual wording
        |--> KeeperMap key rule incl. enableDbTopicSplit (corrected text for gg1/gg2/gg4), rollback script on dry-run merge
 Wave 3  documents (inside the tree that will be merged)
        ROLLOUT-v1.7.0_gg4.md + ROLL-05 edits to gg1/gg2 (+ gg3 line 263?) + CHANGELOG 1.7.0_gg4 + FORK-DELTA rows/numbers
 Wave 4  close record (D-05) + disposition flips
 Wave 5  ORCHESTRATOR-run review rounds (<=3) --> executors fix --> 29-REVIEW.md / 29-REVIEW-DISPOSITION.md
 Wave 6  frozen-tree suite gate: clean compileJava, testUnit (Docker stopped), test --rerun x2, cumulative anchors gate + selftest,
         spotless baseline, OPS-01/04/05, dry-run merge + rollback script, PUSH-READY      --> approval bundle
 Wave 7  USER "push" --> scratch push sync/v1.7.0 (explicit refspec) --> exactly one green tests.yaml run (X0)
 Wave 8  USER "publish" --> re-read origin --> merge --no-ff in throwaway worktree --> shape + rollback re-check on M
         --> push custom_build only --> one green run on M --> late re-check --> push tags BY NAME --> delete scratch branch --> FINAL-REFS
```

Waves 7 and 8 are one orchestrator-inline plan each, or two plans, as in Phase 24 (24-06, 24-07).

### Recommended plan shape (follows 24-01..24-07)

| Plan | Content | Notes |
|------|---------|-------|
| 29-01 | VERSION bump commit, clean build, MBean and client name probe, archive compare and the `pre-v1.7.0_gg4` hazard build, re-checks (R2), OPS-04 and OPS-05 first read | Land the only source-like change (VERSION, anchor row) first so every later measurement is on the final tree (Pitfall 5 of Phase 24). |
| 29-02 | Rollout measurements: 999.4 and 999.5 detection queries on fixtures, WARN greps, in-flight both directions on the two trees, alert greps, KeeperMap key rule | Probe tests are scratch copies, never committed. |
| 29-03 | `ROLLOUT-v1.7.0_gg4.md`, ROLL-05 edits, CHANGELOG, FORK-DELTA (rows, raw numbers, coverage audit, path resolution, cross-document consistency) | Every quoted command gets an evidence ID; verdicts flip from `pending` to `yes`. |
| 29-04 | Close record, disposition flips, doc-side fixes of carried findings | Orchestrator or executor, no reviewer involved. |
| 29-05 | Review rounds | **Orchestrator steps.** Executors only apply fixes between rounds. |
| 29-06 | Suite gate, anchors gate, dry-run merge and rollback script, rehearsal bundle, user "push", scratch push, run X0 | `blocking-human`, orchestrator-inline. |
| 29-07 | User "publish", merge, push, one run, tags by name, delete scratch, final refs, local-tail record, D-06 reminders | `blocking-human`, orchestrator-inline. |

### Pattern 1: three-part delta items with placeholders only
**What:** each item has *What changed*, *Am I affected?* (a check plus what an empty result means) and *What to do*, with placeholders (`<db>`, `<table>`, `<column>`, `<topic>`, `<zkDatabase>`, `<connector-name>`, `<alert-rule-directory>`, `<connector-config-directory>`, `<connector-log-file>`, `<rollback-commit>`) and no host. **When:** always (16-D-02, 20-D-01). Template: `docs/ROLLOUT-v1.7.0_gg3.md` (items 1-7; its intro lines 1-27 are the frame). [VERIFIED: Read of docs/ROLLOUT-v1.7.0_gg3.md]

### Pattern 2: measured, not cited; every claim carries a D-09 label
Each statement of behaviour is labelled "measured" (with the server version), "read from connector code" or "inferred, to be pinned live", and each quoted command or SQL block is run verbatim with placeholders substituted. Anti-pattern from Phase 20 and 24 reviews: "byte-identical", "new" and "measured" used where the evidence said "changed", "existing" and "simulated".

### Pattern 3: delta on gg3
The note opens like gg3 does: "If you are coming from a release before `v1.7.0_gg3`, work through `ROLLOUT-v1.7.0_gg3.md` first", which points to gg2 and gg1. The gg3 note describes 999.4 and 999.5 as known open defects; the gg4 note says plainly that gg4 closes them and what that costs.

### Anti-Patterns to Avoid
- **Saying "a build of `pre-v1.7.0_gg4` has the same jar as gg3".** It builds 25 nested jars (finding 1).
- **Phase 26 E-11 fact 2 as written ("defeats block deduplication").** Word it from the parse-failure measurement (finding 4).
- **"Stop cleanly, rename, start" as the whole 999.5 precondition** (finding 3).
- **Editing `verify-merge-invariants.sh`, its self-test, or the Phase 25 allow-list for the fork classes.** Add a new allow-list file or new exemption rows; the scripts stay read-only (Phase 25 D-02).
- **Running the review inside an executor** (Phase 15, 24 recurrence).
- **`git push --tags` or `push.followTags`.** Local-only tags exist (`baseline-v1.7.0_gg4`, `pre-v1.7.0-deps`, `pre-v1.7.0-tail`, `v1.7.0_gg3-hardening`, ...). [VERIFIED: `git for-each-ref refs/tags`]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rollback of the release | `git revert -m 1` | `git read-tree -u --reset pre-v1.7.0_gg4` plus commit, re-run on the real merge commit with the E-31 script of Phase 20 | `revert -m 1` hit 97 conflicts on a post-publication tip (Phase 16). Script text: `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-COMMAND-EVIDENCE.md` E-31 (line 579 onward). |
| Reading CI | `gh` | unauthenticated REST: `actions/runs?head_sha=<sha>`, `/jobs`; 60 requests an hour | `gh` is deliberately absent. |
| Archive identity | `unzip -l` by eye | `compare-confluent-archives.py A.zip B.zip ALLOWLIST` | Per-entry SHA-256 in both directions, with `STALE-ROW` detection. |
| Suite tally | trusting "BUILD SUCCESSFUL" | tally `build/test-results/test/*.xml`: tests, failures, errors, skipped, skip names as a set, XML timestamps newer than the last non-`.planning` commit, one server version | A rerun can print `:test UP-TO-DATE`; a crashed OpenJ9 fork writes no XML. |
| The version bump | editing only `VERSION` | `VERSION` and `.planning/scripts/fork-anchors.txt:41` in one commit, then a `clean` build | `Version.java` is not a task input; the row is `VERSION<TAB>v1.7.0_gg3<TAB>1<TAB>BLD-07` [VERIFIED: Read of fork-anchors.txt:41]. |
| Tolerating a bumped anchored line | a script edit | a SHA-pinned row in `fork-anchor-exemptions.txt`, generated from the gate's own stderr | Only if origin moved (OPS-05); 32 lines today. |
| Server-side mechanism checks | a new Java test | `docker run --rm -i --entrypoint clickhouse-local clickhouse/clickhouse-server:latest` with stdin SQL | Works with the Docker VM's file sharing limits (a bind mount of `/tmp` did not work on colima `vz`; pipe the script on stdin). |

**Key insight:** every hazard Phases 11, 15, 16, 20 and 24 hit was a procedure error (wrong tip, relayed approval, tag cancelling a run, stale rollback command, review inside an executor). Prove procedures by running them on the real object.

## Research captures

All captures: server `26.9.19.9` (image `bdb5c911a23d`, 2026-10-14), client V2 where a client is involved, 2026-10-23. Scratch work was under `/tmp/gsd29r` (outside the repo; no git state changed; no tag, branch or worktree created). The plan re-measures everything it quotes.

### R1. Starting state [VERIFIED unless noted]

| Item | Value |
|------|-------|
| Local HEAD | `c1dc6e59` on `sync/v1.7.0`; unstaged: `.planning/state.json`, `gradlew.bat` (CRLF diff; `git ls-files --eol` shows `attr/text eol=crlf`) |
| `git ls-remote origin custom_build` | `69cd680bf548bfb0329142752d37f8a6c3190c26` (= CONTEXT, = Phase 25 E-24) |
| `git rev-list --left-right --count origin/custom_build...HEAD` | `0 204`; `git merge-base origin/custom_build HEAD` = `69cd680b` |
| origin heads (`git ls-remote --heads origin`) | `custom_build` `69cd680b`, `main` `bec167d8`, `dependabot/gradle/clickHouseDriver-0.10.0` `ca52b81a`, `dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2` `011bc42b`; no `sync/v1.7.0` |
| origin tags | `pre-v1.6.0-sync`, `pre-v1.7.0-sync`, `pre-v1.7.0_gg2`, `pre-v1.7.0_gg3`, `v1.6.0_gg1`, `v1.7.0_gg1`, `v1.7.0_gg2`, `v1.7.0_gg3` (8); after publication expect these plus `pre-v1.7.0_gg4` (at `69cd680b`) and `v1.7.0_gg4` (at the merge) |
| local tags | `pre-v1.7.0-sync` `c61d90ce`, `pre-v1.7.0-tail` `bb88f23e`, `pre-v1.7.0-deps` `a884032d`, `baseline-v1.7.0_gg4` `aa450709`; `pre-v1.7.0_gg4` and `v1.7.0_gg4` do not exist |
| local `custom_build` | `6a2a87a5` (18 behind origin); the throwaway worktree must fast-forward it to `69cd680b` first (Phase 24 E-71 form) |
| `tests.yaml` runs on `69cd680b` | exactly 1: id `39584176889`, push, completed, success |
| `integrationTests.yaml` (OPS-01) | state `disabled_manually`, `updated_at` 2026-10-16T09:38:23.000Z; 2 schedule runs ever (2026-10-15T09:39:18Z, 2026-10-16T09:32:28Z), both before the disable |
| OPS-04 | `labels/dependencies` answers 200; 15 PRs in total, newest #15 created 2026-10-21T06:29:01Z, before the label (2026-10-21T15:11:05Z); open: #4, #10 → E-25 stays "pending the next monthly run" |
| GitHub releases | none (`releases` returned `[]`), so "the artifact" is always the operator's own build |
| upstream | `git ls-remote upstream main` = `a6f1df83...` (the tail already merged); no tag past `v1.7.0` |
| Baselines for the gate | `testUnit` 1248 tests, 2 skipped; full suite 1835 tests, 101 XML files, 18 skipped, one server version [VERIFIED: 28-COMMAND-EVIDENCE E-13] |
| Host | colima `vz` Running; Docker 29.8.2; swap 27.1 of 28 GB used (OpenJ9 crash risk, Pitfall 7) |

The scratch push publishes the whole local history above `69cd680b` (204 commits, `.planning/` included), as Phase 24's precedent did.

### R2. D-10 re-checks and the OPS-06 inputs

**R2.1 Kafka 4.4.0 upgrade notes.**
- The release exists: `https://archive.apache.org/dist/kafka/4.4.0/` lists `RELEASE_NOTES.html`, `.asc`, `.md5/.sha1/.sha512` and `kafka_2.13-4.4.0-site-docs.tgz`, all dated 2026-10-06 16:20; `refs/tags/4.4.0` exists in `apache/kafka`; Maven Central metadata for `connect-api` lists `4.4.0`. [VERIFIED: curl listings]
- The website page `https://kafka.apache.org/44/getting-started/upgrade/` answered **HTTP 404** on 2026-10-23, and `https://kafka.apache.org/community/downloads/` lists 4.3.1 as latest and no 4.4.0. The notes are in source: `https://raw.githubusercontent.com/apache/kafka/4.4.0/docs/getting-started/upgrade.md`, section `## Upgrading to 4.4.0` (lines 29-62). [VERIFIED: curl/WebFetch]
- Connect-facing content of that section: only KIP-1209 (distributed workers get `internal.topics.automatic.creation.enable`, default `true`; worker-side) and a sentence under KIP-909 saying Connect does not support asynchronous bootstrap resolution and forces `bootstrap.resolve.timeout.ms` to `0`. Nothing on `connect-api`, `SinkTask`, `ConfigDef`, transformations or converters. [CITED: apache/kafka@4.4.0 docs/getting-started/upgrade.md:55,59]
- `RELEASE_NOTES.html` Connect-related titles: "Add configuration to control internal topic creation in Kafka Connect", "Cached connector plugin version validation failures lose available versions on repeated validation", "Connect's plugin.path documentation does not match behavior". [CITED: archive.apache.org/dist/kafka/4.4.0/RELEASE_NOTES.html]
- The connector's own compatibility rests on Phase 25's static check: the compiled connector's 176 `org.apache.kafka` references (39 classes, 137 members) are present in the worker's 3.9 API jars, in `4.1.1` and in `4.4.0` (`REFERENCE worker classes-missing 0 members-missing 0`, same for both versions), and the archive holds 0 `org/apache/kafka/` entries. [VERIFIED: 25-COMMAND-EVIDENCE E-12]

**R2.2 KIP-1161 (OPS-06).** The upgrade notes list it under **Kafka 4.2.0** (`## Upgrading to 4.2.0` starts at line 122; the KIP-1161 bullets are lines 168-173): null values rejected for most LIST configs, duplicate entries rejected (dropped with a warning), some empty lists rejected. [CITED: apache/kafka@4.4.0 docs/getting-started/upgrade.md:122,168-173] Comparing `ConfigDef.java` at tags `4.1.1` and `4.4.0`, the `case LIST` branch of `parseType` is unchanged (no dedup, no null check there); the strictness lives in `ValidList` validators, whose use grew from 4 to 9 mentions. [VERIFIED: diff of the two raw files this session] The connector declares seven `ConfigDef.Type.LIST` keys (`ClickHouseSinkConfig.java:525,534,682`; `HeaderToValue.java:49,54`; `HeaderToValueConfig.java:23,28`) and no `ValidList` anywhere in `src/main`. [VERIFIED: `grep -rn "ValidList\|Type.LIST" src/main/java`] So the connector's own keys are not subject to the new validators, and a 4.2+ worker applies the strictness only to its own and other plugins' configs. Wording: "applies only from Kafka 4.2 workers; the production worker is Kafka 3.9; this connector's LIST keys carry no validator" [labels: CITED + VERIFIED by grep].

**R2.3 Worker facts for the note.** Production is `confluentinc/cp-kafka-connect-base:7.9.7` (Confluent Platform 7.9, Apache Kafka 3.9; stated by the user 2026-10-21, REQUIREMENTS OPS-06). Image digest `sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055` (pulled by tag; amd64 and arm64 copies of the three API jars are byte-identical). Its `SinkTask` still declares `onPartitionsRevoked`, the 4.1.1 and 4.4.0 jars do not, which is why the connector keeps the method without `@Override`. [VERIFIED: 25-COMMAND-EVIDENCE E-12] "The oldest worker the connector is verified against" = Apache Kafka 3.9 (the 7.9.7 jars), **by a static bytecode reference check plus `ConnectApiReferencesUnitTest`, not by a run on a live worker**; the suite compiles and runs against `connect-api` 4.4.0. Say exactly that (Open Question 6).

**R2.4 Guava advisory identity (re-read 2026-10-23 15:4x UTC).**
- NVD `CVE-2026-102554`: CNA `cve-coordination@google.com`, published 2026-10-09T16:17:20.410, last modified 2026-10-09T18:17:01.380, `vulnStatus` "Awaiting Analysis", no CPE configuration, CWE-770 (and CWE-502 in OSV), description "Google Guava versions 4.0 through 33.7.1 ... OutOfMemoryError" during deserialization of `CompactHashMap`, `CompactHashSet` or `MapMakerInternalMap`; a secondary CVSS 4.0 score 8.2 HIGH from the CNA. [VERIFIED: NVD REST `cves/2.0?cveId=CVE-2026-102554`]
- OSV record id `CVE-2026-102554`, alias `GHSA-xxph-c9ww-hj94`, modified 2026-10-21T09:31:55Z, summary "Denial of Service via Eager Array Allocation During Deserialization in Guava"; a GIT range with fix commit `b931fe9d6d5cf00bc55714ad3308d086f71850fe`; `database_specific.unresolved_ranges` read "introduced 4.0, fixed 33.7.2" (affected field) and "introduced 4.0, last_affected 33.7.1" (description); no Maven range. A direct `GET /v1/vulns/GHSA-xxph-c9ww-hj94` answers 404 "not found, but the following aliases were: CVE-2026-102554". `POST /v1/query` Maven `com.google.guava:guava` returns `{}` for `33.4.6-jre` and for `33.7.2-jre`. [VERIFIED: api.osv.dev]
- **New this session:** `https://github.com/google/guava/security/advisories/GHSA-xxph-c9ww-hj94` answers 200 (a repository advisory), while `https://github.com/advisories/GHSA-xxph-c9ww-hj94` and `GET api.github.com/advisories/GHSA-xxph-c9ww-hj94` answer 404: GitHub's global advisory database does not hold it. [VERIFIED: curl status codes]
- Net: the identity is unchanged from Phase 25 E-22 (the pair `CVE-2026-102554` / `GHSA-xxph-c9ww-hj94`, fixed in `33.7.2`). The two machine sources disagree about gg3's `33.4.6-jre`: the NVD text range includes it, OSV's Maven query does not flag it. The note states both, labelled by source, and makes no exposure assessment (FORK-DELTA row, Adopted wholesale, already does). Upstream's own comment on the same override is "temp to override client's with CVE" [CITED: .planning/MERGE-CHECKLIST-v1.7.0-tail.md, Section 2 row for build.gradle.kts].

**R2.5 httpclient5 advisory identity.** `CVE-2026-64607` = `GHSA-hjcp-jmpx-g3qm`, "Apache HttpComponents Client: Connection Leak on Content-Encoding Decode Error Leads to Pool Exhaustion DoS", medium. GitHub global advisory (REST 200): vulnerable `>= 5.0-alpha1, < 5.6.3`, first patched `5.6.3`, published 2026-07-31T12:30:30Z, updated 2026-08-13T18:40:19Z. OSV (alias `CVE-2026-64607`, modified 2026-09-10): ECOSYSTEM range introduced `5.0-alpha1`, fixed `5.6.3`; Maven query returns this id for `5.6.2` and `{}` for `5.6.5`. gg3 shipped `5.6.2`; gg4 ships `5.6.5`. [VERIFIED: api.github.com, api.osv.dev; E-11 for the shipped versions]

### R3. What the note must carry: ledger rows and facts to items

| Source | Operator-visible content | Note item |
|--------|--------------------------|-----------|
| 28 ledger L-58, L-59, L-60, L-62 | Names match under the raw identifier on both clients. Old and new form per character and shape, both clients (72 cells, 28 ledger handoff table). Root field in the earlier form: DEFAULT stored, no error, no WARN. Element in the earlier form (Tuple, Array(Tuple), Nullable(Tuple), Map-value Tuple): the put fails with `DataException ... column '<path>' (INT32): no value provided for a non-nullable column without a default ...` and stores no row. A table with a double quote in a name is now mapped (V2 unmapped before, V1 U+FFFD). V1 is the default client (`client_version` default `V1`), so V1 tab, CR, slash and double-quote tables start mapping. | Item A (names) |
| 28 ledger L-63, L-64 | Enum8/Enum16 labels are keyed under the raw label; the earlier V2 key form fails as the lookup miss (9 of 12 entries have a second form); an apostrophe label followed by another label, or a label holding `)`, `,` or `=`, no longer leaves the table unmapped. | Item A (enum labels): ROLL-04 does not list it, the ledger destination is ROLL (Open Question 3) |
| 26 E-11 facts 1-5 | 999.4: affected shapes now insert; the two WARNs; no clean stop on upgrade; clean stop before rollback; short writes that parsed are an unrepaired tail; the retried batch bytes change. | Item B (size-named elements) |
| 27 ledger L-52 | Jackson failure text no longer reaches status, DLQ stack trace or `Error inserting records`; the Map-key exception of gg3 item 1 is gone; accepted residual (Jackson exception whose chain also holds a non-Jackson `IOException`). Alerts keyed on Jackson text stop matching. | Item C (writer text) |
| 27 ledger L-54 | Unknown Enum label: `java.lang.IllegalArgumentException` instead of `java.lang.NullPointerException` in the `Failed to write column` line; the column is named, the lookup-miss text is withheld (27-REVIEW WR-01 is open). | Item C |
| 25 E-11, E-22, E-24 | Bundled bumps: jackson-core/databind 2.22.3 → 2.22.4, gson 2.13.1 → 2.14.0, httpclient5 5.6.2 → 5.6.5, httpcore5 and httpcore5-h2 5.4.3 → 5.4.4, guava 33.4.6-jre → 33.7.2-jre, j2objc 3.0.0 → 3.1, error_prone_annotations 2.38.0 → 2.50.0; unchanged: lz4-java 1.12.0, slf4j-api 2.0.7, commons-lang3 3.20.0 in the archive. A fresh compare at the release tip (R5) replaces these as the cited numbers. | Item E (dependencies) |
| 25 E-12, E-13 | `connect-api` 4.4.0 compile only; archive holds 0 Kafka entries; worker supplies the API; static reference check against the 3.9 jars. | Item E |
| 25 E-09..E-11 | Shadow 9.6.1 repair; Gradle 9.8.1; no operator action; archive compare against gg3. | Item E |
| REQUIREMENTS OPS-06 / R2 | Production worker, oldest verified worker, KIP-1161. | Item E |
| MBean / client name | `version=v1.7.0_gg4`; client name `clickhouse-kafka-connect/v1.7.0_gg4`; the repository's exporter config (`jmx-export-connector.yml:5`) uses `version=(.*)` (wildcard), so it keeps working. The rest of the user agent changes too (the HttpClient token follows httpclient5: `5.6.2` → `5.6.5`). | Item F |
| D-04 / R4 | In-flight batches, both directions, per change. | Item D |
| D-03 / R5 | Rollback, what it brings back, the tree hazard. | Item G |

Not in the note (record only): L-53, L-55, L-56, L-57, L-61, L-65, L-66, `commons-lang3` 3.21.0 (`testFixturesImplementation` only [VERIFIED: build.gradle.kts:153]), ben-manes 0.66.0 (build-time), the CI archive step and pin-guard changes (CHANGELOG Build and CI only).

### R4. D-04: the clean-stop claims, confirmed or corrected

| Claim | Verdict | Wording and label |
|-------|---------|-------------------|
| 999.4 upgrade (gg3 → gg4): no clean stop, failing inserts stored nothing | **Confirmed with a qualifier.** 24 of 25 affected shapes: `rows=0`, 23 `CANNOT_READ_ALL_DATA` and 1 `ATTEMPT_TO_READ_AFTER_EOF`. The 25th (`r01`) stored size `1667391751` and `hex(v)` `78797A` instead of `7` and `6162630378797A`. [VERIFIED: 26 E-08] | "measured" for the 24; the `r01` shape leaves a stored shifted row; a gg3-left in-flight batch of such a shape retried by gg4 follows the table's dedup window (gg2 note item 1 rule: earlier content wins with a window, a second row without one) → "read from the dedup rule, pin live if cheap". Rows already stored short are an unrepaired tail ("measured", E-08). |
| 999.4 rollback (gg4 → gg3): clean stop first | **Confirmed in direction; mechanism corrected.** SQL-level check, 26.9.19.9, `MergeTree` with `non_replicated_deduplication_window=100`, `Tuple(size UInt16, v String)`: whole-tuple row inserted under token `tok-0-0-0` (count 1); the short block (element `size` dropped) under the same token fails `Code: 33 ... Cannot read all data. Bytes read: 1. Bytes expected: 98: (at row 1)` and count stays 1; the byte-identical retry is accepted and dropped (count stays 1). [VERIFIED: clickhouse-local this session, script in Code Examples] So the gg3 retry fails on parse before the token is consulted: the task fails or the batch goes to the DLQ although the rows landed (the Phase 24 E-12 mechanism). | "measured at SQL level"; the plan pins it through the connector on a gg3 tree: one affected shape and one look-alike control (`Tuple(size1 ...)`, bytes identical in both releases, retry dropped by the token), rows landed by SQL under the token the connector sends (`<topic>-<partition>-<min>-<max>`, `QueryIdentifier.java:56-61`). |
| 999.5 needs the rename as precondition | **Confirmed, and wider.** Per-record behaviour is measured (28 E-08: root DEFAULT, element fails). The consequence for the backlog is inference: records already in the topic keep the names they were produced with. | Precondition = every record gg4 will read uses the raw names: let gg3 consume everything produced under the earlier names, switch producers, then upgrade; in the rollback direction nothing produced under the raw names may be unread when gg3 starts. Label: "per record measured; backlog consequence inferred from it". The Phase 28 sentence (clean stop BEFORE/AFTER) stays, labelled reasoned. |
| 999.5 clean stop, both directions | **Confirmed, reasoned.** A batch left in `BEFORE_PROCESSING` and retried by the other release is rebuilt under the other name rule, so its bytes differ (root: DEFAULT instead of the value; element: failure). | "reasoned from the BEFORE/AFTER state machine, not measured" (28 ledger rollout line 4); optional live pin in the style of Phase 24 E-12 (Open Question 6 covers the cost). |
| WRT-17 (Jackson text) and WRT-18/19 (enum, metadata helpers) | **No clean stop, either direction.** Text only: a batch that fails this way fails in both releases; retry classification unchanged (L-52, L-54). | "read from connector code, pinned by `WriterFailureClassificationUnitTest`". |
| Bundled patch bumps (jackson, gson, httpclient5, httpcore5, guava) | No clean stop needed. No change in the bytes sent to ClickHouse is expected, none is measured. | "inferred" (A4); the suite and the JSON-route live tests are green on the tip. |
| MBean `version=` and client name | No clean stop (not part of a batch or token). | "read from connector code" (gg3 item 4 sentence). |
| Backlog 999.5 enum labels | A producer sending an enum value in the earlier key form fails the batch after the upgrade; a raw label now lands. Same record-level precondition as names. | "measured" (28 E-09, E-10). |

### R5. Archive compare, rollback tree hazard, and the release-tip compare design

**R5.1 Preview at HEAD (VERSION still `v1.7.0_gg3`).** Archives built with `createConfluentArchive` from `git archive v1.7.0_gg3` and `git archive HEAD` (both excluding `.planning`), compared with the Phase 25 allow-list: inner entries 7376 → 7399; third-party `VERSION` lines identical to Phase 25 E-22 (guava 33.4.6-jre → 33.7.2-jre, httpclient5 5.6.2 → 5.6.5, httpcore5 and h2 5.4.3 → 5.4.4, jackson-core/databind 2.22.3 → 2.22.4, gson 2.13.1 → 2.14.0, j2objc 3.0.0 → 3.1, error_prone 2.38.0 → 2.50.0; lz4-java, slf4j-api, commons-lang3 unchanged); `nested jars in b: 0`, `kafka classes in b: 0`; `RESULT: FAIL` with exactly **20 `UNALLOWED` lines, all under `com/clickhouse/`**: changed `ClickHouseWriter`, `ClickHouseWriter$1`, `ClickHouseWriter$2`, `ClickHouseFieldDescriptor` and its builder, `ClickHouseHelperClient` and `ClickHouseHelperClient$ClickHouseClientBuilder`, `Column`, `Column$1`, `Column$ColumnBuilder`, `DeclaredNullability`, `DeclaredNullability$1`, `DeclaredNullability$Assignment`, `Table`, `Table$1`, `Table$TableDesc`, `SinkTaskStatistics` (a line-number shift from the comment-only edit), and added `DescribeTableBuilder`, `DescribeTableBuilder$RowDecision`, `ParentShape`. [VERIFIED: ran this session; output in `/tmp/gsd29r/cmp-head.txt`]
 `git diff --name-only v1.7.0_gg3 HEAD -- src/main` prints 12 files (`ClickHouseSinkTask`, `ClickHouseWriter`, `ClickHouseFieldDescriptor`, `ClickHouseHelperClient`, `DescribeTableBuilder`, `Column`, `DeclaredNullability`, `ParentShape`, `Table`, `HeaderToValue`, `WithheldMessageException`, `SinkTaskStatistics`); three of them (`ClickHouseSinkTask`, `HeaderToValue`, `WithheldMessageException`) changed comments in place without moving a line, so their class files are unchanged. [VERIFIED: git diff]

**R5.2 Release-tip compare design.** Use the same script with a **new** allow-list file (for example `.planning/scripts/confluent-archive-allowlist-29.txt`) that holds the Phase 25 third-party rows plus one row per changed fork class (glob, reason naming the phase and requirement: DESC-08, WRT-17, WRT-18, NAME-03/04/06) and the two release-identity rows (`com/clickhouse/kafka/connect/sink/Version.class` or wherever `ARTIFACT_VERSION` lands, and the manifest rows). The script already reports `MANIFEST.MF: differs only in Implementation-Version` for a version bump and ignores `release_date` and `version` lines of the outer `manifest.json`; the zip, jar and directory names carry `v1.7.0_gg4`. The Phase 25 file's header says no row may match a `com/clickhouse/` path; that rule belongs to that file, the script does not enforce it. [VERIFIED: Read of the script header and allow-list header; grep for `clickhouse` in the script finds no code use] Do not edit the script or `confluent-archive-allowlist-25.txt`. A row that matches nothing fails as `STALE-ROW`, which catches an allow-list that drifted from the tree.

**R5.3 The rolled-back tree builds a broken archive.** `createConfluentArchive` on `git archive 69cd680b` (the `pre-v1.7.0_gg4` first-parent tree): `BUILD SUCCESSFUL in 4s`, one inner `*-confluent.jar`, **135 file entries, 25 nested `*.jar` entries**, `ClickHouseSinkConnector.class` present, zip still named `...v1.7.0_gg3.zip`. [VERIFIED: ran this session, build log `/tmp/gsd29r/build-pre.log`] This is the E-09 outcome of Phase 25 on the same tree (E-09 measured merge 1, which differs from `69cd680b` by one anchor row). So: (a) the rollback step "redeploy the artifact built at tag `v1.7.0_gg3`" is the only sound artifact step; (b) after `read-tree -u --reset pre-v1.7.0_gg4` the branch tip, its `tests.yaml` (no archive step) and a `release.yaml` run would all build or accept that archive; (c) `pre-release.yaml` still pins `gradle-version: '7.4.2'` and `release.yaml` runs `./gradlew createConfluentArchive` [VERIFIED: Read of both workflows lines 26-34]. The note says (a) and warns about (b) in "Rolling back brings back"; the plan measures it again in the evidence (nested-jar count at `pre-v1.7.0_gg4`) and in the same entry builds the `v1.7.0_gg3` tag to show 0 nested jars (Phase 25 E-11 did that build).

### R6. Detection queries: what exists, what the gg4 note needs

| Query | Source | Status for gg4 |
|-------|--------|----------------|
| 999.4 suspects (two predicates over `system.columns`) | gg3 note item 2, lines 129-139 [VERIFIED: Read] | Reuse unchanged. Re-prove on the 47-table `DescribeFixtureCatalog` (25 affected keys, 22 other tables) and state the one over-flag (`Array(Map(String, Tuple(size1 ...)))`). Meaning flips: before the upgrade it lists tables whose inserts fail (or store a shifted row), after it lists tables the fix changes. |
| 999.4 WARN greps | `have no matching DESCRIBE sub-column` (`DeclaredNullability.java:49-51`) and `Could not read the declared type` (`DescribeTableBuilder.java:35-37`) [VERIFIED: Read] | After the upgrade the first grep is empty for every round-tripped catalog table (26 E-09: zero WARN across 46 cases); a non-empty `Could not read the declared type` line marks the one residual (a parent whose declared type cannot be parsed: a bare `size` or `size0` is still dropped). The second text needs a captured real line or the unit-test line proven on a scratch log (Phase 24 E-23 form). |
| 999.5 detection (gg3 item 3 second query) | lines 203-214 of the gg3 note [VERIFIED: Read] | Run on 26.9.19.9 against nine tables this session: **listed** root backslash, line feed, slash, tab; element with line feed, tab or carriage return (backslash in type text). **Not listed:** a root name with a carriage return, any root or element name with a double quote, enum labels (stripped before matching). |
| Companion candidate | previewed this session: `position(name, '\r') > 0 OR position(name, '"') > 0 OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*(\\\\r\|")[^`]*`')` | Listed `t_cr`, `t_dq`, `t_el_cr`, `t_el_dq`; did not list enum, plain or the other tables. Preview only (A7): the plan proves it on fixtures with look-alikes. |
| Enum label detection | none exists | Raw `system.columns.type` prints an apostrophe label as `\'` and other escapes with a backslash, a double quote and a slash raw; a candidate is "type matches `Enum(8\|16)(` and holds a backslash, `"` or `/`". Not run. Needed only if Item A covers labels (Open Question 3). |
| Config self-check | `grep -rnF 'input_format_binary_read_json_as_string' <connector-config-directory>` (gg3 item 1) | Reuse; meaning flips (gg3: Map key can appear; gg4: it cannot, accepted residual only). |
| Alert greps | `grep -rnF -e 'Invalid UUID string' -e 'For input string' <alert-rule-directory>` (gg3 item 1) | Add a gg4 grep for the Jackson texts (`through reference chain`, `InvalidDefinitionException`) and for `NullPointerException` on Enum columns; prove each on scratch directories as 24 E-21 did (text-literal limits stated). |
| Client name query | gg1 note item 7 query | Unchanged; shows `clickhouse-kafka-connect/v1.7.0_gg4` after the upgrade. |
| KeeperMap in-flight query | gg3 item 4 step 2, lines 248-258 | The corrected text for ROLL-05 (R7). |

### R7. ROLL-05: scope of the gg1 and gg2 corrections

The canonical text is gg3 item 4 step 2 (Read lines 247-263): run the query in the connector's `database`, or write `` `<db>`.`<zkDatabase>` ``; one `IN` entry per topic; with `enableDbTopicSplit=true` also add `'<db><dbTopicSplitChar><topic>'` per topic, because "the connector keys a topic that its `dbTopicSplitChar` divides into exactly two parts under the topic name itself, and any other topic under that prefixed form", and a topic name that ends with `dbTopicSplitChar` can also be keyed without those trailing characters. The key rule is "read from the connector code, not measured" in gg3. [VERIFIED: Read of `RecordConvertor.java:8-19` (`parts.length == 2` → `database = parts[0]; topic = parts[1]`) and `Processing.java:139-141` (`topic = database + clickHouseSinkConfig.getDbTopicSplitChar() + topic;`); the state key is `<topic>-<partition>` per the gg1 note]

Edit sites (all read this session):
- `docs/ROLLOUT-v1.7.0_gg1.md`: item 1 "Am I affected?" step 2 (line 52 onward, the query at 55-60 and the rule sentence at 62: "list `'<db><dbTopicSplitChar><topic>'` instead of `'<topic>'`" is the wrong rule), and the item 2 recovery example (lines 104-108: the comment "with enableDbTopicSplit=true each key is '<db><dbTopicSplitChar><topic>-<partition>'" and the two `DELETE` examples).
- `docs/ROLLOUT-v1.7.0_gg2.md`: item 1 "What to do" step 2 (lines 78-88): query, no database sentence, no `enableDbTopicSplit` rule, and the closing sentence that sends the reader to the gg1 note for the key rule.
- `docs/ROLLOUT-v1.7.0_gg3.md` line 263 says the gg1 and gg2 rule "is shorter than the one in step 2 above and can miss a key, so follow step 2". After ROLL-05 that sentence is stale. gg3 is not in ROLL-05's list (Open Question 4).
- Released CHANGELOG sections stay as released. The gg1/gg2 notes need a short revision notice in the style of the gg3 note's first paragraph (it names the corrected items).
- Do not add any `TRUNCATE` instruction (the gg1 note's "Do not TRUNCATE" paragraph stays).
- Optional upgrade of the label from "read from code" to "measured": run the Phase 24 E-14 recipe (connector-created KeeperMap state table, which needs `keeper_map_path_prefix` on the server; `ClickHouseBase` mounts `keeper_map.xml`) with `enableDbTopicSplit=true` and one two-part and one one-part topic. [VERIFIED: ClickHouseBase.java:124 mounts `/etc/clickhouse-server/config.d/keeper_map.xml`]

### R8. Document refresh scope (PUB-11)

- **VERSION and anchor:** `VERSION` reads `v1.7.0_gg3`; `fork-anchors.txt:41` is `VERSION	v1.7.0_gg3	1	BLD-07`. [VERIFIED: Read] No test references the version string; the generated `Version.java` needs `clean`. [VERIFIED: `git grep v1.7.0_gg3 -- src` finds nothing]
- **CHANGELOG:** the top section is `# 1.7.0_gg4, unreleased` with four Bug Fixes bullets written by Phases 26-28 (999.4, Map key, enum, raw names). Complete it in the gg3 template: intro paragraph, **Important Changes** (rename precondition promoted here: closes 28-REVIEW IN-03's doc-side option; the clean-stop rules; the Map-key exception gone; dependency absorption and the archive facts; MBean and client name), **Known Issues** (the 999.4 fallback for an unreadable declared type; 999.5 unmeasured characters; the accepted Jackson residual), Bug Fixes (existing), **Build and CI** (Shadow repair and `verifyConfluentArchive`, the CI archive step, pin-guard changes, 21-class `jacocoForkReport`). Replace `unreleased` with the publication date; if publication slips a day the tree is no longer the rehearsed tree (Pitfall 12).
- **FORK-DELTA stale text** [VERIFIED: Read]: intro lines 5 and 7 stop at gg3; the `VERSION` row (line 65) and the MBean row (line 33, "currently `v1.7.0_gg3`"); behaviour-ledger pointer (line 25) stops at `L-51` (add `L-52` to `L-57` in `27-BEHAVIOUR-LEDGER.md`, `L-58` to `L-66` in `28-BEHAVIOUR-LEDGER.md`); raw numbers table (lines 121-130) and its prose (143, 145); Adopted wholesale `src/main` counts (line 112: 36 of 63 identical, 14 import-only, 13 fork logic, ten new files); "For the next syncer" has no gg4 paragraph (line 220 already carries two gg4 sentences inside the gg3 paragraph); skip inventory (18, re-run both greps); coverage audit and path resolution (24 E-31, E-32 forms).
- **Raw numbers preview at HEAD** (`git diff --shortstat`, will not change unless `src`, `build.gradle.kts`, `gradle` or `.github` changes): `v1.7.0 → HEAD` wide scope **277 files, 29679 insertions, 643 deletions**; `src/main` **37 files, 2877 insertions, 241 deletions**; against gg3: 174 files, 7270/483 (wide) and 12 files, 792/256 (`src/main`). `v1.7.0 → a6f1df83` differs by 2 files, 11 insertions, 9 deletions in the wide scope, so the upstream tail is inside the "after gg4" figure; keep the `v1.7.0` baseline for continuity and say so in one sentence. [VERIFIED: git]
- **Known costs:** items 10 and 11 are already "closed" text; item 6 already has the Jackson closure. Check each for a stale "published" tense (24-REVIEW IN-02).
- **Carry check:** the existing rows for 999.4/999.5/WRT-17..19/DEBT-17 were written by their phases; Phase 29 edits only the stale text above, the raw numbers and completeness (ROADMAP "Files shared across phases").

### R9. Review (PUB-12) and carried findings (D-05)

**Reviewer scope:** `docs/ROLLOUT-v1.7.0_gg4.md`, `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/ROLLOUT-v1.7.0_gg2.md` (named in the prompt, D-02), `docs/ROLLOUT-v1.7.0_gg3.md` if edited, the `1.7.0_gg4` CHANGELOG section, `docs/FORK-DELTA.md`, the close record. Give it the closest-reading instructions of Phase 24 (every command and query against its evidence ID; overclaims; cross-document agreement; no secret, host or record value; no `TRUNCATE`; rollback uses `read-tree` and the evidence proves it; released CHANGELOG sections unchanged), the heading rule (`### <CR|BL|WR|IN>-NN: title`, counts equal headings), and ID continuity per prefix across rounds. Reviewer-proposed fix text is checked against code before it is applied (Phase 24: WR-10's suggested key form was wrong). Disposition values are `open`, `fixed`, `skipped`, `deferred`. [VERIFIED: 24-REVIEW-DISPOSITION.md frontmatter; memory notes]

**Open findings to carry (all `open` or `skipped` today):**

| Source | IDs | Count |
|--------|-----|-------|
| 25-REVIEW | WR-01, WR-02, WR-03, IN-01, IN-02, IN-03, IN-04 | 7 open |
| 26-REVIEW | IN-02 (`skipped`: production dead code kept only for tests) | 1 skipped; the other 14 are `fixed` |
| 27-REVIEW | WR-01 (unknown-enum-label failure is not actionable; needs a user decision on a pass-through type), IN-01, IN-02, IN-03 | 4 open |
| 28-REVIEW | IN-01, IN-02, IN-03, IN-04, IN-05 | 5 open (WR-01..WR-04 `fixed`) |

**Doc-side findings Phase 29 can close (D-05 allows only these):**
- 28-IN-03 (a missed rename is silent): promote the rename notice into the CHANGELOG **Important Changes** and the note's headline; closes its documented fix option.
- 28-IN-02 (`SinkTaskStatistics` absent from the coverage list): its FORK-DELTA option "record why it is out of scope" is doc-side; the list itself is `build.gradle.kts` (no edit).
- 28-IN-04 (hedges "expected to write" / "not measured"): droppable only after a live one-line `DESCRIBE` for backspace, form feed, NUL and BEL, in `29-COMMAND-EVIDENCE.md`; the pin and `RawNameMatrix` entries are test-side and stay carried.
- 25-IN-04(d) (`pre-release.yaml` pins Gradle `7.4.2`): record in FORK-DELTA/Future Requirements text (doc-side); (c) `distributionSha256Sum` and (a) the guava line are build-side and carried.
- 27-WR-01: the note must tell operators the truth about what they see (class and column, no lookup-miss text); the code decision stays carried with the user's choice recorded.
Everything else gets reason, owner and re-open trigger in `29-MILESTONE-CLOSE-RECORD.md` (shape: 24-MILESTONE-CLOSE-RECORD.md: `## Carried review advisories` with `### <review> <ID>: title` plus **Reason**, **Owner**, **Re-open trigger**, **Source**; fulfilled section; `## Open user items after publication` with **Steps**, **Why it matters**, `Outcome:`; `## Milestone audit`; `## Housekeeping`; "only decisions are written here"). Also list the v2 requirements as "next milestone" lines [VERIFIED: Read of REQUIREMENTS.md lines 100-114]: DEPS-F1 (client 0.10.0, PR #4), DEPS-F2 (protobuf-java 4.x, PR #10), DEPS-F3 (Kotlin DSL deprecations under Gradle 9.8.1), DEPS-F4 (upstream workflows on old actions, which includes the `pre-release.yaml` Gradle `7.4.2` pin), COV-F1 (client V1 coverage in CI) and NAME-F1 (no WARN for a doubled-form name, which also sits behind 28-IN-03). The 17 items carried by `24-MILESTONE-CLOSE-RECORD.md` were closed by Phases 25-28 (DEBT-13 to DEBT-17, WRT-16 to WRT-19, DESC-07, NAME-01, NAME-04), so nothing from that record remains open.

### R10. Gates and publication: what differs from Phase 24

- **Gate form (D-01):** `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (default environment: `PRE_TAG=pre-v1.7.0-sync`, `UPSTREAM_REF=refs/tags/v1.7.0`, `CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0.txt`) [VERIFIED: Read of the script's defaults]. The range from `54e63648` to HEAD is 774 commits. Phase 24 measured about 9-10 minutes at 514 commits; run it in the background with a long timeout. Run `.planning/scripts/verify-merge-invariants-selftest.sh` with it (Phase 28 E-13: 16 PASS, 0 FAIL). Dry run at HEAD `c1dc6e59` this session (before the VERSION bump, nothing else running): exit 0, `invariants: provenance_paths=21 anchor_commits=760 anchor_rows=94 marker_commits=819 anchor_exempt=19 result=PASS`, wall time 12 min 5 s (15:49:59Z to 16:02:04Z). [VERIFIED: ran `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`] Budget 15-20 minutes at the gate, because the Gradle runs share the machine.
- **Exemptions:** `fork-anchor-exemptions.txt` has 32 lines today (the nine absorbed Dependabot merges' rows). Add rows only if origin moved (OPS-05); never edit the script.
- **Tags:** D-07 and PUB-13 push both tags after the single green run (`pre-v1.7.0_gg4` at `69cd680b`, `v1.7.0_gg4` at the merge). Phase 24 pushed the rollback tag before the merge. Follow D-07. Create `pre-v1.7.0_gg4` locally at the gate (lightweight, at `git rev-parse origin/custom_build`) so the rollback script can name it.
- **Rollback script:** adapt E-31's `rollback-demo.sh` (`gg2` → `gg4`, doc path `docs/ROLLOUT-v1.7.0_gg4.md`, target `pre-v1.7.0_gg4`); run it on the dry-run merge of the finished tip and again on the real merge commit before the push. A linked worktree cannot check out `custom_build`; the script's stand-in branch is a known deviation to record.
- **Pre-push checks:** E-61's `PUSH-READY` (HEAD on `sync/v1.7.0`, no `push.followTags`, no `remote.origin.push` or `mirror`, `git fetch --no-tags`, origin `custom_build` at the expected SHA, no `sync/v1.7.0` and no ref at X on origin, no secret pattern in changed files, API rate limit at least 20). Dirty-tree checks must exclude `.planning/state.json` and `gradlew.bat`.
- **Merge checks on M:** exactly two parents, `M^1` = `69cd680b`, `M^2` = X, `git diff --quiet M X`, X0..X `.planning/`-only, no `TRUNCATE` instruction in the merged tree, rollback re-check on M.
- **After the push:** exactly one `tests.yaml` run on M, zero cancelled, late re-check about two minutes after green, then tags by explicit refspec (`git push origin refs/tags/v1.7.0_gg4:refs/tags/v1.7.0_gg4` and the same for `pre-v1.7.0_gg4`), 60-90 s wait showing the run count unchanged, then `git push origin --delete refs/heads/sync/v1.7.0`, then `FINAL-REFS-OK` (heads: `custom_build` at M, `main`, the two dependabot heads; tags: the 8 plus 2).
- **Dispatch:** `blocking-human` gates and every push run orchestrator-inline; the user's answer is quoted verbatim with the receiver. Local tail records after publication stay on local `sync/v1.7.0` (protected base branch).

## Runtime State Inventory

Not a rename phase, but the `VERSION` bump changes a string that surfaces at runtime.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None: KeeperMap state is keyed `<topic>-<partition>`, not by version. | none |
| Live service config | Dashboards, alert rules and JMX exporter rules that spell `version=v1.7.0_gg3` or the client name `clickhouse-kafka-connect/v1.7.0_gg3`. The repository's `jmx-export-connector.yml` uses `version=(.*)` (line 5) [VERIFIED: grep in a `git archive HEAD` copy; whether it is bundled in the zip was not checked]. | State in Item F; wildcard forms keep working. |
| OS-registered state | None. | none |
| Secrets/env vars | None. No credential is added; the CI-log token (Keychain `gsd17-gh-actions-read`) was deleted by OPS-03 [CITED: 24-RESEARCH R9 hazard 8]; run-level REST reads suffice. | none |
| Build artifacts | `build/generated/sources/version/.../Version.java` is not a task input; `build/confluentArchive` keeps old version directories without `clean` (25-REVIEW IN-01); leftover `/tmp/gsd29*` scratch. | `clean` before the version grep and every archive build; remove scratch at the end. |

## Common Pitfalls

### Pitfall 1: the "independent" review runs inside an executor
**What goes wrong:** Phase 24's plan 24-05 reviewed inline in a `gsd-executor` (no Agent tool), closed at `open: 0`, and the post-publication review found 5 warnings in the published note (the same failure as Phase 15, which shipped its docs unreviewed; quick 261020-vcr republished the corrected note). **How to avoid:** review rounds are orchestrator steps; the orchestrator spawns `gsd-code-reviewer` with a fresh context; executors only apply fixes. **Warning sign:** a plan task of type `auto` that says "invoke the review".

### Pitfall 2: a relayed approval
Executors refuse `blocking-human` answers relayed by another agent. The user answers the orchestrator directly (`push`, `publish`); record the words and the receiver in the evidence. A background-task notification is never an approval.

### Pitfall 3: a tag push cancels the branch run
`tests.yaml` concurrency is `group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}` with `cancel-in-progress: true`, trigger `on: push: branches: ['**']` [VERIFIED: Read of tests.yaml:22-31 and the file header]. A tag push starts no run but joins the SHA's group. Tags only after the branch run is green; never two branches at one SHA.

### Pitfall 4: `git push --tags`
Local-only tags would reach origin. Push by explicit refspec only.

### Pitfall 5: measuring on a tree that then changes
Land the VERSION commit first; measure; write documents; freeze; gate. Only `.planning/` may change after the gate (X0..X).

### Pitfall 6: the anchors gate is slow and shares the machine
10-20 minutes plus the self-test; Phase 28 saw the self-test finish late next to two Gradle runs. Run in the background, read the result file.

### Pitfall 7: OpenJ9 crash under swap pressure
Swap is at 27.1 of 28 GB. An exit 255 with 0 failures in the XML is a JVM crash (`javacore.*` first, then delete `core.*.dmp`, `javacore.*`, `Snap.*.trc`, `jitdump.*` one by one, never `git clean`), re-run once and record both runs; `-PjavaVersion=21` is the recorded escape, and a run on a different JVM must be named as such. CI runs Temurin 17.

### Pitfall 8: `latest` moves
`ClickHouseBase` re-pulls the floating tag (`PullPolicy.alwaysPull()` when the version is `latest`, `ClickHouseBase.java:127-129` [VERIFIED: Read]). Record `docker image inspect` id and date and `SELECT version()` (logged by `NullableTupleMatrixLiveTest`, `DedupTokenRetryLiveTest`) at every gate; a red suite on unchanged code starts with the image date.

### Pitfall 9: overclaims and cross-document drift
Phase 20 and 24 reviews found "byte-identical", "new", "measured" where the evidence said otherwise, a FORK-DELTA/rollout contradiction, a per-column versus per-table error, a rollback bullet that was not hedged to the one retried table, a false "safe" for connectors with three or more topics, and an `exactlyOnce=false` hazard the note denied. Generate shared numbers once and check by grep that CHANGELOG, FORK-DELTA and the note agree.

### Pitfall 10: rollback commands proven only on the rehearsal tip
Prove them on the real merge commit (Phase 16).

### Pitfall 11: nested jars on the rolled-back tree
R5.3. The rollback item must not imply the branch tip builds a usable archive.

### Pitfall 12: the CHANGELOG date and tense
`unreleased` becomes the publication date inside the rehearsed tree. If publishing moves to another day, re-rehearse or the merged tree differs from X0. Write "is published" only for facts true at merge time; the rollout note says the tags "are published together with this release" the way gg3 did.

### Pitfall 13: ROADMAP.md repeats `**Wave N**` headings
Slice to `### Phase 29:` before any scripted edit.

### Pitfall 14: dirty tree files and GSD quirks
`.planning/state.json` and `gradlew.bat` stay unstaged; verify strings exclude them. GSD verbs: space form first; `commit` adds no trailer; zsh `echo` mangles JSON.

### Pitfall 15: scratch builds without `clean`
A leftover `build/confluentArchive/<old version>` directory gets zipped too (25-REVIEW IN-01); every archive build in this phase runs `clean createConfluentArchive`.

## Code Examples

### Release-identity check (run in the final tree)
```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileJava
grep -q 'ARTIFACT_VERSION = "v1.7.0_gg4"' build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
```

### Archive compare at the release tip (zips from `clean createConfluentArchive` at each tree)
```sh
python3 -I .planning/scripts/compare-confluent-archives.py <gg3.zip> <gg4.zip> .planning/scripts/confluent-archive-allowlist-29.txt
# exit 0 PASS, 1 FAIL (UNALLOWED or STALE-ROW), 2 unusable input
```

### Freeze, OPS and CI reads (unauthenticated)
```sh
git ls-remote origin custom_build                                   # must print 69cd680bf548bfb0329142752d37f8a6c3190c26
R=https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect
curl -sS "$R/labels/dependencies" -o /dev/null -w '%{http_code}\n'  # 200
curl -sS "$R/pulls?state=all&per_page=100&sort=created&direction=desc" -o pulls.json
curl -sS "$R/actions/workflows/integrationTests.yaml/runs?event=schedule&per_page=5" -o sched.json
curl -sS "$R/actions/runs?head_sha=<sha>"                            # exactly one tests.yaml run, none cancelled
```

### SQL-level mechanism check with `clickhouse-local` (pipe the script on stdin; 26.9.19.9)
```sh
docker run --rm -i --entrypoint bash clickhouse/clickhouse-server:latest <<'EOF'
CL="clickhouse-local --path /tmp/chl"
$CL --query "CREATE TABLE t (c Tuple(size UInt16, v String)) ENGINE=MergeTree ORDER BY tuple() SETTINGS non_replicated_deduplication_window=100"
printf '\x07\x00\x03abc' | $CL --query "INSERT INTO t SETTINGS insert_deduplication_token='tok-0-0-0' FORMAT RowBinary"   # lands
printf '\x03abc'         | $CL --query "INSERT INTO t SETTINGS insert_deduplication_token='tok-0-0-0' FORMAT RowBinary"   # Code: 33 CANNOT_READ_ALL_DATA
printf '\x07\x00\x03abc' | $CL --query "INSERT INTO t SETTINGS insert_deduplication_token='tok-0-0-0' FORMAT RowBinary"   # accepted, dropped
$CL --query "SELECT count() FROM t"                                                                                         # 1
EOF
```
Measured output this session: `full insert rc=0`, count `1`, `short retry rc=33` with `Code: 33 ... Cannot read all data. Bytes read: 1. Bytes expected: 98: (at row 1)`, count `1`, `same-bytes retry rc=0`, count `1`. [VERIFIED]

### Merge and publish (orchestrator, after the user's own answers; shapes from 24 E-71, E-72)
```sh
# throwaway worktree on local custom_build, fast-forwarded to the pre-merge tip
git -C <wt> merge --ff-only 69cd680bf548bfb0329142752d37f8a6c3190c26
git -C <wt> merge --no-ff -F <message file> <X>
git -C <wt> push origin refs/heads/custom_build:refs/heads/custom_build      # only after MERGE-SHAPE-OK and the rollback re-check on M
# after exactly one green tests.yaml run on M and the late re-check:
git push origin refs/tags/v1.7.0_gg4:refs/tags/v1.7.0_gg4
git push origin refs/tags/pre-v1.7.0_gg4:refs/tags/pre-v1.7.0_gg4
git push origin --delete refs/heads/sync/v1.7.0                              # only after both tags
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Shadow 8 unpacks a jar given to `from(...)` | Shadow 9 copies it as-is; unpack with `configurations.add(...)` | Shadow 9.x [CITED: FORK-DELTA row 67, 25 E-09] | A green build can ship 25 nested jars; `verifyConfluentArchive` and the CI step now fail it. |
| Kafka LIST configs accepted nulls and duplicates | stricter `ValidList` validation | Kafka 4.2.0 (KIP-1161) [CITED: upgrade.md] | Only plugins that use the validators; this connector's keys do not. |
| Release notes cite the artifact of a tag | the operator builds `createConfluentArchive` at the tag; no release assets exist | since gg1 [VERIFIED: `releases` empty] | Rollback names the tag to build. |

**Deprecated/outdated:** the "Gradle 7.4.2" line in `.claude/CLAUDE.md` (actual 9.8.1) and the `pre-release.yaml` pin (`gradle-version: '7.4.2'`, upstream-owned, not changed).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The connector-level behaviour of a gg3 retry of landed 999.4 rows matches the SQL-level measurement (parse failure for the shapes that fail) | R4 | The note would mis-describe the rollback mechanism; the plan's live pin settles it |
| A2 | Records already in a topic are processed by gg4 under the new name rule whatever their age (backlog consequence of the per-record measurement) | R4, finding 3 | The precondition would be too strong; one live pin with an old-name record produced before the upgrade settles it |
| A3 | The `enableDbTopicSplit` key rule stays "read from the connector code" unless the plan pins it live | R7 | A mislabelled claim; the gg3 review already accepted the label |
| A4 | Patch bumps of jackson, gson, httpclient5, httpcore5 and guava change no byte sent to ClickHouse | R4 | A hidden in-flight difference; the green JSON-route live tests make it unlikely |
| A5 | NVD, OSV and GitHub records stay as read between this research and the gate | R2 | The note's advisory sentences go stale; re-read at the gate |
| A6 | The CHANGELOG date equals the publication day | R8, Pitfall 12 | A re-rehearsal if publication slips |
| A7 | The companion detection predicates also pass on the plan's full fixture set (previewed on seven tables) | R6 | A look-alike gets flagged; the plan proves it |
| A8 | A different JVM (HotSpot 21) gives the same suite result when OpenJ9 crashes | Pitfall 7 | Recorded as a different run, not the standard toolchain |
| A9 | `pre-release.yaml` cannot configure the current build on Gradle 7.4.2 | Outdated list | Only a note sentence; 25-REVIEW IN-04 asserts it, nobody ran it |
| A10 | A new Dependabot PR may open before the gate (limit 5 open PRs); merging one is the user's action, and the freeze check catches it | R1 | Opening is harmless; a merge triggers the OPS-05 branch |

## Open Questions (RESOLVED)

All seven questions are resolved at plan time. Each resolution is the default recorded in the "Decisions taken at plan time" table of `29-01-PLAN.md` (PT-03 to PT-09) and is carried by the plan task named on its `RESOLVED:` line. They are plan-time defaults: the user may overrule any of them, and an overrule re-opens only the tasks named.

1. **The locked rollback target builds a broken archive (D-03, finding 1).**
   - What we know: `pre-v1.7.0_gg4` = `69cd680b` builds 25 nested jars with a green build. D-03 sends operators to the gg3 artifact, so no operator is misled if the note says so.
   - What's unclear: whether the user wants the branch rollback to target something that builds correctly.
   - Recommendation: keep D-03 (it is locked), word the hazard in "Rolling back brings back" and in the rollback step, and measure it in the evidence. Ask the user at plan review only if they want a different target.
   - RESOLVED: D-03 kept as locked (PT-03). The hazard is measured as `PRE-NESTED-JARS=` in E-04 (29-01 Task 2), and the note's item 7 says to redeploy the archive built at tag `v1.7.0_gg3` and never to build one from the rolled-back branch tip (29-04 Task 1).
2. **Detection query for 999.5 (ROLL-04 says "unchanged").**
   - What we know: the gg3 query misses root carriage-return names and any name with a double quote (R6).
   - Recommendation: keep the gg3 query unchanged and add a proven companion query (carriage return and double quote), stating that the double-quote tables were unmapped before gg4 and are now mapped. The alternative is to state the two gaps.
   - RESOLVED: PT-04. The gg3 query is kept byte for byte and a companion query is added, both proven on fixtures and look-alikes in E-22 (29-03 Task 2); the note's item 1 quotes both (29-04 Task 1).
3. **Enum labels in the note.** L-63 and L-64 are operator-visible and their ledger destination is ROLL, but ROLL-04's list does not name them. Recommendation: include a short Item A paragraph with the measured label behaviour; add a label query only if it is proven on fixtures.
   - RESOLVED: PT-05. A short paragraph in the note's item 1 (29-04 Task 1). A label query is quoted only if E-23 (29-03 Task 2) shows it lists exactly the measured label oracle plus the over-flags on a `LABELQ-OVERFLAG=` line and nothing for the look-alike database; otherwise E-23 carries `LABELQ: none (<reason>)` and the paragraph names the gap and quotes no query.
4. **`docs/ROLLOUT-v1.7.0_gg3.md` line 263 becomes stale after ROLL-05.** Recommendation: add one corrective sentence there in the same commit (it is an operator document in the reviewer's scope) and say so in the gg3 revision notice; otherwise leave it and let the reviewer decide.
   - RESOLVED: PT-06. The corrective sentence at line 263 and one sentence in the gg3 revision paragraph land in the same commit as the gg1 and gg2 corrections (29-03 Task 3), inside the rehearsed tree (D-02).
5. **Carry-forward doc-side fixes (R9).** Recommendation: fix 28-IN-03 (CHANGELOG promotion), 28-IN-02 (FORK-DELTA sentence) and 25-IN-04(d) (FORK-DELTA/Future Requirements sentence) in Phase 29; fix 28-IN-04 only if the one-line `DESCRIBE` is measured; carry the rest with owner and trigger.
   - RESOLVED: PT-07, within D-05 (doc-side fixes only). 28-IN-03: rename notice first in the CHANGELOG Important Changes (29-05 Task 1) and in the note's opening (29-04 Task 1). 28-IN-02 and 25-IN-04(d): doc halves written in FORK-DELTA (29-05 Task 2). 28-IN-04: server half measured live in E-24 (29-03 Task 2) and FORK-DELTA's "expected to write" replaced with it (29-05 Task 2), the test-side pin carried. Every other finding is carried with reason, owner and re-open trigger in the close record (29-05 Task 3).
6. **A live worker smoke on `cp-kafka-connect-base:7.9.7`.** The image is on the host. Recommendation: do not add it; label the claim "verified by a static bytecode reference check against the 3.9 API jars, not by a run on a live worker". Add it only if the user wants the stronger statement.
   - RESOLVED: PT-08. No worker smoke is added. The note's item 6 names Apache Kafka 3.9 (`cp-kafka-connect-base:7.9.7`) as the oldest verified worker, worded as a static bytecode reference check plus `ConnectApiReferencesUnitTest`, not a run on a live worker (29-04 Task 3), from the inputs recorded in E-05 and E-06 (29-01 Tasks 3 and 4).
7. **Raw-numbers baseline.** `v1.7.0` (continuity, includes the upstream tail's 2 files) or `a6f1df83`. Recommendation: keep `v1.7.0` and add the one-sentence footnote.
   - RESOLVED: PT-09. The baseline stays `v1.7.0`, with one sentence that upstream's tail `a6f1df83` sits inside the after-gg4 figure (29-05 Task 2).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | live probes, suite gate | yes (Running) | Docker 29.8.2, colima profile `vz`, 8 CPUs, 16 GiB | `colima start --profile vz` (never bare) |
| `clickhouse/clickhouse-server:latest` | live tests, `clickhouse-local` | yes (local) | 26.9.19.9, `bdb5c911a23d`, 2026-10-14 | a pull moves `latest`; record date and `SELECT version()` |
| Gradle wrapper and toolchain | build, tests | yes | 9.8.1; JDK 17 toolchain (Adoptium 17 in `~/.gradle/jdks`, Semeru 17.0.9 local) | `-PjavaVersion=21` (Homebrew OpenJDK 21.0.12.1) |
| `gh` | CI reads | **no** (by design) | - | unauthenticated REST (60 requests an hour; 55 left after the first call of this session) |
| CI-log token | job logs only | **no** (deleted by OPS-03) | - | run-level REST suffices; ask the user for a new read-only token only if a log is needed |
| `yq` | AGENTS.md matrix lookup | no | - | not needed (target is `latest` x V2) |
| Network: `api.github.com`, `repo1.maven.org`, `archive.apache.org`, `raw.githubusercontent.com`, `api.osv.dev`, `services.nvd.nist.gov` | re-checks | yes | - | web UI |
| `timeout` (coreutils) | none | no | - | use `until` loops with `run_in_background` |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** the CI-log token.

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json`. The phase's proof is the suite gate (R1 baselines, R10), the evidence transcripts and the review rounds.

## Security Domain

`security_enforcement` is on (ASVS level 1, block on high).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1 Architecture / V10 Malicious Code (supply chain) | yes | no new package; entry-by-entry archive compare with allow-list; `verifyConfluentArchive`; advisory identity re-read from NVD, OSV and GitHub, no exposure claim |
| V2 Authentication | no | no new auth; any CI-log token is user-made, Keychain only, never in chat or files |
| V4 Access Control | yes | the user's own approval answers, orchestrator-inline; protected base branch; no force push; tags by explicit refspec |
| V5 Input Validation | yes (docs) | placeholders only in quoted commands; read-only SQL in the note; `grep -F` literal matching |
| V7 Error Handling and Logging | yes | value-free error text is the point of L-52 and L-54; evidence and tests use sentinels only |
| V8 Data Protection | yes | no record value, host, credential or token in any committed file; E-61 secret-pattern scan on changed files |
| V14 Configuration | yes | `tests.yaml` pins unchanged; `integrationTests.yaml` stays upstream-identical and disabled; no `TRUNCATE` instruction |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Relayed or fabricated approval triggers a publish | Spoofing / Elevation | gate runs inline; the user's answer is quoted verbatim with the receiver; an agent or notification message is not approval |
| Tag push cancels the branch run | Tampering (of evidence) | tag only after the single run is green; late re-check |
| Wrong tip published (a Dependabot merge slipped in) | Tampering | re-read origin before the gate and right before the merge; a moved tip means re-absorb, new exemption rows, redo the gates |
| A broken archive passes as green | Tampering | `verifyConfluentArchive`, the CI archive step, the allow-listed compare; rollback names the tag to build (R5.3) |
| Record data in committed evidence or a DLQ transcript | Information Disclosure | sentinels only; secret-pattern scan |
| A rollout command that destroys other connectors' state | Tampering | no `TRUNCATE` or unscoped `DELETE` of the shared KeeperMap table; the key-rule queries only read |
| Misleading advisory statements | Repudiation | each fact labelled by the source queried; both disagreeing sources stated; no exposure assessment |

## Sources

### Primary (HIGH confidence)
- Repository files read this session: `29-CONTEXT.md`, `REQUIREMENTS.md`, `STATE.md`, `ROADMAP.md` (Phase 25-29 sections), `24-CONTEXT.md`, `24-RESEARCH.md`, `24-MILESTONE-CLOSE-RECORD.md`, `24-COMMAND-EVIDENCE.md` (E-01, E-02, E-60..E-72), `25-COMMAND-EVIDENCE.md` (E-09..E-12, E-22..E-25), `26-COMMAND-EVIDENCE.md` (E-08, E-09, E-11), `27-BEHAVIOUR-LEDGER.md`, `28-BEHAVIOUR-LEDGER.md`, `28-COMMAND-EVIDENCE.md` (E-13), the four `*-REVIEW-DISPOSITION.md` files and selected `*-REVIEW.md` findings, `docs/ROLLOUT-v1.7.0_gg1.md`, `gg2.md`, `gg3.md`, `docs/FORK-DELTA.md`, `CHANGELOG.md`, `QueryIdentifier.java`, `ClickHouseSinkConfig.java:331`, `ClickHouseBase.java:124-130`, `DescribeTableBuilder.java:35-37`, `DeclaredNullability.java:46-51`, `RecordConvertor.java`, `Processing.java:136-141`, `.github/workflows/tests.yaml`, `release.yaml`, `pre-release.yaml`, `.planning/scripts/fork-anchors.txt:41`, `fork-anchor-exemptions.txt`, `verify-merge-invariants.sh` (header and defaults), `compare-confluent-archives.py` (header), `confluent-archive-allowlist-25.txt` (header).
- Live measurements this session on ClickHouse 26.9.19.9: archive builds and compare (R5), `clickhouse-local` checks (R4, R6), `git` counts and diffs (R1, R8).
- GitHub REST (unauthenticated): `pulls`, `labels/dependencies`, `actions/workflows/integrationTests.yaml/runs?event=schedule`, `actions/runs?head_sha=69cd680b...`, `releases`, `advisories/GHSA-hjcp-jmpx-g3qm`; `git ls-remote origin` and `upstream`.
- NVD `cves/2.0?cveId=CVE-2026-102554`; OSV `vulns/CVE-2026-102554`, `vulns/GHSA-hjcp-jmpx-g3qm`, `POST /v1/query` (guava, httpclient5).

### Secondary (MEDIUM confidence)
- `https://raw.githubusercontent.com/apache/kafka/4.4.0/docs/getting-started/upgrade.md` (4.4.0 and 4.2.0 sections) and `.../clients/.../ConfigDef.java` at tags `4.1.1` and `4.4.0`.
- `https://archive.apache.org/dist/kafka/4.4.0/` (listing and `RELEASE_NOTES.html`); `https://repo1.maven.org/maven2/org/apache/kafka/connect-api/maven-metadata.xml`.
- `https://kafka.apache.org/43/getting-started/upgrade/` (KIP-1161 wording via a summarising fetch).

### Tertiary (LOW confidence)
- none

## Metadata

**Confidence breakdown:**
- Standard stack and tools: HIGH, probed this session.
- Measured behaviour (R4 SQL level, R5, R6 preview): HIGH on 26.9.19.9; connector-level pins are plan work (A1, A2).
- Publication mechanics: MEDIUM, re-used from Phase 24 and not re-run.
- Pitfalls: HIGH, each traced to a recorded incident or a measurement.

**Research date:** 2026-10-23
**Valid until:** 2026-10-30. `latest` and the advisory records can move any day; re-read the image date, `SELECT version()`, `origin/custom_build`, OPS-04 and the advisory sources at the gate.
