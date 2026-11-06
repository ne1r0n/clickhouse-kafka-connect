# Phase 43: Document Fixes & Phase 37 Dispositions - Research

**Researched:** 2026-11-02
**Domain:** published-document corrections, CI-workflow comment fix, and a decision ledger for 18 carried Phase 37 rows (no new libraries)
**Confidence:** HIGH (every row and every line number below was read from the files this session; the only non-verified items are in the Assumptions Log)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md `## Implementation Decisions` reads, verbatim: "### Claude's Discretion - All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions."

### Claude's Discretion
"All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions."

The ROADMAP text that bounds that discretion (and is therefore binding): "A row that would change `src/main` changes only on the user's approval. Without approval, the row is deferred with an owner and a re-open trigger." And the User-Owned Gates table: "A `src/main` disposition row | 43 | DOC-02" and "Changing or removing an existing assertion | any | Tests are only added, never weakened".

### Deferred Ideas (OUT OF SCOPE)
"None — discuss phase skipped."
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DOC-01 | The gg1 half of WR-08 is fixed in `docs/ROLLOUT-v1.7.0_gg1.md`; FORK-DELTA lines 6, 139 and 286 are in past tense; FUT-05 is named by ID in the rollout note. | Section "DOC-01": exact before/after text for gg1 item 6, the three FORK-DELTA paragraphs (located by content, current line numbers 5, 146, 295), the FUT-05 placement decision (recommend the published gg6 note, row C2). |
| DOC-02 | Every Phase 37 row has a disposition with an owner: the `increment-version.yml` header comment, advisory rows 26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01 and 28 IN-05(b), broad allow-list globs (UF-37-3) and review Infos. `src/main` rows need the user's approval. | Section "DOC-02": the full 16-row ledger with a recommended default per row, and the section "Rows needing user approval" with the exact diff of each row. |
</phase_requirements>

## STOP-LEVEL ITEMS (for the orchestrator, before any plan is executed)

1. **Four ledger rows would change `src/main` and need the user's approval in the user's own words** (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01). Each is written out under "Rows needing user approval". **Recommended default for all four: defer.** Do not assume approval. The approval must be asked by the orchestrator, inline; executors refuse relayed approvals (memory: Phase 11 orchestration gotchas).
2. **Five more rows touch a test assertion** (28 IN-05(b); 37 review IN-04; and the test call-site fallout of 26 IN-02, 27 WR-01, 28 IN-01). The gate "Changing or removing an existing assertion" applies to them. They are listed as category B in the same section so one question to the user covers them.
3. **The research flag is settled:** name FUT-05 in the published `docs/ROLLOUT-v1.7.0_gg6.md`, row C2 (DOC-01 is Phase 43's, and only that file can satisfy criterion 1 inside this phase). The gg7 note (Phase 44) should also name it through its RSK-03 section; that is a handoff, not the Phase 43 choice. One caveat needs a user nod: the Phase 38 brief kept planning identifiers out of the gg6 note, and FUT-05 is one (Assumption A1).
4. **Stale published facts beyond the three named lines** (verified today against the public GitHub REST API): FORK-DELTA open items 12, 22 and 23 still read "stays open" / "condition" although PRs #10 and #17 are closed and the gg6 publish run created the tag. Recommended to fix in the same pass (see "Adjacent stale text").

## Summary

Phase 43 is a documents-and-decisions phase. Its only mandatory non-document edit is one word in a workflow comment (`ReleaseWorkflowGuardUnitTest` to `IncrementVersionWorkflowGuardUnitTest`, `.github/workflows/increment-version.yml` line 5). Everything else is: (a) three prose corrections in already-published documents, (b) one ledger of decisions, (c) an approval conversation the orchestrator owns, and (d) a gate that proves nothing else moved.

The research found four things the ROADMAP text does not say. First, the gg6 close's "FORK-DELTA lines 6, 139, 286" are now lines 5, 146 and 295 (Phases 39-42 added rows), and the "last two commands beneath the table" for the gg6 raw numbers still say `HEAD`; at today's HEAD they print 345/42389/1221 and 43/3183/294, not the table's 331/39665/1161 and 40/2989/272. Pointing them at the tag `v1.7.0_gg6` reproduces the row exactly (measured). Second, FORK-DELTA already names FUT-05 by ID (open items 17 and 24, written in Phase 42), so the open point is the operator note, not FORK-DELTA. Third, the `37-ADVISORY-LEDGER.md` left 18 `dispositioned` rows; the ROADMAP names only 5 of them, and one of the other 13 (30-IN-04) has a finished phase (Phase 38) as its owner, which is an orphan. Fourth, `DeclaredNullabilityFixtures` carries the rule "It must never hold a copy of `DeclaredNullability.ESCAPED_NEWLINE`; tests read the production constant", so the usual fix for 28 IN-01 (move the constant to test sources) contradicts a recorded decision; that is a reason to defer, not to fix.

**Primary recommendation:** Fix now only what is comment/text/test-helper-only and has no release-path runtime effect (the header comment, the four allow-list header comments, the `ConfluentArchiveCheck` zero-jar hint with its new test, and the doc corrections); defer every `src/main` row and every row that edits the release path's behaviour, each with owner `fork maintainer (the user)` and a concrete re-open trigger; write the ledger as `43-DISPOSITION-LEDGER.md` in the 37-ledger format and check it with a copy of `37-ledger-check.py`.

## Project Constraints (from CLAUDE.md)

- `./CLAUDE.md` is `@AGENTS.md`; `.claude/CLAUDE.md` is the project file. Actionable directives: Java 17 / Gradle Kotlin DSL; Google-Java-style via Spotless; "Always run `spotlessApply` after making changes" (**overridden here** by the user instruction: never run `spotlessApply` over the whole tree, 49 files carry pre-existing violations, see Spotless baselines); "Do NOT make commits to the branch without explicitly asking the user" (the GSD workflow commits are the phase workflow; pushes, the merge and `custom_build` stay with the user); "Do not change the PR title or description"; "Deprecated libraries / CVE: do not upgrade dependencies without asking" (nothing is upgraded here); "Existing tests: only add new tests, never delete/weaken existing ones"; "Useful code comments: only write comments that convey important context" (applies to any approved Javadoc line in `src/main`); `VERSION`/`CHANGELOG.md` bump (Phase 44 owns them, Phase 43 does not bump).
- Task-level rules from the orchestrator: this is the last phase that may edit `.github/`, tests, or `src/main` (with approval); published notes edited here go into Phase 44's independent review; `h` is a zsh alias, so use `bash -c`; `src/main` comments must not carry planning IDs (`SourcePlanningIdGuardUnitTest`, pattern quoted in Pitfall 5).
- GSD workflow: all repo edits go through GSD commands (this phase is run by `/gsd-execute-phase`).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Operator-facing corrections (gg1 item 6, gg6 row C2) | Published docs (`docs/ROLLOUT-*.md`) | Phase 44 review | Operators read these; no test reads them (grep of `src/test` finds only a message string that points at row D7 of the gg6 note) |
| Maintainer-facing corrections (FORK-DELTA) | Published docs (`docs/FORK-DELTA.md`) | `fork-anchors.txt` (does not anchor docs) | Read by the next upstream syncer |
| Header comment of `increment-version.yml` | CI workflow file (`.github/workflows`) | Guard tests in `src/test/.../ci` | Comment line; guard classes read non-comment lines |
| Row decisions and owners | Planning record (`43-DISPOSITION-LEDGER.md`) | FORK-DELTA items 19/20 mirror it | Same format and checker as the Phase 37 ledger |
| `src/main` rows | Connector source | Test sources, `fork-anchors.txt`, FORK-DELTA, CHANGELOG, gg7 allow-list | Only on approval; each touches all five artifacts |
| Approval capture | Orchestrator (inline, user's own words) | Ledger records the verbatim quote | Executors refuse relayed approvals |

## Standard Stack

No library is added or changed. [VERIFIED: nothing in this phase installs a package]

| Tool | Version (this session) | Purpose |
|------|------------------------|---------|
| Gradle wrapper | 9.8.2 (task output `docs.gradle.org/9.8.2`) | `testUnit`, `test`, `spotlessJava` |
| JDK for gates | 21.0.12.1 Homebrew (`-PjavaVersion=21`) | default JDK 17 here is IBM Semeru (OpenJ9), which crashes under Mockito inline mocking (memory: OpenJ9 crash) |
| `python3 -I` | system | `fork-anchors` mirror, ledger checker, grep-style doc checks |
| JUnit 5 | 5.11.3 (project catalog) | Docker-free guard tests |

**Package Legitimacy Audit:** not applicable, no external package is installed in this phase. Packages removed due to SLOP: none. Flagged SUS: none.

## DOC-01: the document fixes

### 1. `docs/ROLLOUT-v1.7.0_gg1.md`, item 6 (the gg1 half of WR-08)

The text sits at line 242 (inside "## Item 6: Compression", line 234). [VERIFIED: docs/ROLLOUT-v1.7.0_gg1.md:242, read this session] The defective sentence, verbatim:

> Correction for client `0.11.0`: on V2 of client `0.11.0` that combination decodes ZSTD and loads the `zstd-jni` native, which needs an executable temp folder (an executable `java.io.tmpdir`, or `-DZstdTempFolder`; measured in the public confluentinc/cp-kafka-connect-base:7.9.7 image); on client `0.9.6` and on client V1 of any line it still fails with `Invalid LZ4 magic byte` or `Magic is not correct`.

What the evidence supports, from the gg6 note rows [VERIFIED: docs/ROLLOUT-v1.7.0_gg6.md D9 and D13, read this session]: D13 labels the ZSTD decode on V2 of 0.11.0 `measured` (`NetworkCompressionPinLiveTest`, `Client011BehaviourPinLiveTest`) and says client V1 "cannot decode ZSTD on either client line"; D9 labels worker behaviour (the `noexec` failure, the remedies) `measured in research (not re-run)`. Only two lines were observed (`0.9.6` and `0.11.0`). The 38-REVIEW WR-08 fix text is the recommended replacement, with one change: the gg1 note does not define the label `measured in research (not re-run)` (the gg6 note does), so spell the meaning out and point at the gg6 rows.

Recommended replacement of the sentence:

> Correction for client `0.11.0`: on V2 of client `0.11.0` that combination decodes ZSTD (measured) and loads the `zstd-jni` native, which needs an executable temp folder (an executable `java.io.tmpdir`, or `-DZstdTempFolder`; measured by the maintainers in research, in the public confluentinc/cp-kafka-connect-base:7.9.7 image, and not re-run on the release tip; see rows D9 and D13 of [`ROLLOUT-v1.7.0_gg6.md`](ROLLOUT-v1.7.0_gg6.md)); on client `0.9.6`, and on client V1 on both client lines checked (`0.9.6` and `0.11.0`), it still fails with `Invalid LZ4 magic byte` or `Magic is not correct`.

Also edit the preface (line 3), whose last sentence reads "The compression warning of item 6 is also qualified for client `0.11.0`; the qualification sits in that item." Add the fact that this copy's item 6 labels differ from the tagged copy, mirroring the preface convention the note already uses for items 1 and 2. [VERIFIED: docs/ROLLOUT-v1.7.0_gg1.md:3 preface wording] Searching `docs/` and `CHANGELOG.md` for "of any line" finds exactly one hit, gg1 line 242 (the FORK-DELTA half was already fixed in Phase 38: line 299 reads "on client V1 on both client lines checked (`0.9.6` and `0.11.0`)"). [VERIFIED: grep, docs/FORK-DELTA.md:299]

### 2. FORK-DELTA: the three paragraphs the ROADMAP calls lines 6, 139 and 286

Locate by content. Today's line numbers were read with `grep -n` this session; the tag `v1.7.0_gg6` has the same paragraphs on the same lines as HEAD for 5 and 146/295 shifted only by Phase 39-42 rows (the ROADMAP's "6" is the first body paragraph, which starts "Every row below was checked", line 5 here). [VERIFIED: git show v1.7.0_gg6:docs/FORK-DELTA.md, sed of lines 1-8 and 139/286; grep -n at HEAD]

| Paragraph (by content) | HEAD line | Phrase to put in past tense | Facts for the rewrite |
|---|---|---|---|
| "Every row below was checked against the merged and hardened tree ..." | 5 | `(the tree that `VERSION` names; the git tag and the GitHub release are created by the release workflow at the merged SHA)` | Tag and release were created by publish run 41298074815. After Phase 44 bumps `VERSION`, "names" would be wrong too, so write "named at the merge". |
| "The table gives files changed, insertions and deletions ..." (under "The raw numbers") | 146 | `the release merge publishes that tree unchanged` (the gg5 clause) and `because the tag `v1.7.0_gg6` does not exist before the release workflow creates it` | The gg6 tag exists. Trees of merge `72e8ccff` and its second parent `a46be37f` are equal over `src build.gradle.kts gradle .github` (`git diff --quiet` exits 0, measured). |
| "The release after that is `v1.7.0_gg6`, published from `sync/v1.7.0` ..." | 295 | `are meant to be created by the release workflow ... whether the workflow does so is the open condition of item 22`; `which is written from the final tree before the merge and does not exist before that: with `VERSION` at ... the release workflows stop at ...`; `The release publishes the open items 12 to 23` | Publish run 41298074815, `Release custom_build (publish)`, `workflow_dispatch`, head `72e8ccff`, conclusion success; release id 430511927 by `github-actions[bot]`, one asset of 13558692 bytes; `pre-v1.7.0_gg6` pushed by hand afterwards at `3a3a15a9`. |

The facts in the right column are [VERIFIED: public GitHub REST API read-only GET on 2026-11-02: `actions/runs/41298074815` gives name `Release custom_build (publish)`, event `workflow_dispatch`, head_sha `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, status `completed`, conclusion `success`; `releases/tags/v1.7.0_gg6` gives id `430511927`, draft false, prerelease false, author `github-actions[bot]`, asset `clickhouse-kafka-connect-v1.7.0_gg6.zip` size 13558692] and agree with `38-RELEASE-RECORD.md`. [VERIFIED: git ls-remote origin: refs/tags/v1.7.0_gg6 is 72e8ccff..., refs/tags/pre-v1.7.0_gg6 is 3a3a15a9...] Re-run the two GETs at execution start (unauthenticated, 60 requests an hour; `gh` is not installed locally per STATE.md).

**Reproduction-command drift (found, not named by the ROADMAP).** The two commands under the raw-numbers table that reproduce the gg6 row are `git diff --shortstat v1.7.0 HEAD -- ...`. [VERIFIED: docs/FORK-DELTA.md lines 175-179 region, read this session] At HEAD they print `345 files changed, 42389 insertions(+), 1221 deletions(-)` and `43 files changed, 3183 insertions(+), 294 deletions(-)`; against the tag they print `331 ... 39665 ... 1161` and `40 ... 2989 ... 272`, the table's gg6 row. [VERIFIED: git diff --shortstat, run this session] So in the line-146 rewrite, repoint those two commands to `v1.7.0_gg6`. The other `HEAD`-relative reproduction commands (line 184 `v1.7.0_gg4 HEAD`, line 186 `v1.7.0_gg5 HEAD`, line 188, line 291) have the same drift. `git diff --shortstat v1.7.0_gg5 v1.7.0_gg6` reproduces line 186's `79 files, 7939, 478` and `7 files, 112, 31` (measured). Recommended split: Phase 43 repoints the gg6 row and line 186 (both describe the published gg6 release and the tag now exists); Phase 44's "raw numbers refreshed once" owns the rest and the new gg7 row. Planner may narrow to the gg6 row only.

### 3. Adjacent stale text (same class: facts that changed at or after the gg6 publication)

| FORK-DELTA item | Stale text | Fact | Recommended edit |
|---|---|---|---|
| 22 | "No workflow run had taken `release.yaml` through its publish step as of the `v1.7.0_gg5` release (condition) ... The caveat closes only when a publish-mode run ... completes its `Publish release` step and that run is recorded." and a path `.planning/phases/37-ci-hardening-advisory-debt/37-RELEASE-RECORD.md` that moved | `38-RELEASE-RECORD.md`: "ADV-03 caveat ... closed. The step `Publish release` ran to the end under a workflow token, created the tag and the release" | Retitle "(closed)", state the run and release ids, repoint the path to `.planning/milestones/v1.7.0_gg6-maintenance-phases/...`. Line 295 cross-references this item, so editing 295 alone would leave a contradiction. |
| 12 and 23 | "stays open on GitHub until the user closes it or a Dependabot run does" | PR #10 closed 2026-11-01T10:26:38Z and PR #17 closed 2026-11-01T10:28:35Z, both `merged: false` [VERIFIED: GitHub REST `pulls/10`, `pulls/17`, 2026-11-02]; `38-RELEASE-RECORD.md` addendum: closed by `dependabot[bot]` | Past tense with the close dates. Do not touch PRs #21 and #22 (still open on `origin`; user's, and absorbed locally in Phase 39). |
| 19, 20 | open, wrong path `.planning/phases/37-.../37-ADVISORY-LEDGER.md` | follows the ledger outcome | Rewrite after the ledger exists (see Plan shape). |
| 21 | open, condition "the publication record states that the merged SHA's only `tests.yaml` run is the `custom_build` push" | `MILESTONES.md`: "one green `tests.yaml` run (`41297502026`)" on the published SHA | Optional: record that the condition was met for gg6, keep the item open (the owner decision on 30-IN-04 stays the user's). |

### 4. FUT-05: which file names it by ID (research flag, settled)

Facts: FUT-05 is a Future Requirement of the gg6 milestone: "A retry budget or a 'remove from default list' setting for retriable codes." [VERIFIED: .planning/milestones/v1.7.0_gg6-maintenance-REQUIREMENTS.md:84] FORK-DELTA already names it twice: item 17 ends "(FUT-05)" and item 24 is titled "...; FUT-05 stays open (published knowingly open)". [VERIFIED: docs/FORK-DELTA.md items 17 and 24, read this session] The gg6 note carries no requirement ID at all: a grep for `OI|FUT|WR|IN|RSK|...-NN` over the gg4, gg5 and gg6 notes and CHANGELOG finds none. [VERIFIED: grep this session] `MILESTONES.md` records the gap as "FUT-05 is not named by ID in the rollout note" (the gg6 note). The gg6 note's row C2 is that "FUT-05 rollout item": it says only transient codes belong in the list, no retry budget, outside `errors.tolerance` and the DLQ. [VERIFIED: docs/ROLLOUT-v1.7.0_gg6.md C2, 34-COMMAND-EVIDENCE.md E-10 key `fut05_rollout_item`]

**Recommendation: edit the published `docs/ROLLOUT-v1.7.0_gg6.md`, row C2 (and the one-line pointer after the "Before you upgrade" list).** Reasons: (1) DOC-01 and success criterion 1 belong to Phase 43 and are `[record]` here; the gg7 note does not exist until Phase 44, so choosing it would leave DOC-01 unprovable at Phase 43's close; (2) the gg6 note is the one whose C2 row states the rule an operator on the way to gg7 reads; (3) Phase 39 already edited the published gg6 note once (commit `12379bf1`, row D7), so a post-tag edit has a precedent, and the Phase 44 review covers "the published notes edited in Phase 43"; (4) FORK-DELTA already names FUT-05, so the ID resolves for a reader.

Recommended text for the end of row C2 (label unchanged, `read from the code`): "No retry budget exists and no setting removes a code from the built-in set; that follow-up is tracked as FUT-05 in [`FORK-DELTA.md`](FORK-DELTA.md) (Known costs and open items)." Do not point at item 24 from the gg6 note: item 24 describes a gg7 behaviour (the start-up WARN).

Two consequences to carry: (a) add a short correction notice at the top of the gg6 note, like the gg1 preface ("This copy corrects ... of the copy in tag"), listing both post-tag edits (row D7 from Phase 39 and row C2); the gg6 note differs from its tag today without saying so [VERIFIED: `git diff v1.7.0_gg6 HEAD -- docs/ROLLOUT-v1.7.0_gg6.md` shows one changed row, D7]; (b) Phase 44's gg7 note should state "FUT-05 stays open" in its RSK-03 section (RLS-03 requires RSK-03 coverage; the requirement text does not list FUT-05 itself), and its reviewer brief should say that the gg6 note's one planning ID is deliberate.

## DOC-02: the ledger

### Method

One row per Phase 37 item named by DOC-02: the header comment (1), the five advisory rows (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01, 28 IN-05(b)), the broad-glob row (UF-37-3), and each open review Info. The open Infos are the six of `37-REVIEW.md` (IN-01..IN-06) and the three of `37-REVIEW-UF.md` (IN-01..IN-03). All nine were re-checked against HEAD and are still open (no commit since `v1.7.0_gg6` touched `.github/scripts/compare_confluent_archives.py`, `ReleaseWorkflowGuardUnitTest.java` or `ConfluentArchiveCheck.java`; the allow-list headers, the `release.yaml` regex and the comment are unchanged). [VERIFIED: git log -- those paths shows last commits 2bec20d8, f523cf3c, e1e05604, all Phase 37] 37-REVIEW-FIX closed only WR-01..WR-04; 37-REVIEW-UF-FIX closed only UF WR-01. [VERIFIED: 37-REVIEW-FIX.md, 37-REVIEW-UF-FIX.md]

Owner of every deferral: `fork maintainer (the user)`, as in the Phase 37 ledger. Policy behind the defaults: fix now only what is comment/text/test-helper-only with no release-path runtime effect; Phase 44 publishes through `release.yaml` and `.github/scripts` and no later phase may repair a CI-only defect (Phase 29 D-05), so behaviour edits of those files are deferred.

### The ledger (recommended dispositions)

| Row ID (proposed) | What it is | Touches | Recommended default | Owner | Re-open trigger (deferrals) |
|---|---|---|---|---|---|
| 37-HDR | `increment-version.yml` line 5 names `ReleaseWorkflowGuardUnitTest`; the class that pins the file is `IncrementVersionWorkflowGuardUnitTest` | `.github/workflows/increment-version.yml` (comment) | **Fix now.** Exact diff in "Header comment". FORK-DELTA item 19 becomes closed. | - | - |
| 26-IN-02 | `ClickHouseHelperClient.shouldSkipDescribeRow` (two overloads) has no caller in `src/main`; it is an anchor row (count 3, backs HLP-02) | `src/main` | **Defer** (needs approval; cost exceeds value, see approval section) | user | the next edit of the `shouldSkipDescribeRow` anchor row or of the describe code of `ClickHouseHelperClient`, or an upstream sync that touches that method |
| 27-WR-01 | unknown enum label throws `IllegalArgumentException`, wrapped with withheld text, so the operator sees no cause | `src/main` + two existing test assertions | **Defer** (needs approval; the one `src/main` row with operator value, so ask) | user | an operator report that confuses the withheld text with a defect, the user's decision on a pass-through exception type, or the next edit of `Column.convertEnumValues` |
| 27-IN-01 | `passesThroughUnchanged` returns on `ConnectException`/exact `RuntimeException` before the Jackson check; the exclusion is a property of the call sites | `src/main` (Javadoc only) | **Defer** (needs approval; zero behaviour change) | user | the next edit of the Map, Array or Tuple write lambdas or of `passesThroughUnchanged` |
| 28-IN-01 | `DeclaredNullability.ESCAPED_NEWLINE` is a public constant that only tests read | `src/main` + 4 test files | **Defer** (needs approval; the "move to fixtures" fix contradicts a recorded rule) | user | the next edit of `DeclaredNullability` or its fixtures |
| 28-IN-05(b) | the null assertion of `DescribeUnreadableEnumMockServerTest` is satisfied by any exception in the describe, not only the enum scan | test assertion | **Defer** (tightening needs sign-off; the same input is already pinned at `Column.extractColumn` level) | user | the user's sign-off, the next edit of either test, or an `Enum16` entry in the matrix |
| UF-37-3 | allow-list globs broader than exact paths: a wildcard row with a short literal prefix is accepted | `.planning/scripts/*allowlist*`, `compare_confluent_archives.py` | **Defer** (a); Phase 44 gets a grep check (b) | user | the authoring of the next allow-list, or any generated row whose wildcard follows fewer than two literal path segments |
| 37-IN-01 | workflows accept `_gg[0-9]+`, scripts `_gg[1-9][0-9]*`; the `\|\|` branch message names the wrong cause | `release.yaml` + `pre-release.yaml` (both twins) | **Defer** | user | the next edit of `release.yaml` or `pre-release.yaml`, or a `VERSION` that reads `_gg0` or `_gg05` |
| 37-IN-02 | an allow-list row cannot say changed, added or removed | compare script + list format | **Defer** | user | the next edit of `load_allowlist` or an allow-list whose row hides an added or removed entry |
| 37-IN-03 | a corrupt archive escapes `load_inner` as a traceback with exit 1, not exit 2 (same defect as UF IN-02) | compare script | **Defer** (fails closed today) | user | the next edit of `load_inner`, or a CI log showing a traceback from the compare |
| 37-IN-04 | `ReleaseWorkflowGuardUnitTest.CHAIN` omits the tag gate step although the docs say "before the tag gate" | test constant | **Fix now, category B** (add `TAG_GATE_STEP` between `TESTS_GATE_STEP` and `PUBLISH_STEP`; the real files already satisfy it) | - | if not approved: re-open at the next edit of the release workflows |
| 37-IN-05 | allow-list headers say `compare-confluent-archives.py`; the script is `.github/scripts/compare_confluent_archives.py` | first line of 4 files under `.planning/scripts` | **Fix now** (comment line only) | - | - |
| 37-IN-06 | `ConfluentArchiveCheck.run` prints "lib jars found: ; a version directory ... adds a jar" for an archive with zero lib jars | `src/test/.../ci/ConfluentArchiveCheck.java` | **Fix now** (condition `!= 1` to `> 1`, plus one new test for the zero-jar stderr; no existing assertion reads it) | - | - |
| UF-IN-01 | Unicode `Cf`/`Zs` characters (NBSP, zero-width, bidi) are not escaped in the compare report; cosmetic, not command forgery | compare script | **Defer** | user | the next edit of `UNSAFE_CHARS` or a report line that misleads a reader |
| UF-IN-02 | same defect as 37-IN-03 | compare script | **Defer**, merged with 37-IN-03 | user | as 37-IN-03 |
| UF-IN-03 | the `set -o pipefail` comment cites "a runner log" with no pointer and is 173 columns | both release workflows + one test fixture string | **Defer** (comment-only but edits the publish path in both twins for no operator value) | user | the next edit of `release.yaml` or `pre-release.yaml` |

Also recommended as extra rows, because "every row has an owner" is the phase goal:

| Row | Finding | Recommended |
|---|---|---|
| 30-IN-04 | The Phase 37 ledger's owner is "Phase 38 (RLS-05)", a finished phase. FORK-DELTA item 21 states its condition ("the publication record states that the merged SHA's only `tests.yaml` run is the `custom_build` push"). `MILESTONES.md` records one green `tests.yaml` run `41297502026` on the published SHA. | Re-own to `fork maintainer (the user)`; record the condition as met for gg6; keep the trigger. |
| 13 other `dispositioned` rows of the 37 ledger (31-IN-02, 31-IN-03(b), 25-WR-02(b), 25-WR-03, 25-IN-02, 25-IN-03, 25-IN-04(a), 25-IN-04(c), 27-IN-02, 27-IN-03, 28-IN-02, 28-IN-04(b), and 30-IN-04 above) | Out of the ROADMAP list; each already has owner `fork maintainer (the user)` and a trigger. [VERIFIED: 37-ADVISORY-LEDGER.md, 18 rows with `dispositioned`] | One carried-unchanged line in the ledger, not 12 new rows. |

Not in this ledger but still open from Phase 38 and worth a handoff line to Phase 44: "SAME first-chunk case unstated" (38-03-SUMMARY "Issues Encountered") and the "not characterised" list in section B of the gg6 note.

### Row notes (evidence for the defaults)

- **UF-37-3.** The shipped lists hold dependency namespaces and exact class names: gg6 has 31 rows (`com/clickhouse/client/*`, `com/clickhouse/data/*`, `net/jpountz/lz4/*`, `com/github/luben/zstd/*`, exact connector class names). [VERIFIED: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt first fields] A measurement over all four lists (gg4, gg5, gg6, 25) found zero wildcard rows with fewer than two complete literal leading path segments. [VERIFIED: python3 -I /tmp/p43-glob-check.py, run this session] So an optional script rule "a wildcard row needs two literal leading directory segments" would load all four lists. It is a behaviour change to a release-path script, hence deferred; Phase 44 plans should add the same check as a verify step on the generated gg7 list (RLS-02 already requires "exact-path or literal-prefix rows"). The historical lists are inert after publication (the compare reads only the list named by `VERSION`).
- **28-IN-05(b).** `ColumnTest.enumLabelWithAnEscapeTheServerDoesNotWriteIsRefusedNotMisread` already asserts that `Column.extractColumn` on `Enum8('\\q' = 1)` throws `IllegalArgumentException` with message `Enum type could not be read`. [VERIFIED: ColumnTest.java:216-220] The stub test feeds the same type text. [VERIFIED: DescribeUnreadableEnumMockServerTest.java:27-30] An additive test (a new `@Test` that runs `Column.extractColumn` over the stub's type text) would not change any existing assertion, but adds nothing the `ColumnTest` pin lacks; tightening the existing `assertNull` in place needs the user's sign-off, as the Phase 37 ledger says.
- **37-IN-04.** The real order in `release.yaml` is: compare (line 121), upload (138), tests gate (146), tag gate (162), publish (177). [VERIFIED: grep -n "- name:" .github/workflows/release.yaml] `CHAIN` is quoted in the Read of lines 107-115 (no `TAG_GATE_STEP`). Adding it strengthens the existing `chainOrderProblem` rule; every existing fixture keeps passing because `VALID` already holds the tag-gate step (line 243).
- **37-IN-06.** The only existing assertion that reads `lib jars found` is the two-jar case (`ConfluentArchiveCheckUnitTest.java:254-258`); the zero-jar case is asserted through `violations(...)`, which this edit does not touch. [VERIFIED: grep this session]

## Rows needing user approval for src/main

**Do not assume approval. The orchestrator asks, inline; the approval is recorded in the user's own words in the ledger before any fix; the fix lands tests first, in its own commit; a row without approval is deferred.** Every approved `src/main` change additionally needs: a full suite on ClickHouse `latest` x V2 (baseline 2423 tests, 18 skips), the anchors file edited in the same commit as the count it changes, a FORK-DELTA row, a CHANGELOG entry and gg7-note wording (Phase 44), a new row in the gg7 allow-list for each connector class that changes against the gg6 archive, the E-11 style revert check of Phase 42 for each `src/main` commit, and the Spotless "no new violation" check below. `Column.java` and `DeclaredNullability.java` are unchanged since `v1.7.0_gg6` (the eight changed `src/main` files are `ClickHouseSinkTask`, `ProxySinkTask`, `ClickHouseWriter`, `ClickHouseHelperClient`, `Processing`, `StateProvider`, `KeeperStateProvider`, `Utils`), so touching either adds a class to the gg7 allow-list and moves the FORK-DELTA `src/main` counts. [VERIFIED: git diff --stat v1.7.0_gg6 HEAD -- src/main]

### A1. 26 IN-02: remove the dead `shouldSkipDescribeRow` overloads (default: defer)

Exact diff:
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`: delete the Javadoc and both package-private overloads (the block from the comment starting "Returns true when `fieldDescriptor` is a structural DESCRIBE sub-column row" through line 520). The three-argument body is the single line `return DescribeTableBuilder.decide(fieldDescriptor, columnsSoFar, rawTypes).skipsRow();`. [VERIFIED: ClickHouseHelperClient.java:508-519, read]
- `.planning/scripts/fork-anchors.txt`: the row `...ClickHouseHelperClient.java	shouldSkipDescribeRow	3	HLP-02` (line 29) becomes count 0 or is deleted, in the same commit.
- Tests (category B, existing call expressions change): 15 `shouldSkipDescribeRow` occurrences in `ClickHouseHelperClientForkUnitTest` (14 calls and a Javadoc mention at line 31), line 399 of `DescribeTableBuilderUnitTest`, and a comment at `TableTest.java:51`; each call becomes `DescribeTableBuilder.decide(row, columnsSoFar, rawTypes).skipsRow()` (`decide` and `skipsRow` are package-private and the tests share the package). Retarget the tests first (green on the unchanged code), then delete in a second commit.
- `docs/FORK-DELTA.md` line 55 names `shouldSkipDescribeRow`; reword.
- Spotless: `ClickHouseHelperClient.java` is already dirty (7 changed lines in the hook's output).
**Why defer:** value is nil (an unused package-private method), cost is 17 retargeted test sites, an anchors move and a new gg7 allow-list row (`ClickHouseHelperClient.class` is already changed since gg6, so no new row for that file), and it makes the next upstream sync touch this method.

### A2. 27 WR-01: make the unknown-enum-label failure actionable (default: defer; ask, it is the only row with operator value)

Exact diff (the reviewer's fix, re-checked against the tree):
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`, `convertEnumValues`, line 630: `throw new IllegalArgumentException(String.format(ENUM_LABEL_NOT_FOUND_FORMAT, name));` becomes `throw new DataException(String.format(ENUM_LABEL_NOT_FOUND_FORMAT, name));` plus `import org.apache.kafka.connect.errors.DataException;` (the file has no `errors` import today). [VERIFIED: Column.java:626-636 read; grep of imports] The text stays `Enum label not found for column %s`: column name, no label.
- Effect: `ClickHouseWriter.passesThroughUnchanged` returns true for any `ConnectException` (quoted lines 691-693), so the message reaches Connect status and the DLQ instead of being replaced by `Failed to write column ... IllegalArgumentException` with a withheld cause. Retry classification is expected to be unchanged (a `DataException` either way); pin it.
- Existing assertions that change (category B): `ColumnTest.java:242-243` (`assertThrows(IllegalArgumentException.class, () -> col.convertEnumValues(label))` and the message) and `WriterValueFreeFailureUnitTest.unknownEnumLabelIsALookupMissWithoutTheLabel` (parameterized Enum8/Enum16, lines 536-553, asserts the opaque shape with `IllegalArgumentException.class` and `assertWithheldCauseLine`). [VERIFIED: grep -n this session] Tests first: add new tests that pin the new outcome and a value-free guarantee (no label, no value in any text) before changing the two.
- Anchors: `ENUM_LABEL_NOT_FOUND_FORMAT` stays 2, `NO_ENUM_CONTENT_FORMAT` stays 2 (no row moves). CHANGELOG, FORK-DELTA and the gg7 note must say that the text of this failure changed (an operator alert keyed on the old text is affected).
- Spotless: `Column.java` and `ColumnTest.java` are already dirty (27 and 12 changed lines).

### A3. 27 IN-01: one Javadoc sentence on `passesThroughUnchanged` (default: defer)

Comment-only diff in `ClickHouseWriter.java`, Javadoc of `passesThroughUnchanged` (lines 677-689): add after the last paragraph, for example: "The first two families return before the chain walk, so the Jackson exclusion holds only because a Jackson exception is converted at the innermost write, before the Map, Array or Tuple lambdas can wrap it." No planning ID may appear (Pitfall 5). No behaviour change; the file's class bytes change only through the line-number table [ASSUMED] (A4). The reviewer's alternative, a pinning test for a wrapped Jackson shape, cannot reach `passesThroughUnchanged` (private static) without a seam, so it is not offered. `ClickHouseWriter.java` is dirty (21 changed lines in the hook's output).

### A4. 28 IN-01: `ESCAPED_NEWLINE` (default: defer)

Options, none free: (a) drop `public`: `public static final String ESCAPED_NEWLINE = "\\n";` (DeclaredNullability.java:44) becomes package-private, which breaks compilation of `NullableTupleMatrixLiveTest.java:762` and `SimpleAggregateTupleRoundTripLiveTest.java:117` (package `com.clickhouse.kafka.connect.sink`), so both live tests must change; (b) the reviewer's move to `DeclaredNullabilityFixtures`, which contradicts that class's Javadoc: "It must never hold a copy of `DeclaredNullability.ESCAPED_NEWLINE`; tests read the production constant." [VERIFIED: DeclaredNullabilityFixtures.java:5-8; DeclaredNullability.java:44; grep of the six read sites] Either edits existing assertion expressions in 2 to 4 test files. Optional comment-only add-on: reflow the Javadoc paragraph whose lines wrap mid-sentence ("That is" / "not a mismatch", lines 34-35). `DeclaredNullability.java` is Spotless-clean today, so a new violation would be visible.

### Category B: approvals that are not `src/main` (same question to the user)

| Row | Change | Recommended |
|---|---|---|
| 28-IN-05(b) | tighten the existing `assertNull` in `DescribeUnreadableEnumMockServerTest` (needs a log appender or a `Column.extractColumn` binding) | defer |
| 37-IN-04 | add `TAG_GATE_STEP` to `ReleaseWorkflowGuardUnitTest.CHAIN` (strengthens an existing rule; no assertion edited or removed) | approve (fix now); additive alternative if the user reads the gate literally: a separate new rule and fixture test, `CHAIN` untouched |
| A1, A2, A4 test fallout | existing call expressions or assertions edited | only with their parent row |

## Header comment: exact change (success criterion 4)

Current lines 1-6 [VERIFIED: .github/workflows/increment-version.yml:1-6, read this session]: line 4 `# expected, so a dispatch can no longer put a null version into VERSION. BumpVersionScriptUnitTest pins the`, line 5 `# rules, and ReleaseWorkflowGuardUnitTest pins this file. Every action is pinned to a full commit SHA with a`, line 6 `# version comment, and the token scopes are granted on the job instead of the workflow.`

Guard classes that actually read the file, by grep: `IncrementVersionWorkflowGuardUnitTest` (`WORKFLOW = Path.of(".github", "workflows", "increment-version.yml")`, line 45; eight CIH-02 rules including the header rule) and `WorkflowActionPinGuardUnitTest` (`FORK_WORKFLOWS = List.of("increment-version.yml", "pre-release.yaml", "release.yaml", "tests.yaml")`, which applies the full-SHA pin rule); `TestsWorkflowPinGuardUnitTest` lists the file only as an existing-workflow name in dependabot fixtures. [VERIFIED: IncrementVersionWorkflowGuardUnitTest.java:45; WorkflowActionPinGuardUnitTest.java:50; TestsWorkflowPinGuardUnitTest.java:764,1135] `ReleaseWorkflowGuardUnitTest` reads only `release.yaml` and `pre-release.yaml` (their own comments at line 12 correctly name it).

Replace lines 5-6 with (110 and 94 columns; the existing header lines are 106, 109, 109, 107, 108 and 87 columns, measured with `awk`):

```
# rules, and IncrementVersionWorkflowGuardUnitTest pins this file. Every action is pinned to a full commit SHA
# with a version comment, and the token scopes are granted on the job instead of the workflow.
```

Constraints the new text must keep: line 1 must start with `# FORK MODIFICATION` and the header may not contain `left unfixed` or `vnull` (`HEADER_START` and `FORBIDDEN_HEADER_TEXT`, IncrementVersionWorkflowGuardUnitTest.java:70-71, read); the anchors file counts `node-version`, `setup-node`, `npm install`, `semver` and `npm install fs` as 0 and `python3 -I .github/scripts/bump_version.py` as 1 in this file (fork-anchors.txt:160-166, read), none of which the new text contains. The "pin guard" that must pass is `IncrementVersionWorkflowGuardUnitTest` plus the rest of the six workflow guard classes (below). No file other than the `.yml` changes in that commit. FORK-DELTA item 19 ("names the wrong guard class (published knowingly open)") changes to closed in a later docs commit that cites the header commit SHA.

## Plan shape (recommended)

Documents and ledger edits all touch `docs/FORK-DELTA.md`, so serialize them. Suggested plans:

1. **43-01 docs (DOC-01):** gg1 item 6 and preface; gg6 row C2, the one-line pointer and the correction notice; FORK-DELTA paragraphs 5, 146, 295 (and the two command lines), adjacent items 12, 22, 23. Checks are `grep`-style (no test reads these files): the three phrases are gone, "of any line" has zero hits, `FUT-05` hits in the gg6 note, the two repointed commands print the table's numbers, no `HEAD`-relative claim was added.
2. **43-02 CI and test hygiene:** header comment (own commit, guard gate); 37-IN-05 allow-list headers; 37-IN-06 (new zero-jar test first, seen red against the unfixed helper, then the one-condition fix); 37-IN-04 only if approved.
3. **43-03 ledger and approval gate:** orchestrator asks the category A and B questions inline; the ledger `43-DISPOSITION-LEDGER.md` is written with the verbatim approvals or the deferrals; each approved `src/main` row is its own conditional task (tests first, own commit, own gate), executed inline by the orchestrator.
4. **43-04 FORK-DELTA bookkeeping and phase gate:** items 19 and 20 rewritten to cite the ledger and the fix commits (the commit pointers exist only after plan 02), then the gate.

The ledger format: copy `37-ADVISORY-LEDGER.md`'s columns (`ID | Status | Pointer | Owner | Re-open trigger | Note`; statuses `fixed`, `closed`, `dispositioned`; a fixed row carries a 7-40 hex pointer; a dispositioned row carries `-`, an owner and a trigger of at least 15 characters) and a phase-local copy of `37-ledger-check.py` with its REQUIRED list replaced by the 16 proposed IDs. [VERIFIED: 37-ledger-check.py:1-45 read; the checker's `ROW_ID` pattern is `\d\d-(WR|IN)-\d\d`, so IDs such as `37-HDR` and `UF-37-3` need the pattern widened] Phases 39 and 42 each keep a phase-local `NN-evidence-check.py` the same way.

## Gate Recipe

Run from `/Users/alexandr.chashnikov/repo/other/clickhouse-kafka-connect_my` with `bash -c`. Phase start is `aa415afe`.

1. **Start-of-execution re-measure:** `python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py anchors .planning/scripts/fork-anchors.txt` printed `rows=190 bad=0` this session. [VERIFIED: run today] `git rev-list --left-right --count origin/custom_build...HEAD` printed `0 159` (origin is still the gg6 merge `72e8ccff`). Re-read the four GitHub facts of DOC-01 section 2.
2. **Cache-busted Docker-free tests, after any commit that touches a workflow, a guard, a script or a test** (Gradle does not track workflows or scripts as inputs, so `--rerun` is mandatory; memory: UP-TO-DATE gates): `rm -rf build/test-results/testUnit` then `DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun`. For the header-comment commit run the six workflow guard classes first: `--tests '*IncrementVersionWorkflowGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --tests '*ReleaseWorkflowGuardUnitTest' --tests '*NightlyWorkflowGuardUnitTest' --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest'`. Baseline: 1824 tests, 0 failures, 0 errors, 2 skipped (`42-VERIFICATION.md:86`); expect baseline plus the tests this phase adds.
3. **Anchors:** the mirror after every commit that touches an anchored file or `fork-anchors.txt` (`rows=190 bad=0`, or the new row count with the same commit). The cumulative script `bash .planning/scripts/verify-merge-invariants.sh 54e63648 <explicit head SHA>` takes about 42 minutes (13:11:36Z to 13:53:45Z in Phase 42, 1428 commits), must run once and alone, and needs a re-run only when a commit changes an anchored line or `fork-anchors.txt` (42-04-SUMMARY). A docs-only phase changes none, so run it once on the final head.
4. **Full suite on ClickHouse `latest` x V2, only if `src/main` changed:** `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21`, record `SELECT version()` (Phase 39 recipe: `docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'`), baseline 2423 tests, 18 skips (`42-VERIFICATION.md:92`). If nothing in `src/main` changes, say so with `git diff --quiet aa415afe HEAD -- src/main`.
5. **No assertion weakened:** `git diff -w -U0 aa415afe -- src/test | grep '^-' | grep -v '^---'` must show no removed line matching `assert|fail(|expect`; additions only unless a category B row was approved.
6. **Spotless, scoped:** never run `spotlessApply` over the tree. Baseline measured today: `./gradlew spotlessJavaCheck` fails with 49 files holding violations (`build/spotless-clean/spotlessJava` holds the hook's output for exactly those). For a touched file, compare `diff <file> build/spotless-clean/spotlessJava/<file>` before and after: a clean file must stay absent from that directory, and a dirty file's changed-line count must not grow. [VERIFIED: ran `spotlessJavaCheck --offline` this session; the `-PspotlessFiles` filter was ignored]

| File (touched by a possible row) | Today | Changed lines in the hook output |
|---|---|---|
| ClickHouseWriter.java | dirty | 21 |
| ClickHouseHelperClient.java | dirty | 7 |
| mapping/Column.java | dirty | 27 |
| ColumnTest.java | dirty | 12 |
| DescribeUnreadableEnumMockServerTest.java | dirty | 14 |
| TableTest.java | dirty | not counted |
| DeclaredNullability.java, DeclaredNullabilityTest.java, DeclaredNullabilityFixtures.java | clean | - |
| ReleaseWorkflowGuardUnitTest.java, IncrementVersionWorkflowGuardUnitTest.java, CompareConfluentArchivesScriptUnitTest.java, ConfluentArchiveCheck.java, ConfluentArchiveCheckUnitTest.java, WriterValueFreeFailureUnitTest.java, ClickHouseHelperClientForkUnitTest.java, DescribeTableBuilderUnitTest.java, NullableTupleMatrixLiveTest.java, SimpleAggregateTupleRoundTripLiveTest.java, BumpVersionScriptUnitTest.java | clean | - |

## Anchors and FORK-DELTA implications

- The anchors file does not anchor `docs/`; doc edits move no row. [VERIFIED: grep of fork-anchors.txt: `FORK-DELTA` and `ROLLOUT` occur only in comment line 9]
- Header comment: no anchor row changes (see header section).
- Each approved `src/main` row: A1 moves row 29 (`shouldSkipDescribeRow`, count 3 to 0 or delete); A2, A3 and A4 move none; every one adds or changes a FORK-DELTA row, a CHANGELOG line and gg7-note text (Phase 44), and the commit that changes a counted pattern must carry the anchors edit.
- FORK-DELTA items to rewrite after the ledger: 19 (closed by the header commit), 20 (points at `.planning/phases/37-ci-hardening-advisory-debt/37-ADVISORY-LEDGER.md`, a moved path; repoint to the Phase 43 ledger), 22, 12, 23 (above). The intro sentence "Each item below is either closed ... or published knowingly open" keeps holding.
- Historical allow-lists and FORK-DELTA's line 90 text ("run the archive compare before the tag gate") are unchanged unless 37-IN-04 is declined, in which case line 90 and the guard's Javadoc stay as the narrowing-wording option.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| git, `python3 -I` | all checks | yes | system | - |
| JDK 21 (`/usr/libexec/java_home -v 21`) | gates with `-PjavaVersion=21` | yes | 21.0.12.1 | default JDK 17 is IBM Semeru (OpenJ9) and crashes under Mockito; delete the ~8 GB core dump and re-run once |
| Docker | full suite, only if `src/main` changes | yes: colima profile `vz` Running (8 CPU, 16 GiB), `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock`, `docker ps` answered | Docker 29.8.2 | never run bare `colima start/status` (hits the stopped `default` profile) |
| GitHub REST (unauthenticated) | re-read of run, release and PR facts | yes (answered today) | - | 60 requests an hour; `gh` is not installed |
| `TESTCONTAINERS_HOST_OVERRIDE` | Gradle runs | set to the literal `null` in this shell | - | always `env -u TESTCONTAINERS_HOST_OVERRIDE` |
| `timeout` binary | polling | absent | - | poll in a loop |

Missing dependencies with no fallback: none.

## Don't Hand-Roll

| Problem | Don't build | Use instead | Why |
|---|---|---|---|
| Ledger format check | a new ad hoc checker | a phase-local copy of `37-ledger-check.py` (widen `ROW_ID`) | proven format, 16 rows vs 29 |
| Anchors invariant | a new count script | `42-evidence-check.py anchors` for the mirror, `verify-merge-invariants.sh` for the cumulative gate | already the Phase 39-42 gate |
| Testcase delta proof | manual XML reading | `42-evidence-check.py junit/failed` and `42-testcase-status.py` | exist and are used by 39-42 |
| Published-run facts | memory or planning text | read-only REST GETs, quoted with ids | the stale lines came from text written before the run |

## Common Pitfalls

1. **Line numbers in the ROADMAP are stale.** They are 6/139/286 at the gg6 close and 5/146/295 now. Locate by content; the planner must not use numeric offsets in edit commands.
2. **`HEAD`-relative reproduction commands drift.** A past-tense edit that leaves `git diff --shortstat v1.7.0 HEAD` for the gg6 row makes the doc wrong the day a later commit touches `src`. Use the tag.
3. **`UP-TO-DATE` test gates.** Gradle does not track `.github/` files or `.planning/scripts` as inputs; without `--rerun` a green run proves nothing about a workflow or script edit.
4. **A relayed approval is not approval.** The src/main approval must be asked by the orchestrator and quoted verbatim in the ledger. An executor that reaches the gate stops.
5. **Planning IDs in `src/main` comments.** `SourcePlanningIdGuardUnitTest` scans `src/main/java` for `\b(?:D|L|R|F|A|E|T|IN|WR|CR|RF)-\d{1,3}\b`, the prefixes `WRT|COLS|DESC|TYPE|HLP|DEBT|NAME|JSONP|CMP|CFG|ORCH|MAP|BLD|HARN|CI|DEPS|OPS|ROLL|SEC|SYNC` with a number, `Phase \d+`, `Pitfall \d+`, `RESEARCH`, `.planning/`, review and plan file names, `Backlog \d` and `999.\d`. [VERIFIED: SourcePlanningIdGuardUnitTest.java:38-49] An approved Javadoc line must avoid all of them.
6. **Comment words that trip the header rule.** `left unfixed` and `vnull` are forbidden in the `increment-version.yml` header.
7. **Gate tag vs object.** Do not touch Dependabot PRs #21 and #22, the `scratch/clt-07-*` branches or `NIGHTLY_ENABLED` (user's). The PR-closed facts are read-only.
8. **Working tree noise.** `.planning/config.json` and `.planning/state.json` are orchestrator-owned and stay unstaged; `h` is a zsh alias, so run scripted commands under `bash -c`.
9. **Commit message trailers.** The GSD `commit` verb adds no trailer; use plain git with the attribution trailer the session specifies (memory: Phase 25 orchestration gotchas).
10. **Over-editing published notes.** Every published-note edit lands in Phase 44's review brief; keep each to the corrected claim, and list the edited notes (gg1, gg6, FORK-DELTA) for that brief.

## Code Examples

Ledger row (copied from the Phase 37 format, verified against `37-ADVISORY-LEDGER.md`):

```
| 26-IN-02 | dispositioned | - | fork maintainer (the user) | the next edit of the shouldSkipDescribeRow anchor row or of the describe code of ClickHouseHelperClient, or an upstream sync that touches that method; removal needs the user's approval and an anchors edit. | ClickHouseHelperClient.java: both overloads have no caller in src/main; the user did not approve the change in Phase 43 |
```

Past-tense rewrite of the line-5 clause:

```
(the tree that `VERSION` named at the merge; the git tag and the GitHub release were created at the merged SHA by publish run `41298074815` of the release workflow)
```

Gate for the header commit (one line, six classes, `--rerun`):

```
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*IncrementVersionWorkflowGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --tests '*ReleaseWorkflowGuardUnitTest' --tests '*NightlyWorkflowGuardUnitTest' --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest'
```

## State of the Art

| Old approach | Current approach | Impact |
|---|---|---|
| gg6 close: "FORK-DELTA lines 6, 139, 286" and a note deferred to "the next release that edits them" | Locate by content; the milestone that edits them is gg7 | the sweep happens here, once |
| Phase 38 reviewer rule: no planning identifier in the gg6 note | DOC-01 requires FUT-05 by ID | one deliberate exception, defined inline |

## Security Domain

`security_enforcement` is enabled (absent or true in `.planning/config.json`, level 1, block on high). This phase adds no endpoint, input path, secret or dependency.

| ASVS Category | Applies | Standard Control |
|---|---|---|
| V2/V3/V4/V6 | no | no auth, session, access control or cryptography is touched |
| V5 Input validation | no new input | the one script in scope (`compare_confluent_archives.py`) is not edited by the defaults |
| V14 Configuration (CI) | marginally | comment-only change to a workflow; full-SHA pin rules still enforced by `WorkflowActionPinGuardUnitTest` |

| Threat pattern | STRIDE | Mitigation here |
|---|---|---|
| Published document states a verified-sounding claim the evidence does not support (WR-08) | Repudiation / misinformation | correct the label and the line count to the evidence; cite the gg6 rows |
| Secrets or data in logs/records quoted into the docs | Information disclosure | docs quote ids, sizes and SHAs only; no token, host, connector or user data (the gg6 note rule: names no host, connector, cluster) |
| A relayed agent message taken as user approval for a `src/main` change | Elevation of privilege (process) | orchestrator-inline approval, verbatim quote in the ledger, executors stop at the gate |
| Allow-list rows broad enough to hide a connector class change | Tampering | UF-37-3 deferral plus the Phase 44 grep check on the generated gg7 list |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The user accepts one planning ID (FUT-05) in the published gg6 note, although the Phase 38 brief kept IDs out of it (`38-note-check.py` has a "forbidden NOTE: no planning, requirement or review identifier" rule) | DOC-01 section 4 | the user may prefer the gg7 note; then DOC-01 stays unproven until Phase 44 and the ROADMAP criterion needs rewording |
| A2 | Adding `TAG_GATE_STEP` to `CHAIN` (strengthening one constant, no assertion edited) counts as category B, not as free | approval section | if the user treats it as free, the question is wasted; if it is read literally as a changed assertion and done silently, a user-owned gate is bypassed |
| A3 | Editing the first comment line of the four historical allow-list files under `.planning/scripts` is within "docs/.github/tests" scope for this phase | ledger 37-IN-05 | the planner may need the user's nod; the files are inert after publication and the edit is comment-only |
| A4 | A Javadoc-only edit in `ClickHouseWriter.java` changes the class bytes only through the line-number table | A3 | the gg7 allow-list row for that class already exists (the class changed since gg6), so the practical risk is nil for that file; for `DeclaredNullability.java` the reflow would add an allow-list row |
| A5 | PR #21 and #22 stay open on `origin` and untouched through this phase (they were open today) | Pitfall 7 | a user merge on `origin` would change the left count of `origin/custom_build...HEAD`; re-check at execution start |
| A6 | The 38-REVIEW WR-08 reviewer text is the intended wording basis (it was deferred "as is") | DOC-01 section 1 | wording only; the user can reword |

## Sources

### Primary (HIGH confidence, read this session)
- `docs/ROLLOUT-v1.7.0_gg1.md` (preface, item 6), `docs/ROLLOUT-v1.7.0_gg6.md` (rows C1-C3, D9, D13, preface), `docs/FORK-DELTA.md` (lines 5, 146, 147-180, 229-253, 295, 299), `.github/workflows/increment-version.yml`, `.github/workflows/release.yaml`, `.planning/scripts/fork-anchors.txt` (rows 29, 160-166), the four `confluent-archive-allowlist-*.txt`.
- `IncrementVersionWorkflowGuardUnitTest.java`, `WorkflowActionPinGuardUnitTest.java`, `TestsWorkflowPinGuardUnitTest.java`, `ReleaseWorkflowGuardUnitTest.java`, `ConfluentArchiveCheck(UnitTest).java`, `SourcePlanningIdGuardUnitTest.java`, `ColumnTest.java`, `DescribeUnreadableEnumMockServerTest.java`, `DeclaredNullability.java`, `DeclaredNullabilityFixtures.java`, `Column.java`, `ClickHouseWriter.java`, `ClickHouseHelperClient.java`.
- Row sources: `37-ADVISORY-LEDGER.md`, `37-REVIEW.md`, `37-REVIEW-UF.md`, `37-REVIEW-FIX.md`, `37-REVIEW-UF-FIX.md`, `37-RELEASE-RECORD.md`, `37-SECURITY.md` (UF-37-3), `38-03-SUMMARY.md`, `38-REVIEW.md` (WR-08), `38-RELEASE-RECORD.md`, `MILESTONES.md`; the 26/27/28 review files in `.planning/milestones/v1.7.0_gg4-hardening-phases/`.
- Phase 39-42 evidence and verification files for the gate recipe.
- Public GitHub REST API (read-only GET, 2026-11-02): run 41298074815, release `v1.7.0_gg6`, pulls 10, 17, 21, 22.
- Local measurements this session: `fork-anchors` mirror, `git diff --shortstat` at the tags and HEAD, `spotlessJavaCheck` and `build/spotless-clean`, the allow-list glob measurement, `git ls-remote`.

### Secondary / Tertiary
None. No web search and no Context7 lookup was needed; no external library is involved.

## Metadata

**Confidence breakdown:**
- DOC-01 texts and line locations: HIGH, read from the files and the tag.
- Ledger rows and defaults: HIGH for the facts, MEDIUM for the defaults (they are recommendations; the user owns the approvals).
- Gate recipe: HIGH, taken from Phase 39-42 evidence and re-measured (anchors, Spotless).
- Pitfalls: HIGH.

**Research date:** 2026-11-02
**Valid until:** until `origin/custom_build` moves or any `src` commit lands (re-measure the four start-of-execution facts)

Validation Architecture: skipped, `workflow.nyquist_validation` is `false` in `.planning/config.json`.

## Open Questions (RESOLVED)

1. **Which file names FUT-05 by ID (research flag)?**
   - RESOLVED: the published `docs/ROLLOUT-v1.7.0_gg6.md`, row C2 (plus the correction notice). FORK-DELTA already names FUT-05 (items 17, 24). The gg7 note should also state "FUT-05 stays open" in its RSK-03 section (Phase 44 handoff, checked by RLS-03). User nod wanted on A1 (one planning ID in a note whose Phase 38 brief excluded IDs).

2. **Which guard class actually guards `increment-version.yml`?**
   - RESOLVED: `IncrementVersionWorkflowGuardUnitTest` (reads the file); `WorkflowActionPinGuardUnitTest` additionally enforces the pin form. The header names the first; the wrong name `ReleaseWorkflowGuardUnitTest` occurs only at line 5 of the `.yml` (grep over `.github`).

3. **Where do FORK-DELTA "lines 6, 139, 286" sit now?**
   - RESOLVED: lines 5, 146 and 295 (by content). The gg6 raw-number reproduction commands under the table also need repointing from `HEAD` to `v1.7.0_gg6`.

4. **Is approval given for any `src/main` row?**
   - NEEDS A USER DECISION (STOP-level). Rows: 26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01. Recommended default for each: defer; if the user wants one, 27 WR-01 is the only row with operator value. The orchestrator must ask inline and record the answer verbatim before any `src/main` task runs.

5. **Are category B rows (28 IN-05(b) tightening, 37 IN-04 `CHAIN`) approved?**
   - NEEDS A USER DECISION. Recommended: defer 28 IN-05(b); approve 37 IN-04 (or take the additive-rule alternative).

6. **Should the stale adjacent FORK-DELTA items (12, 22, 23) and the `HEAD`-relative commands of lines 184/186/188/291 be fixed in this phase?**
   - RESOLVED as a recommendation: yes for 12, 22, 23 and the gg6 row/line 186 commands (same class, verified today); the remaining `HEAD`-relative commands are Phase 44's "raw numbers refreshed once". The planner may narrow this; no user approval is needed for documents.

7. **What happens to the 13 other `dispositioned` rows of the Phase 37 ledger and to 30-IN-04's finished-phase owner?**
   - RESOLVED: carried unchanged with one ledger line; re-own 30-IN-04 to the fork maintainer (the user) and record that its FORK-DELTA item 21 condition was met for gg6 (green `tests.yaml` run `41297502026`, per `MILESTONES.md`).

8. **Do UF-37-3 and the behaviour-changing Infos (37-IN-01/02/03, UF-IN-01/02/03) get fixed now?**
   - RESOLVED as a recommendation: defer with owner and trigger (release-path edits with no later phase allowed to repair a CI-only defect). Phase 44 should add a grep check on the generated gg7 allow-list (every wildcard row has two literal leading path segments; measured true for all four existing lists). The user may override per row.

9. **Does the gg6 note need a correction notice?**
   - RESOLVED: yes. It already differs from tag `v1.7.0_gg6` in row D7 (Phase 39) without saying so; add a short notice naming D7 and C2, following the gg1 preface convention.

## User Decisions Recorded (2026-11-02, via AskUserQuestion during /gsd-autonomous)

The user's answers, quoted from the dialog (their selected options, verbatim labels):

- src/main rows (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01): **"Defer all four (Recommended)"**. No src/main change in Phase 43; all four deferred with owner and re-open trigger in the ledger.
- Assertion rows: **"Approve 37 IN-04, defer 28 IN-05(b) (Recommended)"**. 37 review IN-04 (add the tag-gate step to CHAIN) is approved and lands tests-first in its own commit; 28 IN-05(b) is deferred with owner and re-open trigger.
- FUT-05 placement: **"gg6 note C2 + gg7 handoff (Recommended)"**. Edit the published docs/ROLLOUT-v1.7.0_gg6.md (correction notice at the top + FUT-05 in row C2); the gg7 note carries "FUT-05 stays open" in its RSK-03 section as a Phase 44 handoff.

Consequence: because no src/main row is approved, the full suite on latest x V2 is only required if a test-only/CI change makes it necessary (plan decides; at minimum cache-busted testUnit + anchors invariant).
