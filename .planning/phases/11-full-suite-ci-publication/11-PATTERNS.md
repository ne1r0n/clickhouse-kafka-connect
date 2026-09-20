# Phase 11: Full Suite, CI & Publication - Pattern Map

**Mapped:** 2026-09-20
**Files analyzed:** 6 (5 modified, 1 new)
**Analogs found:** 6 / 6 (all tracked source; verified with `git ls-files`)

This phase is a publication/proof phase, not a feature phase — no controllers/services/components exist to classify. Files are documentation, a CI workflow, a shell script, a Java test-fixture doc comment, and two planning bookkeeping documents. All six analog paths below are confirmed **git-tracked** via `git ls-files` (none are gitignored mirrors).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `docs/FORK-DELTA.md` | doc (new) | transform (checklist → ledger) | `docs/DESIGN.md` (structure/tone) + `.planning/MERGE-CHECKLIST.md` (source data) | role-match (structure), exact (source data) |
| `.planning/scripts/verify-merge-invariants.sh:44` | utility/script | batch (git history scan, exit-code gate) | itself — no sibling script demonstrates the idiom; pattern must come from Bash convention, not a codebase analog | no analog (see below) |
| `.github/workflows/tests.yaml:33-35` | config (CI) | event-driven (on: push) | itself (surgical edit — remove 2 lines, adjust comment) | exact (self) |
| `src/testFixtures/.../ClickHouseTestHelpers.java:32-38` | utility (Javadoc only) | n/a (doc comment) | itself (surgical edit — correct 1 sentence) | exact (self) |
| `.planning/MERGE-CHECKLIST.md` Section 2 rows (6 flips) | checklist/doc | CRUD (row status update) | Section 1 "done" rows in the same file, e.g. `KeyToValue.java` row (line 32) and `ClickHouseSinkConfig.java` row (line 23) | exact (same file, same row schema) |
| `.planning/REQUIREMENTS.md` § SUITE checkboxes + Traceability rows | checklist/doc | CRUD (checkbox + table row update) | `HARN-01..07` checkbox block (lines 94-110) and its Traceability rows (lines 218-224) — same file, most recently closed section | exact (same file, same schema) |

## Pattern Assignments

### `docs/FORK-DELTA.md` (new doc)

**Structural analog:** `docs/DESIGN.md` (473 lines... actually 274 lines total)

`docs/DESIGN.md`'s real heading shape (verified via grep, `docs/DESIGN.md:1-274`):
```
# Introduction
## Contents                          <- manual TOC, hand-linked anchors
# Requirements
## Addressing exactly-once
### Known Challenges
### High-level approach
#### Deduplication flow
#### High-level architecture
### Storing State
### State Machine
#### Initial state (`NONE`)
#### Full state machine
#### Cropping batches
#### Invalid states
### Scaling
### Internal Buffering
#### Implementation
#### Flush failure semantics
```

Observations the planner should carry into the new doc's outline:
- `docs/DESIGN.md` opens with a one-paragraph `# Introduction` (`docs/DESIGN.md:1-3`) stating what the doc covers, then a manual `## Contents` anchor-link list (`:5-20`) — no auto-generated TOC tooling.
- It mixes prose, Markdown tables (e.g. the state-machine table at `:157-164` and the compact-rules table at `:196-207`), and fenced `mermaid` diagrams (`:58-77`, `:101-121`) plus links to static PNGs as a fallback ("Original diagram: ...", e.g. `:86`, `:132`). `FORK-DELTA.md` has no diagrams to carry over — skip the mermaid/PNG convention, keep the prose+table mixing.
- Tone is second-person-avoidant, explanatory engineering prose ("We dismissed solutions using...", `:52`), not marketing language — matches D-13's "state it plainly rather than picking the flattering metric."
- It ends with a pointer to a PR for deep detail (`:273`: "For the full design rationale, see [PR #736](...)"). `FORK-DELTA.md` should end with (or open with) a pointer to `MERGE-CHECKLIST.md` as its index, per D-15.
- **How it is linked** (verified, both tracked files): `AGENTS.md:19` — `` Full design rationale: [`docs/DESIGN.md`](./docs/DESIGN.md) `` (a one-line bullet inside a table row, not prose). `README.md:12` — `` For a full overview of the design and how exactly-once delivery semantics are achieved, see the [design document](./docs/DESIGN.md). `` (a standalone prose sentence). Both are cheap one-line additions if the planner takes the Claude's-Discretion option to link `FORK-DELTA.md` (CONTEXT.md "Claude's Discretion").
- Length: `docs/DESIGN.md` is 274 lines — a full design doc, not a stub. `FORK-DELTA.md` per D-13/D-16 needs at minimum: a behaviour-level ledger section, a raw-numbers-disclosed section (including the "insertions grew" admission), and a "Known costs and open items" section (R-01, WR-01, the four D-08 findings, the named skip inventory) — expect comparable length once all of D-13..D-16 are satisfied, not a short stub.

**Source-data analog:** `.planning/MERGE-CHECKLIST.md` — this is what D-15 says to derive the ledger *from*, not summarize vaguely.

Section 1's real row schema (`.planning/MERGE-CHECKLIST.md:9-10` header + 40 data rows `:11-50`):
```
| path | conflict type | resolution shape | owning phase | fork behaviour at risk | adjudicating test | status |
```
- `path` — one file path per row (or a `{brace,expansion}` for a grouped row, e.g. `:63` groups all five convertors in one row).
- `conflict type` — one of `content` / `modify/delete`.
- `resolution shape` — one of `survives-as-is` / `re-express` / `redundant` (the three dispositions D-15 says to index).
- `owning phase` — an integer (6-10).
- `fork behaviour at risk` — free-text prose naming the specific behaviour or `none —` if the row carries no fork logic, e.g. `:32` (`KeyToValue.java`): `` none — fork's diff is Spotless import-reorder only; upstream's rewritten `KeyToValue` (LRUCache stale-schema-cache fix) adopted wholesale ``.
- `adjudicating test` — a requirement ID (e.g. `WRT-01`) or `none`.
- `status` — `done` or `pending`.

Section 2's real row schema (`.planning/MERGE-CHECKLIST.md:56-57` header + 7 data rows `:58-65`), **narrower than Section 1**:
```
| path | why it must be read | owning phase | status |
```
Two already-flipped Section 2 rows as the worked "done" example (`:58-59`):
```
| `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` | Merges clean with no conflict marker at all — this is how the free `ServerException` classification fix (FREE-01) arrives via upstream's rewritten `handleException`/`isServerError`/`getRootCause`. Its test file `UtilsTest.java` (Section 1) **does** conflict and must take upstream's version wholesale | 8 | done |
```
The still-`pending` rows this phase must flip (`:60-65`, D-05's six rows — read the actual current text, they are the literal rows to edit): `Record.java` (:60), `Data.java` (:61), `StructToJsonMap.java` (:62), the five convertors grouped (:63), the two state providers grouped (:64), `settings.gradle.kts` (:65). Flipping means: (a) change `pending`→`done` in the `status` cell, and (b) per the worked example, append evidence prose to the `why it must be read` cell describing the diff re-read and confirming no fork logic was lost — mirroring how `:58`'s cell appends "Its test file ... does conflict and must take upstream's version wholesale" as the closing evidence clause, not just a bare status flip.

**Recorded Facts as source data:** `.planning/MERGE-CHECKLIST.md:67-138`, numbered `1.` through `16.`, each a `**Bold label (CODE, D-xx) — attribution tag.**` prose paragraph (attribution tags observed: `repo-derived`, `operator-confirmed`). These are the "dispositions" D-15 names — FREE-01/02/03 (facts 12, 9, 10), the JMX re-plumb (fact 11), the KEEP decision (fact 14), the image-default reversion (fact 15). `FORK-DELTA.md`'s behaviour-level ledger entries should each trace to one of these numbered facts, verified against the merged tree by grep (D-15's "confirm each dropped entry" instruction), not transcribed as intentions.

---

### `.planning/scripts/verify-merge-invariants.sh:44` (utility script, CR-01 fix)

**No sibling script in this repo demonstrates the correct `git grep` exit-code idiom.** `.planning/scripts/` contains exactly two files: this script and `conflicted-paths.txt` (a data file, not a script). `.github/scripts/` contains only `jacoco_summary.py` (Python, unrelated tool, no `git grep` usage — its own exit-code convention is `sys.exit(main())` / `sys.exit("message")`, not transferable). A repo-wide grep for `git grep` outside `.git/` returns only this script's own line 44. **The planner must specify the fix from Bash/git convention directly, not from an in-repo analog.**

Script's real current structure (full 52 lines read, `.planning/scripts/verify-merge-invariants.sh:1-52`):
- Shebang + `set -uo pipefail` (`:1,3`) — **not** `set -e`; the script deliberately does not exit on first failure, so it can report both invariants and accumulate failures in a `fail` variable.
- `ROOT_DIR` resolution + `cd` (`:5-6`).
- Positional args: `MERGE_SHA="${1:?usage: ...}"` (mandatory, `:8`), `RANGE_END="${2:-HEAD}"` (optional, `:9`) — **this is the invocation contract**: `verify-merge-invariants.sh <merge-commit-sha> [range-end]`.
- Constants: `PRE_TAG="pre-v1.6.0-sync"`, `UPSTREAM_REF="v1.6.0"`, `CONFLICTED_PATHS_FILE=...` (`:11-13`).
- `fail=0` accumulator (`:15`).
- **Invariant 1** (`:17-29`): loops `${CONFLICTED_PATHS_FILE}` lines, runs `git diff --quiet ... -- "${path}"` inside `if ! git diff --quiet ...; then ... fail=1; fi` — this is the sibling branching idiom already in the file: **`git diff --quiet`'s exit code is binary by design (0=identical, 1=different)**, so a simple negated `if !` is correct there — this is *not* the same shape as invariant 2's problem, where `git grep` has a **three-way** exit code (0/1/≥2) collapsed onto a two-way `if`.
- Marker construction comment (`:31-33`): explains *why* the marker is built at runtime (`printf '%.0s<' 1 2 3 4 5 6 7`, `:34`) — so the script's own body never contains the literal 7-char sequence it detects, "or it would flag the very commit that adds it." Keep this comment; it is load-bearing documentation, not decoration.
- `SOURCE_PATHSPEC` array (`:36`) — the pathspec CONTEXT.md's canonical_refs calls out at `:39` (line numbers there are stale by 3 relative to this read — the pathspec is actually at `:36`, not `:39`; **flag this to the planner as a shifted anchor**).
- `RANGE_COMMITS="$(git rev-list "${PRE_TAG}..${RANGE_END}" 2>/dev/null || true)"` (`:38`) — same defensive `|| true` + empty-check pattern the fix should probably mirror for the `git grep` call's error path.
- **The bug** (`:43-48`):
  ```bash
  for c in ${RANGE_COMMITS}; do
    if git grep -q -- "^${MARKER}" "${c}" -- "${SOURCE_PATHSPEC[@]}" 2>/dev/null; then
      echo "INVARIANT 2 VIOLATION: ${MARKER} marker found in commit ${c}" >&2
      fail=1
    fi
  done
  ```
  `2>/dev/null` discards stderr unconditionally; the `if` only branches true/false, so `git grep` exit `1` (clean, marker absent) and exit `≥2` (broken pathspec, unreadable commit, git internal error) both fall into the same `else` (no-op) branch. D-06's fix: capture the exit code explicitly (`git grep ... ; rc=$?`) rather than branching directly on the `if`, so all three cases can be handled: `0` → violation (marker found, `fail=1`, keep the existing echo), `1` → clean (no-op, current behavior for this case only), `≥2` → violation **and surface stderr** (drop the `2>/dev/null` swallow or `tee` stderr into the failure message) rather than discarding it, per D-06's exact wording ("surface stderr rather than discarding it").
- `exit "${fail}"` (`:51`) — the script's own exit-code contract for its caller: 0 = both invariants held, 1 = at least one violated. This convention is unaffected by the fix and should stay as the outer contract; only the inner `git grep` branching changes.

**CI concurrency / gate note (D-06):** "run over `pre-v1.6.0-sync..HEAD`" — i.e. invoke as `verify-merge-invariants.sh <merge-sha>` with `RANGE_END` defaulting to `HEAD` (positional arg 2 omitted), consistent with the existing `${2:-HEAD}` default already in the script (`:9`).

---

### `.github/workflows/tests.yaml:33-35` (CI config, D-09)

Full current file read (56 lines, `.github/workflows/tests.yaml:1-56`). Quoted verbatim, the pieces CONTEXT.md's canonical_refs names:

**`FORK MODIFICATION` header comment** (`:1-5`, verbatim):
```yaml
# FORK MODIFICATION: this workflow was rewritten to drop the upstream client-by-ClickHouse
# matrix (V1/V2 x 23.7/24.3/latest/cloud). This fork tests only ClickHouse `latest` with
# client V2, matching production usage; the removed legs (V1, 23.7, 24.3, and the Cloud leg
# with its credentials guard) are out of scope here.
# See .planning/REQUIREMENTS.md CI-01..04 for the reasoning behind this reduction.
```
This comment does **not** name the env vars directly (it says "tests only ClickHouse `latest` with client V2" as a *behavioral* claim, not "sets `CLICKHOUSE_VERSION: latest`"). D-09's "update that comment if it names the env vars" clause is conditionally moot for this exact wording — the comment describes the *outcome* (fork tests latest+V2), which remains true after the env vars are dropped (they become defaults instead of overrides), so this header likely does **not** need editing. Flag this to the planner: re-verify against the literal env-var-naming test before deciding to touch it.

**`on:` trigger** (`:8`, verbatim): `on: [push]` — array form, not the mapping form; this is what makes D-02's scratch-branch push trigger a run with no extra config (any branch push fires it).

**Concurrency key** (`:10-12`, verbatim):
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}
  cancel-in-progress: true
```
Keyed on `github.sha` for push events (ternary falls to `github.sha` when not a `pull_request`) — confirms the Integration Points note that D-02's scratch push and the later `custom_build` push land in different concurrency groups (different SHAs) and cannot cancel each other.

**`timeout-minutes: 45`** at `:17` (not `:15` as CONTEXT.md's canonical_refs states — **stale anchor by 2 lines**; the job name line `name: ClickHouse latest Client version V2 tests` is at `:18`). This is D-10's budget denominator (threshold = 22.5 min = half of this).

**The exact env block to edit** (`:31-35`, verbatim — this is the D-09 edit site):
```yaml
      - name: Run tests
        env:
          CLICKHOUSE_VERSION: latest
          CLIENT_VERSION: V2
        run: ./gradlew test
```
D-09's default (stated in CONTEXT.md, Claude's Discretion confirms "stated default is to drop both") is to delete the entire `env:` block (both keys), leaving:
```yaml
      - name: Run tests
        run: ./gradlew test
```
This causes `ClickHouseTestHelpers.getClickhouseVersion()` and `extractClientVersion()` to fall through to their code defaults (`CLICKHOUSE_VERSION_DEFAULT` = `"latest"`, and the `!= "V1"` → `"V2"` fallback), which is the entire point of the change per D-09's rationale.

**How coverage is wired in** (`:37-47`, verbatim):
```yaml
      - name: Generate coverage summary
        if: always()
        run: python3 .github/scripts/jacoco_summary.py

      - name: Upload JaCoCo coverage reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-coverage-reports
          path: build/reports/jacoco/
          retention-days: 14
```
`if: always()` on both the summary-generation and upload steps — coverage is published even if the `Run tests` step fails. This is untouched by D-09/D-10 but is CI-05's "publishes coverage" evidence path; the planner does not need to touch it, only cite it as already-satisfying evidence.

---

### `src/testFixtures/.../ClickHouseTestHelpers.java:32-38` (Javadoc fix, D-04/D-09 companion)

Full file read (472 lines). The D-04 Javadoc, quoted **verbatim** (`ClickHouseTestHelpers.java:30-39`):
```java
    /**
     * [D-04] The seventh fork behaviour to silently vanish from the v1.6.0 merge with no conflict
     * marker and no compile error: the merge reverted this default from the floating {@code
     * latest} tag to a pinned LTS version. CI pins {@code CLICKHOUSE_VERSION: latest} in its job
     * environment, so the regression was invisible there; every local run since the merge took
     * its green evidence against the wrong server instead. The milestone's declared test matrix
     * is ClickHouse {@code latest} with client V2 (see PROJECT.md constraints) - this constant is
     * the contract, restored and pinned by {@code ClickHouseVersionDefaultUnitTest}, a Docker-free
     * canary reachable from {@code testUnit}.
     */
    public static final String CLICKHOUSE_VERSION_LATEST = "latest";
```
The sentence CONTEXT.md's D-09 flags as becoming false: `"CI pins {@code CLICKHOUSE_VERSION: latest} in its job environment, so the regression was invisible there;"` — once `tests.yaml`'s env block is dropped, CI no longer pins anything; this sentence must be corrected **in the same commit** as the `tests.yaml` edit (D-09's "mandatory companion edit"). The surrounding sentences (the "seventh fork behaviour," `ClickHouseVersionDefaultUnitTest` naming) remain true and should be kept — only the CI-pin clause needs rewriting, e.g. to something acknowledging CI *no longer* pins the version and instead exercises the code default like local runs do.

Line-number cross-check against CONTEXT.md's canonical_refs claims:
- `:32-34` (CONTEXT.md's claimed Javadoc location) — the actual Javadoc block is `:30-39`; the CI-pin sentence itself is on `:32-34` inside that block, so **this anchor is accurate**.
- `:41` `CLICKHOUSE_VERSION_DEFAULT` — verbatim at `:41`: `public static final String CLICKHOUSE_VERSION_DEFAULT = CLICKHOUSE_VERSION_LATEST;` — **accurate**.
- `:67-69` "the reader" — verbatim at `:66-72`:
  ```java
  public static String getClickhouseVersion() {
      String clickHouseVersion = System.getenv(CLICKHOUSE_VERSION);
      if (clickHouseVersion == null) {
          clickHouseVersion = CLICKHOUSE_VERSION_DEFAULT;
      }
      return clickHouseVersion;
  }
  ```
  CONTEXT.md's `:67-69` covers only the middle 3 lines of this 7-line method — close enough to be usable, slightly narrow. **Accurate, minor range trim.**
- `:91-97` `extractClientVersion()` — verbatim at `:90-97`:
  ```java
  public static String extractClientVersion() {
      String clientVersion = System.getenv(ClickHouseTestHelpers.CLIENT_VERSION);
      if (clientVersion != null && clientVersion.equals("V1")) {
          return "V1";
      } else {
          return "V2";
      }
  }
  ```
  **Accurate** — confirms the "returns `V2` for anything that is not literally `V1`" claim CONTEXT.md's measured-state table makes verbatim.

No other edit is needed in this file for this phase — the `CLICKHOUSE_VERSION`/`CLIENT_VERSION` env-var constants (`:52,56`) and their readers are already correct; only the Javadoc prose is stale.

---

### `.planning/MERGE-CHECKLIST.md` (row flips, D-05)

Already covered above under `docs/FORK-DELTA.md`'s "source-data analog" — same file serves both as a pattern source (row-flip mechanics) and as a data source (ledger derivation). Re-stated for the planner's row-flip task specifically: the 6 pending rows are Section 2 rows at `:60-65` (see exact table cells quoted above); flip `status` `pending`→`done` and append evidence prose to `why it must be read`, matching the shape of the already-`done` neighbor rows at `:58-59`.

**Note the document's own opening paragraph** (`:3`) states the gate this phase must satisfy: "**Phase 11 asserts zero `pending` rows remain before `custom_build` is published**" — the planner should treat a `grep -c pending` (or equivalent) over Section 1+2 as the mechanical verification step after the 6 flips, not just visual inspection.

---

### `.planning/REQUIREMENTS.md` (SUITE checkboxes + Traceability, D-05/D-06/D-07/D-09..D-12 closure)

**Checkbox line format**, verbatim (`.planning/REQUIREMENTS.md:114-119`):
```markdown
- [ ] **SUITE-01**: `./gradlew test` is green against ClickHouse `latest` × client V2 — the fork's tests plus upstream's 56 test files — with every skip accounted for and explained
- [ ] **SUITE-02**: The four fork SMTs (`TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, `ConvertToMapOrJson`) and their sink-landing tests pass untouched; any failure there is investigated as a genuine defect, since upstream touches nothing in that package
- [ ] **SUITE-03**: A written record lists every fork change dropped in favour of upstream and why, and shows the fork's delta over upstream is smaller after this milestone than before it
- [ ] **CI-05**: `tests.yaml` runs the merged suite on push and publishes coverage, with its runtime measured against the job's `timeout-minutes: 45`
- [ ] **CI-06**: A `tests.yaml` run is machine-verified green on the pushed SHA, located by `head_sha` plus workflow `path` and asserting exactly one match
- [ ] **CI-07**: `custom_build` is pushed to the public fork after human approval, by a non-force push from the scratch branch
```
Format: `- [ ] **ID**: description`. Closing the requirement means flipping `[ ]`→`[x]`, exactly matching the already-closed neighbor block immediately above it (e.g. `HARN-05` at `:100`: `- [x] **HARN-05**: ...`).

**Precedent for adding a closure annotation, not just a checkbox flip:** the `HARN-01..07` block is followed by a `> **Correction (Phase 10, 2026-09-20).** ...` blockquote (`:104-110`) recording what was discovered/measured at closure time, in the same file, immediately after the checkbox block. If Phase 11 closure needs similar narrative (re: the "test work is largely done, risk moved downstream" framing CONTEXT.md's Specifics section asks the plan to state plainly), this blockquote is the structural precedent to copy — a `>` blockquote directly under the closed checkbox block, not a separate section.

**Traceability table row format**, verbatim (`.planning/REQUIREMENTS.md:176-177` header + the 6 pending rows `:225-230`):
```markdown
| Requirement | Phase | Status |
|-------------|-------|--------|
| SUITE-01 | Phase 11 | Pending |
| SUITE-02 | Phase 11 | Pending |
| SUITE-03 | Phase 11 | Pending |
| CI-05 | Phase 11 | Pending |
| CI-06 | Phase 11 | Pending |
| CI-07 | Phase 11 | Pending |
```
Closed-row precedent, e.g. `.planning/REQUIREMENTS.md:224`: `| HARN-07 | Phase 10 | Complete |` — the flip is `Pending`→`Complete` (capitalized, not lowercase `done`/`complete` — matches the exact casing already used 44 times above in the same table).

---

## Shared Patterns

### Evidence-cited status flips (not bare flips)
**Source:** `.planning/MERGE-CHECKLIST.md` Section 2's already-done rows (`:58-59`) and `.planning/REQUIREMENTS.md`'s `HARN-*` closure blockquote (`:104-110`).
**Apply to:** `MERGE-CHECKLIST.md`'s 6 row flips (D-05), `verify-merge-invariants.sh`'s gate-evidence run (D-06), `REQUIREMENTS.md`'s SUITE/CI-05..07 checkbox closures. This whole phase's "Established Pattern" per CONTEXT.md's `<code_context>` is "Evidence re-run at execution time, never cited from a planning document" — every status flip in this phase's touched docs should carry the command/diff/measurement that produced it, in-line, not just a symbol change.

### Behavioral, not flattering, framing
**Source:** `docs/DESIGN.md`'s plain engineering prose tone; D-13's explicit rejection of "file count as the headline."
**Apply to:** `docs/FORK-DELTA.md` only. State the awkward number (insertions 8401→9256) next to the flattering one (files 93→82) in the same breath, as CONTEXT.md's Specifics section models.

### Stale-anchor discipline
**Source:** none in-repo — this is a cross-cutting caution this pattern map itself demonstrates by flagging every line-number mismatch found (see `verify-merge-invariants.sh`'s pathspec at `:36` not `:39`, and `tests.yaml`'s `timeout-minutes` at `:17` not `:15`).
**Apply to:** every file this phase touches — CONTEXT.md's canonical_refs line numbers should be re-verified by the planner against a fresh `Read`, not assumed current, exactly as this pattern map did.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `.planning/scripts/verify-merge-invariants.sh:44` fix itself | script | batch/gate | No sibling script in `.planning/scripts/` or `.github/scripts/` demonstrates the `git grep` 0/1/≥2 exit-code idiom (Python's `jacoco_summary.py` uses an unrelated `sys.exit()` convention). The planner must specify the fix from standard Bash/git conventions (capture `$?` explicitly after `git grep`, branch three ways) rather than from an in-repo pattern. |

## Files explicitly out of pattern-mapping scope (per orchestrator guidance)

The nine files behind D-05's checklist rows are **read-only** for this phase (confirming no fork logic was lost, not being edited) and were deliberately not analog-mapped, since an analog map for a read-only confirmation pass has no planner-facing consumer: `src/main/java/com/clickhouse/kafka/connect/sink/data/{Record,Data,StructToJsonMap}.java`, `.../sink/data/convert/{EmptyRecordConvertor,RecordConvertor,SchemaRecordConvertor,SchemalessRecordConvertor,StringRecordConvertor}.java`, `.../sink/state/provider/{InMemoryState,KeeperStateProvider}.java`, `settings.gradle.kts`.

## Metadata

**Analog search scope:** `docs/`, `.planning/scripts/`, `.github/workflows/`, `.github/scripts/`, `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/`, `.planning/MERGE-CHECKLIST.md`, `.planning/REQUIREMENTS.md`, repo-root `AGENTS.md`/`README.md` (for `docs/DESIGN.md` link precedent), repo-wide `grep -rn "git grep"` (idiom search, zero sibling hits).
**Files scanned:** 6 target files fully read (docs/DESIGN.md 274 lines, verify-merge-invariants.sh 52 lines, tests.yaml 56 lines, ClickHouseTestHelpers.java 472 lines, MERGE-CHECKLIST.md 138 lines, REQUIREMENTS.md lines 100-230 of a larger file) + 2 confirmatory greps (docs/DESIGN.md link sites, git grep idiom search) + 1 `git ls-files` tracked-source verification covering all 8 analog/target paths.
**Pattern extraction date:** 2026-09-20
**Stale-anchor findings:** CONTEXT.md's canonical_refs line numbers are accurate for `ClickHouseTestHelpers.java` (all four cited ranges confirmed) and for `verify-merge-invariants.sh:44` (the fail-open line itself, confirmed verbatim) and `:36` (marker construction, confirmed — CONTEXT.md cites this same content at `:36`, accurate). Two anchors are off by 2-3 lines: `tests.yaml`'s `timeout-minutes: 45` is at `:17` not `:15`, and the `SOURCE_PATHSPEC` pathspec CONTEXT.md calls "`:39`" is actually at `:36` (CONTEXT.md's own text says "`:39` is the pathspec" — the pathspec array is in fact 3 lines earlier than stated).
