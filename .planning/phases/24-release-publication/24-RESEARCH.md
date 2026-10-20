# Phase 24: Release & Publication - Research

**Researched:** 2026-10-20
**Domain:** fork release engineering. Operator rollout note, CHANGELOG and FORK-DELTA, a measured artifact diff, one live probe test, a two-run suite gate, and a rehearse-measure-approve-publish merge onto `custom_build`.
**Confidence:** HIGH for every measured finding (all taken this session on ClickHouse `26.9.19.9`, image `sha256:bdb5c911a23d`, created 2026-10-14T18:05:21Z, client V2); MEDIUM for the publication mechanics, which are re-used from Phase 20 and re-measured here only where noted.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
### Known-defect disclosure (ROLL-03, PUB-09)
- **D-01:** **Backlog 999.4** (a Tuple element named `size` or `size0` is dropped at discovery) gets **its own rollout item**, tied to the new unpaired-element WARN (L-51):
  - It explains that this WARN, which gg3 starts logging at every DESCRIBE, marks a table that 999.4 affects.
  - It gives a ready-to-run detection query against `system.columns` that lists affected tables, proven live per the 20-D-03/20-D-16 discipline. The query must flag an affected table and must not flag look-alikes.
  - It adds a Known-issue line to the `1.7.0_gg3` CHANGELOG section and a new FORK-DELTA Known-costs item.
  - Measured facts come from `23-EVIDENCE.md` R-01 (26.9.19.9) and the backlog entry. Every insert into such a table fails with code 33 `CANNOT_READ_ALL_DATA`. A silently wrong stored value is reachable when the shorter bytes happen to parse. The server rejects a Tuple element named `null` (code 36).
- **D-02:** **The 999.4 item's "What to do" offers no workaround.** It says plainly that this release has no fix, that inserts into such a table fail whatever the connector settings, and that the fix is a tracked backlog item. It gives no schema-change advice that the fork has not tested.
- **D-03:** **The 22-WR-01 residual is measured before the note words it.** The residual: Jackson's reference-chain text on the opt-in `binaryFormatWrtiteJsonAsString` route may name a Map key, through the D-01 IOException pass-through.
  - A **committed live JUnit test** runs on `latest` × V2 (20-D-05 precedent: committed, not throwaway). It uses distinct sentinels for the Map key and the Map value. It records what reaches the task status cause chain, the DLQ headers and the `Error inserting records` logs.
  - The note's headline claim ("no longer carries the record value") carries an exception worded on the **measured** result, plus a config self-check ("is `binaryFormatWrtiteJsonAsString` set in your connector config?").
  - FORK-DELTA Known-costs item 6 replaces "was not measured" with the measured result.
  - Note for research: the two records disagree today. `22-SECURITY.md` R-22-02 says "can name a Map key (verifier measurement, review WR-01)", while FORK-DELTA item 6 says it "was not measured". The probe settles which is right, and both texts are aligned to it.
- **D-04:** **Probe contingency:**
  - **Only a Map key appears:** the note states that, and the residual stays accepted (22-UAT test 3, `22-SECURITY.md` R-22-02). There is no code change.
  - **Nothing appears:** the note and FORK-DELTA say that no record text was observed on that route, with the server version.
  - **A Map value, or any record content beyond a key, appears:** execution **stops at a blocking checkpoint**. It runs orchestrator-inline, needs the user's own answer, and shows the captured text, which contains sentinels only and never real data.
- **D-05:** **Backlog 999.5** (a backslash in a column or element name is doubled by `fromJsonRow`, so a Kafka field must use the doubled form) gets a **FORK-DELTA Known-costs item** plus a **short known-limitation paragraph** in the rollout note. The paragraph sits beside the COLS-13 backslash-n item (L-50), which concerns the same names.
  - It gives the doubled-form workaround and a live-proven `system.columns` query for names that contain a backslash.
  - It gets no CHANGELOG line and no item of its own.
  - The behaviour rests on `23-EVIDENCE.md` R-02. Any command the note quotes is re-run live.

### Open review findings (gg2 rule, 20-D-08)
- **D-06:** **Fix only on the doc side, and carry the rest.**
  - **Fixed in Phase 24:** 21-IN-03, the FORK-DELTA BLD-14 evidence not re-established for Lombok 1.18.48. D-08's artifact build re-establishes it.
  - **Closed through the docs:** 23-IN-04, the unpaired-element WARN not saying that inserts will fail. The D-01 rollout item tells operators what the WARN means. The WARN text itself stays unchanged.
  - **Carried, 17 findings:**
    - 21-WR-01..03, 21-IN-01, 21-IN-02 and 21-IN-04;
    - 22-WR-01, the accepted residual, now measured per D-03, with its row pointing at the D-03 test;
    - 22-IN-01..05;
    - 23-WR-01, 23-WR-02 and 23-IN-01..03.

    Each one gets a written reason, an owner and a re-open trigger in a Phase 24 milestone close record, in the shape of `20-MILESTONE-CLOSE-RECORD.md`.
  - The disposition rows are flipped where they live, in `21-REVIEW-DISPOSITION.md`, `22-REVIEW-DISPOSITION.md` and `23-REVIEW-DISPOSITION.md`, following each file's own rules (Phase 16 D-10 precedent).
  - There is no src/main or test edit for any carried finding in Phase 24. The release phase stays lean, and the next milestone clears them the way gg3 cleared the gg2 advisories.

### Rollback target and artifact (ROLL-03)
- **D-07:** **Rollback redeploys the published `v1.7.0_gg2` artifact.** The branch rollback restores the tree of `pre-v1.7.0_gg3` (`6473e24f`), using gg2 item 6's `read-tree` plus commit form. The operator then redeploys the artifact built from tag `v1.7.0_gg2` (`a246ca01`), which is the build that ran in production.
  - The note states that the `pre-v1.7.0_gg3` tree differs from tag `v1.7.0_gg2` only by the three Dependabot bumps, with the same behaviour and data format. D-08 proves this.
  - "Rolling back brings back" names, at least:
    - record values in writer error text (999.3);
    - backslash-n Tuple names failing to pair (COLS-13);
    - the unpaired-element WARN disappearing;
    - the gg2 MBean `version=` and client name.

    Research completes the list from L-42..L-51.
- **D-08:** **Measure the runtime effect of the bumps from the built artifact; don't cite it.** The shipped artifact is the `createConfluentArchive` zip, `build.gradle.kts:397`. Build it in throwaway worktrees and diff its contents:
  - **`a246ca01` ↔ `6473e24f`, the bump-only delta.** Did Lombok 1.18.48 change any class file? Which bundled jars changed (`slf4j-reload4j` 2.0.17 → 2.0.20, and any `reload4j` or other transitive change)? This also proves D-07's "only by the three bumps" claim and re-establishes BLD-14 (21-IN-03), including that the "Ambiguous: Jackson2 and Jackson3" warning stays absent.
  - **`v1.7.0_gg2` ↔ the gg3 publication tip.** This gives the bundled-jar list the note's dependency item states.

  Transcripts go into `24-COMMAND-EVIDENCE.md`. The note says plainly which bumps are build-time only (spotless; Lombok if no class file changed) and which ship (slf4j-reload4j).
- **D-09:** **In-flight batches: research decides, and the note states the result.** Research checks whether any batch that lands under one release encodes to different bytes under the other, for both the gg2 → gg3 upgrade and the gg3 → gg2 rollback.
  - The main candidate is COLS-13 (L-50): backslash-n Tuple element names. gg2 kept DESCRIBE's nullability, and for a `Nullable(Tuple)` every insert failed, so nothing landed. Non-`Nullable` roots must be checked too.
  - Also check L-44 and COLS-14 (L-51), which is not expected to change bytes.
  - **If no shape differs,** the note says, in one sentence each way, that no clean stop is needed and why.
  - **If a shape differs,** the note reuses gg2 item 1's clean-stop procedure and KeeperMap self-check for that shape only. Both are proven live per the 20-D-03 discipline, with `DedupTokenRetryLiveTest`'s measured rule as the basis.

### Dependabot release window
- **D-10:** **Merge freeze.** The user merges no Dependabot PR on origin from now until the `v1.7.0_gg3` tag is pushed.
  - The freeze is checked, not trusted. The pre-gate bundle re-fetches and confirms `origin/custom_build` = `6473e24f` with a left count of 0. The orchestrator checks again right before the release merge.
  - If it moved anyway, the locked recipe applies:
    - a DEPS-01-style merge into `sync/v1.7.0`;
    - an exemption row if an anchored line moved (#9 would hit the BLD-08 `kafka = "4.1.1"` row);
    - a re-run of every gate;
    - the rollback tag follows the actual pre-merge tip;
    - the D-08 diff and the note's dependency item are redone for the new bump.
- **D-11:** **After the tag, a reminder only.**
  - The close record lists the open user items with exact web-UI steps:
    - create the `dependencies` label, or have the fork drop the request;
    - decide on the fork-pin PRs #4 (client 0.10.0, out of scope) and #9 (`connect-api` 4.4.0);
    - decide on #6-#8 now that the freeze is over. Any merge diverges `sync/v1.7.0` again, and the next milestone opens with a DEPS-01-style merge.
  - Nothing waits on the user's answers. The milestone audit and close follow as usual (20-D-12).

### Claude's Discretion
- **The plan split.** Likely shape:
  - a probe and evidence plan: the D-03 live test, the D-08 artifact diff, the D-09 check and the detection-query proofs;
  - a docs plan: the rollout note, CHANGELOG, FORK-DELTA rows and Known costs, the VERSION bump with its anchor row, and the raw numbers;
  - review and disposition, including the carried-findings close record;
  - the suite gate;
  - the publication plan, which runs orchestrator-inline.
- **The note's layout:** item order and titles in the gg3 note, the TOC, and whether the error-text item shows example text from a live capture or from the pinned live test. Any example is still run live per D-03 discipline.
- **The SQL:** the exact text of the 999.4 and 999.5 detection queries, and the set of look-alike fixtures they are proven against. For example, the `Array(Tuple)` `size0` offsets row, a plain column named `size`, and a nested Tuple.
- **The D-03 test:** its class name and placement, for example beside `WriterErrorPathLiveTest`, and its sentinel design.
- **The CHANGELOG Known-issue line:** a `Known Issues` subsection, or inside Important Changes.
- **The merge commit message** (by precedent it names what it publishes) and the **approval-bundle contents**, which are measured, not cited (15-D-12).
- **The close record:** its file name and layout, and the scripted form of the merge checks and the D-08 diff.

### Deferred Ideas (OUT OF SCOPE)
- The 17 carried review findings (21-, 22- and 23-REVIEW). They go in the milestone close record with an owner and a re-open trigger each (D-06), and any fix belongs to a future milestone.
- The 999.4 fix: discovery keeps a declared Tuple element named `size`/`size0`. It is its own phase, in upstream-region `ClickHouseHelperClient`, and it flips the D-04 pin. This is the backlog entry.
- The 999.5 decision on the intended name form for backslashes in names. Backlog entry.
- Dependabot `ignore` entries for the fork-only pins (#4, #9), and creating the `dependencies` label or dropping the request. These are Phase 21 follow-ups and user actions, reminded only (D-11).
- Any of Dependabot PRs #6-#8 shipping in a release. They are unfrozen after the tag and are the user's decision. A merge triggers a DEPS-01-style merge in the next milestone.
- The next upstream sync, once upstream tags past `v1.7.0` (`upstream/main` is 8 commits past the tag, all bumps the fork already carries).

*The "Locked by ROADMAP, STATE and precedent" block of CONTEXT.md `<domain>` (the six-step publication recipe, the merge checks, rollback mechanics, approval gates, one branch per SHA, docs-in-merged-tree, writing disciplines, pre-gate review, CHANGELOG shape, suite mechanics, OPS-01 re-check, milestone close) binds the planner equally and is not repeated here. Read it in `24-CONTEXT.md` lines 30-70.*
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ROLL-03 | `docs/ROLLOUT-v1.7.0_gg3.md`, reviewed before the gate: value-free writer error text, what to grep for, MBean `version=`, client name, dependency bumps, every other operator-visible ledger row (L-42..L-51) | Sections R1 (what the note must say), R2 (D-03 measurement and the config-key correction), R3 (D-09 byte-difference matrix), R4 (D-08 artifact diff), R5 (999.4 / 999.5 queries and scope corrections), R7 (rollback list and script) |
| PUB-09 | `VERSION` = `v1.7.0_gg3`, plain-language `1.7.0_gg3` CHANGELOG section, a FORK-DELTA row for every Phase 21-23 fork-side patch, raw-numbers refresh | R6 (FORK-DELTA coverage audit, gaps, raw numbers), Don't Hand-Roll (VERSION anchor row), Pitfalls 5, 9 |
| SUITE-06 | Full suite twice on `latest` x V2, second with `--rerun`; 0 failures, 0 errors, every skip named; `testUnit` green with Docker stopped | R8 (suite gate recipe, expected numbers, tally rules) |
| PUB-10 | Non-force `--no-ff` merge after the user's own approval; exactly one green `tests.yaml` run on the published SHA; tag only after green; `pre-v1.7.0_gg3` at the pre-merge tip | R9 (Phase 20 publication pattern, every hazard it hit, today's refs) |
</phase_requirements>

## Summary

Phase 24 changes no connector behaviour. It documents, measures and publishes what Phases 21-23 delivered. The research found **seven facts that change what the plan may say or must do**, five of them corrections to statements in CONTEXT.md:

1. **None of the three Dependabot bumps changes the shipped artifact.** The `createConfluentArchive` zip built from `a246ca01` (`v1.7.0_gg2`) and from `6473e24f` (the pre-merge tip, three bumps) is byte-identical: same SHA-256 `2e3583a9a0bd...`, 7375 jar entries, 0 changed. `slf4j-reload4j` is not in the zip at all: the shipped jar is built from `clickhouseDependencies`, which carries only `slf4j-api` (resolving to 2.0.7), so CONTEXT's "which ship (slf4j-reload4j)" is wrong. Lombok 1.18.48 generated byte-identical classes, and with `-Xmaxwarns 100000` the "Ambiguous: Jackson2 and Jackson3" warning is absent, with a negative control that makes it appear. This re-establishes BLD-14 (21-IN-03) by measurement.
2. **D-03's config self-check names a key that does not exist as a connector setting.** `binaryFormatWrtiteJsonAsString` is derived from `clickhouseSettings` containing `input_format_binary_read_json_as_string` set to `1` or `true` (`ClickHouseSinkConfig.java:355-356`). The operator's self-check is a grep for that server setting in the connector config.
3. **D-03 measured:** through the real connector, the Jackson residual names a **Map key** in the task status cause chain, in the DLQ `stacktrace` header and in the `Error inserting records` ERROR throwable, and nowhere else; the DLQ `message` header and the `Error writing value of` line are free of it. A Map **value** does not appear for any Connect-typed content. The two existing records are each stale in a different way (R2.3).
4. **D-09: one shape does encode differently between gg2 and gg3.** `Nullable(Tuple(...))` (as root, `Array` element, `Map` value, or a Tuple field) with a Tuple element whose name holds a backslash-n or a line break. gg2 fails the batch with code 33 and stores 0 rows; gg3 stores the right row. Non-`Nullable` roots are byte-identical. L-44 and L-51 do not change bytes. So D-09's "a shape differs" branch applies, in the rollback direction in practice.
5. **999.4 is wider than the backlog text says.** It also hits a Tuple element named `size<digits>` (`size1`, `size12`) when an `Array` encloses the Tuple, because upstream's `Table.SIZE_FIELD_MATCHER` drops it. The server error is code 32 `ATTEMPT_TO_READ_AFTER_EOF` or code 33 `CANNOT_READ_ALL_DATA` depending on the bytes, not "code 33" only.
6. **999.5 is wider than the backlog text says.** The cause is `fromJsonRow` doubling every backslash (`ClickHouseFieldDescriptor.java:48`), so every name character that the server JSON-escapes is affected: `\`, `/`, tab, line break and `"`. Measured: a column named `a/b` silently loses a value sent as `a/b` (the default is stored, no error), and a name with `"` makes `describeTableV2` fail so the table is not mapped. Identical on gg2 and gg3.
7. **The CI-log token is gone.** OPS-03 deleted Keychain item `gsd17-gh-actions-read`. Run-level REST reads (`actions/runs`, `/jobs`) work unauthenticated; job logs do not. Offer a new read-only token up front.

**Primary recommendation:** split the phase as CONTEXT sketches (probe+evidence, docs, review+close record, suite gate, publication). Put the VERSION bump and the D-03 test first, because the MBean/client-name probe and the raw numbers depend on them. Word every disclosure on what was measured here, not on the backlog text. Treat the 999.4 and 999.5 scope corrections as a user decision at plan review.

## Project Constraints (from CLAUDE.md)

- `./CLAUDE.md` includes `AGENTS.md`; `./.claude/CLAUDE.md` adds the GSD workflow rule. Directives that bind this phase:
  - Start edits through a GSD command (`/gsd-execute-phase`). No direct repo edits outside a GSD workflow.
  - Pre-PR obligations: `./gradlew spotlessApply` (or `spotlessCheck`), new functionality has feature-test coverage, `VERSION` bumped and `CHANGELOG.md` updated (PUB-09 owns the last two).
  - "Do NOT make commits to the branch without explicitly asking the user": GSD plan commits are the user-started workflow; every push, merge and tag still needs the user's own explicit approval (PUB-10).
  - Tests: only add, never delete or weaken; DRY; read `ClickHouseTestHelpers.java` before writing a test; write read-back assertions for anything written to ClickHouse; JUnit 5 + Mockito; do not mock `ClickHouseWriter` in state-machine tests.
  - Security: do not log secrets or record data. Evidence files hold sentinels only.
  - Do not upgrade dependencies without asking (D-10 freezes the Dependabot PRs).
  - Test target is ClickHouse `latest` x client V2 only (`.claude/CLAUDE.md` Constraints). The stale line "Gradle 7.4.2" in `.claude/CLAUDE.md` is wrong: the wrapper is `gradle-9.6.1-bin.zip` [VERIFIED: gradle/wrapper/gradle-wrapper.properties `distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip`].
- Memory-derived rules (project MEMORY.md): every Gradle call as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz` (never a bare `colima start`); an OpenJ9 exit-255 is a JVM crash, not a test failure; `gh` is not installed; ROADMAP.md repeats `**Wave N**` headings, so slice to `### Phase 24:` before any scripted edit; reviewers appending to an existing REVIEW.md must keep `### <CR|BL|WR|IN>-NN: title` headings.

## Architectural Responsibility Map

This is a release phase, so the tiers are the places a claim or an action can live, not web tiers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Operator-facing statements (what changed, how to self-check) | `docs/ROLLOUT-v1.7.0_gg3.md` in the merged tree | `CHANGELOG.md`, `docs/FORK-DELTA.md` | Operators read the note; the CHANGELOG is plain-language; FORK-DELTA is for the next syncer. All three ship inside M. |
| Proof of every quoted command and number | `24-COMMAND-EVIDENCE.md` (planning dir) | live JUnit tests (D-03), throwaway worktrees under `/tmp` | The note quotes only what a transcript in this phase shows (20-D-03). |
| Byte-level behaviour (encoding, artifact) | `src/main` writer and `DeclaredNullability`, the built zip | scratch harness against two class trees | The writer is unchanged by Phase 24; the artifact diff is the proof. |
| Green-on-`latest` proof | local full suite twice, then one CI run per published SHA | `testUnit` with Docker stopped | Local runs gate the push; CI is the proof on M. |
| Publication (push, merge, tag) | orchestrator, inline, after the user's own approval | none | Executors refuse relayed approvals; `custom_build` is the protected base branch. |
| Close bookkeeping (carried findings, reminders) | `24-MILESTONE-CLOSE-RECORD.md` + the three `*-REVIEW-DISPOSITION.md` files | PROJECT.md / STATE.md at transition | D-06, D-11. |

## Standard Stack

No package is installed, added or upgraded in this phase.

### Core (tools the plans run)
| Tool | Version | Purpose | Source |
|------|---------|---------|--------|
| Gradle wrapper | 9.6.1 | `createConfluentArchive`, `testUnit`, `test` | [VERIFIED: gradle-wrapper.properties] |
| JDK toolchain | 17 (Gradle toolchain; local Semeru 17.0.9 OpenJ9 and Adoptium 17 in `~/.gradle/jdks`) | build and test JVM | [VERIFIED: build.gradle.kts `JavaLanguageVersion.of(17)`; `/usr/libexec/java_home -V`] |
| ClickHouse server | `clickhouse/clickhouse-server:latest` = 26.9.19.9 (image `bdb5c911a23d`, created 2026-10-14T18:05:21Z) | live tests and probes | [VERIFIED: `docker image inspect`, `SELECT version()`] |
| ClickHouse client | `client-v2` 0.9.6 | V2 path under test | [VERIFIED: gradle/libs.versions.toml `clickHouseDriver = "0.9.6"`] |
| JUnit Jupiter | 5.14.4 | new live test | [VERIFIED: libs.versions.toml `junitJupiter = "5.14.4"`] |
| git | 2.50.1; colima 0.10.3; Docker 29.8.2; Python 3.14.8 (`python3 -I` for scripts that read built output) | publication, evidence | [VERIFIED: `--version` calls] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Whole-zip SHA-256 as the artifact-identity proof | Per-entry SHA-256 of the extracted jar plus the whole-zip SHA | The zip SHA matched across two builds a minute apart here, and Gradle 9 makes archives reproducible by default [CITED: docs.gradle.org/9.0.0/release-notes.html], but a different JDK minor could change class bytes. Record both; rely on the per-entry compare. |
| Re-using `/tmp/gsd20/...` Phase 20 scripts | Re-create them from the evidence text | The Phase 20 scratch scripts are gone. `rollback-demo.sh` is reproduced verbatim in `20-COMMAND-EVIDENCE.md` E-31 (lines 579-637); the XML tally and coverage scripts must be re-written (R6, R8). |

**Package legitimacy:** not applicable. No `npm`/`pip`/`cargo` install and no new Gradle coordinate. The only external reads were GitHub REST calls and one web search for the Gradle 9 archive default.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| none | - | - | - | - | - | No external package is installed by this phase |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 Wave 1  probes + evidence (24-COMMAND-EVIDENCE.md)
   VERSION bump commit (anchor row moves in the same commit)
        |--> MBean / client-name probe (gg2 tree vs gg3 tree)            --> rollout item 5
   D-03 live test (committed, ClickHouseBase)  -- red stop? --> blocking checkpoint (D-04)
        |--> key/value sentinels: task status, DLQ headers, ERROR logs   --> rollout headline exception, FORK-DELTA item 6
   artifact diff: worktrees a246ca01 / 6473e24f / publication tip
        |--> unzip, per-entry SHA-256, javac -Xmaxwarns negative control --> rollout dependency item, BLD-14, 21-IN-03
   D-09 byte matrix (gg2 classes vs gg3 classes, same server)            --> rollout in-flight / rollback wording
   detection-query proofs (999.4, 999.5) against fixture DB             --> rollout known-defect items
        |
 Wave 2  docs (inside the tree that will be merged)
   ROLLOUT-v1.7.0_gg3.md + CHANGELOG 1.7.0_gg3 + FORK-DELTA rows/Known costs/raw numbers
        |
 Wave 3  gsd-code-reviewer rounds (2-3) --> 24-REVIEW-DISPOSITION.md ; carried-findings close record ; flip 21/22/23 dispositions
        |
 Wave 4  suite gate on the frozen tree: clean compileJava, testUnit (Docker stopped), test x2 (--rerun), anchors invariant, OPS-01 re-check, SELECT version()
        |
 Wave 5  publication, orchestrator-inline
   re-fetch origin --> [moved? DEPS-01-style merge, redo gates]
   push sync/v1.7.0 scratch (explicit refspec) --> one tests.yaml run green
   approval bundle (measured) --> USER's own answer, direct
   push tag pre-v1.7.0_gg3 at the pre-merge tip --> no run started
   worktree: git merge --no-ff sync tip into custom_build  --> checks on M (2 parents, M^1 = origin/custom_build, tree(M)=tree(X), rollback re-run on M)
   push custom_build only --> poll REST until the single run finishes green
   push tag v1.7.0_gg3 by name --> delete scratch branch --> final refs check
```

### Recommended file layout
```
.planning/phases/24-release-publication/
├── 24-COMMAND-EVIDENCE.md        # E-nn entries, "Matches text: yes|n/a" (Phase 20 format)
├── 24-REVIEW-DISPOSITION.md      # findings of the pre-gate docs review
├── 24-MILESTONE-CLOSE-RECORD.md  # D-06 carried findings (reason, owner, re-open trigger), D-11 reminders
└── 24-0N-PLAN.md ...
docs/ROLLOUT-v1.7.0_gg3.md        # new, ships in M
src/test/java/com/clickhouse/kafka/connect/sink/<D-03 test>.java   # committed live test (+ optional Docker-free twin)
```

### Pattern 1: three-part delta items with placeholders only
**What:** every rollout item is *What changed* / *Am I affected?* (check plus what an empty result means) / *What to do*, with `<db>`, `<table>`, `<column>`, `<topic>`, `<connector-name>`, `<alert-rule-directory>` placeholders and no host. **When:** always (16-D-02, 20-D-01). Template: `docs/ROLLOUT-v1.7.0_gg2.md` (items 4, 5, 6 are the closest models). Add one placeholder for the connector-config search, for example `<connector-config-directory>`.

### Pattern 2: measured, not cited
**What:** a statement of behaviour in the note carries the server version and has a transcript in `24-COMMAND-EVIDENCE.md`; every quoted command or SQL block is run verbatim with placeholders substituted (Phase 20 E-30, E-31). **Anti-pattern from Phase 20's own review:** "byte-identical", "new", "measured" used where the evidence showed "changed", "existing", "simulated" (20-REVIEW WR-01, IN-04).

### Anti-Patterns to Avoid
- **Writing the headline "no longer carries the record value" with no exception.** R2 shows a Map key can still appear on the opt-in JSON-as-string route.
- **Citing 23-EVIDENCE R-01 for "code 33".** It is code 32 or 33 by shape (R5).
- **Telling operators slf4j-reload4j ships.** It does not (R4).
- **Fixing the exemption list or `verify-merge-invariants.sh`'s merge-parent checks.** They do not apply to this merge (CONTEXT).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rollback of the release | `git revert -m 1` | `git read-tree -u --reset pre-v1.7.0_gg3` + commit, re-run on the real M with the E-31 script | `revert -m 1` hit 97 conflicts on a post-publication tip (Phase 16). |
| Reading CI | `gh` | unauthenticated REST: `actions/runs?head_sha=<sha>`, `/jobs`; 60 requests/hour | `gh` is deliberately absent. |
| Artifact identity | eyeballing `unzip -l` | per-entry SHA-256 compare (R4 script) and `shasum -a 256` of the zip | Detects a one-byte class change. |
| Suite tally | trusting Gradle's "BUILD SUCCESSFUL" | tally `build/test-results/test/*.xml` (tests, failures, errors, skipped, skip names, XML timestamps newer than the last non-`.planning` commit) | A rerun on unchanged inputs can print `:test UP-TO-DATE`; a crashed OpenJ9 fork writes no XML. |
| The `VERSION` bump | editing only `VERSION` | `VERSION` + `.planning/scripts/fork-anchors.txt:41` in one commit, then a `clean` build | The generated `Version.java` is not a task input; the anchor row is `VERSION<TAB>v1.7.0_gg2<TAB>1<TAB>BLD-07` [VERIFIED: fork-anchors.txt:41]. |
| Tolerating a bumped anchored line | editing the script | a new SHA-pinned row in `fork-anchor-exemptions.txt` | CONTEXT D-10; Phase 21 D-02. |

**Key insight:** every hazard Phases 11, 15, 16 and 20 hit was a *procedure* error (wrong tip, relayed approval, tag cancelling a run, stale rollback command). Prove procedures by running them on the real object, not by review.

## Research captures

All captures: server `26.9.19.9`, image `bdb5c911a23d` (created 2026-10-14T18:05:21Z), client V2, container `gsd24r-ch` on port 18124 (removed afterwards), scratch harness sources in `/tmp/gsd24r-scripts` (outside the repo, not committed), class trees built from throwaway worktrees at `a246ca01`, `6473e24f` and `644b1df8` (the current `sync/v1.7.0` tip, removed afterwards). Planners must **re-measure in execution**; these are the proven recipes and the expected answers.

### R1. What the rollout note must carry (ROLL-03), from the ledgers

| Ledger rows | Operator-visible content | Where it lands |
|-------------|--------------------------|----------------|
| L-42, L-43 | A value-conversion failure no longer puts the record value into task status, DLQ stack-trace header or `Error inserting records`. Text: ``Failed to write column `<name>`: Kafka value of type <K> (value class: <C>) could not be written as ClickHouse type <Type>: <exception class>``, ending `Caused by: <class> (message withheld: may contain record data)` [VERIFIED: ClickHouseWriter.java:570-572 constants (`private static final String WRITE_FAILURE_COLUMN_PREFIX = "Failed to write column `";`) and 643-654; 22-BEHAVIOUR-LEDGER.md L-42/L-43] | Headline item, with the R2 exception |
| L-45 | Grep `Failed to write column ` (finds the new text and the older `ClassCastException` text). Alerts keyed on library messages (`Invalid UUID string: `, `For input string: `) stop matching. `Type` token is the connector's (`UUID`, `INT128`, `Decimal`). | Item with a grep self-check on `<alert-rule-directory>` (E-30 pattern) |
| L-48 | DLQ: `__connect.errors.exception.class.name` stays `java.lang.RuntimeException`, `__connect.errors.exception.message` stays the `Topic: [..], Partition: [..], MinOffset: [..], MaxOffset: [..], (QueryId: [..])` text; column, type and class are in `__connect.errors.exception.stacktrace` | Same item |
| L-44 | V1 only; a record whose text contains `ClickHouseException: Code: 33` or `Code: 131` no longer triggers one mapping refresh and retry. Rare; no bytes change (R3). | Short item or a line in the headline item |
| L-50 | Backslash-n or line break in a Tuple element name under `Nullable(Tuple)`: pairing now succeeds; the fallback WARN `Could not take element nullability` stops for such a table; inserts that failed with code 33 succeed (R3). Kafka field name uses the doubled form (999.5). | Item next to the 999.5 paragraph (D-05) |
| L-51 | New WARN `Declared Tuple element(s) {} of column {} of table {}.{} (declared type {}) have no matching DESCRIBE sub-column; applying the declared nullability to the elements that paired`; names a 999.4 table (D-01). It fires for every table the connector *describes*, which is every table of the database (subject to `targetTableFilter`) at task start, not only the tables that topics map to [VERIFIED: ClickHouseWriter.java:131 `this.updateMapping(csc.getDatabase())` in `start`; ClickHouseSinkConfig.java:89 `tableRefreshIntervalDefault = 0`]. | D-01 item |
| L-46, L-47, L-49 | Not operator-visible (hand-built `Column`; test flips; null shapes discovery never produces). One sentence or leave to FORK-DELTA. | FORK-DELTA only |
| MBean / client name | `version=v1.7.0_gg3`, client name `clickhouse-kafka-connect/v1.7.0_gg3`. Re-measure with the E-19 recipe (a probe that starts a task, lists `com.clickhouse:type=ClickHouseKafkaConnector,*` and reads `system.query_log` after `SYSTEM FLUSH LOGS` as its own statement). The gg1 note's item 7 `user-agent` query is reused unchanged. | Item 5 analogue |
| Dependencies | See R4: **none of the three bumps changes the shipped artifact.** | Dependency item |

### R2. D-03: the Jackson residual, measured through the connector

**R2.1 Setup (scratch, against the `644b1df8` tree).** Table `(id Int32, j JSON)`; `clickhouseSettings=input_format_binary_read_json_as_string=1`; `ClickHouseSinkTask.start/put`; record `Struct{id, j: Struct{m: Map<String,String>}}`. Connect's `Struct.put` validates, so the unserializable entry is added by mutating the Map **after** both `put` calls (`((Map) in.get("m")).put(KEY, new Object())`); a failing entry cannot be built through Connect's validated types. Tolerance `none` and `all` (DLQ rendered the way Connect 4.1.1 does). Sentinels: Map key `SENTKEYa1b2`, a value sentinel `SENTVALc3d4`.

**R2.2 Result.**

| Place | Map key | Map value / value content | Notes |
|-------|---------|---------------------------|-------|
| Task status: `put` cause chain, rendered | **yes**: `(through reference chain: java.util.HashMap["m"]->java.util.HashMap["SENTKEYa1b2"])` | no (only the class name: `No serializer found for class java.lang.Object and no properties discovered...`) | `"m"` is the Struct field name (schema metadata); the Map key is record data |
| DLQ `__connect.errors.exception.message` | no (query-id text) | no | unchanged by Phase 22 |
| DLQ `__connect.errors.exception.stacktrace` | **yes** | no | |
| `Error inserting records` ERROR throwable (`Caused by: ...InvalidDefinitionException`) | **yes** | no | |
| `Error writing value of STRUCT to the column `j` of type JSON: exception com.fasterxml.jackson.databind.exc.InvalidDefinitionException` | no | no | names column, type, class only |
| List instead of Map (`ArrayList[1]`) | no (an index) | no | |
| Contrived: a bean whose getter throws `IllegalStateException("getter failed for <value>")` | yes | **yes** (the user object's own exception text) | not a Connect value type; see Open Question 4 |

Reading: **only a Map key** appears for any value Connect can produce. Source lines: pass-through `ClickHouseWriter.java:643-654` (`if (t instanceof IOException) {` / `return true;` inside `passesThroughUnchanged`), Jackson call `ClickHouseWriter.java:873` (`byte[] jsonBytes = OBJECT_MAPPER.writeValueAsBytes(value);`), Jackson reference chain is added by `MapSerializer`/`BeanSerializer` `wrapAndThrow`. `JacksonStructSerializer` (DataJson.java) calls `provider.defaultSerializeValue(value, gen)` per field and adds no path of its own.

**R2.3 The two records disagree because each is partial.** `22-VERIFICATION.md:99` measured plain `ObjectMapper.writeValueAsBytes(Map{"SENTINEL_KEY"->unserializable bean})` in a scratch run: key present, **not through the connector** and nothing about DLQ headers or logs. FORK-DELTA Known-costs item 6 ("whether a Jackson reference chain ... can name a record's map key was not measured") was written in 22-03 before that verification, and `22-SECURITY.md` R-22-02 ("can name a Map key (verifier measurement, review WR-01)") cites it. Neither measured the three destinations. R2.2 settles it; align both texts to it.

**R2.4 What the committed live test must capture.** Class beside `WriterErrorPathLiveTest` (same package `com.clickhouse.kafka.connect.sink`, `extends ClickHouseBase` so the `clickhouse` tag excludes it from `testUnit`). Copy the `putBatch` shape (create table before `start()`, swap nothing, real writer, `LogCapture.attach(ClickHouseWriter.class)`, `ThrowableTexts.printedStackTrace`, `InMemoryDLQ.connectHeaders`). Distinct sentinels: one for the Map key, one for a sibling Map value and one inside a sibling Struct string field. Assert, for `errors.tolerance=none`: put throws; rendered trace contains the key sentinel (pinning the residual) and not the value sentinels; `Error inserting records` throwable lines contain the key and not the value; the `Error writing value of` line contains neither. For `all`: message header matches `ValueFreeDlqHeadersUnitTest.QUERY_ID_MESSAGE`; stacktrace header contains the key, not the values. Add the list-index variant (no sentinel at all) and a control with the setting absent (`RuntimeException` text `Writing JSON in binary is not supported yet...`, no key). Record `SELECT version()`. An optional Docker-free twin can drive `writeColValue(config, column(JSON), data)` the way `WriterValueFreeFailureUnitTest.unsupportedJsonFieldTypePassesThrough` does (`JSON_AS_STRING_SETTING = "input_format_binary_read_json_as_string=1"`, test file line 71) to give `testUnit` a pin.

**R2.5 Correction to D-03's operator self-check.** There is no `binaryFormatWrtiteJsonAsString` connector key. The flag is computed: `String jsonAsString = getClickhouseSettings().get("input_format_binary_read_json_as_string");` and `this.binaryFormatWrtiteJsonAsString = jsonAsString != null && (jsonAsString.equalsIgnoreCase("true") || jsonAsString.equals("1"));` [VERIFIED: ClickHouseSinkConfig.java:355-356]. The note's self-check is: "does the connector's `clickhouseSettings` contain `input_format_binary_read_json_as_string` with the value `1` or `true`?" (a grep over `<connector-config-directory>` with `-F` on that name, plus the REST `GET /connectors/<connector-name>/config` read, both with placeholders). Empty result: the route is off, and the headline claim holds without exception.

**R2.6 D-04 contingency reading.** With Connect-typed content only a key appears, so D-04's first branch applies (note states "a Map key can still appear", residual stays accepted, no code change). Decide before execution that the contrived throwing-getter variant is outside the matrix, or the executor will stop at the blocking checkpoint on a trigger the user never meant.

### R3. D-09: which batches encode differently under gg2 and gg3

**R3.1 Code evidence.** Between `a246ca01` and `644b1df8`, `src/main` differs in four files [VERIFIED: `git diff --stat a246ca01 HEAD -- src/main`]: `ClickHouseWriter.java` (+38 lines, only the generic catch and its helpers), `Column.java` (+3, only the text of `convertEnumValues`), `DeclaredNullability.java` (+133); new `WithheldMessageException.java`. So bytes can differ only through `Column.isNullable` flags set by `DeclaredNullability`. Its changes: L-49 null totality (unreachable from discovery), L-50 quote-aware `normalise`, L-51 unpaired WARN that still applies.

**R3.2 Column-tree diff, gg2 classes vs gg3 classes, same server.** Harness `DescribeDump` ran the real `describeTable` over 41 DDL shapes (3 name styles x 11 Tuple roots, 4 enum-label shapes, 4 `size` shapes) and dumped name, type and `nullable` of every Column node. Result: identical trees for every plain-name shape, every non-`Nullable` Tuple root (including `Array(Tuple)`, `Map(String,Tuple)`, nested Tuples), enum-label shapes, and every 999.4 shape. **Different** (gg2 marks elements nullable = true where gg3 says false) for exactly these, for both a literal backslash-n name and a real line break in the name: `Nullable(Tuple(...))`, `Nullable(Tuple(.., Nullable(Int32), ..))` (the non-null sibling flips), `Array(Nullable(Tuple))`, `Map(String, Nullable(Tuple))`, `Nullable(Tuple(x Tuple(..)))`, and `Tuple(x Nullable(Tuple(<name> Int32)))`. gg2 logs 23 fallback WARNs `(structure mismatch)` for these; gg3 logs none for them.

**R3.3 Live insert, one record per table, V2.**

| Table root | gg2 classes (`a246ca01`) | gg3 classes (`644b1df8`) |
|------------|--------------------------|--------------------------|
| `Tuple(` `` `a\\nb` `` `Int32, d String)` | stored `(7,'x')` | stored `(7,'x')` |
| `Array(Tuple(` `` `a\\nb` `` `...))` | stored | stored |
| `Nullable(Tuple(` `` `a\\nb` `` `...))` | **failed, code 33 `CANNOT_READ_ALL_DATA`, 0 rows** | stored `(7,'x')` |
| `Array(Nullable(Tuple(...)))` | failed code 33, 0 rows | stored |
| `Map(String, Nullable(Tuple(...)))` | failed code 33, 0 rows | stored |
| `Nullable(Tuple(` `` `a\nb` `` `...))` (real line break) | failed code 33, 0 rows | stored |
| `Nullable(Tuple(a Int32, d String))` (no backslash) | stored | stored |

The server read the first row of the double-flagged bytes as garbage and failed at row 2 (`Bytes read: 3. Bytes expected: 4: (at row 2)`), so a batch that parses by chance is possible in principle: that tail was **not** measured [ASSUMED] (same character as 23-EVIDENCE R-01's crafted `size` case).

**R3.4 Verdict per shape (answer to D-09).**
- **gg2 -> gg3 (upgrade):** the only differing shape is `Nullable(Tuple)` with a backslash-n or line-break element name. Under gg2 the measured batch landed nothing, so the gg3 retry of such a range (same `insert_deduplication_token`, derived from topic, partition and offsets, not bytes) lands once with correct bytes. No clean stop is needed for correctness. The unmeasured tail (a gg2 batch that parsed by chance and landed wrong rows) would be kept, as gg2's item 1 states for earlier-content-wins.
- **gg3 -> gg2 (rollback):** a range of such a table that gg3 left in `BEFORE_PROCESSING` is retried by gg2, whose bytes fail the server parse before any dedup decision (measured failure), so the task fails or the batch goes to the DLQ even if gg3 had landed the rows. This is the one shape where a clean stop before rollback matters. Reuse gg2 item 1's KeeperMap query and mirror it (gg2 item 6 step 3 already instructs this for every rollback); prove the query live on a connector-created state table as E-17 did (Phase 20 evidence, line 407).
- **L-44 (V1 refresh)** and **COLS-14 / L-51:** no byte change (the 999.4 shapes produce identical trees in both trees; the only difference is the new WARN). One sentence each: no clean stop needed.
- **Everything else** (L-42, L-43, L-45, L-48): error text only; a failing batch fails the same way in both releases (`WriterFailureClassificationUnitTest` pins classification; 184 cases per STATE).
- Recommendation: the note states "shape differs" for the single shape, gives one sentence for every other change, and reuses the gg2 clean-stop procedure for that shape only (CONTEXT D-09 second branch).

### R4. D-08: the artifact diff, built and measured

**R4.1 Recipe (ran in 6 s per tree on the warm cache).**
```sh
git worktree add --detach /tmp/gsd24/wt-<id> <sha>          # a246ca01, 6473e24f, the publication tip
( cd /tmp/gsd24/wt-<id> && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean createConfluentArchive --console=plain > /tmp/gsd24/build-<id>.log 2>&1 )
mkdir -p /tmp/gsd24/x-<id>/zip /tmp/gsd24/x-<id>/jar        # new, empty directories
unzip -q /tmp/gsd24/wt-<id>/build/confluent/*.zip -d /tmp/gsd24/x-<id>/zip
unzip -q /tmp/gsd24/x-<id>/zip/*/lib/*.jar -d /tmp/gsd24/x-<id>/jar
python3 -I /tmp/gsd24-scripts/cmp.py /tmp/gsd24/x-A/jar /tmp/gsd24/x-B/jar   # per-file SHA-256: only-in-a, only-in-b, changed
```
The shadow jar is a flat merge, not nested jars: there are no "bundled jars" to list, so the dependency evidence is the per-entry compare plus `META-INF/maven/*/*/pom.properties` for the versions. Keep the extracted trees and the compare script in different directories, and run Python with `-I` (extracted content is untrusted data).

**R4.2 Results.**

| Compare | Result |
|---------|--------|
| `a246ca01` zip vs `6473e24f` zip | **whole-zip SHA-256 identical** (`2e3583a9a0bd20b12172cc0efae6a1adb461e23dbbb9b3fe08b06dfca46689fb`); inner jar SHA-256 `95256b62c112...` identical; 7375 entries, 0 only-in-a, 0 only-in-b, **0 changed**; `manifest.json` identical. A second `clean createConfluentArchive` of `a246ca01` a minute later gave the same zip SHA. |
| `6473e24f` vs `644b1df8` (a tip with VERSION still `v1.7.0_gg2`) | 7375 vs 7376 entries; one added: `util/WithheldMessageException.class`; 7 changed, exactly the Phase 22-23 classes: `ClickHouseWriter.class`, `ClickHouseWriter$1.class`, `ClickHouseWriter$2.class`, `Column.class`, `DeclaredNullability.class`, `DeclaredNullability$1.class`, `DeclaredNullability$Assignment.class`. At the real publication tip also expect `Version.class`, the jar `MANIFEST.MF` `Implementation-Version`, `manifest.json` `version` and the zip, jar and directory names to carry `v1.7.0_gg3`. |
| Bundled third-party content | unchanged across all three trees (same 7375 non-fork entries). |
| `slf4j-reload4j` / `reload4j` in the zip | **absent** (`find` for `*reload4j*` returns nothing; only `org/slf4j/spi` and `META-INF/maven/org.slf4j/slf4j-api`). `./gradlew dependencies --configuration clickhouseDependencies` shows `slf4j-api` resolving to 2.0.7; `runtimeClasspath` shows `slf4j-reload4j:2.0.20` with `reload4j:1.2.22` and `slf4j-api 2.0.20`. |

So: **spotless 8.10.4 (build-time only), lombok 1.18.48 (annotation processor, byte-identical output) and slf4j-reload4j 2.0.20 (`implementation`, i.e. `runtimeClasspath`: tests, local runs; not in the Confluent archive)** all leave the shipped zip unchanged. The release workflows build `createConfluentArchive` [VERIFIED: .github/workflows/release.yaml line 32 `run: ./gradlew createConfluentArchive`; pre-release.yaml `arguments: createConfluentArchive`]. There is **no GitHub Release or asset** for any tag (REST `releases` returned an empty list), so "redeploy the artifact built from tag `v1.7.0_gg2`" means the operator's own build at that tag (D-07); building at `pre-v1.7.0_gg3` gives the same zip bytes.

**R4.3 BLD-14 / 21-IN-03 re-established.** Default javac prints 100 warnings and stops, so a grep of a normal build log cannot show a Lombok warning. Build with `-Xmaxwarns 100000` through an init script (`allprojects { tasks.withType(JavaCompile).configureEach { options.compilerArgs += ['-Xmaxwarns', '100000'] } }`, passed with `-I`): 273 warnings in both the `a246ca01` and the `644b1df8` trees, 0 containing `Ambiguous`/`Jackson3`/`lombok`. **Negative control** (`lombok.jacksonized.jacksonVersion += 2` removed in a throwaway worktree): 274 warnings, the extra one being `ClickHouseFieldDescriptor.java:18: warning: Ambiguous: Jackson2 and Jackson3 exist; define which variant(s) you want in 'lombok.config'.` Restored afterwards. Lombok-generated class bytes are identical between 1.18.46 and 1.18.48 (R4.2), which is the stronger form of the old `javap` diff.

**R4.4 Version facts for FORK-DELTA's catalog row.** Upstream `v1.7.0` has `lombok = "1.18.46"` and `spotless = "8.10.1"` [VERIFIED: `git show v1.7.0:gradle/libs.versions.toml` lines 9 and 31]; this tree has `lombok = "1.18.48"`, `spotless = "8.10.4"` [VERIFIED: gradle/libs.versions.toml], so both are bumps past upstream, like the existing row's Jackson/mockito entries.

### R5. D-01 / D-05: detection queries and what they must and must not flag

**R5.1 999.4 ground truth.** The tip's own WARN is the oracle: the set of tables for which `DescribeDump2` logged `have no matching DESCRIBE sub-column`. Rules in code [VERIFIED: ClickHouseHelperClient.java:478 `if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {`; Table.java:24 `Pattern.compile(".+\\.size[0-9]+$")` and :120 `if (SIZE_FIELD_MATCHER.test(childName) || childName.endsWith(".null"))` inside the `ARRAY` case of `updateParent`]. Measured on 54 fixture tables (55 shapes tried, 1 rejected by the server):

- Affected (WARN fires; insert fails, code 32 or 33, 0 rows): a Tuple element named `size` or `size0` at any depth (plain, `Nullable`, `Array`, `Map` value, nested Tuple, Array in Tuple); **and** an element named `size<digits>` anywhere an `Array` encloses its Tuple (`Array(Tuple(size1 ..))`, `size12`, `Array(Array(Tuple(size1 ..)))`, `Tuple(x Array(Tuple(size1 ..)))`, `Array(Tuple(x Tuple(size1 ..)))`, the element in second position).
- Not affected (no WARN, inserts succeed): `Tuple(size1 ..)`, `Nullable(Tuple(size1 ..))`, `Map(String, Tuple(size1 ..))`, `Tuple(a Array(UInt8), size1 UInt8)`, elements named `sizes`, `size_x`, `size_1`, `sizeA`, `Size`, `xsize`, `` `my size` ``, a plain column named `size` / `size0`, a column `size` of `Array(UInt8)`, `Nested(size UInt8, ..)`, enum labels `'size'`/`'size0'`, an unnamed Tuple.
- Live inserts (V2, tip and gg2, identical): `Array(Tuple(size1 UInt16, v String))` and `Array(Tuple(size UInt16, v String))` fail with code 32 `ATTEMPT_TO_READ_AFTER_EOF`; `Array(Tuple(v String, size1 UInt16))` fails with code 33; `Tuple(size UInt16, v String)` fails with code 32; `Tuple(size1 ..)`, `Map(String, Tuple(size1 ..))`, `Tuple(sizes ..)` store the row.

**R5.2 The query (single statement over `system.columns`, no placeholder other than `<db>`).** Proven live: it returned exactly the 25 tables whose WARN fires, plus one over-flag (`Array(Map(String, Tuple(size1 UInt8)))`, a shape the code does not drop), and no other look-alike. Enum-label text is stripped first so a label such as `'x, size1 '` cannot match.
```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND (
        match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '[(,] ?size0? [A-Za-z]')
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''),
                 'Array\\((?:[^()]|\\((?:[^()]|\\((?:[^()]|\\([^()]*\\))*\\))*\\)|\\()*?[(,] ?size[0-9]+ [A-Za-z]')
      )
ORDER BY `table`;
```
The second predicate says "an `Array(` is still open at the element": the lazy prefix may contain balanced groups (depth up to 3) and lone `(` but no unmatched `)`. Decide in the plan whether to keep the second predicate (accurate scope, one known over-flag) or ship the first only and state the `size<digits>` case in prose. Either way the note must say the query lists suspects of the *declared type text* and that the log grep (`have no matching DESCRIBE sub-column`) is the exact check, because it uses the same code path as the insert.

**R5.3 999.5 ground truth and scope.** `ClickHouseFieldDescriptor.fromJsonRow` returns `OBJECT_MAPPER.readValue(json.replace("\\", "\\\\").replace("\n", "\\n"), ClickHouseFieldDescriptor.class)` [VERIFIED: ClickHouseFieldDescriptor.java:48]. The server's JSON output escapes more than backslash, so the Column name differs from the server's name for every escaped character. Measured on the tip (gg2 identical):

| Server name | Column name in the connector | Kafka field that stores the value | Effect of the natural field name |
|-------------|------------------------------|-----------------------------------|----------------------------------|
| `a\b` (one backslash) | `a\\b` (two) | `a\\b` | field `a\b` ignored, **default stored, no error** |
| `a\nb` literal backslash-n | `a\\nb` | `a\\nb` | same |
| a real line break (as a Tuple element name, from the column dump) | `a\nb` (backslash, `n`) | that | same |
| `a/b` | `a\/b` | `a\/b` | field `a/b` ignored, **default stored, no error** (measured: `(1,'DEFAULTED')` vs `(1,'VALUE')`) |
| tab | `a\tb` | that | same class |
| `a"b` | n/a: `JsonParseException` in `describeTableV2`, table returns null (not mapped) | n/a | table unusable |
| `é`, `日本` | unchanged | unchanged | fine |

A `Nullable(Tuple(`` `a/b` ``` Int32, d String))` falls back with the `(structure mismatch)` WARN, so the double-flag failure applies to it as well.

**R5.4 The query.** Flags root names with a backslash or line break, element names with an escape in backticks (the type text prints a backslash for a backslash, a line break and a tab), and, per R5.3, a forward slash. Proven live on 19 fixtures for the first three predicates (flagged all 9 backslash, line-break and trailing-backslash tables; no enum-label, DEFAULT, COMMENT, `a.b` or plain look-alike):
```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND (
        position(name, '\\') > 0
        OR position(name, '\n') > 0
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*\\\\[^`]*`')
      )
ORDER BY `table`;
```
**Not yet proven:** the `/` predicates (`position(name, '/') > 0` and a backtick-quoted element containing `/`) and tab/`"`. A plan that widens 999.5 per R5.3 must add them to the fixture set and re-run. The workaround statement is the measured one: the Kafka field name equals the Column name column of the table above.

### R6. FORK-DELTA coverage audit and the raw numbers

**R6.1 Rows that already exist** [VERIFIED: docs/FORK-DELTA.md]: row 42 (WRT-13 generic catch, with `WithheldMessageException`), 52 (`convertEnumValues`), 51 (COLS-10, COLS-12, COLS-13, COLS-14 in one row), 65 (`slf4j-reload4j:2.0.20`), 67 (`lombok.config`, BLD-14, "Lombok 1.18.48" text), 60 (`TestsWorkflowPinGuardUnitTest`), Known costs items 1-9 (item 6 at line 160).

**R6.2 Gaps to close (counts from `grep -c` this session; `0` means absent).**

| Gap | Evidence | Fix |
|-----|----------|-----|
| `lombok = "1.18.48"` and `spotless = "8.10.4"` are not in the catalog row (68) | `8.10.4` count 0; upstream has 1.18.46 / 8.10.1 (R4.4) | extend row 68's "bumps past upstream" list |
| `fork-anchor-exemptions.txt` and the invariant's exemption branch (Phase 21) | `exemption` count 0 | one sentence in "For the next syncer" and the invariant paragraph |
| CI-18, CI-19 and the narrowed `RUNS_ON` / `WRITE_SCOPE` pin guard | `CI-18` count 0 | add the IDs to row 60's Pinned-by cell |
| HARN-08 and the Phase 22-23 test fixtures (`CountingHelperClient`, `ScriptedWriteFailureWriter`, `ThrowableTexts`, `InMemoryDLQ.connectHeaders`, `DeclaredNullabilityFixtures`) | `CountingHelperClient` count 0 | one Test harness row |
| DEPS-01 | `DEPS-01` count 0 | named in the new catalog clause |
| DEBT-10, DEBT-11, DEBT-12 | counts 0 | optional Pinned-by additions on row 51 (test-only) |
| `jacocoForkReport` "15-class" list lacks `WithheldMessageException` and `DeclaredNullability` | build.gradle.kts `forkChangedClasses` (lines 286-317) has neither; 22-03 SUMMARY flagged it | a `build.gradle.kts` edit is out of Phase 24's scope; record as a carried item and say "15-class" is the listed set |
| `VERSION` row (63) and MBean row (33) say `v1.7.0_gg2`; intro lines 5 and 7 stop at gg2; "For the next syncer" has no gg3 paragraph | text | bump with the release |
| Known costs: item 6 ("was not measured"), new items for 999.4 and 999.5 (D-01, D-05) | R2, R5 | per CONTEXT |
| Behaviour-ledger pointer line 25 stops at `L-41` | text | add `L-42`..`L-48` (`22-BEHAVIOUR-LEDGER.md`) and `L-49`..`L-51` (`23-BEHAVIOUR-LEDGER.md`), named by file |

Re-create the Phase 20 E-41 coverage script (it lived in `/tmp/gsd20-cov.py`): for each Phase 21-23 requirement ID with a code or build effect (DEPS-01, CI-18, CI-19, WRT-13..15, HARN-08, COLS-12..14) check it is named in the last column of a table row, and check each `fork-anchors.txt` row's backs-ID likewise. Also re-run E-42's `.planning/` path resolution over the finished document.

**R6.3 Raw numbers.** Phase 20's pattern (E-40): measure on `sync/v1.7.0` at the commit that carries the document, add two `after gg3` rows (scope `src build.gradle.kts gradle .github`, and `src/main`), add the six-command block's two new commands, reproduce both on the tag after it exists. `VERSION`, docs and `.planning/` are outside both scopes, so the figures are stable once the last `src` change is committed (the D-03 test is that change). For orientation only, at `644b1df8` (before the D-03 test): `git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github` = 129 files, 22476 insertions, 534 deletions; `-- src/main` = 33 files, 2245 insertions, 145 deletions; gg2 published figures 119/18802/529 and 32/2042/143 [VERIFIED: git]. Update the "One number does bear" counts (33 `src/main` files differ; recount identical / import-only / logic with the E-40 method; `WithheldMessageException` is a new file, so 8 files exist only here). The Named skip inventory stays 18 if the new test adds no skip; re-run both `grep` lines.

### R7. Rollback: today's refs, the list, and the script

**R7.1 Today's refs** [VERIFIED: `git ls-remote --heads origin`, `git fetch origin`]: `origin/custom_build` = `6473e24f372a8b37dfd8cc8cf3124e6cff52edca`; `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` = `0 122` (CONTEXT said `0 120`; two planning commits since); `custom_build` is an ancestor of `sync/v1.7.0`; origin heads: `custom_build`, `main`, five `dependabot/gradle/*` branches (#4, #6, #7, #8, #9). No `sync/v1.7.0` on origin. Tags on origin exclude `pre-v1.7.0_gg3` and `v1.7.0_gg3`. `6473e24f` already has exactly one green `tests.yaml` run (id 38887912912). OPS-01: workflow `integrationTests.yaml` state `disabled_manually` since `2026-10-16T09:38:23.000Z`; exactly two `schedule` runs ever (38714528739 on 2026-10-15, 38881153720 on 2026-10-16T09:32:28Z), both before the disable instant. PRs: #1-#3 merged 2026-10-16, #5 closed unmerged, #4, #6-#9 open. After M, expect left count 1 (M) and right 0.

**R7.2 "Rolling back brings back" (completes D-07).** Branch rollback restores the tree of `pre-v1.7.0_gg3` (`6473e24f`, three bumps on top of `a246ca01`); the deployable artifact is the operator's build of that tree or of tag `v1.7.0_gg2` (byte-identical zip, R4). It brings back:
1. record values in writer error text: task status, DLQ stack-trace header, `Error inserting records` (L-42, L-43, L-48; backlog 999.3);
2. library message texts that alerts may have keyed on, and the loss of the `Failed to write column ` match for conversion failures (L-45);
3. on client V1 only, the mapping refresh triggered by a record text containing `ClickHouseException: Code: 33` or `Code: 131` (L-44);
4. `Nullable(Tuple)` with a backslash-n or line-break element name failing with code 33 again, with the `Could not take element nullability ... (structure mismatch)` WARN at every DESCRIBE (L-50; measured R3);
5. the unpaired-element WARN disappearing, so a 999.4 table fails silently as before (L-51);
6. the `v1.7.0_gg2` MBean `version=` and client name (`clickhouse-kafka-connect/v1.7.0_gg2`);
7. on the branch, the gg3 documents themselves (rollout note, CHANGELOG section, FORK-DELTA rows) are removed; read the note with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg3.md`;
8. a range in `BEFORE_PROCESSING` for the R3 shape cannot be re-sent (R3.4).
It does **not** bring back any bundled-library change: R4.

**R7.3 Script.** `rollback-demo.sh` is in `20-COMMAND-EVIDENCE.md` E-31 (lines 579-637, verbatim). Adapt `gg2` -> `gg3`, `pre-v1.7.0_gg2` -> `pre-v1.7.0_gg3` (a local lightweight tag at `6473e24f`), the doc path `docs/ROLLOUT-v1.7.0_gg3.md`, and run it twice: on a dry-run merge of the finished tip (`git worktree add --detach <wt> 6473e24f`, then `git merge --no-ff --no-edit <X>`), and again on the real M before the push. The `-c user.name/user.email` options only give throwaway commits an identity. A linked worktree cannot check out `custom_build`, so the script's stand-in branch is a known deviation to record, as Phase 20 did.

### R8. Suite gate recipe (SUITE-06)

From E-60 (Phase 20), minus the lost scripts: (1) freeze check: branch dirty-free, paths changed since the last green gate inside the allowed set, all three `*-REVIEW-DISPOSITION.md` at their final state, `custom_build` still `6473e24f`; (2) `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileJava`, grep `ARTIFACT_VERSION = "v1.7.0_gg3"` in `build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java`; (3) `colima stop --profile vz`, `docker info` must fail, `testUnit --rerun` green, `colima start --profile vz` (never bare), `docker info` must work; (4) `rm -rf build/test-results/test`, `test --rerun` twice, copying run 1's XML before run 2; (5) tally: classes, tests, failures, errors, skipped, skip names as a set vs the inventory, `exact` test counts for the live matrices, XML timestamps newer than the last non-`.planning` commit, `SELECT version()` taken from a test that logs it (`DedupTokenRetryLiveTest`, `NullableTupleMatrixLiveTest`), `docker image inspect` creation date; (6) anchors invariant `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`: **it took about 10 minutes wall time** here (`anchor_commits=478 anchor_rows=69 marker_commits=525 anchor_exempt=1 result=PASS` at `644b1df8`), so run it with a long timeout or in the background; (7) the OPS-01 re-check.

Baselines to compare [VERIFIED: 23-EVIDENCE.md E-07]: `testUnit` 838 tests, 836 succeeded, 0 failed, 2 skipped; full suite 1277 tests, 1259 succeeded, 0 failed, 18 skipped, 89 XML files. Phase 24 adds the D-03 test(s), so expect more tests and the same 18 skips; derive the exact new count from the tally, do not hard-code it. On an OpenJ9 exit 255: read `javacore.*` first, delete `core.*.dmp`, `javacore.*`, `Snap.*.trc`, `jitdump.*` one by one (never `git clean`), re-run once, record both runs. If the suite is red on unchanged code, check the `latest` image date first; the local image is 2026-10-14 (`latest` moved a patch release between Phase 19 and 20 and again to `26.9.19.9`).

### R9. The Phase 20 publication pattern and every hazard it hit

What the planner must reproduce (evidence IDs in `20-COMMAND-EVIDENCE.md`): E-60 gate; E-61 pre-push checks (`PUSH-READY`: HEAD on `sync/v1.7.0`, no `push.followTags`, no `remote.origin.push` or mirror, `git fetch --no-tags`, origin `custom_build` at the expected SHA, no `sync/v1.7.0` and no ref at X on origin, no secret pattern in changed files, API rate limit at least 20) and `MERGE-DRY-RUN-OK`; E-62 rehearsal push (`git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0`, exactly one run for `X0`, user's answer recorded verbatim with the receiver); E-70 rollback tag pushed first (`git push origin refs/tags/pre-v1.7.0_gg2:refs/tags/pre-v1.7.0_gg2`, then a 62 s wait showing the run count on the pre-merge SHA unchanged); E-71 merge in a throwaway worktree (`merge --no-ff -F <msg file>`), `MERGE-SHAPE-OK` (exactly two parents, `M^1` = pre-merge tip, `M^2` = X, `git diff --quiet M X`, X0..X planning-only, worktree clean), rollback re-run on M, `git push origin refs/heads/custom_build:refs/heads/custom_build`, CI polling, late re-check (still one run, none cancelled); E-72 `git tag <v> <M>`, push the tag by name after green, 62 s wait, delete the scratch branch only after the tag, `FINAL-REFS-OK` (exact origin heads and tags). `20-MILESTONE-CLOSE-RECORD.md` shape: `## Carried review advisories` (a `### <review> <ID>: title` per finding with **Reason**, **Re-open trigger**, **Source**), a fulfilled-obligation section, per-finding **Owner** / **Trigger to re-open** / **Pin** lines, `## Manual checks after publication` (**Steps**, **Why it matters**, `Outcome:`), `## Milestone audit`, `## Housekeeping`; "only decisions are written here, no record value, host, credential or token".

Hazards (all measured or recorded earlier; each needs a plan step or check):
1. Tag push on a pushed SHA cancels the branch run (`tests.yaml` concurrency `group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}` with `cancel-in-progress: true` [VERIFIED: .github/workflows/tests.yaml:28-30]); trigger is `on: push: branches: ['**']` [VERIFIED: tests.yaml:24-26], so a tag push starts no run but joins the group. Tag only after the branch run finishes. One branch per SHA; also never push X0 and another branch at one SHA.
2. `custom_build` is GSD's protected base branch and the fork's default branch: executors cannot commit bookkeeping there; post-publication `.planning` commits live on local `sync/v1.7.0`.
3. Relayed approvals are refused: the gate and every post-gate push run orchestrator-inline, the user's answer quoted verbatim with who received it.
4. `git revert -m 1` is broken; the `read-tree` form must be re-run on the real M, not only on the rehearsal tip (Phase 16: 97 conflicts).
5. Docs reviewed first after publication hid a procedure bug (Phase 15); Phase 20 review found "byte-identical", "new" and "measured" overclaims, a cross-document contradiction (FORK-DELTA vs rollout item 1) and a per-column/per-table wording error. Budget two to three rounds, keep `### ID: title` headings when appending.
6. The security auditor checks mitigations, not procedures: prove procedures by running them.
7. `git rev-list --left-right --count custom_build...sync/vX` is not a fast-forward test after a publication (left 2 in Phase 16); today it is `0 122`.
8. Unauthenticated REST is 60 requests an hour; poll at about 60-90 s intervals. Job logs need a token; **the Keychain item `gsd17-gh-actions-read` no longer exists** (OPS-03, checked this session), so offer a new fine-grained read-only token (Actions: read, one repository, 1-day expiry, stored by the user in Keychain) only if a log is needed.
9. Dependabot: the repo has no `dependencies` label (comment spam, harmless); PRs #4 and #9 bump fork-only pins; the freeze (D-10) is checked by `git fetch` plus `git ls-remote --heads origin custom_build` before the gate and again right before the merge.
10. ROADMAP.md repeats `**Wave N**` headings: slice to `### Phase 24:` before any scripted edit (a truncating edit shipped once).
11. `colima` is the `vz` profile; never a bare start.
12. Stop-the-line items for the user: the approval itself, any D-04 third-branch result, and anything that reaches outside the declared scope (a main-code change stops for a decision).

## Runtime State Inventory

The `VERSION` bump renames a string that surfaces at runtime. Not a repo-wide rename.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None: KeeperMap state is keyed `<topic>-<partition>`, not by version. | none |
| Live service config | Operators' dashboards, alert rules and JMX exporter rules that spell `version=v1.7.0_gg2` or the client name `clickhouse-kafka-connect/v1.7.0_gg2`. | state in the note (item 5 analogue); wildcard forms keep working |
| OS-registered state | None. | none |
| Secrets/env vars | None. No credential is added; the CI-log token is absent and stays absent unless the user makes a new one. | none |
| Build artifacts | `build/generated/sources/version/.../Version.java` is not a task input, so a build after editing `VERSION` needs `clean`; leftover scratch worktrees and `/tmp/gsd24*` directories. | `clean` before the version grep; remove worktrees and scratch at the end |

## Common Pitfalls

### Pitfall 1: the rollout note quotes a CONTEXT sentence that research disproved
**What goes wrong:** the note says "slf4j-reload4j ships", "code 33", "999.4 = size/size0 only", "set `binaryFormatWrtiteJsonAsString`". **Why:** CONTEXT was written before these measurements. **Avoid:** word every statement from R2, R4, R5. **Warning sign:** a statement with no `24-COMMAND-EVIDENCE.md` entry.

### Pitfall 2: D-04's blocking checkpoint fires on a contrived probe
**Why:** the throwing-getter bean shows a value. **Avoid:** fix the matrix to Connect-typed values in the plan (R2.6) and record the contrived case as out of scope.

### Pitfall 3: a build log cannot show the Lombok warning
Default javac prints 100 warnings, and the tree has 273. Use `-Xmaxwarns 100000` and the negative control (R4.3).

### Pitfall 4: the whole-zip SHA is treated as proof of reproducibility
It matched here (Gradle 9 reproducible archives [CITED: docs.gradle.org/9.0.0/release-notes.html]) but depends on identical inputs and JDK. Keep the per-entry compare as the primary evidence.

### Pitfall 5: evidence quoting live output goes stale when a later commit moves `src` or `VERSION`
Order the work: the D-03 test and the VERSION bump first, measure on the actual final tree, then docs; the suite gate runs on a frozen tree; only `.planning/` evidence may follow the gate (E-61 "X0..X planning-only").

### Pitfall 6: `testUnit` with Docker stopped fails on a live test
**Avoid:** the D-03 live test must extend `ClickHouseBase` (tag `clickhouse`); any twin must be genuinely Docker-free.

### Pitfall 7: the anchors invariant takes about 10 minutes
A short tool timeout kills it. Run it in the background and read the result.

### Pitfall 8: scope creep into the code
There is no `src/main` or existing-test edit in this phase (D-06). The D-03 test is a new file. The two scope corrections (R5) change documents, not code.

### Pitfall 9: FORK-DELTA, CHANGELOG and the rollout note disagreeing
Phase 20's review caught exactly this. Generate the shared numbers and sentences once (a small script or one canonical paragraph per fact) and check by grep that the three files agree.

## Code Examples

### The artifact comparison script (`/tmp/gsd24-scripts/cmp.py`, outside any extracted directory)
```python
import sys, os, hashlib
def tree(root):
    d = {}
    for dp, _, fs in os.walk(root):
        for f in fs:
            p = os.path.join(dp, f)
            d[os.path.relpath(p, root)] = hashlib.sha256(open(p, 'rb').read()).hexdigest()
    return d
a, b = tree(sys.argv[1]), tree(sys.argv[2])
print("files:", len(a), len(b))
print("only-in-a:", sorted(set(a) - set(b)))
print("only-in-b:", sorted(set(b) - set(a)))
diff = sorted(k for k in set(a) & set(b) if a[k] != b[k])
print("changed:", len(diff))
for k in diff: print("  ", k)
```
Run as `python3 -I cmp.py <dirA> <dirB>`.

### The byte-matrix harness idea (D-09)
Compile a small `main` (not committed) against each worktree's `runtimeClasspath` (get it with an init script: `allprojects { afterEvaluate { tasks.register("printRuntimeCp") { doLast { println("CP=" + project.sourceSets.main.runtimeClasspath.asPath) } } } }` and `gradlew -q -I <init> printRuntimeCp`), have it create tables over HTTP, call `ClickHouseHelperClient.describeTable` and print each `Column`'s `getName()`, `getType()`, `isNullable()`; diff the two outputs. For inserts use `ClickHouseSinkTask.start/put` with a Kafka field named as the Column name (doubled backslash form) and read the row back with typed SQL.

### Rollout-note self-check commands (placeholders; each must be run live before it is quoted)
```sh
# headline exception self-check: is the JSON-as-string route on?
grep -rnF 'input_format_binary_read_json_as_string' <connector-config-directory>
# alert matchers keyed on the old library messages
grep -rnF -e 'Invalid UUID string' -e 'For input string' <alert-rule-directory>
# 999.4 confirmation from the connector log: the exact check
grep -F 'have no matching DESCRIBE sub-column' <connector-log-file>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Archive tasks keep file timestamps and order | Gradle 9 archives are reproducible by default | Gradle 9.0.0 [CITED: docs.gradle.org/9.0.0/release-notes.html] | A whole-zip SHA is a usable cross-build identity on one toolchain |
| Rollback by `git revert -m 1` | `read-tree -u --reset <pre-tag>` + commit, re-run on the real merge | Phase 16 | Works on any tip |
| One shared `-Xmaxwarns 100` blind spot | explicit `-Xmaxwarns 100000` for warning-absence claims | this phase | A warning-absence claim needs a negative control |

**Deprecated/outdated:** the "Gradle 7.4.2" line in `.claude/CLAUDE.md` (actual 9.6.1); the "30 days" lifetime of any `latest`-derived number (the image moves).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | A gg2 batch of the R3 shape could in principle parse by chance and land wrong rows (not observed; one-row measurements failed at row 2) | R3.3, R3.4 | The note would understate; low, and the recommended wording keeps gg2 item 1's "earlier content stays" rule |
| A2 | The whole-zip SHA stays equal across days and machines with the same toolchain | R4.2, Pitfall 4 | Evidence would show a different zip SHA with identical entries; the per-entry compare still holds |
| A3 | The expected tip diff at the real publication tip is the set in R4.2 plus the version-bearing files | R4.2 | A surprise changed class would mean an unexpected `src/main` edit; investigate, do not wave through |
| A4 | Operators' alerts may be keyed on `Invalid UUID string: ` and `For input string: ` | R1 | Only an example list; the note should say "texts such as" |
| A5 | `size<digits>` outside an `Array` and the `/`, tab, `"` predicates of R5.4 behave as measured on other server versions | R5 | Measured on 26.9.19.9 only; the note names the version |
| A6 | A new Dependabot PR may appear before the tag (PR limit 5, #5 closed) | R9 | Opening is harmless; merging is the user's action and the freeze check catches it |
| A7 | The user wants 999.4 and 999.5 widened per R5 (scope correction) | Open Questions 1, 2 | If not, the note states a narrower, measured-incomplete scope |

## Open Questions

1. **Does the user want 999.4's disclosure widened to `size<digits>` under an `Array`, and the error code stated as "32 or 33"?**
   - Known: measured, wider than the backlog text; the disclosure is documentation only.
   - Unclear: D-01 was decided on the narrower text.
   - Recommendation: widen the note, CHANGELOG line and Known-costs item to the measured scope (accuracy beats the narrower sentence) and say so at plan review; keep D-02 (no workaround, no fix this release).
2. **Does the user want 999.5 widened to `/`, tab and `"`?** `/` is the likely real-world name character and loses data silently on both releases.
   - Recommendation: include `/` in the known-limitation paragraph and the query after proving the predicate live; mention `"` (table not mapped) in one clause. It is still no fix and no CHANGELOG line (D-05), but because the loss is silent consider whether a CHANGELOG Known-issue line is warranted; user decision.
3. **D-03 wording of the headline exception.** Proposed: "the only exception we know of is a Map key, on connectors whose `clickhouseSettings` sets `input_format_binary_read_json_as_string` to `1` or `true` and whose records carry a value Jackson cannot serialize; Connect's own value types do not produce one, so we could only reach it with a constructed record." Needs the user's nod because 22-UAT test 3 accepted the residual on the older wording.
4. **D-04 matrix.** Confirm that the throwing-getter bean is out of scope (R2.6).
5. **D-09 documentation level.** Recommended: shape-limited clean-stop paragraph for the R3 shape, reusing gg2 item 1 (R3.4). The alternative (a sentence saying no clean stop is needed) is not supported for the rollback direction.
6. **Close-record carried count.** CONTEXT says 17 carried findings; the 21-REVIEW has 3 WR + 4 IN, 22-REVIEW 1 WR + 5 IN, 23-REVIEW 2 WR + 4 IN = 19 total, minus 21-IN-03 (fixed) and 23-IN-04 (closed through the docs) = 17, which matches. 22-IN-01 and 22-IN-02 touch error text that this note now describes (an unknown enum label reads `java.lang.NullPointerException`; `No content in enum` lacks the column): consider one sentence in FORK-DELTA only.
7. **Artifact wording for D-07.** No GitHub Release exists for any tag, so the note says "build the artifact from tag `v1.7.0_gg2` with `./gradlew createConfluentArchive`" and states that the build of `pre-v1.7.0_gg3` is byte-identical.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | live tests, probes, suite gate | yes | Docker 29.8.2, colima 0.10.3, profile `vz` Running | none; `colima start --profile vz` |
| `clickhouse/clickhouse-server:latest` | live tests | yes (local, no pull) | 26.9.19.9, image `bdb5c911a23d`, 2026-10-14 | a pull moves `latest`; record the date and `SELECT version()` |
| JDK 17 toolchain | Gradle | yes | Semeru 17.0.9 (3 installs) and Adoptium 17 | none needed |
| Gradle wrapper | build | yes | 9.6.1 | none |
| git, python3, curl | evidence | yes | 2.50.1, 3.14.8, 8.7.1 | none |
| `gh` | CI reads | **no** (by design) | - | unauthenticated REST |
| CI-log token (Keychain `gsd17-gh-actions-read`) | job logs only | **no** (revoked, OPS-03) | - | run-level REST suffices; ask the user for a new read-only token only if a log is needed |
| Network to `api.github.com` | CI polling, OPS-01 | yes (60/hr unauthenticated) | - | web UI |
| `yq` | AGENTS.md matrix lookup | not needed (target is `latest` x V2) | - | - |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** the CI-log token.

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json`. The phase's proof is the SUITE-06 gate (R8), the D-03 live test and the evidence transcripts.

## Security Domain

`security_enforcement` is on (ASVS level 1, block on high).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (no new auth) | CI-log token handling only: user-made, Keychain, never in chat or files |
| V3 Session Management | no | - |
| V4 Access Control | yes (publication) | the user's own explicit approval, orchestrator-inline; protected base branch; no force push |
| V5 Input Validation | yes (docs) | placeholders only in quoted commands; read-only SQL only in the note; `grep -F` literal matching |
| V6 Cryptography | no | SHA-256 only for evidence |
| V7 Error Handling and Logging | yes | value-free error text; evidence and tests use sentinels only; the Map-key residual is disclosed, not hidden |
| V8 Data Protection | yes | no record value, host, credential or token in any committed file (20 close-record rule) |
| V14 Configuration | yes | `tests.yaml` pins unchanged; `integrationTests.yaml` stays upstream-identical and disabled |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Relayed or fabricated approval triggers a publish | Spoofing / Elevation | gate runs inline, user's answer quoted verbatim with receiver; a background-task notification or an agent message is not approval |
| Tag push cancels the branch run | Tampering (of evidence) | tag only after the single run is green; late re-check |
| Wrong tip published (a Dependabot merge slipped in) | Tampering | re-fetch before the gate and right before the merge; recompute the rollback tag from the actual pre-merge tip |
| Record data in committed evidence or a DLQ transcript | Information Disclosure | sentinels only; scan changed files for secret patterns (E-61) |
| Rollout command that destroys other connectors' state | Tampering | no `TRUNCATE` instruction on the shared KeeperMap table (Phase 15 finding); read-only queries |
| Residual Map-key disclosure on the opt-in JSON route | Information Disclosure | measured and disclosed (R2); accepted at 22-UAT test 3; D-04 stop if a value appears |

## Sources

### Primary (HIGH confidence)
- Repository files read this session: `24-CONTEXT.md`, `REQUIREMENTS.md`, `STATE.md`, `22-BEHAVIOUR-LEDGER.md`, `23-BEHAVIOUR-LEDGER.md`, `23-EVIDENCE.md`, `22-VERIFICATION.md`, `22-REVIEW.md`, `22-SECURITY.md`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg2.md`, `CHANGELOG.md` (gg2 section), `build.gradle.kts`, `gradle/libs.versions.toml`, `ClickHouseWriter.java`, `ClickHouseSinkConfig.java`, `ClickHouseHelperClient.java`, `ClickHouseFieldDescriptor.java`, `Table.java`, `DeclaredNullability.java` (diff), `DataJson.java`, `WriterErrorPathLiveTest.java`, `ValueFreeDlqHeadersUnitTest.java`, `.github/workflows/tests.yaml`, `release.yaml`, `.planning/scripts/fork-anchors.txt`, `fork-anchor-exemptions.txt`, `20-COMMAND-EVIDENCE.md` (E-19, E-30, E-31, E-40..E-42, E-60..E-72), `20-MILESTONE-CLOSE-RECORD.md`, `20-REVIEW.md`.
- Live measurements this session (R2-R5), ClickHouse 26.9.19.9, client-v2 0.9.6.
- GitHub REST (unauthenticated): `actions/workflows/integrationTests.yaml/runs?event=schedule`, `pulls?state=all`, `releases`, `actions/runs?branch=custom_build`.

### Secondary (MEDIUM confidence)
- docs.gradle.org/9.0.0/release-notes.html (archive reproducibility default): https://docs.gradle.org/9.0.0/release-notes.html

### Tertiary (LOW confidence)
- none

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new packages, tools probed.
- Architecture / procedure: HIGH for the steps re-used from Phase 20, MEDIUM for anything not re-run (the rehearsal, merge and polling are not run here by instruction).
- Measured behaviour (R2-R5): HIGH on 26.9.19.9 / V2; version-specific.
- Pitfalls: HIGH, each traced to a recorded incident or a measurement.

**Research date:** 2026-10-20
**Valid until:** 2026-10-27. `latest` can move any day; re-check the image date and `SELECT version()` at each gate, and re-run R2-R5 if it moved.
