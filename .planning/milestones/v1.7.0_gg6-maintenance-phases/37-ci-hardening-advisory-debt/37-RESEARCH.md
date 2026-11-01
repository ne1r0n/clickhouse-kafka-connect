# Phase 37: CI Hardening & Advisory Debt - Research

**Researched:** 2026-10-31
**Domain:** GitHub Actions release tooling, Python stdlib scripts tested from JUnit, archive-compare gating, review-advisory closure, GitHub release evidence
**Confidence:** HIGH for repo facts and the ADV-03 readings (all measured this session), MEDIUM for the CI-only mechanics that no run has exercised yet

<user_constraints>
## User Constraints (from CONTEXT.md)

No CONTEXT.md exists for this phase (`skip_discuss: true` in `.planning/config.json`). The ROADMAP Phase 37 goal and success criteria are the locked scope. They are copied here so the planner has them first.

### Locked Decisions
- Goal: "`increment-version.yml` can bump a `_ggN` version without writing `vnull`, because its logic is a script with a test. The release workflows compare the built Confluent archive entry by entry against the previous release, under an allow-list keyed by `VERSION`. ... Every review advisory carried from Phases 25-31 is fixed, or dispositioned with an owner and a re-open trigger. The real `v1.7.0_gg5` release is on the record. No connector behaviour changes."
- Requirements: CIH-01, CIH-02, CIH-03, CIH-04, CIH-05, ADV-01, ADV-02, ADV-03.
- Version rules (criterion 1): input and output validated against `^v\d+\.\d+\.\d+(_gg[1-9]\d*)?$`; `gg` bumps the counter (`v1.7.0_gg5` -> `v1.7.0_gg6`) and turns a suffix-less `vX.Y.Z` into `vX.Y.Z_gg1`; `patch`, `minor`, `major` reset the suffix to `_gg1`; invalid input fails and nothing is written.
- `increment-version.yml` calls the script, installs no npm `semver`, keeps SHA pins and permissions, passes the pin guard, loses its "known and left unfixed" header note; FORK-DELTA's row for the file no longer calls the defect open.
- CIH-03: the bump edits the `VERSION` row of `fork-anchors.txt`, or the script fails when that row does not match; the failure is seen red on a fixture.
- CIH-04/05: compare script under `.github/scripts/`; synthetic-archive self-test covers UNALLOWED, STALE-ROW and exit codes 0, 1, 2; `release.yaml` and `pre-release.yaml` run it against an allow-list keyed by `VERSION`, baseline rebuilt from the previous tag in a separate worktree, report published as an artifact; `REL-xx` anchor counts and `TestsWorkflowPinGuardUnitTest` updated and both pass.
- ADV-01/02: every Phase 30 row (WR-01..03, IN-01..04), Phase 31 row (WR-01..03, IN-01..03) and each of the 16 unfixed findings of `29-MILESTONE-CLOSE-RECORD.md` is fixed (commit pointer) or dispositioned (owner + re-open trigger). A diff check shows no existing assertion deleted or weakened; an assertion changes only with the user's sign-off.
- ADV-03: the real `v1.7.0_gg5` GitHub release recorded from the REST API (release, workflow run, tag commit, asset name/size/SHA-256).
- Standing rules: Phase 37 changes `src/main` only with the user's approval; Phase 38 cannot edit `.github/` (Phase 29 D-05), so every workflow fix belongs here; nothing is pushed to `origin` without the user; no mutating `gh` call; the standing hygiene rule on environment names applies to every committed file.
- Phase gate: cache-busted `testUnit` with the pin-guard and script self-tests, a full suite on `latest` x V2, the anchors invariant, an empty `src/main` diff unless the user approved a fix.

### Claude's Discretion
- Script language and wiring; allow-list naming (research flag); what the compare step runs against before `VERSION` moves (research flag); how each advisory row is closed (fix vs disposition), within the src/main rule.

### Deferred Ideas (OUT OF SCOPE)
- Out of scope per the ROADMAP and REQUIREMENTS: any connector behaviour change; `ServerTimeoutExceeded` retry-list work; `socket_rcvbuf`/`socket_sndbuf`; removing D-17; Phase 38's `VERSION` bump, gg6 allow-list and rollout note.
- Phase 36 follow-ups F-1, F-2, F-3 (`36-FOLLOWUPS.md`) are `src/main` or harness behaviour and are not Phase 37 requirements.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CIH-01 | `increment-version.yml` never writes `vnull`; strict validation, `gg` step, suffix reset | Section 1: Python script, regex pitfalls verified, table of rules |
| CIH-02 | Logic in a script with a Docker-free table-driven test; npm `semver` gone; pins and permissions unchanged; note removed | Section 1: JUnit-runs-python precedent (`FailedTestsSummaryScriptUnitTest`), anchor rows to move |
| CIH-03 | Bump edits the `VERSION` row of `fork-anchors.txt` or fails when it does not match | Section 1: row is `.planning/scripts/fork-anchors.txt:41`, keyed by path field `VERSION` |
| CIH-04 | Release workflows run the compare against a `VERSION`-keyed allow-list, baseline from the previous tag in a worktree, report as artifact, anchors and pin-guard tests updated | Section 2: wiring, naming, D-05 constraint on allow-list location, anchor deltas |
| CIH-05 | Compare script under `.github/scripts/` with synthetic-archive self-test (UNALLOWED, STALE-ROW, exit 0/1/2) | Section 2: script facts, JUnit synthetic-zip test design |
| ADV-01 | Phase 30 and 31 rows closed | Section 3 table (13 rows) |
| ADV-02 | 16 unfixed gg4 findings closed | Section 3 table (16 rows) |
| ADV-03 | Real `v1.7.0_gg5` release recorded from the REST API | Section 4: measured values, and a finding that changes what the record can claim |
</phase_requirements>

## Summary

Phase 37 is almost entirely additive tooling: a Python bump script plus a JUnit table test, a move of the existing Python compare script under `.github/scripts/` plus a JUnit synthetic-archive test, compare wiring in two near-identical release workflows, one new workflow guard test class, and a ledger that closes 29 advisory rows. No `src/main` change is needed for any recommended fix. Four of the 16 gg4 findings sit in `src/main` and are recommended for disposition, not fix. The repo already has the exact pattern for the new tests: `FailedTestsSummaryScriptUnitTest` runs `python3 -I .github/scripts/failed_tests_summary.py` from JUnit with `@TempDir` fixtures, extends nothing, and so stays inside `testUnit`. The compare script (`.planning/scripts/compare-confluent-archives.py`) already implements allow-list matching, UNALLOWED, STALE-ROW and exit codes 0/1/2, standard library only, so CIH-05 is a move plus a test, not a rewrite.

Three findings change the plan. First, ADV-03 as worded cannot be satisfied literally: no workflow run created the `v1.7.0_gg5` release. The only publish-mode run of `release.yaml` (run 40474419526) was cancelled during the archive build step; the release asset was uploaded by the user account on 2026-10-28, and its SHA-256 (`4c69b6fb...`) differs from the archive the later successful dry run built on the same commit (`d61a6932...`). The "no run exercised the publish path" caveat therefore stays open until Phase 38. Second, the allow-list cannot live under `.github/`: Phase 38 owns the gg6 allow-list and may not edit `.github/`, so it must stay under `.planning/scripts/`. Third, the research-flag questions were answered by measurement: the `v1.7.0_gg5` tag rebuilds to an archive whose entries are identical to the published asset (only the build-date line of `manifest.json` differs, which the compare ignores), and the compare gives PASS for gg4-to-gg5 under the existing allow-list and FAIL with 238 UNALLOWED lines for gg4-to-current-tip, including all three zstd-jni natives.

**Primary recommendation:** Write `bump_version.py` and `compare_confluent_archives.py` as stdlib Python under `.github/scripts/`, test both from new Docker-free JUnit classes, keep allow-lists in `.planning/scripts/confluent-archive-allowlist-<VERSION>.txt`, wire the compare after "Check archive" in both release workflows with `fetch-depth: 0` and `git worktree add`, and record ADV-03 as "release created outside any workflow run; publish path still unexercised".

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Version bump logic and VERSION/anchor edit | Repo script (`.github/scripts`) | CI workflow (thin caller) | Logic must run under `testUnit` without Docker; the workflow only passes the input and opens the PR |
| Archive compare and allow-list match | Repo script (`.github/scripts`) | Release workflows (invoke, upload report) | One implementation for CI and local use; the JUnit self-test pins its exit codes |
| Allow-list data per release | `.planning/scripts` data files | Phase 38 (writes gg6 file) | D-05 forbids Phase 38 editing `.github/`; data must live outside it |
| Baseline archive build | Release workflow job (runner) | Baseline tag's own Gradle wrapper | Same runner as the candidate build, so platform effects cancel out |
| Safety-gate pinning for release workflows | JUnit guard test (`src/test`) | `fork-anchors.txt` rows | Line-level checks, no YAML library, same style as existing guards |
| ADV-03 evidence | Planning record (read-only REST API values) | None | Evidence only; no mutation |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Python 3 stdlib (`re`, `zipfile`, `hashlib`, `fnmatch`, `argparse`, `os`, `tempfile`) | runner `ubuntu-24.04` python3; local 3.14.8 | `bump_version.py`, `compare_confluent_archives.py` | Existing scripts `failed_tests_summary.py`, `jacoco_summary.py` and the compare script are stdlib-only and run with `python3 -I` [VERIFIED: .github/scripts listing, tests.yaml:`python3 .github/scripts/jacoco_summary.py`] |
| JUnit 5.11.3 + `ProcessBuilder` | existing | Table-driven and synthetic-archive tests that run the scripts | `FailedTestsSummaryScriptUnitTest.java:33-79` runs `python3 -I` on a script, reads output as UTF-8, extends nothing [VERIFIED: file read] |
| `actions/upload-artifact` | `cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2` | Publish the compare report | Already pinned in `tests.yaml:84,92` and the nightly; the tag resolves to that SHA [VERIFIED: `gh api repos/actions/upload-artifact/git/ref/tags/v7.0.2`] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.util.zip.ZipOutputStream` | JDK 17 | Build synthetic outer zip and inner jar in the compare test | CIH-05 self-test; no fixture binaries committed |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Python bump script | Bash + sed | No table-driven test story, Unicode-digit and newline pitfalls are harder to pin; repo has no bash unit-test precedent under `testUnit` (the bash gate is a `.planning` tool) |
| Python bump script | Keep node `semver` | Cannot represent `_ggN`; the defect itself |
| JUnit-run script tests | Python `unittest` + a CI step | Not run by `./gradlew test`, so not gated by `tests.yaml` or the phase gate |
| Reusable workflow for release/pre-release (30 IN-03) | Normalised-diff JUnit test | `uses: ./...` is rejected by `WorkflowActionPinGuardUnitTest` (`LOCAL_OR_DOCKER`, lines 56-62), so a reusable workflow needs a guard change; a diff test is additive |

**Installation:** none. No new external package is installed; the npm `semver@7.8.5` install is removed. Version verification and the legitimacy gate are therefore not applicable.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none added) | - | - | - | - | n/a | No external package is installed by this phase |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none
Removed from the repo: `semver@7.8.5` (npm install line in `increment-version.yml:47`) - a supply-chain reduction, not a verdict.

## Architecture Patterns

### System Architecture Diagram

```
workflow_dispatch(bump=patch|minor|major|gg)
  -> increment-version.yml (checkout, env BUMP)
       -> bump_version.py: read VERSION + fork-anchors.txt VERSION row
            -> validate old (fullmatch) -> compute new -> validate new
            -> row pattern must equal old VERSION, else exit != 0, nothing written
            -> write VERSION and the row (after all checks)
       -> git branch, commit VERSION + fork-anchors.txt, gh pr create

workflow_dispatch(release.yaml | pre-release.yaml, dry_run)
  -> checkout (fetch-depth 0, tags) -> read VERSION -> build candidate archive (wrapper, verifyConfluentArchive)
  -> Check archive (size, sha256)
  -> NEW: derive baseline tag from VERSION
       -> git worktree add --detach $RUNNER_TEMP/baseline refs/tags/<tag>
       -> baseline wrapper: createConfluentArchive (its own tag's Gradle)
       -> compare_confluent_archives.py A=baseline B=candidate ALLOWLIST=.planning/scripts/confluent-archive-allowlist-<VERSION>.txt
            exit 0 PASS | 1 FAIL (UNALLOWED, STALE-ROW, nested jar, MANIFEST.MF) | 2 unusable input
       -> upload-artifact(report, if: always())
  -> tests.yaml-green gate -> tag-absent gate -> publish (custom_build only) | draft create/check/delete (dry run)
```

### Recommended Project Structure
```
.github/scripts/
  bump_version.py                      # new: CIH-01..03
  compare_confluent_archives.py        # moved from .planning/scripts/compare-confluent-archives.py (git mv)
.planning/scripts/
  confluent-archive-allowlist-v1.7.0_gg5.txt   # git mv of -32 (baseline v1.7.0_gg4)
  confluent-archive-allowlist-v1.7.0_gg4.txt   # git mv of -29 (baseline v1.7.0_gg3), optional
  confluent-archive-allowlist-25.txt           # stays: intermediate merge-tip evidence, not a release
src/test/java/com/clickhouse/kafka/connect/ci/
  BumpVersionScriptUnitTest.java               # new, table-driven, no clickhouse tag
  CompareConfluentArchivesScriptUnitTest.java  # new, synthetic archives
  ReleaseWorkflowGuardUnitTest.java            # new, 30 WR-02 + IN-03 + compare-step pins
```

### Pattern 1: Python script run from a Docker-free JUnit class
**What:** `ProcessBuilder(["python3","-I",script,args...])`, output redirected to temp files, read as UTF-8, `waitFor(60s)`, hard-fail (not skip) when python3 cannot start. Paths (VERSION file, anchors file, allow-list, zips) are passed as arguments so the child never depends on the current directory.
**When to use:** both new scripts.
**Example:**
```java
// Source: src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java:33-79
// private static final String PYTHON = "python3";
// List<String> command = new ArrayList<>(List.of(PYTHON, "-I", SCRIPT.toString()));
// ... redirectOutput(out.toFile()).redirectError(err.toFile()) ... waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
```
Tests must extend nothing: extending `ClickHouseBase` inherits `@Tag("clickhouse")`, which `testUnit` excludes (`build.gradle.kts:282-284`: `excludeTags("clickhouse")`) [VERIFIED: file read].

### Pattern 2: strict version grammar in Python
Use `re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+(_gg[1-9][0-9]*)?", s)`, not the ROADMAP regex verbatim. Measured this session with Python 3.14: `\d` in a `str` pattern accepted the Arabic-indic digit in `v١.7.0`, and `$` accepted `"v1.7.0_gg5\n"`; the `[0-9]` + `fullmatch` form rejected both, rejected `_gg0`, accepted `_gg10` [VERIFIED: python3 -I run in /tmp/gsd37]. Read the VERSION file and strip exactly one trailing newline, so a CRLF or a second line fails validation instead of being trimmed away.

### Pattern 3: bump rules table (pin every row in the JUnit table)
| Input | Bump | Output |
|-------|------|--------|
| `v1.7.0_gg5` | gg | `v1.7.0_gg6` |
| `v1.7.0_gg9` | gg | `v1.7.0_gg10` |
| `v1.7.0` | gg | `v1.7.0_gg1` |
| `v1.7.0_gg5` | patch | `v1.7.1_gg1` |
| `v1.7.0_gg5` | minor | `v1.8.0_gg1` |
| `v1.7.0_gg5` | major | `v2.0.0_gg1` |
| `v1.7.0` | patch | `v1.7.1_gg1` ([ASSUMED] A1: criterion 1 says patch/minor/major "reset the suffix to `_gg1)`, read here as also adding it to a suffix-less input) |
| `v1.7.0_gg0`, `1.7.0`, `v1.7`, `v1.7.0_gg`, `v1.7.0-rc1`, `vnull`, empty, `v١.7.0`, `v1.7.0_gg5\n`, unknown bump word | any | exit != 0, files byte-identical afterwards |
Leading zeros (`v01.2.3`) pass the grammar; state in the table whether the output keeps or normalises them (recommend normalise through `int()`), because a silent choice is the kind of thing a fork rebase later trips on.

### Pattern 4: CIH-03 keyed row
The row is `.planning/scripts/fork-anchors.txt:41`: `VERSION<TAB>v1.7.0_gg5<TAB>1<TAB>BLD-07` (four TAB-separated fields: path, fixed-string pattern, expected count, backs-ID) [VERIFIED: `od -c` of line 41, and `git show v1.7.0_gg5:.planning/scripts/fork-anchors.txt` line 41 identical]. The script must: find rows whose first field is exactly `VERSION`; require exactly one; require its pattern field to equal the current VERSION value, else exit non-zero ("row does not match") before writing anything; rewrite that row's pattern and VERSION in one step (compute both new contents, then write). Zero rows, two rows, a missing anchors file, or a count field other than `1` also fail. The file header says an anchor "can only change in the commit that rewrites this file" and "any commit that intentionally changes an anchor updates this file in the same commit" (`fork-anchors.txt:7-8`), so the workflow commit must `git add VERSION .planning/scripts/fork-anchors.txt` together. `.planning/scripts/fork-anchors.txt` exists on `origin/custom_build` [VERIFIED: `git ls-tree origin/custom_build`], so the dispatch from `custom_build` can read it.

### Anti-Patterns to Avoid
- **`${{ }}` inside `run:`** for the bump input or `NEW_VERSION` (30 IN-02): pass `BUMP: ${{ inputs.version }}` through `env:` and use shell variables afterwards.
- **Writing before validating:** compute and validate both new files in memory first; write VERSION last or via temp-and-rename so a failure leaves nothing behind.
- **A catch-all allow-list row:** `*` would make UNALLOWED unreachable. The allow-list header says "no row is a catch-all" but nothing enforces it; optionally make the compare script exit 2 on a bare `*` glob (see Open Question 4).
- **Downloading the previous published asset as the baseline:** the published asset is built elsewhere and carries a different build date (section 4); rebuild the baseline on the same runner from the tag, as CIH-04 says.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Archive entry compare, allow-list matching, STALE-ROW | A new compare tool | `.planning/scripts/compare-confluent-archives.py` (215 lines, moved) | Already implements UNALLOWED/STALE-ROW/exit 0/1/2 and has been used for gg3->gg4 and gg4->gg5 [VERIFIED: file read] |
| SHA-pin rule for new `uses:` lines | New regex | `TestsWorkflowPinGuardUnitTest.PINNED_USES` via `WorkflowActionPinGuardUnitTest` | The guard already covers every non-`integrationTests.yaml` workflow and discovers new ones |
| Line-level workflow checks | A YAML library | `TestsWorkflowPinGuardUnitTest.nonCommentLines` and the `stepBlock` helpers | Existing convention, "No YAML library is involved" |
| Release creation, tag gate, dry-run cleanup | New steps | Existing steps in `release.yaml:77-183` | Phase 30 built and measured them |
| Version arithmetic via third-party semver | npm/pypi package | 15 lines of integer code | `semver.inc` returning `null` for `_ggN` is the defect |

**Key insight:** every mechanism this phase needs already exists once in the repo; the work is moving, wiring and pinning, which is why "additive, small, tested" is the right shape for a fork that rebases on upstream.

## Section 1: increment-version.yml, the vnull defect, pins and tests

**Current logic** (`.github/workflows/increment-version.yml`):
- Lines 47, 51-54: `npm install --no-save --ignore-scripts semver@7.8.5`, then `VERSION=$(cat VERSION)`, `NEW_VERSION=$(node -p "require('semver').inc('$VERSION', '${{ github.event.inputs.version }}')")`, `echo "v$NEW_VERSION" > VERSION`.
- Defect: `semver.inc('v1.7.0_gg4','patch')` returns `null`, so the file receives the text `vnull` [CITED: `30-COMMAND-EVIDENCE.md` E-06 in `.planning/milestones/v1.7.0_gg5-ci-phases/30-release-workflows-pin-guard/`; not re-run this session, node is not needed by the fix].
- Pins and permissions to keep: `actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1` (line 32), top-level `permissions: contents: read` (20-21), job `contents: write` + `pull-requests: write` (27-29), `runs-on: ubuntu-24.04`, `timeout-minutes: 10`. After the change `actions/setup-node` (line 42) and the npm step disappear; the file keeps one `uses:` line, which satisfies `usesLineCount(lines) >= 1` (`WorkflowActionPinGuardUnitTest.java:185-187`).
- The checkout keeps persisted credentials on purpose (comment at lines 34-35) because "Commit and push" pushes; do not add `persist-credentials: false` to this file.
- The dispatch input is a `choice` with options `patch`, `minor`, `major` (lines 12-18); add `gg` as the fourth option. Header lines 1-5 carry the "Known and left unfixed" note to delete.

**Pin guard and anchors touching this file:**
- `WorkflowActionPinGuardUnitTest.java:49-50` lists `increment-version.yml` in `FORK_WORKFLOWS`; it must stay.
- `fork-anchors.txt:146` `increment-version.yml	node-version: '24'	1	REL-02` goes to 0 when `setup-node` is removed. Replace it in the same commit with 0-count rows (`node-version`, `npm install`, `semver`) and a positive row pinning the script call, backs-ID `CIH-02`. Row 147 (`npm install fs` 0) stays valid. Anchors are counted by line, comments included, so the new header comment must not contain `semver` or `node-version` if those rows are 0.
- `TestsWorkflowPinGuardUnitTest.java:698-700` and `1069-1071` only list `increment-version.yml` in Dependabot `exclude-paths` fixtures; nothing in that class reads the workflow body [VERIFIED: grep]. "TestsWorkflowPinGuardUnitTest updated" (criterion 3) is therefore satisfied by adding new rules; there is no existing assertion that needs editing for this file.

**Docs to edit with it:** `docs/FORK-DELTA.md:82` (the row that says "Known and not fixed: `semver.inc` returns `null` ..."): rewrite as fixed, pointing at the script and test, keep backs-IDs `REL-02` and add `CIH-01..03`. `CHANGELOG.md:22-23` is in the published 1.7.0_gg5 section (historical); leave it, Phase 38 writes the gg6 section.

**Wiring:** workflow step `env: BUMP: ${{ inputs.version }}`, `run: NEW_VERSION="$(python3 -I .github/scripts/bump_version.py "$BUMP")"`, then use `"$NEW_VERSION"` in shell for the branch name, commit and `gh pr create`, and `echo "NEW_VERSION=..." >> "$GITHUB_ENV"` only if a later step needs it. Python runs on the runner without a setup step (same as `tests.yaml` `python3 .github/scripts/jacoco_summary.py`).

**Test:** `BumpVersionScriptUnitTest`, `@ParameterizedTest` over the Pattern 3 table, each case in its own `@TempDir` holding a `VERSION` file and a fixture anchors file; assertions: exit code, stdout equals the new version, both files' new bytes, and for failing rows both files byte-identical to the input. Red-first fixtures for CIH-03: anchors row pattern `v1.7.0_gg4` while VERSION is `v1.7.0_gg5` (mismatch), zero `VERSION` rows, two `VERSION` rows, anchors file absent. The criterion says the failure "was seen red on a fixture", so run the mismatch fixture against a deliberately broken script once and record it in the evidence.

## Section 2: release workflows, compare tooling, allow-list

**Where the archive is built:** `release.yaml:66-75` runs `./gradlew createConfluentArchive --console=plain | tee "$RUNNER_TEMP/archive-build.log"` and requires the log line `> Task :verifyConfluentArchive`; `RELEASE_VERSION` and `ARCHIVE=build/confluent/clickhouse-kafka-connect-${version}.zip` are set at lines 63-64; "Check archive" is lines 77-83. `pre-release.yaml` differs from `release.yaml` only in header text, `name`, `run-name`, `--prerelease` on two creates and `expected="true true ..."` [VERIFIED: `diff` of the two files, 9 differing lines]. Checkout (lines 40-43) is shallow with `persist-credentials: false`; the repo is public [VERIFIED: `gh api repos/ne1r0n/clickhouse-kafka-connect --jq .private` printed `false`].

**Existing compare tool:** `.planning/scripts/compare-confluent-archives.py`, added in `8b0a4cae` (Phase 25-04). Usage line in its docstring: `python3 -I compare-confluent-archives.py A.zip B.zip ALLOWLIST`. Behaviour [VERIFIED: full read]: loads exactly one `lib/*-confluent.jar` per archive, hashes every inner entry and the outer entries (prefix `outer/`; `manifest.json` ignoring lines containing `"release_date"` or `"version"`); allow-list rows are `glob<TAB>reason`, `fnmatch.fnmatchcase`, comments and blanks skipped, empty reason rejected; a difference no row matches prints `UNALLOWED <kind> <path>`, a row matching nothing prints `STALE-ROW <glob>`; it prints a `VERSION <groupId:artifactId> a -> b` table from bundled `pom.properties`, `MANIFEST.MF` status, totals, `nested jars in b`, `kafka classes in b` and `RESULT: PASS|FAIL`; PASS needs no nested jar, no UNALLOWED, no STALE-ROW and a manifest that differs at most in `Implementation-Version`. Exit codes: 0 PASS, 1 FAIL, 2 unusable input (wrong argument count, unreadable zip, not exactly one inner jar, empty inner jar, malformed or unreadable allow-list).

**Move (CIH-05):** `git mv .planning/scripts/compare-confluent-archives.py .github/scripts/compare_confluent_archives.py` (snake_case like `failed_tests_summary.py`). No file outside `.planning/phases` and `.planning/milestones` evidence names the old path (grep of `docs`, `README.md`, `CHANGELOG.md`, `AGENTS.md`, `.planning/scripts/*.sh` found none); the evidence documents are historical records and keep their old path text. Update the docstring `Usage` line and the "A is the baseline archive (v1.7.0_gg3)" sentence.

**Synthetic-archive self-test (`CompareConfluentArchivesScriptUnitTest`):** build zips in `@TempDir` with `ZipOutputStream` (outer zip `<top>/lib/x-confluent.jar` holding an inner jar built the same way, plus `<top>/manifest.json`). Cases: identical archives + empty allow-list -> exit 0, `RESULT: PASS`; a changed inner file with no row -> exit 1 and `UNALLOWED CHANGED <path>`; an added and a removed entry -> `UNALLOWED ONLY-B` / `ONLY-A`; a row matching nothing -> exit 1 and `STALE-ROW <glob>`; an allowed difference with a matching row -> exit 0; `manifest.json` differing only in `release_date`/`version` lines -> exit 0; `MANIFEST.MF` differing beyond `Implementation-Version` -> exit 1; a nested `.jar` inside the inner jar in B -> exit 1 (`nested jars in b: 1`); exit 2 for each of: argument count != 3, missing file, non-zip file, outer zip with zero or two `lib/*-confluent.jar`, inner jar with zero file entries, allow-list row without a tab, empty reason, missing allow-list. A deliberate regression run (neutralise STALE-ROW in a scratch copy, see the test red) belongs in the evidence.

**Allow-list naming (research flag, and the D-05 constraint):**
- Existing files: `confluent-archive-allowlist-25.txt` (gg3 baseline vs merge tips, intermediate), `-29.txt` (header: "A is v1.7.0_gg3 rebuilt from the tag; B is the archive built at the v1.7.0_gg4 tip"), `-32.txt` (header: "A is v1.7.0_gg4 rebuilt from the tag v1.7.0_gg4; B is the archive built at the v1.7.0_gg5 tip"). They are keyed by plan number, not by version [VERIFIED: file reads].
- Recommendation: key by the exact `VERSION` string, `confluent-archive-allowlist-<VERSION>.txt` (for example `confluent-archive-allowlist-v1.7.0_gg5.txt`), and keep the files in `.planning/scripts/`, not `.github/scripts/`. Reason: Phase 38 writes the gg6 file last (RLS-03) and the standing D-05 rule (`STATE.md:253`: "A release phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file") forbids that phase from writing under `.github/`. `.planning/scripts/` is on `origin/custom_build`, so the release workflow's checkout sees it [VERIFIED].
- Rename: `git mv` `-32` to `-v1.7.0_gg5` and `-29` to `-v1.7.0_gg4`; leave `-25` (not a release). A file missing for the current `VERSION` makes the compare exit 2, and the workflow should print an `::error` naming the expected path.

**Baseline selection:** derive the previous release tag from `VERSION` by the rule "highest tag matching the strict grammar that is not `VERSION` itself and is an ancestor of HEAD, ordered numerically on (X, Y, Z, N with suffix-less = 0)". For `v1.7.0_gg6` that selects `v1.7.0_gg5`; for the current `v1.7.0_gg5` tree it selects `v1.7.0_gg4`. Both tags are ancestors of HEAD [VERIFIED: `git merge-base --is-ancestor`]. Put the rule in the Python layer so it is table-testable. Do not use `git describe`: non-release tags such as `v1.7.0_gg5-ci` and `pre-v1.7.0_gg6` exist [VERIFIED: `git tag --list`]. Alternative the planner may prefer: an explicit `# baseline: <tag>` header line in the allow-list file, cross-checked against the derived tag.

**Baseline build:** `actions/checkout` with `fetch-depth: 0` (fetches all history and tags, with `persist-credentials: false` still valid because checkout does the fetch itself) [CITED: README of `actions/checkout@3d3c42e5...`, "Set `fetch-depth: 0` to fetch all history for all branches and tags", verified via `gh api` contents read]. Then `git worktree add --detach "$RUNNER_TEMP/baseline" "refs/tags/$BASELINE_TAG"` and build with the baseline's own wrapper: `./gradlew -p "$RUNNER_TEMP/baseline" --console=plain createConfluentArchive`. The gg4 tag wraps Gradle 9.8.1 (`git show v1.7.0_gg4:gradle/wrapper/gradle-wrapper.properties`) and has `createConfluentArchive` (BLD-15 ran in Phase 25), so any `ggN` tag from gg4 up can be a baseline; gg3 and earlier cannot [VERIFIED]. Measured builds with a warm cache took 10 s (gg4) and 16 s (gg5) locally (this session); the CI dry run of the gg5 commit took 1 min 46 s end to end (run 40693904004), so a second build fits inside `timeout-minutes: 30`.

**Report as artifact:** `python3 -I .github/scripts/compare_confluent_archives.py "$BASE_ARCHIVE" "$ARCHIVE" "$ALLOWLIST" | tee "$RUNNER_TEMP/archive-compare-report.txt"` (the workflow `defaults.run.shell: bash` gives `-e -o pipefail`, so `tee` does not hide exit 1/2), then `actions/upload-artifact@cf430e03... # v7.0.2` with `if: always()`. `upload-artifact` works under a read-only token: the nightly runs it with `contents: read` and succeeded (run 40474535605) [VERIFIED: runs list]. Append a one-line verdict to `$GITHUB_STEP_SUMMARY` because job logs are not readable without admin rights (existing convention, `tests.yaml` header).

**Strictness:** the compare step has no `if:` and no `continue-on-error`, so a publish dispatch fails closed on UNALLOWED, STALE-ROW or exit 2. Keep a dry run strict too: the report is uploaded either way, and an advisory-on-dry-run compare would let a green dry run hide a red compare. The draft cleanup step is already `always()` (`release.yaml:160`).

**Anchor deltas (release.yaml rows 119-132, pre-release.yaml rows 133-145), counted by `grep -cF` per line, comments included:**
- Current counts re-measured: `!inputs.dry_run && github.ref == 'refs/heads/custom_build'` 3, `inputs.dry_run || github.ref != 'refs/heads/custom_build'` 5, `./gradlew createConfluentArchive` 1, `gh release create` 2, `--draft` 1, in both files [VERIFIED: grep].
- The baseline build call written as `./gradlew -p "$RUNNER_TEMP/baseline" ... createConfluentArchive` does not contain the contiguous fixed string `./gradlew createConfluentArchive`, so that row stays 1. If the planner prefers `cd` + `./gradlew createConfluentArchive`, move it to 2 in both files in the same commit.
- 30 WR-01 (concurrency group): a per-run group for dry runs reuses the dry-run expression on the `group:` line, moving `inputs.dry_run || ...` from 5 to 6 in both files; GitHub allows `inputs` and `github` in workflow-level `concurrency` [CITED: docs.github.com contexts page, row "`concurrency` | `github, inputs, vars`"]. Update in the same commit.
- Avoid the literals `gh release create`, `--draft`, `--prerelease`, `--cleanup-tag`, `git push`, `7.4.2`, `gradle-version`, `gradle-build-action` in any new comment or step text, or the counts move.
- Add rows (backs-ID `CIH-04`): the compare script call (1 each), `git worktree add` (1 each), `actions/upload-artifact@` (1 each), `fetch-depth: 0` (1 each), `continue-on-error` (0). Add `ReleaseWorkflowGuardUnitTest` for the semantic pins; anchors only pin text.

**Resolution of the ROADMAP research flag ("the compare before VERSION moves"):** measured with real archives this session (all built from `git archive` copies in `/tmp/gsd37`, no worktree, no push):

| Compare | Allow-list | Result |
|---------|------------|--------|
| baseline `v1.7.0_gg4` rebuilt vs `v1.7.0_gg5` rebuilt from its tag | `confluent-archive-allowlist-32.txt` | exit 0, `RESULT: PASS`, totals `files-a 7399 files-b 7399 only-a 0 only-b 0 changed 4 same 7395` |
| baseline `v1.7.0_gg4` rebuilt vs the archive built from current HEAD `3003b763` (VERSION still `v1.7.0_gg5`, client 0.11.0) | same file | exit 1, `RESULT: FAIL`, 238 `UNALLOWED` lines (116 CHANGED, 118 ONLY-B, 4 ONLY-A), 0 STALE-ROW, 3 `libzstd-jni-1.5.7-23` natives (`darwin/aarch64`, `linux/aarch64`, `linux/amd64`) among them, 31 `com/clickhouse/kafka/connect` paths; totals `files-a 7399 files-b 7513 only-a 4 only-b 118 changed 120 same 7275` |
| published gg5 asset vs gg5 rebuilt from the tag | empty allow-list | exit 0, PASS, `changed 0 same 7399` |

Recommendation: do not generate a provisional allow-list in Phase 37 (a list generated from the current tip is the catch-all anti-pattern the headers forbid, and RLS-03 generates the real gg6 list last). Instead (a) prove locally, Docker-free, both polarities with the numbers above, recorded as evidence; (b) the CIH-05 JUnit synthetic test pins exit codes and the two failure kinds; (c) one CI dry-run dispatch of `release.yaml` from a scratch branch at the Phase 37 tip (needs the user's push approval and the user starts the run) is worth doing because Phase 38 cannot edit `.github/`: any CI-only defect (fetch, worktree, artifact upload) found in Phase 38 has no phase that may fix it. That run is expected RED at the compare step (VERSION gg5 against gg4 with the gg5 list, 238 UNALLOWED expected) and proves the plumbing: baseline worktree built on the runner, report uploaded and downloadable. A CI-green proof would need a scratch branch whose tree equals the gg5 tag plus the Phase 37 `.github` and script commits; offer it as an optional extra, not a requirement. RLS-03/RLS-05 own the first passing CI compare.

**UF-36-4 (`36-SECURITY.md:98`):** "The compare script's `VERSION` table has no `zstd-jni` row ... no gradle/verification-metadata.xml". Measured: the VERSION table printed one version move for gg4-to-HEAD (`at.yawk.lz4:lz4-java 1.12.0 -> 1.12.1`) and none for `client-v2` or `zstd-jni`, because neither ships a `pom.properties` in the inner jar; the entry names `libzstd-jni-1.5.7-23.*` carry the version. Neither item is in CIH-01..05 or ADV-01..03, so treat them as candidate scope only: the compare already makes the zstd natives UNALLOWED, which is what criterion 3 asks for. Recommend dispositioning UF-36-4 in the ledger (owner: next dependency-absorption milestone, re-open trigger: any dependency bump that adds a native jar, or a Dependabot gradle PR) and asking the user whether `gradle/verification-metadata.xml` is wanted at all; adding it is a build change that touches every dependency PR.

## Section 3: ADV-01 and ADV-02 ledger

State was verified by reading the cited files and `git log v1.7.0_gg4..HEAD` on each file this session. Of the 29 rows, none of the Phase 30/31 rows and none of the 16 gg4 rows is already fixed in the tree, except that sub-point (d) of 25 IN-04 was fixed by Phase 30 and the documentation half of 28 IN-02 and 28 IN-04's server half were done in Phase 29. All 16 gg4 rows carry "Owner: the next milestone" in the close record; this milestone is that next one, so each needs a concrete owner. Close ledger location: write one new `37-ADVISORY-LEDGER.md` in the phase directory and leave the archived `*-REVIEW-DISPOSITION.md` and close-record files untouched. A fix row needs its commit pointer, so write the ledger in the last plan. Proposed dispositions are recommendations; the planner decides.

### Phase 30 (`.planning/milestones/v1.7.0_gg5-ci-phases/30-release-workflows-pin-guard/30-REVIEW.md`, all `open`)

| ID | Description | File | Current state | Proposed | src/main? |
|----|-------------|------|---------------|----------|-----------|
| WR-01 | Shared `release-publish` group lets a pending dry run supersede a queued publish | `release.yaml:22-24`, same in `pre-release.yaml` | Unchanged: `group: release-publish`, `cancel-in-progress: false` | FIX with CIH-04: dry runs get a per-run group; anchors 5->6 in both files | no |
| WR-02 | Nothing pins the release safety gates | `WorkflowActionPinGuardUnitTest.java:166-193`, `release.yaml:116-128` | Unchanged; no test reads the release workflow bodies beyond SHA pins | FIX: new `ReleaseWorkflowGuardUnitTest`: single top-level `contents: read`, publish `if:` and every `PUBLISH:` env equal the custom_build expression, compare step has no `if:` or `continue-on-error`, `ubuntu-24.04`, `persist-credentials: false` | no |
| WR-03 | Dependabot comment overstates the guard | `.github/dependabot.yml:16-19` | Unchanged: "WorkflowActionPinGuardUnitTest fails if this list changes." | FIX: reword to name only the exclude-paths of the github-actions entry; comment-only | no |
| IN-01 | FORK-DELTA/header undersell `increment-version.yml` changes | `docs/FORK-DELTA.md:82`, workflow header | FORK-DELTA row already lists Node 24, exact semver install, timeout, token scopes; header still says "hardening only, the behavior is upstream's" | FIXED by CIH-02 (row and header rewritten); pointer = that commit | no |
| IN-02 | Shell/JS interpolation in the bump | `increment-version.yml:51-52, 60-64` | Unchanged | FIXED by CIH-01/02 (`env:` input, shell variables, no `${{ }}` in `run:`) | no |
| IN-03 | ~180 duplicated lines in release/pre-release | both files | Unchanged; diff is 9 lines | FIX by test: normalised-diff assertion in `ReleaseWorkflowGuardUnitTest` (name, run-name, two `--prerelease`, `isPrerelease` expectation). No reusable workflow: the pin guard rejects `uses: ./` | no |
| IN-04 | tests.yaml gate accepts a green run from any branch | `release.yaml:90` | Unchanged | DISPOSITION: accepted (same SHA, same code); owner Phase 38 RLS-05 (it asks for a green run on the published SHA); re-open trigger: a publish whose only green run came from a branch with different workflow files. Optionally reword the notice text while editing the file | no |

### Phase 31 (`.planning/milestones/v1.7.0_gg5-ci-phases/31-fork-owned-nightly-integration-tests/31-REVIEW.md`, all `open`)

| ID | Description | File | Current state | Proposed | src/main? |
|----|-------------|------|---------------|----------|-----------|
| WR-01 | All-unreadable reports summarised as "No failed tests." + success notice | `.github/scripts/failed_tests_summary.py:128-161` | Unchanged: `render` prints `No failed tests.` and a `::notice` whenever `failed` is empty, even with `unreadable` set | FIX: "Result unknown" line and no success notice when `unreadable` is non-empty; add a test for an all-unreadable directory. Existing `malformedReportIsNamedAndTheOthersAreStillRead` (a good file with a failure) is unaffected | no |
| WR-02 | Markdown escaping misses `<`, `&`, `[`, `*`, `\` | `failed_tests_summary.py:39-46` | Unchanged: only `\|`, backtick, CR, LF | FIX with the review's backslash-first order. Existing `specialCharactersInTestNamesAreEscaped` assertions (`contains("\\|")`, no backtick, lines 252-255) still hold under that order; check the other fixtures at plan time, since any changed assertion needs the user's sign-off | no |
| WR-03 | Byte guard fails on autocrlf clones | `.gitattributes`, `UpstreamIntegrationWorkflowBytesGuardUnitTest.java:34-37,105-108` | Unchanged: `.gitattributes` holds only `*.bat text eol=crlf` | FIX: add `.github/workflows/integrationTests.yaml -text` (one line, no blob change); add a FORK-DELTA mention since `.gitattributes` is upstream-owned | no |
| IN-01 | Zero-testcase run reports a green notice | `failed_tests_summary.py:154-161` | Unchanged | FIX: `::warning` when `tests == 0`; verify no existing test expects a notice with zero tests (`runWithoutFailuresWritesNoticeWithAndWithoutSummaryFile`, lines 151-175, to be read at plan time) | no |
| IN-02 | Harness images tag-pinned, Docker Hub throttling | `ConfluentPlatform.java:54,58` | Unchanged: `cnfldemos/cp-server-connect-datagen:` and `confluentinc/cp-server-connect:` by tag | DISPOSITION: owner user; re-open trigger: a nightly red traced to a pull failure or throttling. Digest pinning changes harness behaviour | no (`src/integrationTest`) |
| IN-03 | Double glob; `python3` requirement on Windows | `failed_tests_summary.py:68,114`; `FailedTestsSummaryScriptUnitTest.java:33,64-66` | Unchanged | FIX the script half (one glob result passed down, `glob.escape(directory)`); DISPOSITION the test half: the class Javadoc states the hard fail on missing `python3` is intentional | no |

### The 16 gg4 findings (`.planning/milestones/v1.7.0_gg4-hardening-phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md`)

`git log v1.7.0_gg4..HEAD` over every file below shows only `a1cc3382` (Phase 36, `ClickHouseHelperClient.java`, unrelated to 26 IN-02's `shouldSkipDescribeRow`), so each described state is unchanged since the record was written.

| ID | Description | File | Current state | Proposed | src/main? |
|----|-------------|------|---------------|----------|-----------|
| 25 WR-01 | Archive-step guard accepts a call that verifies nothing | `TestsWorkflowPinGuardUnitTest.java:297-310` (`archiveStepProblem`) | Open: checks `continue-on-error` only | FIX (test-only): reject `-x`, `--dry-run`, trailing `\|\| true`, a step `if:`; require the step before "Run tests"; fixtures red first. Strengthens, deletes nothing | no |
| 25 WR-02 | `ConnectApiReferencesUnitTest` drops unresolvable inherited members, resolves on the test classpath | `ConnectApiReferencesUnitTest.java:183-187, 251-278` | Open | MEASURE then decide: make an unresolvable reference fail if the current list resolves fully; otherwise DISPOSITION owner user, trigger as recorded | no |
| 25 WR-03 | Derivation reads Class/Field/Method constants only | `ConnectApiReferencesUnitTest.java:223-239` | Open, not reachable today | DISPOSITION: owner user, trigger: a connector change using a Kafka type only in a signature or annotation | no |
| 25 IN-01 | Stale version directory gives "found 2" | `ConfluentArchiveCheck.java:82-84`, `build.gradle.kts:396-415` | Open | FIX the message only (test-source `ConfluentArchiveCheck`: name the jars); leave `build.gradle.kts` | no |
| 25 IN-02 | Coverage edges (`META-INF/versions/<n>/`, nested `.zip`/`.war`, first 20) | `ConfluentArchiveCheck.java:43,61,71-76,114` | Open, not present in any measured archive | DISPOSITION: owner user, trigger: a Shadow bump that changes multi-release entries. Note the new compare also reports nested jars | no |
| 25 IN-03 | Brittle exact-text assertions | `TestsWorkflowPinGuardUnitTest.java:67-68`, `ConnectApiReferencesUnitTest.java:63-69,81-84` | Open | DISPOSITION: owner user, trigger: Kotlin DSL rewrite of the archive task (DEPS-F3) | no |
| 25 IN-04 | Hygiene: (a) guava lines, (b) `.gitignore` typo, (c) no `distributionSha256Sum`, (d) pre-release Gradle 7.4.2 | `build.gradle.kts:97,117`; `.gitignore:46`; `gradle-wrapper.properties`; `pre-release.yaml` | (a) still present (Phase 36 E-09 measured guava as redundant on client 0.11.0); (b) typo `# ecliose` present; (c) absent; (d) FIXED by Phase 30 (anchor `7.4.2 0`) | (b) FIX one comment line; (d) closed by `496de32a`; (a) DISPOSITION: upstream-owned lines, owner user, trigger: client bump or CVE on guava; (c) USER DECISION: add the Gradle-published checksum for 9.8.1 or defer (two Dependabot wrapper PRs for 9.8.1/9.8.2 are open on origin) | no |
| 26 IN-02 | `shouldSkipDescribeRow` has no production caller, kept for tests | `ClickHouseHelperClient.java:475-487`, `ParentShape.java:49` | Open; anchor `shouldSkipDescribeRow 3 HLP-02` | DISPOSITION: owner user, needs approval and an anchor edit; trigger as recorded | YES: needs the user |
| 27 WR-01 | Unknown enum label not actionable for the operator | `Column.java:531-534` | Open | DISPOSITION: needs the user's decision on a pass-through exception type | YES: needs the user |
| 27 IN-01 | `passesThroughUnchanged` short-circuits before the Jackson check | `ClickHouseWriter.java:654-664` | Open | DISPOSITION: owner user, trigger: next edit of the Map/Array/Tuple write lambdas | YES: needs the user |
| 27 IN-02 | `JsonAsStringKeyResidualLiveTest` name and intro | `JsonAsStringKeyResidualLiveTest.java:34-43` | Javadoc intro now reads as the current behaviour (the key must reach none of the destinations); only the class name still says "Residual" | DISPOSITION for the name (FORK-DELTA rows name the class); re-read the intro at plan time | no |
| 27 IN-03 | Guard scans `src/main/java` only, broad single-letter prefix list | `SourcePlanningIdGuardUnitTest.java:34-45` | Open | DISPOSITION: owner user, trigger: an upstream sync comment trips the guard | no |
| 28 IN-01 | `DeclaredNullability.ESCAPED_NEWLINE` public constant read by tests | `DeclaredNullability.java:43-44` | Open; read from test packages `sink` and `sink.db.mapping`, so it cannot become package-private | DISPOSITION: owner user | YES: needs the user |
| 28 IN-02 | Coverage list cannot detect a missing fork class | `ForkCoverageScopeUnitTest.java:44-51,98-106`, `build.gradle.kts:308-353` | Documentation half done in Phase 29 | DISPOSITION: owner user, trigger: new fork class or `jacocoForkReport` edit | no |
| 28 IN-04 | Enum-escape pin and wording say "not measured" | `ColumnTest.java:195-201`, `RawNameMatrix` | Server half measured (29-COMMAND-EVIDENCE E-24) | FIX if the measured values from E-24 can be pinned Docker-free in `ColumnTest` (additions only); else DISPOSITION | no |
| 28 IN-05 | Helper clients not closed; null assertion not tied to the enum | `DescribeUnreadableEnumMockServerTest.java:62-75` | Open | FIX the close (try-with-resources). Tightening the null assertion changes an assertion: needs the user's sign-off | no |

Counts: Phase 30 = 7 rows (3 fix, 2 closed by CIH, 1 fix-by-test, 1 disposition); Phase 31 = 6 rows (5 fix or partial fix, 1 disposition plus the test half of IN-03). gg4 = 16 rows: 4 `src/main` rows all recommended for disposition, which keeps the `src/main` diff empty and needs no approval; the other 12 split between fix and disposition as tabled. Not in the 16 and not in scope: 28 IN-03 (fulfilled in Phase 29), and the `src/main` finding the ROADMAP note cites (26-REVIEW IN-13, `Table.registerValidColumn` anchor), which `26-REVIEW-DISPOSITION.md` already records as `fixed`.

## Section 4: ADV-03, the real v1.7.0_gg5 release (read-only REST readings, 2026-10-31)

All calls were `GET` through `/opt/homebrew/bin/gh api`; `gh auth status` reported account `ne1r0n` with token scopes `gist`, `read:org`, `repo`. No mutating call was made. Raw JSON is in `/tmp/gsd37/release.json` and `/tmp/gsd37/tagref.json` (scratch only).

| Item | Value |
|------|-------|
| Release | id `426690002`, tag `v1.7.0_gg5`, name `v1.7.0_gg5`, `draft` false, `prerelease` false, `target_commitish` `custom_build`, author `ne1r0n` (User), only release in the list |
| Dates | `created_at` 2026-10-26T22:56:14Z (equals the tag commit's timestamp), `published_at` 2026-10-28T13:16:09Z |
| Tag | `refs/tags/v1.7.0_gg5`, lightweight, `object.type` commit, sha `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; local tag `v1.7.0_gg5` is `3a3a15a9`; `custom_build` head is the same SHA (`compare` status `identical`) |
| Tag commit | merge commit by Alexander Chashnikov, 2026-10-26T22:56:14Z, parents `21c51598b06b6f5af58430765d57b1e5f6747c5a` and `41e7d5465e8353abb43a5f56dcf9f697c96b537a`; its message contains the sentence "No GitHub release is published for v1.7.0_gg5." |
| Asset | id `685055288`, name `clickhouse-kafka-connect-v1.7.0_gg5.zip`, size `12453556`, API digest `sha256:4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767`, uploader `ne1r0n`, asset `created_at` 2026-10-28T13:15:52Z, `download_count` 0 at read time |
| Downloaded copy | `/tmp/gsd37/asset/clickhouse-kafka-connect-v1.7.0_gg5.zip` (own empty directory): `shasum -a 256` = `4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767`, 12453556 bytes: matches the API digest and size |
| Green `tests.yaml` on that SHA | run `40416215635`, `push`, `custom_build`, success, 2026-10-26T22:56:30Z |

**Workflow runs of `release.yaml` (all on `ne1r0n`'s dispatch):**

| Run | Event/branch | SHA | Conclusion | Notes |
|-----|--------------|-----|------------|-------|
| `40296481765` (#1) | dispatch, `sync/v1.7.0` | `b847fbdb` | success | dry run, 2026-10-26T09:02:04Z |
| `40474419526` (#2) | dispatch, `custom_build` | `3a3a15a9` | cancelled | title "Release custom_build (publish)", 2026-10-27T08:34:11Z; cancelled during "Build and verify Confluent archive"; annotation: "The run was canceled by @ne1r0n."; steps 7-14 (including "Publish release") skipped |
| `40693904004` (#3) | dispatch, `custom_build` | `3a3a15a9` | success | title "Release custom_build (dry run)", 13:09:39Z to 13:11:25Z on 2026-10-28; "Publish release" skipped; annotation: `Archive clickhouse-kafka-connect-v1.7.0_gg5.zip 12453661 bytes sha256=d61a69324393f961410b35a861551f3f6783a77b957262516cb813efb264fa99`; Gradle 9.8.1 from the wrapper; the draft was created, checked and deleted; tag gate warned "tag v1.7.0_gg5 already exists" (advisory on a dry run) |
`pre-release.yaml` has one run (`40296330498`, dry run, `sync/v1.7.0`, success). The annotations came from `GET /check-runs/<job id>/annotations`, which needs no log access.

**What this means for the record (finding, high confidence on the facts, inference marked):**
1. No workflow run created the release. The only publish-mode run was cancelled before "Publish release", the later run was a dry run, and the asset was uploaded by the user account `ne1r0n` six minutes after that dry run finished, not by `github-actions[bot]`. The release was therefore created outside `release.yaml` ([ASSUMED] A2: by hand, via the web UI or a local `gh`/API call; the REST data show who and when but not which tool).
2. The published asset is not the archive the dry run built: 12453556 bytes vs 12453661, and SHA-256 `4c69b6fb...` vs `d61a6932...`. The cause is not determinable from the record (the draft and its asset were deleted by the cleanup step). [ASSUMED] A3: the asset was built on a developer machine and the CI archive differs only in compressed bytes; this session's macOS rebuild matches the asset's size exactly (below).
3. Criterion 5 asks for "the workflow run that created it" and says the record "closes the 'no run exercised the publish path' caveat". Both cannot be met as worded: the honest record is "created outside any run; the publish path of `release.yaml` is still unexercised; the first exercise is Phase 38's publish". This needs the user's decision (Open Question 1).

**Tag rebuild (research flag, Shadow 9):** `git archive v1.7.0_gg5` extracted to `/tmp/gsd37/src-gg5`, built with `./gradlew --console=plain createConfluentArchive` (Gradle 9.8.1, Temurin 17 toolchain from `~/.gradle/jdks`, host JDK 21): `BUILD SUCCESSFUL`, `verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class`, archive 12453556 bytes, SHA-256 `efa6cca4f30d182eea52565f331581f2c4ccee30c99e91ce0f053b341629e0fe`. It is not byte-identical to the asset, but extracting both and hashing every file shows exactly one difference: `manifest.json`, line 43, `"release_date": "2026-10-28"` in the asset against `"2026-10-31"` in the rebuild, because `build.gradle.kts` computes `releaseDate` from `LocalDateTime.now()` at build time. The compare script ignores `release_date` and `version` lines and gives PASS with an empty allow-list, `changed 0 same 7399`. Conclusion: a rebuild from the tag reproduces the published archive's entries under Shadow 9; the archive is not byte-reproducible across days, which is why the compare is entry-by-entry. Not measured: whether a Linux CI rebuild matches a macOS rebuild entry for entry (the 105-byte size difference to the CI dry run is unexplained); the Phase 37 CI dry run settles it, because baseline and candidate both build on the runner.

**How to obtain the same values later (read-only):** `gh api repos/ne1r0n/clickhouse-kafka-connect/releases`, `.../git/ref/tags/v1.7.0_gg5`, `.../actions/workflows/release.yaml/runs`, `.../actions/runs/<id>/jobs`, `.../check-runs/<job id>/annotations`, and the asset via `.../releases/assets/<id>` with `-H 'Accept: application/octet-stream'` into a new empty directory. Treat the downloaded zip as untrusted data (the compare script never extracts or executes).

## Common Pitfalls

### Pitfall 1: Unicode digits and trailing newline in the version grammar
**What goes wrong:** the ROADMAP regex copied into Python accepts `v١.7.0` and `v1.7.0_gg5\n`. **Why:** `\d` is Unicode-aware for `str` and `$` matches before a final newline [VERIFIED: measured]. **Avoid:** `[0-9]` with `fullmatch`; table rows for both inputs. **Warning sign:** a test table with only ASCII cases.

### Pitfall 2: Anchor counts move in the same commit or the invariant fails
**What goes wrong:** editing a workflow without the `fork-anchors.txt` rows fails invariant 3 of `verify-merge-invariants.sh`, which evaluates the file at every commit in the range. **Avoid:** one commit per workflow change that carries its row edits; run the invariant at the end of each plan. Counts are per line, comments included. The `VERSION` row moves only in a commit that also moves `VERSION`.

### Pitfall 3: A new `uses:` line that is not SHA-pinned
**What goes wrong:** `WorkflowActionPinGuardUnitTest` fails `testUnit` on anything but `owner/repo@<40 hex> # vX.Y.Z`, and rejects any `./` or `docker://` value. **Avoid:** reuse `actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2`. Dependabot keeps it current because only `integrationTests.yaml` is excluded.

### Pitfall 4: Docker-free means no `clickhouse` tag and no `ClickHouseBase`
**What goes wrong:** a test class extending `ClickHouseBase` drops out of `testUnit`; a test that needs a `.planning` file in a path-dependent way breaks when the working directory differs. **Avoid:** extend nothing, pass every path as an argument, use `@TempDir`; run the new classes with Docker stopped (the Docker runtime is the colima profile `vz`) before the phase gate.

### Pitfall 5: The workflow checkout is shallow
**What goes wrong:** `git worktree add ... refs/tags/<tag>` fails with an unknown revision. **Avoid:** `fetch-depth: 0` on the checkout; the `fetch-depth` key is not an anchored text, so adding it moves no count.

### Pitfall 6: Allow-list under `.github/`
**What goes wrong:** Phase 38 (RLS-03) must write the gg6 file and may not edit `.github/`. **Avoid:** keep data in `.planning/scripts/`, scripts in `.github/scripts/`.

### Pitfall 7: Spotless baseline
The Spotless config is `importOrder()` and `removeUnusedImports()` only (`build.gradle.kts:564-570`, `googleJavaFormat()` is commented out). There are 46 pre-existing violations (project memory). Run `./gradlew spotlessApply`, then revert anything in a file this phase did not otherwise touch; new files must be clean. Compare the failing-file list before and after rather than expecting zero.

### Pitfall 8: Existing assertions
Success criterion 4 forbids deleting or weakening an assertion; changing one needs the user's sign-off. Before each test-side fix run a diff check of `assert*` lines (`git diff -U0 | grep '^-.*assert'`) and list any removal for the user. 25 WR-01 strengthens a rule; 28 IN-05 tightens an assertion and so needs sign-off.

### Pitfall 9: Standing hygiene rule
Every file this phase commits (evidence, ledger, tests, fixtures, docs, workflow comments) must name no deployment, no external reproduction or production environment, and none of the identifiers the rule lists. Use angle-bracket placeholders such as `<connector-name>`. Run the existing read-only gate `python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-hygiene-check.py FILE...` over every new file; it prints `FILE:LINE` only. This research file was checked with it.

### Pitfall 10: Pushes and mutating `gh`
Nothing goes to `origin` and no mutating `gh` call is made without the user's direct approval. A CI dry run needs the scratch branch on `origin` at a SHA no other pushed branch holds; the push publishes the local `.planning` tail one way and runs `tests.yaml` once (`STATE.md:324`). The user starts the dispatch from GitHub's `Run workflow` control and reports the run.

## Code Examples

### Strict grammar and bump core (sketch for the plan, stdlib only)
```python
# Source: derived from the measurements in Pattern 2; plan owns the final form.
import re
VERSION_RE = re.compile(r"v([0-9]+)\.([0-9]+)\.([0-9]+)(?:_gg([1-9][0-9]*))?")

def bump(old, kind):
    m = VERSION_RE.fullmatch(old)          # fullmatch: no trailing-newline escape hatch
    if m is None:
        raise ValueError("invalid version: %r" % old)
    x, y, z, gg = int(m[1]), int(m[2]), int(m[3]), m[4]
    if kind == "gg":
        new = "v%d.%d.%d_gg%d" % (x, y, z, (int(gg) + 1) if gg else 1)
    elif kind in ("patch", "minor", "major"):
        x, y, z = {"patch": (x, y, z + 1), "minor": (x, y + 1, 0), "major": (x + 1, 0, 0)}[kind]
        new = "v%d.%d.%d_gg1" % (x, y, z)
    else:
        raise ValueError("unknown bump: %r" % kind)
    assert VERSION_RE.fullmatch(new)       # output validated too (criterion 1)
    return new
```

### Workflow step shape (illustrative; avoid the anchored literals in comments)
```yaml
      - name: Increment version
        env:
          BUMP: ${{ inputs.version }}
        run: |
          NEW_VERSION="$(python3 -I .github/scripts/bump_version.py "$BUMP")"
          echo "NEW_VERSION=${NEW_VERSION}" >> "$GITHUB_ENV"
```
Later steps use `$NEW_VERSION` in shell, not `${{ env.NEW_VERSION }}` inside the script text.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| npm `semver.inc` on the full `vX.Y.Z_ggN` string | Fork-owned script with strict grammar | This phase | `vnull` impossible by construction |
| Compare run by hand, allow-lists keyed by plan number | Compare in the release workflows, allow-lists keyed by `VERSION` | This phase | An archive change cannot ship unseen |
| Phase 36 before/after listing by ad-hoc worktrees (`36-COMMAND-EVIDENCE.md` E-09) | Same technique, automated on the runner | This phase | The zstd-jni natives surface as UNALLOWED without a manual review |

**Deprecated/outdated:** `actions/setup-node` + npm install in `increment-version.yml` (removed here).

## Runtime State Inventory

Not a rename or migration phase; omitted. One related runtime fact: the Dependabot `github-actions` entry watches every workflow except `integrationTests.yaml`, so the new `upload-artifact` pin in the release files is kept current automatically (`.github/dependabot.yml:22-31`).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `patch`/`minor`/`major` on a suffix-less `vX.Y.Z` yields `vX.Y.(Z+1)_gg1` (criterion 1 says "reset the suffix to `_gg1`") | Pattern 3 | The table test pins the wrong output for suffix-less input |
| A2 | The `v1.7.0_gg5` release was created by hand (web UI or a local API/`gh` call) | Section 4 | The record would mis-describe the creation path; the REST facts (no run, uploader login) hold either way |
| A3 | The CI dry-run archive differs from the asset only in compressed bytes (platform), not in entries | Section 4 | A real content difference between CI and local builds would surface only in the Phase 37 CI dry run |
| A4 | Dependabot rewrites `distributionSha256Sum` together with a wrapper version bump | Section 3, 25 IN-04 (c) | If not, adding the checksum would break each wrapper PR until hand-edited |

## Open Questions

1. **ADV-03 wording versus the facts.**
   - What we know: no run created the release; the publish path was never exercised (Section 4).
   - What's unclear: whether the user wants criterion 5 reworded to "recorded as created outside the workflow, caveat remains open", or wants the release regenerated or re-verified another way.
   - Recommendation: record the facts as measured, keep the caveat open, and let Phase 38's real publish close it; ask the user to confirm before the ledger says otherwise.
2. **CI dry-run dispatch.**
   - What we know: it needs a scratch-branch push and the user starting the run; at VERSION gg5 the compare is expected RED with ~238 UNALLOWED lines; a green CI proof needs a scratch branch built on the gg5 tag.
   - What's unclear: whether the user approves the push, and red-only vs also-green.
   - Recommendation: request approval for the red-expected run as a plumbing proof; mention the optional green variant.
3. **Allow-list rename.** Move `-29` and `-32` to the `-<VERSION>` names with `git mv`, or copy and keep both? Recommendation: `git mv`, since no script reads the numbered names.
4. **Catch-all guard in the compare script.** Reject a bare `*` glob with exit 2? It changes the script's behaviour beyond a move; recommended as one extra self-test row, user may decline.
5. **`gradle/verification-metadata.xml` (UF-36-4) and `distributionSha256Sum` (25 IN-04 c).** Candidate scope only, not in the requirements. Recommendation: disposition both with owner and trigger unless the user asks for them.
6. **Four `src/main` rows** (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01) are proposed for disposition; if the user wants any fixed, that is an explicit approval.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| python3 | Scripts and their JUnit tests | yes | 3.14.8 (`/opt/homebrew/bin/python3`); CI runner python3 [VERIFIED: used by `tests.yaml`] | none needed |
| JDK 17 toolchain | Gradle builds | yes | `~/.gradle/jdks/adoptium-17-aarch64-hotspot-mac`; host JDK 21.0.12.1 | `-PjavaVersion=21` is a test-JVM escape only |
| Gradle wrapper | Archive builds | yes | 9.8.1 (gg4, gg5 and HEAD wrappers) | none |
| `gh` | Read-only release evidence | yes | `/opt/homebrew/bin/gh`, account `ne1r0n` (scopes `gist`, `read:org`, `repo`); only `GET` used | WebFetch of public API URLs |
| Docker | Full suite on `latest` x V2 at the phase gate only | not probed this session | colima profile `vz` | Docker-free tests must run with it stopped |
| `timeout` | (not available on macOS) | no | - | do not use in local commands |

**Missing dependencies with no fallback:** none for the Docker-free work.

## Security Domain

`workflow.security_enforcement` is `true` in `.planning/config.json`; ASVS level 1.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1 Architecture / V14 Configuration | yes | Least-privilege tokens (job-level `contents: write` only where a push happens), SHA-pinned actions, no `${{ }}` in `run:` |
| V5 Input Validation | yes | Strict `fullmatch` grammar on the dispatch input and on VERSION; allow-list and archive parsing reject malformed input with exit 2 |
| V12 Files and Resources | yes | Compare never extracts or executes archive content; downloaded assets are untrusted data in their own directory |
| V2/V3/V4/V6 | no | No authentication, session, access-control or crypto code is changed |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Script injection through a dispatch input into `run:` | Tampering | Pass inputs via `env:`; validate in the script (30 IN-02) |
| Unpinned or tag-pinned action | Tampering | SHA pin + `# vX.Y.Z`, enforced by `WorkflowActionPinGuardUnitTest` |
| Supply-chain install at CI time (`npm install semver`) | Tampering | Removed; stdlib only |
| Silent archive content change (native jars, dependency swaps) | Tampering | Entry-by-entry compare under a `VERSION`-keyed allow-list, fail closed on publish |
| Catch-all allow-list row | Tampering | Header rule today; optional script rejection of a bare `*` |
| Over-broad token on the release job | Elevation | `contents: write` + `actions: read` already job-scoped; the new steps add none |

## Sources

### Primary (HIGH confidence)
- Repo files read this session: `.github/workflows/{increment-version.yml,release.yaml,pre-release.yaml,tests.yaml}`, `.github/dependabot.yml`, `.github/scripts/failed_tests_summary.py`, `.planning/scripts/{compare-confluent-archives.py,fork-anchors.txt,confluent-archive-allowlist-25/29/32.txt}`, `src/test/.../ci/{WorkflowActionPinGuardUnitTest,TestsWorkflowPinGuardUnitTest,FailedTestsSummaryScriptUnitTest}.java`, `build.gradle.kts`, `.gitattributes`, `docs/FORK-DELTA.md`, `CHANGELOG.md`, the Phase 30 and 31 REVIEW and disposition files, `29-MILESTONE-CLOSE-RECORD.md`, `36-FOLLOWUPS.md`, `36-SECURITY.md`, `36-COMMAND-EVIDENCE.md` E-09.
- GitHub REST API read-only (`gh api`): release `426690002`, tag ref, commit `3a3a15a9...`, workflow runs `40296481765`, `40474419526`, `40693904004`, `40296330498`, `40416215635`, job annotations, asset download.
- Local measurements in `/tmp/gsd37`: three archive rebuilds from `git archive` copies (gg4, gg5, HEAD), compare runs, outer-entry hash diff, Python regex probes.

### Secondary (MEDIUM confidence)
- docs.github.com contexts page: `concurrency` context availability (`github, inputs, vars`).
- `actions/checkout@3d3c42e5...` README `fetch-depth` text, read through the contents API.

### Tertiary (LOW confidence)
- None used as authority; assumptions A1-A4 are listed above.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - every tool is already in the repo and was read or run.
- Architecture: MEDIUM - the CI wiring (fetch-depth, worktree on the runner, artifact upload on a read-only token for this exact job) is reasoned from precedent and has not run; Phase 37's dry run is the proof.
- Pitfalls: HIGH - regex and anchor behaviour measured; guard behaviour read from source.
- ADV-03: HIGH for the readings; the interpretation of how the release was created is A2.

**Research date:** 2026-10-31
**Valid until:** 2026-11-30 (GitHub API values are point-in-time; re-read if a new release or run appears)
