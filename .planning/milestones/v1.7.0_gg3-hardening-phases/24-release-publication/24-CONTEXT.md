# Phase 24: Release & Publication - Context

**Gathered:** 2026-10-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Publish `v1.7.0_gg3` on `custom_build`. Four requirements:

- **ROLL-03:** write and review `docs/ROLLOUT-v1.7.0_gg3.md` before the publication gate. It covers:
  - writer error text in the task status, the DLQ headers and the ERROR logs no longer carrying the record value, and what to grep for instead;
  - the MBean `version=` segment and the client name;
  - the lombok, spotless and slf4j-reload4j bumps;
  - every other operator-visible change in ledger rows L-42..L-51.
- **PUB-09:** set `VERSION` to `v1.7.0_gg3`, add a plain-language `1.7.0_gg3` CHANGELOG section, and give `docs/FORK-DELTA.md` a row for every fork-side patch from Phases 21-23 plus a raw-numbers refresh.
- **SUITE-06:** pass the full suite twice on ClickHouse `latest` × client V2. The second run uses `--rerun`. Both need 0 failures, 0 errors and every skip named. `testUnit` must be green with Docker stopped.
- **PUB-10:** after the user's own explicit approval, land a non-force `--no-ff` merge on `custom_build`.
  - The published SHA has exactly one green `tests.yaml` run.
  - The `v1.7.0_gg3` tag is pushed only after that run is green.
  - `pre-v1.7.0_gg3` points at the pre-merge `custom_build` tip.

**Starting state (checked 2026-10-20 during this discussion):**
- `origin/custom_build` = `6473e24f`, which is `a246ca01` (`v1.7.0_gg2`) plus the three Dependabot merges. `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` = `0 120`, so no DEPS-01-style merge is needed today.
- `VERSION` = `v1.7.0_gg2`, and there is no gg3 note.
- `upstream/main` = `f4952f38`, 8 commits past `v1.7.0` with no new tag. The 4 non-merge commits are Dependabot bumps that the fork already carries since gg1: jackson 2.22.3, mockito 5.24.0, okhttp 5.5.0 and org.json 20260814. There is nothing to sync. The upstream sync stays out of scope.
- Five Dependabot PRs are open on origin: #4 (client 0.10.0, out of scope), #6 (commons-lang3 3.21.0), #7 (ben-manes 0.66.0), #8 (Gradle wrapper 9.8.1) and #9 (`connect-api` 4.4.0, a fork-only pin).
- Open review findings: 21-REVIEW has 3 WR and 4 IN, 22-REVIEW has 1 WR (accepted at 22-UAT) and 5 IN, and 23-REVIEW has 2 WR and 4 IN. All are `open` in their `*-REVIEW-DISPOSITION.md`.

**Locked by ROADMAP, STATE and precedent, not re-opened here:**

*Publication*
- **Recipe (ROADMAP Phase 24 note, Phase 20 shape):**
  1. Push `sync/v1.7.0` once as a scratch branch, with an explicit refspec and never at a SHA another pushed branch already holds. Wait for green.
  2. Push the rollback tag `pre-v1.7.0_gg3` at the pre-merge `custom_build` tip, before the release merge (20-D-15).
  3. Merge `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip.
  4. Push `custom_build` only, and wait for its single run.
  5. Push the `v1.7.0_gg3` tag by name.
  6. Delete the scratch branch.
- **Merge checks:**
  - The merge has two parents, its first parent is `origin/custom_build`, and its tree equals the rehearsed tip.
  - The upstream-sync parent checks of `verify-merge-invariants.sh` do not apply, and must not be "fixed".
  - The anchors invariant runs on the branch tip as `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`, with the SHA-pinned exemption list (21-D-01/D-02). If another published bump of an anchored line has to be absorbed, it gets a new exemption row and no script edit.
- **Rollback mechanics:** use the `read-tree -u --reset` plus commit form, because `git revert -m 1` is broken. The rollback commands are re-run on a dry-run merge of the publication tip before the push (SC1).
- **Approval gates** run orchestrator-inline and need the user's own explicit answer, given directly. A relayed approval is refused. `custom_build` is GSD's protected base branch.
- **One branch per SHA.** `tests.yaml` triggers on branch pushes only, so a tag push starts no run. CI is read through the unauthenticated GitHub REST API, because `gh` is not installed.
- **Before the gate**, re-fetch `origin/custom_build`. If it moved, merge it into `sync/v1.7.0` and re-run the gate, and the rollback tag follows the actual pre-merge tip.

*Docs and bookkeeping*
- **Docs ship inside the merged tree**, written and reviewed before the gate. The `VERSION` anchor row (`fork-anchors.txt:41`) moves in the bump commit. FORK-DELTA's raw numbers are refreshed only here. PUB-09 owns the AGENTS.md pre-PR obligation: the VERSION bump plus the CHANGELOG entry.
- **Rollout note shape (20-D-01):** a delta note on top of gg2. It opens by pointing operators who come from a release before `v1.7.0_gg2` at `docs/ROLLOUT-v1.7.0_gg2.md`, and through it at gg1. The gg1 and gg2 notes and their CHANGELOG sections stay as released.
- **Writing disciplines (16-D-02, 16-D-07/D-08, 20-D-03):**
  - plain-language prose, with planning IDs only as trailing ledger tags;
  - every quoted command or SQL in the note is run live and its output matches the text;
  - transcripts go into `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md`;
  - every measured statement names the server version.
- **Pre-gate review (20-D-07):** `gsd-code-reviewer` reviews everything Phase 24 commits, and its findings go into `24-REVIEW-DISPOSITION.md` before the approval is asked for.
  - Budget two to three review rounds for the operator command text.
  - Procedures are proven by running them, not only by review, because the auditor misses procedure bugs (memory: Phase 15 review-gate gotchas).
- **CHANGELOG shape (20-D-10):** Important Changes, Bug Fixes and Build and CI, with the `1.7.0_gg2` section as the template.

*Suite gate and close*
- **Mechanics:**
  - every Gradle call runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew …`;
  - the second run uses `--rerun`;
  - `testUnit` runs with the Docker daemon stopped (colima profile `vz`);
  - `SELECT version()` is recorded at each gate;
  - if the suite goes red on unchanged code, check the `latest` image date first.
- **The pre-gate bundle re-checks OPS-01:** no `schedule` run of `integrationTests.yaml` since 2026-10-16T09:38:23Z.
- **Milestone close:** the milestone audit runs after Phase 24 verification and before `/gsd-complete-milestone` (20-D-12).

</domain>

<decisions>
## Implementation Decisions

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

### Post-research decisions (user, 2026-10-20, answering 24-RESEARCH Open Questions 1, 2, 4/3 and 5)
- **D-12 (OQ1, 999.4 scope):** **widen the disclosure to the measured scope.** D-01's rollout item, CHANGELOG Known-issue line and FORK-DELTA Known-costs item state:
  - a Tuple element named `size` or `size0` at any depth, **plus** an element named `size<digits>` wherever an `Array` encloses its Tuple (24-RESEARCH R5.1);
  - inserts fail with **code 32 or 33**, depending on shape, not only code 33.

  Ship the two-predicate `system.columns` query (R5.2), re-proven live in execution, with its one known over-flag (`Array(Map(String, Tuple(size1 …)))`) stated. The note says the query lists suspects from the declared type text, and that the log grep `have no matching DESCRIBE sub-column` is the exact check. D-02 holds: no fix and no workaround in this release.
- **D-13 (OQ2, 999.5 scope):** **widen the known-limitation paragraph and the FORK-DELTA Known-costs item** (R5.3):
  - a backslash, a `/` or a tab in a column or element name means the Kafka field must use the connector's escaped form (the measured Column-name column), or the default is stored silently with no error;
  - one clause says that a `"` in a name leaves the table unmapped.

  The detection query adds the `/` and tab predicates only after they are proven live against fixtures, including look-alikes. **No CHANGELOG line**, as D-05 decided.
- **D-14 (OQ4 + OQ3, D-03/D-04 matrix and wording):**
  - The D-03 probe matrix is **Connect-typed values only**. The contrived bean whose getter throws with the value in its message is **out of scope**. It is recorded as such in the evidence, so D-04's third branch cannot fire on it. With Connect-typed content only a Map key appears (R2.2), so D-04's **first branch** applies: no code change, and the residual stays accepted.
  - The headline exception is worded: "the only exception we know of is a Map key, on connectors whose `clickhouseSettings` sets `input_format_binary_read_json_as_string` to `1` or `true` and whose records carry a value Jackson cannot serialize".
  - The operator self-check greps the connector config for `input_format_binary_read_json_as_string`. **There is no `binaryFormatWrtiteJsonAsString` connector key**; that flag is derived at `ClickHouseSinkConfig.java:355-356`.
- **D-15 (OQ5, D-09 level):** **use a shape-limited clean stop.** `Nullable(Tuple)` with a backslash-n or line-break element name, whether a root, an `Array` element, a `Map` value or a Tuple field, is the one shape whose bytes differ (R3). For that shape, in the rollback direction only, the note reuses gg2 item 1's clean-stop procedure and KeeperMap self-check, proven live (20-D-03 discipline, `DedupTokenRetryLiveTest`'s measured rule). Every other change gets one sentence each way saying that no clean stop is needed.

**Research corrections to the decisions above** (facts measured in 24-RESEARCH, not new choices; word the documents from these):
- **D-08 "which ship":** none of the three bumps changes the shipped `createConfluentArchive` zip. `slf4j-reload4j` and `reload4j` are **not in the zip**: they are `runtimeClasspath` only. Lombok 1.18.48 output is byte-identical, and spotless is build-time only (R4.2). The note's dependency item says all three leave the shipped artifact unchanged. Re-measure in execution.
- **D-07 artifact:** no GitHub Release or asset exists for any tag. "Redeploy the artifact built from tag `v1.7.0_gg2`" therefore means the operator's own `./gradlew createConfluentArchive` build at that tag. A build of `pre-v1.7.0_gg3` gives the same zip bytes (R4.2).
- **D-06 count:** 17 carried findings, confirmed (19 total, minus 21-IN-03 and 23-IN-04).
- **Starting refs:** the left/right count is now `0 122`, because of the two planning commits since the discussion. Re-measure at the gate.

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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements
- `.planning/ROADMAP.md` § "Phase 24: Release & Publication": goal, the 4 success criteria and the publication-recipe note.
- `.planning/ROADMAP.md` § Backlog, "Phase 999.4" and the 999.5 entry: the defect facts, severity and fix sketch that D-01/D-05 disclose.
- `.planning/REQUIREMENTS.md`: ROLL-03, PUB-09, SUITE-06, PUB-10, the milestone scope decisions and Out of Scope.
- `.planning/STATE.md` § Blockers/Concerns: the [Env], [Tooling], [Publish], [CI], [Phase 23] and [Dependabot] items.
- `.planning/PROJECT.md` § Requirements → Active: the Phase 21, 22 and 23 follow-ups, the source of D-06's carried list and D-11's reminders.

### Rollout, CHANGELOG and FORK-DELTA source material
- `.planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md`: L-42..L-48, covering the value-free error text, the withheld-message cause line, the Code 33 refresh loss, the grep targets and the DLQ headers.
- `.planning/phases/23-declared-nullability-hardening-review-closure/23-BEHAVIOUR-LEDGER.md`: L-49..L-51, covering null totality, the quote-aware `normalise` and the unpaired-element WARN.
- `.planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md`: R-01 (999.4 live outcome) and R-02 (backslash escaping, 999.5).
- `.planning/phases/22-writer-record-data-exposure/22-SECURITY.md`: R-22-02, the accepted Jackson residual, and its "verifier measurement" wording against FORK-DELTA's "not measured" (D-03).
- `.planning/phases/22-writer-record-data-exposure/22-UAT.md`: test 3, the user's acceptance of the 22-WR-01 residual.
- `docs/ROLLOUT-v1.7.0_gg2.md`: the note gg3 deltas against. Use its intro, three-part item format and placeholder vocabulary as the template: item 4's alert-rule grep self-check, item 5's MBean/client name item, and item 6's rollback procedure and "rolling back brings back" list.
- `docs/ROLLOUT-v1.7.0_gg1.md`: item 7 (the client-name query) and item 8 (the older rollback). Read-only.
- `docs/FORK-DELTA.md`: the patch tables (the BLD-14 row at `:65` per 21-IN-03), § The raw numbers, and § Known costs (item 6 per D-03, and new items for 999.4 and 999.5).
- `CHANGELOG.md`: the `1.7.0_gg2` section is the structural template.

### Review dispositions
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-REVIEW.md` and `21-REVIEW-DISPOSITION.md`
- `.planning/phases/22-writer-record-data-exposure/22-REVIEW.md` and `22-REVIEW-DISPOSITION.md`
- `.planning/phases/23-declared-nullability-hardening-review-closure/23-REVIEW.md` and `23-REVIEW-DISPOSITION.md`
- `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md`: the shape for D-06's carried findings (owner and re-open trigger) and D-11's reminders.

### Publication mechanics and precedent
- `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-CONTEXT.md`: D-01..D-16, the decisions carried here.
- `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-COMMAND-EVIDENCE.md`: the evidence format. E-19 (MBean and client name), E-30 (the alert-rule grep on a scratch directory), E-31 (rollback on a dry-run merge with `rollback-demo.sh`), E-40..E-42 (FORK-DELTA raw numbers and coverage audit), E-60..E-71 (suite gate, pre-push checks, rehearsal, rollback tag, release merge).
- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-CONTEXT.md`: D-11..D-13, the publish mechanics and approval-bundle shape.
- `.planning/milestones/v1.7.0_gg2-hardening-phases/16-early-docs-publication/16-CONTEXT.md`: the writing disciplines and red-CI handling.
- `.planning/scripts/fork-anchors.txt`: the `VERSION` row at `:41` moves in the bump commit.
- `.planning/scripts/fork-anchor-exemptions.txt`, `.planning/scripts/verify-merge-invariants.sh`, `.planning/scripts/verify-merge-invariants-selftest.sh`: the anchors invariant with the exemption list. Its merge-parent checks do not apply to this merge.
- `.github/workflows/tests.yaml`: branch-only trigger, concurrency group on `github.sha`.
- `.github/workflows/integrationTests.yaml`: OPS-01 re-check, file upstream-identical.
- `.github/dependabot.yml`: the `dependencies` label request (D-11).
- `AGENTS.md`: the pre-PR obligations and test rules.

### Code the probes touch
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`: the D-01 IOException pass-through (~L640-651, reached through the JSON case ~L873, per 22-REVIEW WR-01).
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`: `binaryFormatWrtiteJsonAsString` (`:137`, `:356`).
- `build.gradle.kts`: `:106` `slf4j-reload4j:2.0.20` (`implementation`, shipped), `:98-99` lombok `compileOnly`, `:397` `createConfluentArchive`. `gradle/libs.versions.toml` has the lombok and spotless versions.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WriterErrorPathLiveTest` and `ValueFreeDlqHeadersUnitTest`: the live and Docker-free shape for the D-03 probe. They drive `ClickHouseSinkTask.put()` under `errors.tolerance=none`/`all`, render the DLQ headers the way Connect 4.1.1 does, and use `LogCapture` for the `Error inserting records` logs.
- `WriterValueFreeFailureUnitTest`: sentinel-assertion helpers (absent from every message, `toString()` and suppressed entry in the chain, and present for column, type and class).
- `DedupTokenRetryLiveTest`: the measured same-token retry rule that D-09 relies on if any shape's bytes differ.
- `ClickHouseHelperClientForkUnitTest` (the D-04 skip-aware discovery pin, labelled known defect 999.4) and `NullableTupleMatrixLiveTest` (the COLS-13 live premise): fixtures for the 999.4 and 999.5 detection-query proofs.
- `20-COMMAND-EVIDENCE.md` E-31's `rollback-demo.sh`: the rollback re-run on a dry-run merge, adaptable to `pre-v1.7.0_gg3`.

### Established Patterns
- **Delta rollout notes with three-part items:** What changed, Am I affected? (with an empty-result meaning), What to do. Placeholders only, no hosts.
- **Behaviour ledgers feed both the CHANGELOG and the rollout note.** FORK-DELTA rows already exist per commit (D-13 discipline), so Phase 24 verifies coverage and refreshes the raw numbers.
- **Measured, not cited.** Every gate bundle and every operator claim rests on a transcript taken in this phase.

### Integration Points
- The D-03 live test rides the SUITE-06 gate. The rollout note and FORK-DELTA item 6 quote it.
- The D-08 artifact diff feeds the note's dependency item, D-07's rollback wording, the FORK-DELTA BLD-14 row and the 21-IN-03 disposition.
- The carried-findings close record (D-06) and the reminders (D-11) feed the milestone audit and `/gsd-complete-milestone`.

</code_context>

<specifics>
## Specific Ideas

- The gg3 note opens like the gg2 note: "coming from a release before `v1.7.0_gg2`? Work through `docs/ROLLOUT-v1.7.0_gg2.md` first".
- An operator who sees the new unpaired-element WARN right after upgrading should find, in one item, what it means, a query to confirm the table, and a plain statement that this release has no fix.
- The headline value-free claim must not overclaim for anyone who enabled `binaryFormatWrtiteJsonAsString`. The exception is worded on a measurement.

</specifics>

<deferred>
## Deferred Ideas

- The 17 carried review findings (21-, 22- and 23-REVIEW). They go in the milestone close record with an owner and a re-open trigger each (D-06), and any fix belongs to a future milestone.
- The 999.4 fix: discovery keeps a declared Tuple element named `size`/`size0`. It is its own phase, in upstream-region `ClickHouseHelperClient`, and it flips the D-04 pin. This is the backlog entry.
- The 999.5 decision on the intended name form for backslashes in names. Backlog entry.
- Dependabot `ignore` entries for the fork-only pins (#4, #9), and creating the `dependencies` label or dropping the request. These are Phase 21 follow-ups and user actions, reminded only (D-11).
- Any of Dependabot PRs #6-#8 shipping in a release. They are unfrozen after the tag and are the user's decision. A merge triggers a DEPS-01-style merge in the next milestone.
- The next upstream sync, once upstream tags past `v1.7.0` (`upstream/main` is 8 commits past the tag, all bumps the fork already carries).

</deferred>

---

*Phase: 24-release-publication*
*Context gathered: 2026-10-20*
