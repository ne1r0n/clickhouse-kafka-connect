# Phase 21: Dependabot Integration & CI Hygiene - Pattern Map

**Mapped:** 2026-10-17
**Files analyzed:** 9 (7 edited/new in tree, 1 new evidence file, 1 merge-commit bookkeeping pair)
**Analogs found:** 8 / 9 (all analog paths verified git-tracked)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `.planning/scripts/verify-merge-invariants.sh` (invariant 3 exemption) | utility (gate script) | batch / transform | itself: `CONFLICTED_PATHS_FILE` read (L144-156) + anchor loop (L187-253) | exact |
| `.planning/scripts/fork-anchor-exemptions.txt` (new) | config (data) | batch | `.planning/scripts/fork-anchors.txt` | exact |
| `.planning/scripts/verify-merge-invariants-selftest.sh` (ST5-ST9, comment fix) | test (shell) | request-response | its own ST2-ST4 + `synth_tree`/`expect_failure`/`report` | exact |
| `.planning/scripts/fork-anchors.txt` L43 (in merge commit) | config | batch | its own row format | exact |
| `docs/FORK-DELTA.md` L63, L65 (in merge commit) | docs | n/a | in-place row edit | exact |
| `src/test/java/.../ci/TestsWorkflowPinGuardUnitTest.java` | test | transform (text lines to verdict) | itself (5 existing rules, `nonCommentLines`) | exact |
| `21-EVIDENCE.md` (new, phase dir) | docs | n/a | `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-EVIDENCE.md` (also `20-release-publication/20-COMMAND-EVIDENCE.md`) | exact |

## Pattern Assignments

### `.planning/scripts/verify-merge-invariants.sh` (utility, batch)

**Analog:** itself.

**Header/env-override pattern** (L7-26): add `ANCHOR_EXEMPTIONS_FILE` doc line to the header block and a default beside L26. An explicitly empty value must stay empty (`${VAR-default}`, not `:-`).
```bash
ANCHORS_FILE="${ANCHORS_FILE-.planning/scripts/fork-anchors.txt}"
```
New: `ANCHOR_EXEMPTIONS_FILE="${ANCHOR_EXEMPTIONS_FILE-.planning/scripts/fork-anchor-exemptions.txt}"`, `exempt_used=0` beside the counters at L33-36.

**Read-from-RANGE_END-tree pattern** (L145-148), copy for the exemption file:
```bash
if ! git show "${RANGE_END}:${CONFLICTED_PATHS_FILE}" > "${WORK_DIR}/conflicted-recorded.raw" 2>/dev/null; then
```
(Exemption file missing at RANGE_END = no exemptions, not an error; fails closed because 6473e24f then violates.)

**Row-parse pattern** (L190-195, L215-233): reuse `is_data_row`, TAB counting via `tabs="${row//[^${TAB}]/}"`, `${row%%${TAB}*}` / `${rest#*${TAB}}` field splitting, a separate fd (use `5<`; fd 3 and 4 are taken), and the `fail=1; continue` malformed-row idiom.
```bash
tabs="${row//[^${TAB}]/}"
if [ "${#tabs}" -ne 3 ]; then
  echo "ANCHOR VIOLATION: ${short_c} malformed row (need 4 TAB-separated fields): ${row}" >&2
  fail=1
  continue
fi
```
Exemption rows: 4 fields (40-hex SHA, path, fixed-string, reason); validate `[[ "${e_sha}" =~ ^[0-9a-f]{40}$ ]]`; emit `ANCHOR EXEMPTION VIOLATION: malformed row ...`. Build a keys file `sha TAB path TAB pattern`; lookup with `grep -qxF -e` (bash 3.2: no `declare -A`).

**Core edit point** (L247-250), the only branch forgiven:
```bash
elif [ "${a_actual}" != "${a_expected}" ]; then
  echo "ANCHOR VIOLATION: ${short_c} ${a_path} '${a_pattern}' expected ${a_expected} got ${a_actual} (${a_backs})" >&2
  fail=1
fi
```
Wrap with `grep -qxF -e "${c}${TAB}${a_path}${TAB}${a_pattern}" "${WORK_DIR}/exempt-keys"` (`${c}` is the full 40-hex). Missing-path (L235-239), search-failed (L244-246) and malformed-anchor branches stay unforgiven. Summary line L256 gains `anchor_exempt=${exempt_used}` before `result=` (selftest greps only `result=PASS`). Prototype is in 21-RESEARCH.md "Pattern 2".

---

### `.planning/scripts/fork-anchor-exemptions.txt` (config, batch)

**Analog:** `.planning/scripts/fork-anchors.txt` (L1-14 header style, L15+ TAB rows).

**Header pattern** (L1-10): comment block stating format, who reads it, and the "same-commit" rule. Format line to mirror:
```text
# Format: four TAB-separated fields per row:
#   path <TAB> fixed-string pattern <TAB> expected count <TAB> backs-ID
```
Use: `40-hex SHA <TAB> path <TAB> fixed-string <TAB> reason`. Row text must be the pattern as the exempted commit's own anchors copy spells it (`slf4j-reload4j:2.0.17`), not HEAD's:
```text
6473e24f372a8b37dfd8cc8cf3124e6cff52edca	build.gradle.kts	slf4j-reload4j:2.0.17	<reason>
```
Real tabs are required. Never key on the backs-id (`BLD-08` backs `build.gradle.kts slf4j-reload4j:2.0.17` and `gradle/libs.versions.toml kafka = "4.1.1"`).

---

### `.planning/scripts/verify-merge-invariants-selftest.sh` (test, request-response)

**Analog:** itself, ST2-ST4 (L80-110) and helpers (L33-65).

**Helpers to reuse unchanged:** `report` (L33-39), `synth_tree <base> <path> <content-file>` (L42-50), `expect_failure <name> <needle> <env...> -- <args...>` (L53-65).

**Dangling-commit case pattern** (ST4, L104-109):
```bash
st4_tree="$(synth_tree HEAD "${ANCHORS_PATH}" "${WORK_DIR}/anchors-wrong")"
st4_commit="$(git commit-tree "${st4_tree}" -p HEAD -m "selftest ST4")"
expect_failure ST4 'ANCHOR VIOLATION' -- "${MERGE_ARG}" "${st4_commit}"
```
New ST5-ST9 follow this: `x_slf4j` = commit-tree of 6473e24f's tree on parent `pre-v1.7.0-sync`; `x_both` = same tree with the kafka line changed via `synth_tree`; `y_*` = `synth_tree HEAD <exemptions-path> <file>` as range end. Env for each: `ANCHOR_EXEMPTIONS_FILE` default plus `CONFLICTED_PATHS_FILE=`. Add a positive control (ST9: rc 0 and `anchor_exempt=1 result=PASS`, modelled on ST1 L71-77). Cases: ST5 empty file fails; ST5-disabled (`ANCHOR_EXEMPTIONS_FILE=`) fails; ST6 other-row / other-commit; ST7 kafka-row violation and slf4j absent from stderr; ST8-* malformed rows; ST9 exact row passes. Full list in 21-RESEARCH.md "Pattern 3".

**Comment/lookup fix:** L6 ("single first-parent merge") is wrong after DEPS-01; add a comment above L28. Lookup `git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD | tail -1` stays (tail = oldest = `54e63648`). Optional: strengthen ST4's needle (L109) to the first row's `TARGET_TABLE_FILTER`.

---

### `.planning/scripts/fork-anchors.txt` L43 and `docs/FORK-DELTA.md` L63/L65 (in the merge commit)

Edit after `git merge --no-commit --no-ff origin/custom_build`, before `git commit`. L43 `build.gradle.kts<TAB>slf4j-reload4j:2.0.17<TAB>1<TAB>BLD-08` becomes `2.0.20`. Do not touch L48 (`gradle/libs.versions.toml<TAB>kafka = "4.1.1"<TAB>1<TAB>BLD-08`). FORK-DELTA L63 `2.0.17` becomes `2.0.20`, L65 "Lombok 1.18.46" becomes `1.18.48`. Keep TABs intact (edit with a tool that does not expand them). Merge message names these edits.

---

### `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` (test, transform)

**Analog:** itself. No other `*UnitTest` in `src/test` uses inline text-block fixtures (`TestRuntimeClasspathLz4UnitTest.java` is the only sibling in `ci/`, and a grep for `"""` finds none). Use `List.of("...")` fixtures; indentation-sensitive rules need exact leading spaces.

**Imports/class conventions** (L1-28): static `Assertions` imports, 4-space indent, no base class (extending `ClickHouseBase` would add the `clickhouse` tag and drop it from `testUnit`). Add imports `java.util.Optional`, `java.util.function.Predicate` as needed.

**Helper to keep** (L51-55), UTF-8 explicit because the test JVM uses windows-1252:
```java
private static List<String> nonCommentLines(Path file) throws IOException {
    return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
            .filter(line -> !line.stripLeading().startsWith("#"))
            .collect(Collectors.toList());
}
```
Read `build.gradle.kts` the same way (`Files.readString(path, StandardCharsets.UTF_8)`), then strip `/* */` and `//` lines.

**Regexes to change** (L36, L38):
```java
private static final Pattern RUNS_ON = Pattern.compile("^\\s*runs-on:\\s*(.*?)\\s*$");
private static final Pattern WRITE_SCOPE = Pattern.compile(".*:\\s*write\\b.*");
```
Replace with `RUNS_ON` (strip trailing `\s#...`), `WRITE_SCALAR` and `WRITE_FLOW` per 21-RESEARCH.md Pattern 4.

**Extraction target shape (D-06):** each existing `@Test` body becomes `static Optional<String> rule(List<String> lines)` returning the offending message; the `@Test` keeps its name, `@DisplayName` and exact message, and asserts on the real file. Existing assertions and messages to preserve byte-identical:
- `L65-75` pinned-uses: `"Action is not pinned to a 40-hex commit with a '# vX.Y.Z' comment: '" + line.strip() + "'"` plus the `MIN_PINNED_ACTIONS` empty-list guard (L65-68).
- `L89-102` runs-on: `"runs-on must be the pinned runner, not '...'"`, `"No 'runs-on:' line found in "`, and the floating-runner contains check (strip trailing comment before it).
- `L110-134` token read-only: one `permissions:` line, `READ_ONLY_CONTENTS` next-line check.
- `L143-160` checkout: step boundary via `LIST_ITEM` (L39, L149), the pattern for the CI-18 step scan (prefer indent-based boundary for the new rule).
- `L166-176` dependabot: `stripped.contains(GITHUB_ACTIONS_ECOSYSTEM)` (L48-49 constants).

**New rules** (code in 21-RESEARCH.md Pattern 4): `forkCountStepProblem`, `nprocProblem`, `forkTaskProblem`/`ktsCode`, `dependabotExcludeProblem(List<String>, Predicate<String>)`. Real-file targets: `.github/workflows/tests.yaml` L51-64 (step `Report test fork count`, `./gradlew -q --console=plain reportTestForks`, single `msg="runner nproc=$(nproc), ..."` line), `build.gradle.kts` L254-262 (`tasks.register("reportTestForks") {` at column 0, closing `}` at column 0, reads `tasks.named<Test>("test")` and `maxParallelForks`), `.github/dependabot.yml` (`exclude-paths` under the `github-actions` entry lists the four upstream workflows).

**D-07 sequence:** (1) extract with old regexes plus `Optional.empty()` stubs for new rules; (2) add fixture tests and real-file tests, run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --tests '*TestsWorkflowPinGuardUnitTest'`, record red names; (3) swap in new regexes and bodies, commit tests and fix together. Must-reject D-10 fixtures: `contents: write`, `permissions: write-all`, job-level write block, quoted `"write"`, `runs-on: ubuntu-latest # x`.

---

### `21-EVIDENCE.md` (docs)

**Analog:** `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-EVIDENCE.md` (L1-22 header and index; entries like E-03 "pin guard red then green", L94-136, are the direct precedent for the red fixture run). Secondary: `20-release-publication/20-COMMAND-EVIDENCE.md` (E-61 dry-run merge L842, E-62 rehearsal).

**Header/entry format** (17-EVIDENCE L1-8):
```markdown
# Phase 17 Evidence

- Phase: 17 (CI, Build & Test Hygiene)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 17 plans. Nothing here was pushed.
- Entry format: one `### E-NN: <title>` section per entry with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail` or `Verdict: unmeasured (<reason>)`. Long Gradle output stays in `/tmp/gsd17/exec/`.
```
Followed by an `## Index` table (`Entry | Requirement | Verdict`) and a closing `## Coverage Summary` (L430). Planned entries: merge dry-run and merge commit, gate fail-without/pass-with, self-test (old-gate FAIL list), baseline-equal spotless md5 `3393873370f9b48d2239131eedba8ff8`, `dependencyInsight` x3, Lombok-warning check, red fixture run, OPS records (user words verbatim; no token value; Keychain item name only), final REST check at or after 2026-10-17T12:00Z. Use `/tmp/gsd21/exec/` for long output.

## Shared Patterns

### Gradle invocation
**Source:** carried rule (research, config.json). **Apply to:** every Gradle command in the plans.
`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`

### Docker-free test classification
**Source:** `TestsWorkflowPinGuardUnitTest.java` L18-28 javadoc. **Apply to:** the test edit. No `@Tag("clickhouse")`, no `ClickHouseBase`, so `testUnit` picks it up.

### Anchor change in the same commit
**Source:** `fork-anchors.txt` L6-8. **Apply to:** merge commit (L43 row), any later exemption row. Each commit is checked against its own anchors copy.

### Spotless scope (D-14)
**Apply to:** `TestsWorkflowPinGuardUnitTest.java` must be spotless-clean on its own; tree gate is baseline-equal (`spotlessApply` diff md5 `3393873370f9b48d2239131eedba8ff8`, 46 files), checked in a throwaway worktree per 21-RESEARCH.md Pitfall 3. No tree-wide reformat. `-PspotlessFiles` does not scope.

### Bash 3.2 portability
**Source:** `verify-merge-invariants.sh` style (no `declare -A`, `read -r` with explicit fds, `${var//[^${TAB}]/}`). **Apply to:** gate and selftest edits.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| Text-block fixture style in `*UnitTest` | test | transform | No existing unit test uses inline text-block fixtures; use `List.of(...)` per RESEARCH (text blocks strip common indentation, which breaks indent-aware parsers). |

## Metadata

**Analog search scope:** `.planning/scripts/`, `src/test/java/com/clickhouse/kafka/connect/ci/`, `.planning/milestones/v1.7.0_gg2-hardening-phases/{17,20}-*`, `.github/`, `build.gradle.kts`
**Files scanned:** about 14 (all analogs confirmed with `git ls-files`; no gitignored mirror paths used)
**Pattern extraction date:** 2026-10-17
