# Phase 48: Phase 37 Dispositions & Document Fixes - Research

**Researched:** 2026-11-06
**Domain:** closing the carried review rows (four `src/main` advisory rows, the null assertion, 28 IN-05(b), the allow-list glob rows, about 55 review rows in all) with a fix or a written reason, and repairing the published documents (the gg6 note, FORK-DELTA evidence paths), on connector tip `86140ad9` (Phase 47 complete)
**Confidence:** HIGH for the four `src/main` rows (code read, cost measured by throwaway edits: 5 red Docker-free tests for 27 WR-01, 15 compile errors for 26 IN-02, 8 for 28 IN-01, a green additive binding probe for the null assertion); HIGH for the allow-list (five lists measured, published pair re-run to `PASS`, a draft gg8 list measured at 11 exact rows); HIGH for the FORK-DELTA path census (scripted resolver over the tip); MEDIUM for the wording of the document fixes (the wording is the planner's, the facts are read). The real repo is untouched: `git status --short` shows only the two pre-existing dirty files `.planning/config.json` and `.planning/state.json`. All measurements ran under `/tmp/gsd48`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. `.planning/phases/48-phase-37-dispositions-document-fixes/` holds no CONTEXT.md: discuss was skipped (`workflow.skip_discuss: true`) and the user records decisions after reading this research. Every choice that is the user's is in "Decisions that belong to the user at plan time" below, with a recommendation, and none is taken here.

### Claude's Discretion
All implementation choices not listed under the user decisions. Use the ROADMAP Phase 48 section, the standing rules and the codebase conventions.

### Deferred Ideas (OUT OF SCOPE)
None recorded. Constraints restated from the ROADMAP Phase 48 text and its User-Owned Gates table (binding, they are the user's own roadmap): this is the last phase that may edit `.github/`, tests, `build.gradle.kts` or `src/main`, and `src/main` only for the rows fixed under P37-01; a fix that changes an existing assertion or an operator-visible text lands only after the user's approval in their own words; changing or removing an existing assertion is the user's gate in any phase; a `src/main` change outside the phase's declared scope is the user's gate; no existing assertion is weakened; the gg6 note edited here and the FORK-DELTA changes go into Phase 49's independent review; no push, no merge, no tag.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| P37-01 | The four `src/main` advisory rows and the one null assertion are each fixed in their own commit with a test, or closed with a written reason | Finding text, current code, measured cost and blast radius per row; recommendation per row; which need approval in the user's own words (table in "P37-01") |
| P37-02 | The 28 IN-05(b) items each have a fix or a named disposition | 28 IN-05 is two findings in one: (a) client closing, already fixed `1c583b46`; (b) = two items: the null assertion and the `Enum8` literal in `RawIdentifierNameLiveTest` (section "P37-02") |
| P37-03 | The broad allow-list glob rows are narrowed or justified; the dry run reads `0 unallowed, 0 stale rows` | No shipped row is broad (minimum 2 literal segments, measured); the script rule would flip 6 existing test rows, so the rule is moved to an additive unit test; dry-run results measured (section "P37-03") |
| P37-04 | The review Infos each have a disposition | Inventory of 55 rows with a proposed disposition each (section "P37-04") |
| DOC-01 | IN-03's planning identifier leaves the published gg6 note | One occurrence, row D7 of `docs/ROLLOUT-v1.7.0_gg6.md`, "since Phase 39"; the public reference and a second stale clause (FUT-05 in C2) are in "DOC-01" |
| DOC-02 | FORK-DELTA evidence paths resolve under `.planning/milestones/`; the test path carries its package; item 20 names its ledger by a path that survives archiving | Resolver and result (8 literal paths do not resolve; only 3 are repairs), the `KeyToValueTest` trap, item 20's form (section "DOC-02") |
</phase_requirements>

## Pre-flight check (done first)

| Check | Result | Evidence |
|---|---|---|
| Tree | `sync/v1.7.0` at `86140ad9 docs(47): complete phase 47 task stop and retry classification`; only `.planning/config.json` and `.planning/state.json` dirty | [VERIFIED: `git status --short`, `git log --oneline -1`] |
| Anchors file | 218 data rows | [VERIFIED: `grep -v '^#' .planning/scripts/fork-anchors.txt \| grep -c .` = 218; equals the Phase 47 record "anchors 218"] |
| `testUnit` baseline | 2295 tests, 2 skipped (the run below with the 27 WR-01 throwaway edit had 2295 testcases, so the baseline count is 2295; the baseline itself was recorded by Phase 47 E-10) | [VERIFIED: JUnit XML of `/tmp/gsd48/m1` summed = 2295; Phase 47 record "testUnit 2295"] |
| Published tags | `v1.7.0_gg6`, `v1.7.0_gg7` exist; `.github/` and `src/test/java/.../ci/` have no diff against `v1.7.0_gg7`; `gradle/libs.versions.toml` differs only by the added line `kafka-connect-json = { module = "org.apache.kafka:connect-json", version.ref = "kafka" }`; no new top-level file under `src/main` since gg7 | [VERIFIED: `git diff v1.7.0_gg7 HEAD --stat -- .github ...`, `--diff-filter=A -- src/main`] |
| Re-open triggers fired by Phases 45-47? | None of the carried rows' triggers fired: Connect API pin unchanged (25-WR-02(b)), no edit of `.github` or the `ci` tests (25-IN-03, 31-IN-03(b), 37-IN-0x), `SourcePlanningIdGuardUnitTest` still green after the Phase 45 upstream sync (27-IN-03), no new top-level fork class (28-IN-02) | [VERIFIED: the diffs above; Phase 47 record for the green `testUnit`] |
| Docker | colima profile `vz` Running (8 CPU, 16 GiB); `default` profile Stopped (use `vz`); local `clickhouse/clickhouse-server:latest` image id `739e099abd11` | [VERIFIED: `colima list`, `docker images`] |
| Toolchain | OpenJDK 21.0.12.1 default, Gradle 9.8.2 wrapper, Python 3.14.8; use `-PjavaVersion=21` (the OpenJ9 17 toolchain is crash-prone, memory note) | [VERIFIED: `java -version`, `./gradlew --version`] |

## Summary

Six findings change the plan relative to the roadmap text.

1. **27 IN-01 is already closed by Phase 45, with no src change needed.** The review's worry was that `passesThroughUnchanged` returns true for any `ConnectException` or exact `RuntimeException` before it walks the chain, so a Jackson message could pass if a lambda wrapped it. Phase 45 added `throwRebuiltJacksonChain(e, ...)` on exactly that branch (`ClickHouseWriter.java:1095-1097`, commits `f13d8378`, `0c8be1da`) and pinned the review's shapes (`RUNTIME_OVER_JACKSON_ROOT`, `RETRIABLE_OVER_JACKSON_ROOT` in `JacksonOverIoValueFreeUnitTest`, `876a9bd1`). The Javadoc already says a chain that passes and holds a Jackson link is rebuilt. Disposition: `closed`, superseded, pointer to those commits.
2. **The "null assertion" can be tightened without changing an existing assertion.** The Phase 43 ledger said tightening "changes an existing assertion". It does not have to: a new, additive test with the repo's `LogCapture` binds the `null` to the enum scan on both clients. Measured: green on the real stub, and a non-enum failure (malformed JSON row) keeps `describeTable` null while the binding says `false`, which is the red proof that the existing `assertNull` alone cannot tell the two apart. No approval is needed under the roadmap rule (no assertion or text changes), but the ledger tied it to the user's sign-off, so ask once.
3. **27 WR-01 has a measured cost: 5 red Docker-free tests, up to 12 live rows, an operator-visible text, and a published gg4 statement.** The reviewed fix (`DataException` instead of `IllegalArgumentException` in `Column.convertEnumValues`, plus one `import`) turns exactly 3 `ColumnTest` and 2 `WriterValueFreeFailureUnitTest` tests red, flips `RawIdentifierNameLiveTest.legacyEnumLabelKeyFailsLoudly` (up to 12 matrix rows, Docker), and makes the sentence "An unknown Enum label is an `IllegalArgumentException`" of the published gg4 note and the gg4 CHANGELOG line untrue from gg8. It needs the user's approval in their own words. Recommended: close.
4. **26 IN-02's removal deletes an existing assertion.** Both overloads have no caller in `src/main`; deleting them breaks 15 test call sites (measured: 14 + 1 compile errors) and leaves `DescribeTableBuilderUnitTest:399`, `assertEquals(expected.skipsRow(), ClickHouseHelperClient.shouldSkipDescribeRow(row, columnsSoFar, rawTypes), label)`, with nothing to compare. That is a deleted assertion plus an anchor row (`shouldSkipDescribeRow 3 HLP-02`) and a FORK-DELTA row. Recommended: close.
5. **P37-03: nothing is broad, and the script rule would flip 6 existing test rows.** Minimum literal segments over all wildcard rows of the five shipped lists is 2 (`META-INF/versions/*/...`); no row has fewer. A rule in `load_allowlist` would refuse `a/*`, `a*`, `a/*.class`, `a/?.class`, `a/[A].class`, `a/[!x].class`, six of the seven values of the existing `aRowWithALiteralPrefixIsStillAccepted`: a changed existing assertion, on the release path, in the last phase that may edit it. Recommended: no script edit; pin the "two literal segments" rule as a new unit test over every shipped list (it also gates the gg8 list Phase 49 generates).
6. **Two traps in the document rows.** (a) FORK-DELTA row 134 cites `src/test/java/transforms/KeyToValueTest.java` as a file the fork REMOVED (upstream moved it); "correcting" the path to the package form would make a true statement false, so the fix names the surviving package path beside the removed one. (b) The gg6 note has a second stale clause besides IN-03: row C2 and the correction notice call FUT-05 "open", and Phase 47 closed FUT-05 by decision (FORK-DELTA items 17 and 24). Phase 47 handed both to this phase, together with FORK-DELTA item 8, which points at a "Future Requirements" list that no longer carries `FUT-0x` identifiers.

**Primary recommendation:** fix 28 IN-01 (move a dead public constant into the test fixtures) and add the additive pins (null binding, the allow-list rule, four cheap Docker-free test gaps), close everything that would edit `src/main` outside P37-01 or the release path with a reason and a re-open trigger, repair the documents in one wave after the code, and write the 55-row ledger last, so every row has a commit pointer or a reason and none reads "owner: user" alone. The only `src/main` change recommended is the 28 IN-01 move (3 lines out, no behaviour change), so a full suite on `latest` x V2 is required once; if the user closes 28 IN-01 too, Phase 48 changes no `src/main` and the gate is `testUnit` plus the anchors invariant.

## Decisions that belong to the user at plan time

| # | Decision | Options measured | Recommended default (the planner may propose, the user confirms in their own words) |
|---|---|---|---|
| U-1 | 26 IN-02: remove the two `shouldSkipDescribeRow` overloads? | **A close:** dead code, no operator effect, anchor and landmark kept. **B fix:** delete both overloads, retarget 14 + 1 call sites, delete the parity assertion at `DescribeTableBuilderUnitTest:399`, delete or zero anchor row `shouldSkipDescribeRow 3 HLP-02` (218 -> 217), reword FORK-DELTA row 58 | **A.** B deletes an existing assertion (user gate) and edits an anchor for a gain nobody can observe. Re-open trigger: the next edit of the describe code of `ClickHouseHelperClient` |
| U-2 | 27 WR-01: make an unknown enum label a visible, column-named `DataException`? | **A close:** the text stays value-free and names the column, the type and the exception class, as published for gg4. **B fix:** 1 throw line and 1 import in `Column.java`; red: 3 `ColumnTest`, 2 `WriterValueFreeFailureUnitTest`; live: `RawIdentifierNameLiveTest` (up to 12 rows); text changes from ``Failed to write column `c`: ... Enum8: java.lang.IllegalArgumentException`` to `Enum label not found for column c`; the gg4 note item and gg4 CHANGELOG line become history; the gg8 rollout note needs an upgrade line; FORK-DELTA rows 47 and 64 and the gg8 CHANGELOG change | **A.** A second flip of a text the gg4 note told operators about, for diagnosability only. If the user wants the better message, B is cheap and fully measured; it needs their approval |
| U-3 | 27 IN-01: close as superseded by Phase 45? | A close with pointers `f13d8378`, `0c8be1da`, `876a9bd1` (no change) | **A.** Nothing is left to fix; stating it is a ledger row, not a risk |
| U-4 | 28 IN-01: move the unused public `ESCAPED_NEWLINE` into `DeclaredNullabilityFixtures`? | **A fix:** delete 2 lines (+ blank) from `DeclaredNullability.java`, add the constant to the fixtures, repoint 8 references in 3 test files, invert the fixtures Javadoc rule ("must never hold a copy ... tests read the production constant"), reflow one Javadoc paragraph; anchor delta 0; no assertion value changes. **B close:** keep it public and document why | **A.** Production no longer reads it (the anchor row `declaredType.replace(ESCAPED_NEWLINE, SPACE) 0` pins that), so the constant is test data in production code. The roadmap rule does not require approval (no assertion or operator text changes), but the 2026-11-02 ledger tied this row to approval, so ask once |
| U-5 | The null assertion (28 IN-05(b) item i): add the additive binding test? | **A fix:** a new Docker-free test in the same package, `LogCapture` on `ClickHouseHelperClient`, ERROR event whose throwable text holds `Enum type could not be read`, both clients; existing `assertNull` untouched. **B close** | **A.** Same reasoning as U-4: no existing assertion changes, ask once because the ledger said "the user's sign-off to tighten" |
| U-6 | Authorise editing the published notes: `docs/ROLLOUT-v1.7.0_gg6.md` (DOC-01, the FUT-05 clause) and, for 43 IN-02, `docs/ROLLOUT-v1.7.0_gg1.md` | Phase 44's decision O-1: a published note is not edited "without the user's direct authorization"; the roadmap names DOC-01 but the user records it in their own words | **Yes for gg6** (the roadmap requires it). **Yes for gg1 too** (one clause), and name the gg1 note in Phase 49's reviewer brief; the alternative is to close 43 IN-02 |
| U-7 | How the gg6 note says FUT-05 now | **A (recommended):** replace "That follow-up is FUT-05, open in FORK-DELTA under Known costs and open items" in row C2 and the matching clause of the correction notice (line 3) with the plain statement that FORK-DELTA Known costs item 17 records the missing budget as a decided limit; the note then names no planning identifier at all. **B:** keep `FUT-05` as a public handle and say "closed by decision". **C:** leave it | **A.** It satisfies the literal criterion 4 ("names no planning identifier"), removes the stale "open", and the user already chose in Phase 43 that FUT-05 be named only while it was open |
| U-8 | The `src/main`-touching Infos outside P37-01 (45 WR-03, IN-02, IN-03, IN-04; 46 WR-05, IN-05, IN-06; 47 IN-04; 40 IN-01, IN-02; 41 IN-01, IN-02; 42 IN-01, IN-02): fix any? | Each would be a `src/main` change outside the phase's declared scope (user gate), each its own commit and a full suite | **Close all** with the reasons in the ledger. The two with an operator-facing symptom (46 WR-05 misleading double ERROR on a deadline cancel; 45 IN-04 the same family) get one documentation sentence, not code |
| U-9 | P37-03: add a script rule or an additive test? | **A (recommended):** no script edit; new unit test over every `confluent-archive-allowlist-*.txt` requiring at least two literal segments before a wildcard, with refusal fixtures. **B:** rule in `load_allowlist`; changes 6 values of an existing parameterised test and the release path (last phase allowed) | **A** |
| U-10 | DOC-02: the `KeyToValueTest` row | **A (recommended):** keep the removed path as removed and name the surviving `src/test/java/com/clickhouse/kafka/connect/transforms/KeyToValueTest.java`. **B:** rewrite the path to the package form | **A.** B would claim the fork still holds the file it removed |
| U-11 | DOC-02: item 20's ledger name | **A (recommended):** name the new ledger by file, plus the lookup `git ls-files '*48-DISPOSITION-LEDGER.md'`, which resolves wherever the archive puts it. **B:** a tag-bound `git show v1.7.0_gg8:...` (only verifiable after the tag exists). **C:** a fixed non-phase path | **A**, optionally with B as a second form that Phase 49 verifies after the tag |

## Architectural Responsibility Map

| Capability | Primary tier | Secondary tier | Rationale |
|------------|-------------|----------------|-----------|
| Disposition ledger (fix pointer or written reason per row) | Planning records (`.planning/phases/48-*`) | published docs (FORK-DELTA item 20 points at it) | The ledger is a record, not product code; the public doc only names it |
| Enum label miss text, unused public constant, dead overloads (P37-01) | Connector library (`src/main`, `sink.db.mapping`, `sink.db.helper`) | tests and anchors | The only production-code tier touched; each edit is its own commit |
| Null binding, allow-list rule, test-gap pins | Test suite (`src/test`, Docker-free) | `.planning/scripts` lists (read only) | Additive tests; nothing in `src/main` or `.github/` changes |
| Allow-list lists and the compare script | CI / release tooling (`.github/scripts`, `.planning/scripts`) | release workflows | Frozen: no list and no script edit is recommended |
| gg6 note, FORK-DELTA | Published documentation (`docs/`) | reviewer (Phase 49) | Operator-facing text; no test reads these files |

## Standard Stack

### Core
No new library, plugin or tool. The phase uses what exists: JUnit 5 and the in-repo `LogCapture` (`src/test/java/.../sink/util/LogCapture.java`, a reload4j appender already on the test classpath), `python3 -I` stdlib helpers kept in the phase directory, Gradle `testUnit`, and `.github/scripts/compare_confluent_archives.py` run read-only.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `LogCapture` for the null binding | a Mockito spy on `Column.extractColumn` | The repo forbids inline mocks in this area (`.claude/CLAUDE.md`, "no Mockito inline mocks" standing rule); `LogCapture` is the existing seam |
| A new test class for the allow-list rule | a method in `CompareConfluentArchivesScriptUnitTest` | Same file already owns `everyShippedAllowListStillLoads` and the path constants; a method keeps the glob parsing next to its users |

**Installation:** none.

## Package Legitimacy Audit

No external package is installed or changed in this phase (no `build.gradle.kts`, `gradle/libs.versions.toml` or `requirements` edit; Python helpers use the standard library only). The `package-legitimacy` gate is not applicable.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
Carried rows (55)                      Evidence in the repo                    Output
-----------------                      ---------------------                   ------
Phase 37 ledger (43)  -----------.
37-REVIEW / 37-REVIEW-UF  -------+--> read finding text + current code
39-44 reviews (open Infos) ------+    measure cost in /tmp/gsd48 (throwaway)
45-47 reviews (open items) ------'          |
                                            v
                         +---------------- decide per row ----------------+
                         |                                                 |
                  fix (test-first,                                  close (reason,
                  one commit, anchor row                            re-open trigger,
                  in the same commit)                               no "owner: user" alone)
                         |                                                 |
   src/main row? --yes--> approval gate (assertion/text change) --no--> proceed
                         |                                                 |
                         +---------------------+---------------------------+
                                               v
   documents (gg6 D7 + C2, FORK-DELTA paths/item 8/20/94/14, gg1 clause)
                                               v
   48-DISPOSITION-LEDGER.md (pointers = 40-hex commits) + ledger check + resolver run
                                               v
   gates: testUnit (Docker unreachable) + anchors; full suite latest x V2 only if src/main changed
                                               v
   handoff to Phase 49 (reviewer brief: CHANGELOG, FORK-DELTA, gg6 note, gg1 note if edited)
```

### Recommended phase layout
```
.planning/phases/48-phase-37-dispositions-document-fixes/
├── 48-RESEARCH.md
├── 48-DISPOSITION-LEDGER.md      # the new ledger (item 20 names it by file)
├── 48-ledger-check.py            # adapted from 37-ledger-check.py / 43-ledger-check.py (python3 -I)
├── 48-doc-check.py               # the resolver below plus the planning-identifier scan
├── 48-COMMAND-EVIDENCE.md        # E-NN blocks, as in Phases 43-47
└── 48-0N-PLAN.md / SUMMARY.md
```

### Pattern 1: The ledger row
**What:** the Phase 43 vocabulary: `fixed` (pointer is a 40-hex commit read from the evidence file), `dispositioned` (pointer `-`, owner, re-open trigger). The criterion forbids "owner: user alone", so use `closed` for a decided row: a reason that stands without the user, a re-open trigger that names a code or doc event, and the owner column set to `fork maintainer` only as the person who re-opens.
**When to use:** every one of the 55 rows plus the five P37-01 rows.
**Example:** [VERIFIED: `43-DISPOSITION-LEDGER.md:25`, the row being replaced]
```
| 26-IN-02 | dispositioned | - | fork maintainer (the user) | The next edit of the `shouldSkipDescribeRow` anchor row or of the describe code of `ClickHouseHelperClient`, ... | `src/main`, `ClickHouseHelperClient.java`: both `shouldSkipDescribeRow` overloads have no caller in `src/main` and back an anchor row; removal retargets 17 test sites; the user's answer of 2026-11-02: "Defer all four (Recommended)" |
```

### Pattern 2: Additive pin, red by mutation
**What:** a test that passes on the current code gets its teeth shown by a throwaway mutation (the standing rule: "every pin red-first except characterization pins, whose teeth are shown by a mutation run"). For the null binding the mutation is a stub that serves a non-enum failure.
**Example (measured green on `/tmp/gsd48/m4`, 4 tests, plus the existing 2 unchanged):**
```java
// Source: throwaway probe /tmp/gsd48/probes/Probe48EnumCauseUnitTest.java (LogCapture, StubServer pattern of DescribeUnreadableEnumMockServerTest)
private boolean nullBecauseOfTheEnum(boolean v2) {
    try (LogCapture log = LogCapture.attach(ClickHouseHelperClient.class);
            ClickHouseHelperClient chc = client(v2)) {
        assertNull(chc.describeTable("default", "t"));
        return log.events("ERROR").stream().anyMatch(e ->
                e.throwableTexts().stream().anyMatch(t -> t.contains("Enum type could not be read")));
    }
}
// real row (Enum8('\\q' = 1)):      binding true on V1 and V2
// malformed JSON row (mutation):    describeTable still null, binding false on V1 and V2
```
V1 logs ``Exception when running describeTable <query>`` with the throwable (`ClickHouseHelperClient.java:541`), V2 logs `describeTableV2 failed` with the throwable (`:559`); both carry the `IllegalArgumentException("Enum type could not be read")` of `Column.MALFORMED_ENUM_TYPE_MESSAGE`.

### Anti-Patterns to Avoid
- **Fixing a `src/main` Info "while here":** the roadmap allows `src/main` only for the P37-01 rows. Each other `src/main` Info is closed with a reason (U-8).
- **Writing `IN-01`, `WR-01`, `Phase 4x` or `.planning/` in any `src/main` comment:** `SourcePlanningIdGuardUnitTest` fails `testUnit` on `\b(?:D|L|R|F|A|E|T|IN|WR|CR|RF)-\d{1,3}\b`, `\bPhase \d+` and `\.planning/` [VERIFIED: `SourcePlanningIdGuardUnitTest.java:39-50`]. The 28 IN-01 commit must describe the move in the commit message, not in the code.
- **Editing the gg6 or gg7 allow-list, or the tag-copy of a rollout note beyond the named rows:** the two lists are byte-identical to their tag copies [VERIFIED: `git show v1.7.0_gg6:... | cmp` and the gg7 twin print no difference]; the gg6 note edit is limited to row D7 and the FUT-05 clauses.
- **Rewriting a removed path to a present one (row 134):** see U-10.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Ledger format check | a new parser | adapt `.planning/milestones/v1.7.0_gg7-followup-phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py` (and its `37-` ancestor) | Row grammar, pointer regex (`^[0-9a-f]{7,40}`), status tally are already tested in two phases |
| Capturing what the connector logged | a custom appender | `LogCapture.attach(Class)` | Restores the logger level, snapshots throwable texts of every cause |
| Allow-list glob semantics | a second matcher | `fnmatch.fnmatchcase` as the compare script does; for the new test, split at the first of `*`, `?`, `[` | A star crosses slashes; the literal prefix before the first wildcard is what "anchored" means |
| Path census of a document | grep by hand | the resolver in "Code Examples", run on a clone of the tip | 99 candidate tokens in FORK-DELTA alone |

**Key insight:** every row of this phase is a decision plus a pointer. The failure mode is not hard code, it is a ledger that says "fixed" with a pointer that does not carry the change, or "closed" with a trigger nobody can observe.

## Runtime State Inventory

Not a rename or migration phase. None.

## P37-01: the four `src/main` rows and the null assertion

The ledger of Phase 43 (`43-DISPOSITION-LEDGER.md`) recorded the user's answer of 2026-11-02, "Defer all four (Recommended)", with the reasons below as re-open triggers.

### 26 IN-02: `shouldSkipDescribeRow` has no production caller

**Finding (verbatim, `26-REVIEW.md`):** "Both `shouldSkipDescribeRow` overloads have no caller under `src/main`. `describeTableV1` and `describeTableV2` go through `DescribeTableBuilder.addRow`. The two-argument form passes `Map.of()` as `rawTypes`, so it is a different, weaker rule than the one production runs ... **Fix:** Point the tests at `DescribeTableBuilder.decide(...).skipsRow()` and delete both overloads together with the anchor row. Make `arrayDepth()` package-private or remove it."
**Current code** [VERIFIED: `ClickHouseHelperClient.java:508-520`]:
```java
static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Map<String, Column> columnsSoFar) {
    return shouldSkipDescribeRow(fieldDescriptor, columnsSoFar, Map.of());
}
static boolean shouldSkipDescribeRow(
        ClickHouseFieldDescriptor fieldDescriptor, Map<String, Column> columnsSoFar, Map<String, String> rawTypes) {
    return DescribeTableBuilder.decide(fieldDescriptor, columnsSoFar, rawTypes)
            .skipsRow();
}
```
Grep over `src/main` finds `shouldSkipDescribeRow` only on these lines (508, 509, 516, 518 via `decide`); the only `decide(` caller in production is `DescribeTableBuilder.java:138`. `ParentShape.arrayDepth()` (line 49) is still `public` and referenced only by a Javadoc link and the field read at line 74.
**Cost, measured** (`/tmp/gsd48/m2`: both overloads deleted, `compileTestJava`): 15 compile errors, 14 in `ClickHouseHelperClientForkUnitTest` and 1 in `DescribeTableBuilderUnitTest`. Together with the two comment mentions (`ClickHouseHelperClientForkUnitTest.java:31`, `TableTest.java:51`) this is the "17 sites" of the ledger. Other edits: anchor row [VERIFIED: `fork-anchors.txt:29`] `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	shouldSkipDescribeRow	3	HLP-02` (three lines: 508, 509, 516) moves 3 -> 0 or the row is deleted (218 -> 217) in the same commit; FORK-DELTA row 58 ("what `shouldSkipDescribeRow` returns") is reworded; `MERGE-CHECKLIST.md:128` is a historical record and stays.
**Assertion impact:** the 14 calls are of the two-argument form, which equals `decide(row, columns, Map.of()).skipsRow()`, so retargeting keeps each assertion's expectation. But `DescribeTableBuilderUnitTest.java:399` [VERIFIED: read] asserts the wrapper equals the decision's `skipsRow()`; with the wrapper gone that assertion has no counterpart and would be deleted.
**Blast radius:** none at runtime; the anchor is also the documented sync landmark (FORK-DELTA, MERGE-CHECKLIST).
**Recommendation: close.** Reason for the ledger: no production caller, behaviour identical to `decide(..., Map.of())`, the fix deletes an existing parity assertion and an anchor for a gain no operator or reader of production code can see. Re-open trigger: the next edit of the describe code of `ClickHouseHelperClient`, or an upstream sync that touches `shouldSkipDescribeRow`. **Approval:** a fix needs the user's approval (deleted assertion); a close does not.

### 27 WR-01: an unknown enum label is a withheld `IllegalArgumentException`

**Finding (`27-REVIEW.md`, Warning):** "Unknown-enum-label failure is not actionable for the operator ... What an operator sees ... is ``Failed to write column `c`: ... ClickHouse type Enum8: java.lang.IllegalArgumentException``. Nothing says the label is not one of the column's labels ... `WriterValueFreeFailureUnitTest.unknownEnumLabelIsALookupMissWithoutTheLabel` pins this opaque output, so it is now a contract. **Fix:** Throw a `ConnectException` subtype (for example `DataException`) from `convertEnumValues` ... Then update `ColumnTest`, `WriterValueFreeFailureUnitTest` and the CHANGELOG and FORK-DELTA wording."
**Current code** [VERIFIED: `Column.java:626-636`, constants at `:43-45`]:
```java
    public Integer convertEnumValues(String value) {
        if ( this.enumValues != null ) {
            Integer code = enumValues.get(value);
            if (code == null) {
                throw new IllegalArgumentException(String.format(ENUM_LABEL_NOT_FOUND_FORMAT, name));
            }
            return code;
        }
        // This exact RuntimeException reaches Connect status unchanged, which is why the text carries the column only.
        throw new RuntimeException(String.format(NO_ENUM_CONTENT_FORMAT, name));
    }
```
with `ENUM_LABEL_NOT_FOUND_FORMAT = "Enum label not found for column %s"`. `Column.java` has no `DataException` import today (`grep -c DataException` = 0), so the fix adds one `import`.
**Cost, measured** (`/tmp/gsd48/m1`: `IllegalArgumentException` -> `DataException`, `testUnit --rerun` with Docker unreachable): 2295 tests, **5 failures** [VERIFIED: JUnit XML]: `ColumnTest.unknownEnum8LabelIsALookupMissNamingTheColumn`, `.unknownEnum16LabelIsALookupMissNamingTheColumn`, `.nullEnumLabelIsALookupMiss` (all through the helper `assertLookupMissNamesTheColumn`, `ColumnTest.java:241-246`, which asserts `IllegalArgumentException.class`), and the two rows of `WriterValueFreeFailureUnitTest.unknownEnumLabelIsALookupMissWithoutTheLabel` (`:537-554`, expects the withheld ``Failed to write column `c_enum`: ... : java.lang.IllegalArgumentException`` text and a withheld cause line). Docker-needing: `RawIdentifierNameLiveTest.legacyEnumLabelKeyFailsLoudly` asserts `ENUM_WRITE_FAILED_COLUMN_TEXT` and `ENUM_LOOKUP_MISS_TEXT = "could not be written as ClickHouse type Enum8: java.lang.IllegalArgumentException"` (`:52-55`, `:346-352`), once per entry whose legacy label key differs from the label (the matrix has 12 `*-enum.jsonl` files [VERIFIED: `ls | wc -l` = 12]; the filter was not run, so "up to 12"). Anchors: `ENUM_LABEL_NOT_FOUND_FORMAT 2`, `NO_ENUM_CONTENT_FORMAT 2`, `return enumValues.get(value); 0` (rows 102-104) do not move; delta 0.
**Operator-visible text:** yes. Published statements that become history: gg4 CHANGELOG ("An unknown `Enum8` or `Enum16` label now fails as `java.lang.IllegalArgumentException` where it ...", `CHANGELOG.md:284`), the gg4 note ("**An unknown Enum label is an `IllegalArgumentException`.**", `docs/ROLLOUT-v1.7.0_gg4.md:199`), FORK-DELTA rows 47 and 64. A gg8 CHANGELOG bullet and a gg8 rollout-note line (Phase 49) would be needed.
**Behaviour of the failure:** unchanged class at the writer boundary (`DataException` either way) and unchanged retry classification (a `DataException` with no `RetriableException` in its chain); under `errors.tolerance=all` it goes to the DLQ as before.
**Recommendation: close**, reason: the text is value-free by design and carries the column, the type and the exception class, in the shape the gg4 note told operators to expect; the fix flips that text a second time in two releases and changes 5 Docker-free assertions plus the live matrix for a diagnosability gain. Re-open trigger: an operator report that confuses the withheld text with a defect, or the next edit of `Column.convertEnumValues`. **Approval:** a fix needs the user's approval in their own words (existing assertions and an operator-visible text change); a close does not.

### 27 IN-01: `passesThroughUnchanged` short-circuits before the Jackson check

**Finding (`27-REVIEW.md`, Info):** "The first `if` returns true for any `ConnectException` or exact `RuntimeException` before the chain walk ... If a Jackson exception ever reached one of those lambdas ... the wrapper would pass through with the Jackson message ... Nothing tests a `RuntimeException(JsonMappingException)` or `DataException(msg, JsonMappingException)`. **Fix:** Either note in the Javadoc ... or add a pinning test for the wrapped-Jackson shape."
**Current code** [VERIFIED: `ClickHouseWriter.java:1093-1100`]:
```java
        } catch (Exception e) {
            LOGGER.error(WRITE_VALUE_FAILURE_LOG_FORMAT, value == null ? "<value null>" : value.getFieldType(), col.getName(), columnType,
                    e.getClass().getName());
            if (passesThroughUnchanged(e)) {
                throwRebuiltJacksonChain(e, buildValueConversionFailureMessage(col, columnType, value, e));
                throw e;
            }
```
and the Javadoc of `passesThroughUnchanged` (`:696-709`) now ends "A chain that passes through and holds a Jackson link is rebuilt value-free by {@link #withheldJacksonChain} before it is thrown."
**Why it is closed:** `throwRebuiltJacksonChain` walks the whole chain of whatever passed, so the short-circuit no longer decides whether a Jackson message leaves; it only decides the classification path. Pins: `JacksonOverIoValueFreeUnitTest.Shape` rows `RUNTIME_OVER_JACKSON_ROOT`, `RETRIABLE_OVER_JACKSON_ROOT`, `IO_OVER_DATA_OVER_JACKSON`, `IO_OVER_CONNECT_OVER_JACKSON` (`:106-121`). Introducing commits: `f13d8378` (the rebuild), `0c8be1da` (JSON route), `876a9bd1` (pins).
**Recommendation: close as superseded**, pointer `f13d8378, 876a9bd1`. No `src/main` change, no approval. (Optional, cheap, not needed: a `DataException(msg, JsonMappingException)` top shape; the family is `ConnectException`, which the same rebuild covers.)

### 28 IN-01: `ESCAPED_NEWLINE` is a public production constant only tests read

**Finding (`28-REVIEW.md`):** "After the `normalise` rewrite nothing in `src/main` uses `ESCAPED_NEWLINE`; five test files do ... **Fix:** Move the constant to `DeclaredNullabilityFixtures` (test sources) and reflow the paragraph."
**Current code** [VERIFIED: `DeclaredNullability.java:43-44`]: `/** The two characters backslash and n that {@code fromJsonRow} leaves for a server-escaped newline. */` `public static final String ESCAPED_NEWLINE = "\\n";`. `grep -rn ESCAPED_NEWLINE src` finds the definition plus readers in `DeclaredNullabilityTest` (6 errors when removed), `NullableTupleMatrixLiveTest:762`, `SimpleAggregateTupleRoundTripLiveTest:117`, and the fixtures Javadoc (`DeclaredNullabilityFixtures.java:7`: "It must never hold a copy of {@code DeclaredNullability.ESCAPED_NEWLINE}; tests read the production constant."). The class Javadoc paragraph (`DeclaredNullability.java:32-37`) is still wrapped mid-sentence ("That is" ends a line).
**Cost, measured** (`/tmp/gsd48/m3`: constant removed, `compileTestJava`): 8 compile errors (6 + 1 + 1). Anchor delta 0: the only anchor row naming it is `declaredType.replace(ESCAPED_NEWLINE, SPACE)	0	COLS-13` [VERIFIED: `fork-anchors.txt:86`], a zero-count row that stays true. No assertion value changes (same two characters), no operator-visible text, no runtime effect. The two live readers compile in the Docker-free gate and run in the full suite.
**Recommendation: fix** as a `src/main` commit with its test edits in the same commit (the constant move is test-first in the sense that the red state is the compile error; the "red" evidence is the 8 errors). The fixtures rule is inverted in the same commit. **Approval:** not required by the roadmap rule; ask once because the Phase 43 row says narrowing "needs the user's approval because tests in two packages read it".
**Commit message note, not a code comment** (planning ids in `src/main` fail `SourcePlanningIdGuardUnitTest`).

### The null assertion (28 IN-05(b), item i)

`DescribeUnreadableEnumMockServerTest` [VERIFIED: read in full]: `assertNull(chc.describeTable(DATABASE, UNREADABLE_TABLE))` then `assertNotNull(chc.describeTable(DATABASE, READABLE_TABLE))`, on both clients, with a stub serving `Enum8('\\q' = 1)`. The null holds for any exception in the describe. `ColumnTest.enumLabelWithAnEscapeTheServerDoesNotWriteIsRefusedNotMisread` (`:216-220`) pins the same type text at `Column.extractColumn` with message `Enum type could not be read`, so the pair is correct by composition; the binding makes it explicit.
**Recommendation: fix, additively** (U-5): a new test method (or class) beside the existing two, `LogCapture` binding as in Pattern 2. The existing two tests stay byte-identical. Red-by-mutation: the non-enum stub row. Docker-free, 4 new test rows, no anchor, no `src/main`.

## P37-02: the 28 IN-05(b) items

`28-REVIEW.md` IN-05 holds three sentences: (1) the helper client is never closed, (2) `assertNull(describeTable(unreadable))` "is satisfied by any exception inside the describe, not only the enum scan", (3) "`RawIdentifierNameLiveTest.ENUM_LOOKUP_MISS_TEXT` also hard-codes `Enum8` in a message prefix, so adding an `Enum16` matrix entry would fail the pin for a reason unrelated to the behaviour". Part (a) of the Phase 37 ledger is (1), fixed in `1c583b46249ab9bd30ff717c256c7f583c4f0ed9` (the test now has `try (chc)` inside the helper [VERIFIED: read]). Part (b) is therefore **two items**; the Phase 43 row names only the first.

| Item | Current state | Proposed disposition |
|---|---|---|
| (b-i) the null is not tied to the enum | read above | `fixed` by the additive binding test (U-5); pointer is that commit |
| (b-ii) `ENUM_LOOKUP_MISS_TEXT` hard-codes `Enum8` | the constant reads "could not be written as ClickHouse type Enum8: java.lang.IllegalArgumentException" [VERIFIED: `RawIdentifierNameLiveTest.java:54-55`]; the matrix builds only `e Enum8(...)` [VERIFIED: `RawNameMatrix.java:97`]; no `Enum16` entry exists | `closed`: consistent today, a live-only literal; the trigger is an `Enum16` entry in the matrix (the 43 trigger already says so). If U-2 B (27 WR-01) is approved the constant changes anyway |

## P37-03: allow-list glob rows (UF-37-3)

**What UF-37-3 says** [VERIFIED: `37-SECURITY.md:101`]: "`c*`, `com/*`, `outer/*`, `META-INF/*` load ... The shipped lists hold dependency namespaces and exact connector class names only." Phase 43 measured no shipped row with fewer than two literal segments.
**Re-measured now** (script `/tmp/gsd48/wild.py`, literal segments before the first `*`, `?` or `[`): `confluent-archive-allowlist-25.txt` 20 wildcard rows, `-v1.7.0_gg4.txt` 20, `-gg5.txt` 0, `-gg6.txt` 7, `-gg7.txt` 2. Minimum is **2**, one row: `META-INF/versions/*/com/fasterxml/jackson/core/internal/shaded/fdp/*` (in `-25` and `-gg4`). Every other row has 3 or more (`com/google/gson/*` 3, `META-INF/versions/22/com/github/luben/zstd/*` 7). **No row is broad; none is narrowed.**
**Which lists may change:** none needs to. `-gg6` and `-gg7` are byte-identical to their tag copies [VERIFIED: `git show v1.7.0_gg6:<path> | cmp - <path>` and the gg7 twin print nothing]; the `-gg4` and `-gg5` files do not exist at those paths in their tags, and `-25` is a historical Phase 25 list read only by `everyShippedAllowListStillLoads` (any file with the prefix). The gg8 list is Phase 49's, generated last.

**Script rule vs additive test.** `load_allowlist` currently refuses a glob of only stars and any glob whose first character is `*`, `?` or `[` [VERIFIED: `compare_confluent_archives.py:149-155`]. A "two literal segments" rule there would refuse, from the existing parameterised test `aRowWithALiteralPrefixIsStillAccepted` [VERIFIED: `CompareConfluentArchivesScriptUnitTest.java:501-508`], the values `a/*`, `a/*.class`, `a/?.class`, `a/[A].class`, `a/[!x].class` and `a*` (the literal prefix of each is one segment or a partial segment); only `a/A.class` (no wildcard) survives. That is a changed existing assertion on the release path, in the last phase that may edit the script. **Recommended (U-9 A):** no script edit; add one test that reads every `confluent-archive-allowlist-*.txt` under `.planning/scripts` and asserts that every row containing `*`, `?` or `[` has at least two `/` in the text before its first wildcard, with fixtures `com/*`, `outer/*`, `META-INF/*`, `c*`, `com/clickhouse*` refused and `com/google/gson/*`, `META-INF/versions/*/x/*` accepted. It pins the rule for the gg8 list that Phase 49 generates and runs in `testUnit`, so Phase 49 needs no test edit. Red-by-mutation: append `com/*` to a copy of a list in the fixture.
**Ledger row:** UF-37-3 `fixed` by that test's commit, with the measurement as the reason no row is narrowed; 37-IN-05(b) `closed` (see P37-04).

**Dry run, measured** (archives built with `createConfluentArchive` from `git archive` copies of `v1.7.0_gg6`, `v1.7.0_gg7` and `HEAD` in `/tmp/gsd48`, script run as `python3 -I`; the gg7 and tip archives are named `...-v1.7.0_gg7.zip` because `VERSION` at the tip still reads `v1.7.0_gg7`):

| Run | Result |
|---|---|
| gg6 archive vs gg7 archive, list `-v1.7.0_gg7.txt` (the published pair; the script of the tip) | `totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393`, `RESULT: PASS` (no `UNALLOWED`, no `STALE-ROW`) |
| gg6 archive vs tip archive, list `-v1.7.0_gg7.txt` (what the workflow would select while `VERSION` reads gg7) | `RESULT: FAIL`, 4 `UNALLOWED`, 0 stale: `ClickHouseSinkConfig.class` changed, `ClickHouseWriter$3.class`, `$InsertBody.class`, `$InsertWriteDeadline.class` only in B. Expected: the gg8 list does not exist yet |
| gg7 archive (the real gg8 baseline) vs tip archive, comment-only list | 11 `UNALLOWED`, all exact class paths: `ClickHouseSinkConfig`, `ClickHouseSinkTask`, `ClickHouseWriter` (`.class`, `$1`, `$2`, new `$3`, `$InsertBody`, `$InsertWriteDeadline`), `ClickHouseHelperClient` (`.class`, `$ClickHouseClientBuilder`), `Utils`; `totals ... only-b 3 changed 8` |
| the same pair with a draft list of those 11 exact rows (throwaway, not committed) | `exit 0`, 0 `UNALLOWED`, 0 `STALE-ROW`, `RESULT: PASS`; the draft holds 0 wildcard rows |

So "the dry run reads `0 unallowed, 0 stale rows`" has two checkable forms in Phase 48: the published pair with the committed gg7 list (green now), and the tip against gg7 with a scratch draft (green by construction, rows grow by each `src/main` class Phase 48 changes: `DeclaredNullability.class` if 28 IN-01 is fixed). The committed gg8 list is Phase 49's (RLS-02); recommend the criterion text be read that way and the planner record both runs as evidence.

## P37-04: review rows still open (55 rows)

Source of each row: the Phase 43 ledger (`43-DISPOSITION-LEDGER.md`), `37-REVIEW.md`, `37-REVIEW-UF.md`, the `*-REVIEW-DISPOSITION.md` of Phases 39-47 (status `open`/`skipped`/`deferred`). Disposition vocabulary: **fix** = commit with a test; **doc** = document edit with a commit; **close** = written reason and re-open trigger. "User" marks a decision of the table above.

### A. Phase 37 lineage (the 43 ledger and the two Phase 37 reviews)

| ID | Finding in one line | Proposed | Reason / trigger |
|---|---|---|---|
| 26-IN-02, 27-WR-01, 27-IN-01, 28-IN-01, 28-IN-05(b) | see P37-01, P37-02 | close, close, close (superseded), fix, fix + close | U-1..U-5 |
| UF-37-3 | broad allow-list glob rows | fix (test) | P37-03, U-9 |
| 37-IN-01 | workflows accept `_gg[0-9]+`, scripts need `_gg[1-9][0-9]*`; wrong failure text | close | Fails closed; `VERSION` is written by `bump_version.py`, which refuses `_gg0` and leading zeros; the fix edits both release-workflow twins (the publish path) and a fixture, in the last phase that may edit `.github/`. Trigger: next edit of `release.yaml`/`pre-release.yaml` |
| 37-IN-02 | an allow-list row cannot say changed/added/removed | close | A kind column changes the list format on the release path; the gg8 draft is 11 exact rows, so no row hides an add or remove. Trigger: a list with a wildcard row over a namespace |
| 37-IN-03 and UF-IN-02 (same defect) | corrupt archive escapes `load_inner` as a traceback, exit 1 not 2 | close | Fails closed (the workflow fails the step on any non-zero exit); a script edit needs byte-patched corrupt zips in tests. Trigger: a traceback in a CI log |
| 37-IN-05(b) | `-25.txt` and `-gg6.txt` first line names the removed script path | close | gg6 is identical to its tag copy (published); `-25` is historical and read only by one test; Phase 49's new list carries the correct header and its check reads it. Trigger: the next list authored |
| UF-IN-01 | zero-width and bidi characters pass through `safe_line` | close | Cosmetic: they cannot start a workflow command (reviewer's own analysis). Trigger: next edit of `UNSAFE_CHARS` |
| UF-IN-03 | `set -o pipefail` comment cites "a runner log" without a pointer, 173 columns | close | Comment-only edit of both publish twins plus a fixture string for no operator value; the claim is recorded in `37-SECURITY` A-04 |
| 30-IN-04 | the tests gate accepts a green run of any branch on the SHA | close | Condition checked per release by the publication record (FORK-DELTA item 21); Phase 49 records the single `custom_build` run. Trigger: a publish whose only green run came from a branch with different workflow files |
| 31-IN-02 | image tags not digest-pinned | close | Harness change outside this phase. Trigger: a nightly red from an image pull |
| 31-IN-03(b) | the test requires `python3` | close | Declared intentional in the Javadoc. Trigger: a machine without `python3` |
| 25-WR-02(b) | classpath half of the Connect-reference derivation | close | No cheap Docker-free fix; Connect pin unchanged since gg7 (measured). Trigger: a Connect API pin or Confluent test-stack change |
| 25-WR-03 | a type used only in a signature leaves no member reference | close | Not reachable today. Trigger: such a connector change |
| 25-IN-02 | `ConfluentArchiveCheck` misses `META-INF/versions/<n>/` Kafka classes, nested zips, caps at 20 | close | None occurs in the release archive (`kafka classes in b: 0`, `nested jars in b: 0` in the compare output above). Trigger: a Shadow bump |
| 25-IN-03 | exact-text assertions fail misleadingly after a refactor | close | Never miss a regression. Trigger: Kotlin DSL rewrite of the archive task |
| 25-IN-04(a), (c) | guava lines upstream-owned; no wrapper checksum | close | The lines are upstream-owned (guava) and a wrapper checksum touches every wrapper bump; no operator value. Triggers as in the 43 ledger |
| 27-IN-02 | `JsonAsStringKeyResidualLiveTest` name stale | close | Rename touches four FORK-DELTA lines and four anchors; accepted name. Trigger: next edit of the class |
| 27-IN-03 | `SourcePlanningIdGuardUnitTest` scope and single-letter prefixes | close | Upstream sync of Phase 45 did not trip it (measured: green). Trigger: a sync whose comment trips it |
| 28-IN-02 | `ForkCoverageScopeUnitTest` cannot see a missing fork class | close | No new top-level fork class since gg7 (measured). Trigger: a new fork class |
| 28-IN-04(b) | no live matrix entry for the bell | close | Needs a Docker matrix run and a data edit; the Docker-free pin exists (`1c583b46`). Trigger: a server release that changes the measured escapes |

### B. Phases 39-44 reviews (Infos left `open`)

| ID | Finding | Proposed | Reason / trigger |
|---|---|---|---|
| 39-IN-01, 39-IN-02 | integration-test topic literal; hard-coded partition counts in a failure message | close | `src/integrationTest`, diagnostics only; an edit costs a Confluent-platform rerun for no behaviour. Trigger: next edit of those tests |
| 39-IN-03 | `_CONTROL` names that expect `RETRIABLE`; dead 0.9.6 branch | close | Renaming shapes changes test ids that Phase 45-47 evidence tables track by name. Trigger: next edit of the two pin classes |
| 40-IN-01 | `ownedClient` duplicates `chc` in `KeeperStateProvider` | close | `KeeperStateProvider` is a user gate (exactly-once state). Trigger: the injected-client constructor is repaired |
| 40-IN-02 | late `updateMapping` after `stop()` logs `null` | close | `src/main` outside P37-01; shutdown only. Trigger: next edit of `updateMapping` |
| 40-IN-03 | `ProxySinkTaskStopUnitTest` misses a throwing `close()` and a throwing unregister | fix (additive) | Two new cases, Docker-free, mutation by flattening the `finally` in a clone |
| 41-IN-01 | three identical `InsertSettings` setups | close | Hot-path refactor of three insert sites; outside scope; the stall pins cover all three. Trigger: a fourth insert site |
| 41-IN-02 | redundant blank check in `operatorNamesSocketTimeout` | close | `src/main`, no behaviour. Trigger: next edit |
| 41-IN-03 | stall-pin POST count may race the stub | close | Theoretical; `InsertStallBoundPinUnitTest` was green (15 tests, 0 failures) in the Phase 47 pre-flight run. Trigger: the first CI flake |
| 42-IN-01 | pass-through branch logs nothing; ERROR line contradicts a retriable outcome | close | `Utils.handleException` decides classification: a diff there stops for the user (Phase 47 note); log text only. Trigger: next edit of `handleException` |
| 42-IN-02 | vetted code set defined in two places | close | `src/main`, constant move. Trigger: next edit of the vetted set |
| 42-IN-03 | port race, blank/duplicate entry shapes untested | fix (additive, the shapes only) | Add `" "`, `","`, `"159,,60"`, `"60,60"`, `"60,"` to the existing value sets with expected lists; the port race stays (close, unreproduced) |
| 42-IN-04 | `RetriableExceptionPassThroughUnitTest` has no plain `DataException` control | fix (additive) | Add `PLAIN_DATA_EXCEPTION_CONTROL` rows `(none, DATA_EXCEPTION)`, `(all, RETURNS)` |
| 43-IN-01 | FORK-DELTA row 94 says "before the tag gate" though `CHAIN` now pins the tests gate, the tag gate and the publish step | doc | One-clause reword [VERIFIED: row 94 text read: "the compare chain in order before the tag gate and the publish step"] |
| 43-IN-02 | gg1 notice says "measured on the release" | doc (U-6) or close | [VERIFIED: `ROLLOUT-v1.7.0_gg1.md:3`, last sentence]; say "measured on the `v1.7.0_gg6` tree"; needs the gg1 authorisation and a Phase 49 brief line |
| 43-WR-03 (Warning, `skipped`) | repointed commands in FORK-DELTA | close as fixed by Phase 44 | `git diff --shortstat v1.7.0_gg4 HEAD` no longer occurs in FORK-DELTA; lines 193 and 199 use tag pairs [VERIFIED: grep]; the Phase 44 commit is `e979004e` ("tag-pair commands, raw numbers") |
| 44-IN-03 | "Phase 39" in the gg6 note | doc | DOC-01 |

### C. Phases 45-47 open items

| ID | Finding | Proposed | Reason / trigger |
|---|---|---|---|
| 45-WR-03 | retry-classification knowledge duplicated between `handleException` and the writer rebuild, no tying test | close | The fix is a new `Utils` API (outside scope, hot path). `JacksonOverIoValueFreeUnitTest` has `keepsTheClassifiedLinks` and `keepsTheRetryOutcome` over its 21 `Shape` rows (names read; I did not read their bodies, so the planner should confirm they compare the rebuilt chain's retry outcome with the original's before citing them). Trigger: `handleException` reads another fact |
| 45-WR-04 | three new test classes copy one helper block | close | A refactor that moves assertion code between files; "only add tests" rule. Trigger: a fourth copy |
| 45-IN-02 | failure-path text formatted eagerly | close | `src/main`, failure path only, small cost |
| 45-IN-03 | two "message withheld" constants | close | Tied transitively: both texts are asserted against the same literal in `WithheldMessageExceptionUnitTest:27` and `JacksonOverIoValueFreeUnitTest:65` (and four other test files) [VERIFIED: grep "message withheld"]; a change to either fails a test |
| 45-IN-04 and 46-WR-05 (same family) | two ERROR lines per failing RowBinary column; on a deadline cancel they name a record and column | close + doc | `src/main`; add one sentence to FORK-DELTA item 14 (and Phase 49's gg8 Known Issues) that a deadline cancel logs two ERROR lines naming a record and column while the exception thrown is the timeout [VERIFIED: neither FORK-DELTA nor CHANGELOG says so, grep "misleading" and "two ERROR" empty] |
| 45-IN-05 | edited upstream tests weaken assertions | close | Recorded user decision U-2 of Phase 45; FORK-DELTA carries the named-edit row |
| 46-IN-05 | V1 timeout ignores `cancel(true)` result (microsecond window) | close | No deterministic red-first pin; needs a seam in `awaitInsertResponseV1`. Trigger: next edit of that method |
| 46-IN-06 | `stop()` during an insert can surface `RejectedExecutionException` | close | Shutdown only; `chc` is nulled the same way. Trigger: next edit of `stop()` |
| 47-IN-01 | `assertProxyStoppedOnce` assertions never seen red; no MBean precondition | fix (additive) | `assertTrue(harness.mBeanRegistered())` before `stop()`; a mutation of the writer stop shows `CLOSE_COUNT` red |
| 47-IN-02 | stop pin mutates a `private final` field by reflection | close | Works on JDK 17 and 21; Trigger: a JDK bump that warns (JEP 500) |
| 47-IN-04 | `ClickHouseWriter.stop()` swallows an interrupt (pre-existing) | close | `src/main`, outside the diff of Phase 47 and outside this phase's scope |

Row count (counted from the tables above): A 26 ids, B 17 ids, C 12 ids (45-IN-04 and 46-WR-05 are two ids sharing one table line) = **55**. The planner's ledger check must fix the exact list from the ledger rows and the `*-REVIEW-DISPOSITION.md` files; the Phase 41 disposition front matter says `open: 4` for three open rows, a record oddity, so count rows and not that field.

**Not review Infos, listed so the ledger can say so:** UF-37-5 (the `37-assets/*.txt` copies, archived), UF-37-6 (the `scratch/clt-07-*` branches, RLS-06 in Phase 49), UF-37-7 (ROADMAP wording, archived), UF-37-8 (the header comment, closed as 37-HDR `566f92e4`), UF-36-4 (FORK-DELTA item 18).

## DOC-01: IN-03 and the gg6 note

**Where** [VERIFIED: `grep -nE "Phase [0-9]+|FUT-[0-9]+" docs/ROLLOUT-v1.7.0_gg6.md` over the tip]: one `Phase NN` occurrence, line 148, row D7, column "How known": "...at this release no test in the repository asserted the forwarding; since Phase 39, `ClientEndpointAndOptionsPinUnitTest.customKeysAreForwardedWithDoubledEncodingClientVersionTripwire` pins it, so a later client bump fails that test instead of changing it unnoticed." The tag copy has 0 occurrences of either identifier (`git show v1.7.0_gg6:docs/ROLLOUT-v1.7.0_gg6.md | grep -cE "Phase [0-9]+|FUT-[0-9]+"` = 0); "Phase 39" came in with `12379bf1` (`fix(39): WR-02 point rollout row D7 at the Phase 39 forwarding pin`), and `FUT-05` with Phase 43's `965e646f`. The 44 review and the gg7 audit attribute the identifier to Phase 43; `git log` says Phase 39.
**Public reference to use:** the release tag, `v1.7.0_gg7`: the test method exists in the gg7 tree (1 hit for `Tripwire` in `ClientEndpointAndOptionsPinUnitTest.java` at `v1.7.0_gg7`) and not in the gg6 tag (0 hits) [VERIFIED: `git show <tag>:<path> | grep -c Tripwire`]. Replacement: "since `v1.7.0_gg7`, `ClientEndpointAndOptionsPinUnitTest.customKeysAreForwardedWithDoubledEncodingClientVersionTripwire` pins it ...". The method name resolves on the tip [VERIFIED: `ClientEndpointAndOptionsPinUnitTest.java:114`]. This is 44-REVIEW IN-03's own suggested fix.
**Second stale clause (Phase 47 handoff, `47-04-SUMMARY.md:135`, `47-VERIFICATION.md:213`):** line 3 (the correction notice, "Row C2 now names FUT-05, the open follow-up for a budget ...") and row C2 (line 114, "That follow-up is FUT-05, open in [`FORK-DELTA.md`](FORK-DELTA.md) under Known costs and open items.") are stale: FORK-DELTA closes FUT-05 by decision (items 17 and 24) [VERIFIED: `FORK-DELTA.md:232`, "FUT-05, a retry budget or a \"remove from default list\" setting for retriable codes, is closed by decision (items 17 and 24)"]. `FUT-05` is also the only other planning identifier in the note (two occurrences, lines 3 and 114), so criterion 4's literal reading ("names no planning identifier") needs U-7 A. Phase 43's handoff 2 said FUT-05 there was deliberate, because it was open; that reason is gone. Do not claim in the gg6 note that the decision was made at gg6: it is dated 2026-11-05 (FORK-DELTA item 17 and 24).
**What must not change:** the note's rule "No instruction, label or measured value changed" (line 3); D7's labels and measured values; row C2's instruction (only transient codes belong in the list).
**Check:** the resolver's identifier scan in "Code Examples" (regexes below) must read 0 on `docs/ROLLOUT-v1.7.0_gg6.md` after the edit; the same scan reads 0 today on the tag copy.

## DOC-02: FORK-DELTA evidence paths

**Resolver** (kept as `48-doc-check.py`; the version run here is at `/tmp/gsd48/scripts/resolve.py`): tokens that start with `.planning/`, `src/`, `docs/`, `.github/`, `build/`, `gradle/`, `scripts/` or `config/` (plus `.gitattributes`, `.gitignore`), resolved against a `git archive` copy of the tip; a `...` in a path stands for `**/`; `<...>` placeholders are skipped. Run over `docs/FORK-DELTA.md`: 99 candidate tokens, 69 literal paths resolve, 20 abbreviated `src/test/.../X.java` paths each resolve to exactly one file, 2 placeholders skipped, **8 literal paths do not resolve**:

| Token | Lines | What it is | Fix |
|---|---|---|---|
| `.planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md` | 37 | evidence moved with the gg4 archive | `.planning/milestones/v1.7.0_gg4-hardening-phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md` [VERIFIED: resolver found it there] |
| `.planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md` | 32 | evidence moved with the gg6 archive | `.planning/milestones/v1.7.0_gg6-maintenance-phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md` [VERIFIED] |
| `src/test/java/transforms/KeyToValueTest.java` | 134 | a file the fork REMOVED (row text: "the fork's orphaned ... and the fork's `src/test/java/transforms/KeyToValueTest.java` ... Upstream deleted or moved these; the fork's copies were removed rather than re-expressed"); the surviving file is `src/test/java/com/clickhouse/kafka/connect/transforms/KeyToValueTest.java` | U-10 A: keep the removed path, mark it removed and name the surviving package path; this is "the test path missing its package" of the audit |
| `.github/workflows/test-head.yaml` | 89 | documented upstream deletion, FORK-DELTA records it as deleted | none; list as by-design absent |
| `.planning/scripts/compare-confluent-archives.py` | 92 | the "moved from" name of the script | none; by design |
| `build/raw-name-matrix` | 114 | a generated, uncommitted build directory | none; by design |
| `gradle/actions/setup-gradle` | 90 | a GitHub action name, not a path (the same token is in `CHANGELOG.md:98`) | none |
| `gradle/verification-metadata.xml` | 252 | a file that does not exist (item 18 explains its absence) | none; by design |

So three repairs (two moved evidence files, one removed-path row) and five by-design absences that the check must carry as an exception list, otherwise it never reads 0.
**Other FORK-DELTA reference that does not resolve semantically:** item 8 (line 232) says "The Future Requirements list in `.planning/REQUIREMENTS.md` remains open ... (FUT-01) ... (FUT-02) ... (FUT-03) ... (FUT-04)". The file exists, but its Future Requirements section [VERIFIED: `REQUIREMENTS.md:61-64`] now lists only "Upstream PRs #861, #855, #854 and #788" and "Many-to-one rename and partition-changing SMTs", with no `FUT-0x` identifier (Phase 47 E-11 row 10). The identifiers live in `.planning/milestones/v1.7.0_gg6-maintenance-REQUIREMENTS.md:80-84` [VERIFIED: grep]. Fix: point item 8 at that file under `.planning/milestones/`.
**Bare file names:** every `NN-NAME.md` token of FORK-DELTA (38 names, e.g. `13-BEHAVIOUR-LEDGER.md`, `45-COMMAND-EVIDENCE.md`) resolves to exactly one tracked file [VERIFIED: `git ls-files .planning | grep -c "/<name>$"` = 1 for each]; that is the form line 26 deliberately uses "because that path moves".
**Item 20** [VERIFIED: `FORK-DELTA.md:256`]: "Each has its row, owner and re-open trigger in `43-DISPOSITION-LEDGER.md`, which disposes of every row Phase 37 left open (the Phase 43 directory under `.planning/phases/` until the milestone is archived, then under `.planning/milestones/`)." Two defects: the ledger is already under `.planning/milestones/v1.7.0_gg7-followup-phases/43-document-fixes-phase-37-dispositions/` (gg7 is archived), so the sentence is stale, and the rows are about to be closed, so "published knowingly open" and "stay dispositioned, not fixed" become wrong. Rewrite item 20 for the Phase 48 outcome and name the new ledger by file plus the lookup `git ls-files '*48-DISPOSITION-LEDGER.md'` (U-11 A); that command resolves on any tip, before and after the gg8 archive.
**Will break at the gg8 archive (not fixable now):** the `.planning/phases/45-...`, `46-...` and `47-...` `COMMAND-EVIDENCE.md` paths resolve today (lines 47, 53, 117, 228, 242, 244, 250 and others) and move at the next milestone close. The target directory does not exist yet, so they cannot be repointed in Phase 48. Record it as a handoff to the milestone close, as the gg7 audit did for 25-* and 35-*; the resolver check should be in the close checklist. An optional guard test over FORK-DELTA paths is not recommended: it would turn the archive step red by design and needs an exception list.
**Not part of DOC-02, same cause:** `CHANGELOG.md`, the gg6 and gg4 notes resolve with 0 broken paths except the action name above [VERIFIED: resolver run on each].

## Docker split

| Work | Docker | How |
|---|---|---|
| Ledger, ledger check, resolver, document edits (gg6, gg1, FORK-DELTA) | none | text, `python3 -I` |
| Additive pins: null binding, allow-list rule, 40 IN-03, 42 IN-03/IN-04, 47 IN-01 | **Docker-free** | `testUnit` with `DOCKER_HOST=unix:///nonexistent.sock`, `--rerun`, `-PjavaVersion=21`; mutation runs are throwaway edits in a clone |
| 28 IN-01 move (and 26 IN-02 / 27 WR-01 if approved) | compile and `testUnit` Docker-free | red state of the move is the compile error; of 27 WR-01 the 5 red tests |
| Live readers of the moved constant (`NullableTupleMatrixLiveTest`, `SimpleAggregateTupleRoundTripLiveTest`) and the live enum pin | **Docker needed** | in the full suite |
| Dry-run compare (builds of two tags and the tip) | none | `createConfluentArchive` needs no Docker (3 builds ran in about 12 s each, cached) |
| Phase gate: full suite on `latest` x V2 | **Docker needed only if `src/main` changed** (28 IN-01 does) | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ... --rerun`; record `SELECT version()` and the image digest first (`latest` drifts; local image id `739e099abd11`); colima profile `vz` |
| Anchors gate | none | the standing command below, once, on an explicit SHA |

## Anchors (the invariant)

Baseline 218 data rows (Phase 47 recorded `anchors 218`). Standing gate [VERIFIED: `MERGE-CHECKLIST-v1.7.0-tail-3.md`, "Gate commands"]:
```bash
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 <range end>
```
(self-test `bash .planning/scripts/verify-merge-invariants-selftest.sh`, last line `selftest: no failures`; the cumulative form took about 3 minutes in Phase 47). Rule: a row changes in the same commit as the code it pins.

| Commit | Rows | Delta |
|---|---|---|
| 28 IN-01 move (recommended) | none: the only row naming the constant is `...DeclaredNullability.java	declaredType.replace(ESCAPED_NEWLINE, SPACE)	0	COLS-13`, a zero row that stays 0 (optional new zero row `public static final String ESCAPED_NEWLINE	0`) | 0 (or +1) |
| 26 IN-02 removal (only if approved) | `...ClickHouseHelperClient.java	shouldSkipDescribeRow	3	HLP-02` -> delete (or count 0) | 218 -> 217 |
| 27 WR-01 fix (only if approved) | none move (`ENUM_LABEL_NOT_FOUND_FORMAT 2`, `NO_ENUM_CONTENT_FORMAT 2`, `return enumValues.get(value); 0`); optional new row for the `DataException` throw line | 0 (or +1) |
| every test-only and doc commit | none | 0 |

## Gate shape

- **Per commit:** cache-busted `testUnit` (`--rerun`, Docker unreachable, `-PjavaVersion=21`), expected 2295 plus the new cases, no removed row, 0 failures (the 2 skips are the baseline); the tail gate once at the end of the code wave on an explicit SHA.
- **Mutation evidence for each additive pin** (throwaway edit in a clone, never committed), recorded as E-NN: the non-enum stub for the null binding; a `com/*` row for the allow-list test; flattening the nested `finally` for 40 IN-03; dropping the `DataException` branch for 42 IN-04.
- **Phase gate:** cache-busted `testUnit`, the anchors invariant, and a full suite on `latest` x V2 if `src/main` changed (Phase 47 recorded 2898 tests with 18 named skips); if the user closes 28 IN-01 too, the full suite is not required by the roadmap.
- **Formatting:** the repo has 46 pre-existing Spotless-violating files [memory: code-review --fix gotchas, restated by Phase 47 research]; format per touched file, do not run a repo-wide `spotlessApply`.

## Proposed plan decomposition (sequential waves on one working tree)

| Wave | Plan | Content | Commits |
|---|---|---|---|
| 1 | 48-01 | Record the user decisions (CONTEXT), baselines (`testUnit`, anchors, tail gate, the published-pair dry run), `48-ledger-check.py`, `48-doc-check.py` with the by-design exception list | docs only |
| 2 | 48-02 | Additive Docker-free pins, one commit each: null binding (U-5), allow-list rule (UF-37-3), 40 IN-03, 42 IN-03 shapes, 42 IN-04, 47 IN-01 | 6 test commits |
| 3 | 48-03 | `src/main` rows under P37-01, one commit each with its tests and anchor rows in the same commit: 28 IN-01 (recommended); 26 IN-02 and 27 WR-01 only if approved. Then the tail gate and the full suite on `latest` x V2 | 1 to 3 code commits |
| 4 | 48-04 | Documents: DOC-01 (gg6 D7, C2, notice), DOC-02 (FORK-DELTA lines 32, 37, 134, 232, 256, row 94, item 14 sentence), gg1 clause if authorised; resolver and identifier scan read 0 | 3 to 4 doc commits |
| 5 | 48-05 | `48-DISPOSITION-LEDGER.md` (every pointer a 40-hex commit read from `48-COMMAND-EVIDENCE.md`), ledger check, dry-run record (published pair and draft), phase gate, handoff list for Phase 49 | 1 to 2 |

**Handoff to Phase 49 (write it into the ledger):** the reviewer brief must name `docs/ROLLOUT-v1.7.0_gg6.md`, `docs/FORK-DELTA.md`, `CHANGELOG.md` and, if edited, `docs/ROLLOUT-v1.7.0_gg1.md`; the gg8 CHANGELOG gets a Known Issues sentence for 46 WR-05 and, if U-2 B is approved, a bullet for the enum text and an upgrade line in the gg8 note; the gg8 allow-list header must name `.github/scripts/compare_confluent_archives.py` and every wildcard row must follow two literal segments (the new test checks it); the gg8 draft is 11 exact rows today (one more if `DeclaredNullability.class` changes); FORK-DELTA's `.planning/phases/45-47` evidence paths move at the gg8 milestone close.

## Common Pitfalls

### Pitfall 1: Closing a row whose reason the next phase invalidates
**What goes wrong:** 27 IN-01's reason ("needs a test") was true at gg4 and false after Phase 45. **How to avoid:** read the current code before copying a Phase 43 reason; the ledger note must cite the line or commit that makes it true now. **Warning signs:** a reason with a line number older than `v1.7.0_gg7`.

### Pitfall 2: A "fixed" pointer that is the test commit, not the change
**What goes wrong:** the Phase 43 rule is a 40-hex commit "that carries the fix". For a two-commit fix (test, then code) point at the code commit and name the test commit in the note. **How to avoid:** `48-ledger-check.py` resolves each pointer with `git cat-file -e` and, for `fixed`, requires the commit to touch a file named in the row.

### Pitfall 3: Planning identifiers leaking into `src/main`
`SourcePlanningIdGuardUnitTest` fails `testUnit` on `IN-01`, `WR-01`, `Phase 48` and `.planning/` in any `src/main` line. The 28 IN-01 commit carries the row id in its message only.

### Pitfall 4: Treating `KeyToValueTest` as a typo
See U-10. The row documents a removal; the resolver lists it as "removed by design" unless the wording changes.

### Pitfall 5: Reading "dry run" as the tip against the gg7 list
That run fails by design (4 `UNALLOWED`) until Phase 49 writes the gg8 list. Record the published-pair run and the draft run, and say which is which.

### Pitfall 6: A new test file under `.planning/scripts` or a list edit
`everyShippedAllowListStillLoads` loads every file with the prefix `confluent-archive-allowlist-`; a scratch list left there is read as shipped. Keep drafts in `/tmp`.

### Pitfall 7: Editing a published note beyond the named rows
The gg6 note's header promises "No instruction, label or measured value changed". Keep the edit to D7's "since" clause and the FUT-05 clauses; a diff against `v1.7.0_gg6:docs/ROLLOUT-v1.7.0_gg6.md` is the check.

## Code Examples

### Resolver core (the 99-token census of FORK-DELTA)
```python
# Source: /tmp/gsd48/scripts/resolve.py (run with python3 -I against a git archive copy of the tip)
ROOTS = (".planning/", "src/", "docs/", ".github/", "build/", "gradle/", "scripts/", "config/")
for m in re.finditer(r"[^\s`()\[\],;|]+", text):
    tok = m.group(0).rstrip(".:'\"").lstrip("'\"")
    if not tok.startswith(ROOTS) and tok not in (".gitattributes", ".gitignore"): continue
    if any(c in tok for c in "<>{}"): continue                      # placeholders
    p = tok.rstrip("/")
    if "..." in p:                                                  # src/test/.../X.java
        hits = glob.glob(os.path.join(root, p.replace(".../", "**/")), recursive=True)   # must be exactly 1
    else:
        hits = glob.glob(...) if any(c in p for c in "*?") else ([...] if os.path.exists(...) else [])
```

### Planning-identifier scan for the published notes
```python
# DOC-01 check: reads 3 hits on the gg6 note today (Phase 39, FUT-05 x2), 0 on the v1.7.0_gg6 tag copy, 0 wanted after
PAT = r"\.planning|\bPhase [0-9]+|\b[A-Z]{2,6}-[0-9]{2}\b|\bFUT-[0-9]+|\b[DLREFT]-[0-9]{1,3}\b|[0-9]{2}-(REVIEW|RESEARCH|COMMAND-EVIDENCE)"
```
[VERIFIED: the equivalent `grep -noE` over `docs/ROLLOUT-v1.7.0_gg6.md` printed `148:Phase 39`, `3:FUT-05`, `114:FUT-05`.]

### Allow-list wildcard rule as a test helper
```java
// Source: derived from compare_confluent_archives.py:149-155 and the measured lists; refuses com/*, outer/*, META-INF/*, c*
static boolean followsTwoLiteralSegments(String glob) {
    int wild = indexOfFirst(glob, '*', '?', '[');
    return wild < 0 || glob.substring(0, wild).chars().filter(c -> c == '/').count() >= 2;
}
```

## State of the Art

| Old approach | Current approach | When changed | Impact |
|---|---|---|---|
| Phase 43 ledger: five rows `dispositioned`, owner "the user" | `closed` with a reason and a trigger that stands without the user, or `fixed` with a commit | this phase | Criterion 2 |
| gg6 note: "FUT-05, open in FORK-DELTA" | FORK-DELTA item 17 (decided limit) | Phase 47 (2026-11-05) | Row C2 and the notice are stale |
| `.planning/phases/25-*`, `35-*` evidence paths | `.planning/milestones/<milestone>-phases/...` | gg4 and gg6 archives | DOC-02 |
| Phase 45 rebuild on the pass-through branch | closes 27 IN-01 | Phase 45 | no src change for that row |

**Deprecated/outdated:** the Phase 43 ledger's reason for 27 IN-01; item 20's sentence about where the ledger lives.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The live `legacyEnumLabelKeyFailsLoudly` row count under U-2 B is "up to 12" (12 matrix enum files counted; the legacy-key filter was not run) | P37-01, 27 WR-01 | Only the size of the live edit; no decision depends on the number |
| A2 | `e979004e` is the Phase 44 commit that repointed the HEAD-relative commands (its subject says "tag-pair commands, raw numbers"; I confirmed the lines now use tag pairs, not the diff of that commit) | P37-04 B, 43-WR-03 | A wrong pointer in a `closed (fixed by Phase 44)` row; the ledger check should resolve it |
| A3 | A `-25.txt` header edit would be harmless, which is why closing 37-IN-05(b) is a choice and not a necessity | P37-04 A | None if kept closed |
| A4 | Operators may match on `java.lang.IllegalArgumentException` in the `Failed to write column` text (the gg4 note names that class); not verified against any deployment | U-2 | Only affects the weight of the "flip twice" argument |
| A5 | The wording of the replacement for the FUT-05 clauses and the item 20 rewrite is the planner's; only facts are fixed here | DOC-01, DOC-02 | None |

## Open Questions

1. **Does the user want the 27 WR-01 diagnosability, accepting a second text flip?**
   - What we know: the cost is fully measured (5 red Docker-free tests, up to 12 live rows, one import, docs, an upgrade line).
   - What's unclear: operator impact of the second flip.
   - Recommendation: close (U-2 A) unless the user asks for the fix.
2. **Which dry run does criterion 3 mean?**
   - What we know: the tip against the gg7 list fails by design; the gg8 list is Phase 49's.
   - What's unclear: whether the user reads criterion 3 as the published-pair run, the draft run, or both.
   - Recommendation: both, recorded as separate evidence entries.
3. **Should FORK-DELTA's `.planning/phases/45-47` evidence paths be made archive-proof now?**
   - What we know: they resolve today and move at the gg8 close; the target does not exist.
   - Recommendation: hand off to the milestone close; do not add a guard test.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | full suite, live pins | yes | Running, 8 CPU, 16 GiB (`default` profile Stopped; use `vz`) | `testUnit` with `DOCKER_HOST=unix:///nonexistent.sock` for everything Docker-free |
| `clickhouse/clickhouse-server:latest` | full suite on `latest` x V2 | yes (local) | image id `739e099abd11`; re-record `SELECT version()` and the digest before the gate | none |
| JDK | Gradle | yes | OpenJDK 21.0.12.1 | `-PjavaVersion=21` (OpenJ9 17 toolchain is crash-prone) |
| Gradle wrapper | build, archives | yes | 9.8.2 (3 archive builds and a 38 s `testUnit` ran from cache) | `--offline` |
| `python3 -I` | resolver, ledger check, compare script | yes | 3.14.8 | none |
| `git archive`, `git cat-file` | throwaway trees, pointer check | yes | system | none |

**Missing dependencies with no fallback:** none.

## Validation Architecture

Skipped: `.planning/config.json` has `workflow.nyquist_validation: false` [VERIFIED: read]. The gate is the roadmap's, described under "Gate shape".

## Security Domain

`security_enforcement` is enabled, `security_asvs_level: 1`, `security_block_on: high` [VERIFIED: `.planning/config.json`].

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V7 Error Handling and Logging | yes | The only log or text surface touched is 27 WR-01 (if U-2 B): the new `DataException` text carries the column name only and no label; `LogCapture` in the binding test reads throwable texts of metadata only; no new log line carries a record value |
| V14 Configuration / supply chain | yes | No allow-list edit and no script edit; the new allow-list test makes a broad wildcard row a `testUnit` failure (T-37-05 residual) |
| V2, V3, V4, V5, V6, V8 | no | no auth, session, access-control, input-validation, crypto or data-protection change |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| An allow-list row that silences a whole namespace of archive differences | Tampering | Test that every wildcard row follows two literal segments; compare script's existing refusals stay unchanged |
| A ledger row closed with no evidence | Repudiation | `48-ledger-check.py`: pointers resolve with `git cat-file`, `fixed` rows touch a named file, no `owner: user` alone |
| A record value reaching an operator-visible text through a rewording | Information disclosure | Texts built from column metadata only; `SourcePlanningIdGuardUnitTest` and the value-free pins stay green |
| An edit of a published note that changes an instruction | Tampering (documentation) | The diff against the tag copy limited to named rows; Phase 49's independent review |

## Project Constraints (from CLAUDE.md)

- `AGENTS.md`: Java 17, Google Java Style through Spotless (format per touched file); only add tests, never delete or weaken existing ones; high-risk files (`Processing`, `KeeperStateProvider`, `ClickHouseWriter`, `ClickHouseSinkConfig`, `RangeContainer`) are not touched under the recommended branches; do not commit without being asked (the GSD workflow commits per plan); logs and messages carry no record data; never change dependencies without asking.
- `.claude/CLAUDE.md`: behaviour preservation (code limited to genuine bug fixes, the fork runs in production), test target ClickHouse `latest` x client V2, additive and rebasable changes, reuse of `ClickHouseBase` and the existing helpers, GSD workflow enforcement.
- `MEMORY.md` standing notes that bind this phase: commit the per-plan STATE.md; the orchestrator spawns a real reviewer on release documents before the push gate (Phase 49, not here); the wave `post` gates run per wave; `OpenJ9` crash escape `-PjavaVersion=21`; the code-review `--fix` run skips the reviewer on the first run; 46 pre-existing Spotless violations.
- ROADMAP User-Owned Gates that apply: an assertion change, an operator-visible text change, a `src/main` change outside P37-01, any push (none planned).

## Sources

### Primary (HIGH confidence)
- This session's runs under `/tmp/gsd48`: `git archive` copies of `v1.7.0_gg6`, `v1.7.0_gg7` and `HEAD`; three `createConfluentArchive` builds; the compare script on four pairs; `testUnit` on a copy with the 27 WR-01 edit (2295 tests, 5 failures, listed above); `compileTestJava` on copies with the 26 IN-02 and 28 IN-01 edits (15 and 8 errors); the binding probe `probes/Probe48EnumCauseUnitTest.java` (4 green, plus the 2 existing tests green); the wildcard-segment script and the FORK-DELTA resolver.
- Files read in full or by line range: `ROADMAP.md` (Phase 48 and Gates sections), `REQUIREMENTS.md`, `docs/ROLLOUT-v1.7.0_gg6.md` (lines 3, 114, 148), `docs/FORK-DELTA.md` (lines 26, 32, 37, 47, 58, 134, 232, 256), `MERGE-CHECKLIST-v1.7.0-tail-3.md` (gate), `43-DISPOSITION-LEDGER.md`, `37-ADVISORY-LEDGER.md`, `37-REVIEW.md`, `37-REVIEW-UF.md`, `37-SECURITY.md` (UF-37-1..8), the `26`, `27`, `28` reviews and dispositions, the `39` to `47` reviews and dispositions, `47-04-SUMMARY.md`, `47-VERIFICATION.md`, `47-RESEARCH.md` (shape), `compare_confluent_archives.py` (`load_allowlist`), the five allow-lists, `release.yaml` (lines 63-135), `ClickHouseHelperClient.java`, `Column.java`, `ClickHouseWriter.java`, `DeclaredNullability.java`, `DescribeUnreadableEnumMockServerTest.java`, `ColumnTest.java`, `WriterValueFreeFailureUnitTest.java`, `RawIdentifierNameLiveTest.java`, `CompareConfluentArchivesScriptUnitTest.java`, `SourcePlanningIdGuardUnitTest.java`, `LogCapture.java`, `fork-anchors.txt`.

### Secondary (MEDIUM confidence)
- The wording proposals for the document fixes; the "up to 12" live row count.

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new dependency.
- Architecture: HIGH, one working tree, sequential waves as in Phases 45-47.
- Pitfalls: HIGH for 1-6 (each measured or read), MEDIUM for 7.

**Research date:** 2026-11-06
**Valid until:** until the next `src/main` or `docs/` edit lands in Phase 48; re-run the resolver and the wildcard script after any list or FORK-DELTA change.
