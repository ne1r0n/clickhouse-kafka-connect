# Phase 25: Command evidence

- Phase: 25 (Dependency Absorption & Upstream Mini-Sync)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 25 plans. Nothing here was pushed.
- Shell: macOS zsh; every Gradle call is `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz`
- Entry format: one `### E-NN: {title}` section per entry, with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. A red-run entry's verdict is `pass` when the red set equals the prediction. Long output stays in `/tmp/gsd25/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | OPS-04 (facts at phase start) | pass |
| E-02 | OPS-04 (operator answer) | pass |
| E-03 | DEBT-13 (red run) | pass |
| E-04 | DEBT-13 (green run, spotless, testUnit) | pass |
| E-05 | DEPS-02 (pre-flight) | pass |
| E-06 | DEPS-03 (merge 1 rehearsal and exemption rows) | pass |
| E-07 | DEPS-02, DEPS-03 (merge 1, cumulative gate, self-test) | pass |
| E-08 | DEPS-02 (merged-tree Docker-free checks) | pass |
| E-09 | BLD-15 (unrepaired archive) | pass |
| E-10 | BLD-15 (repair) | pass |
| E-11 | BLD-15 (compare against v1.7.0_gg3) | pass |
| E-12 | BLD-16 (worker jars and bless run) | pass |
| E-13 | BLD-16 (reference test) | pass |
| E-14 | CI-20 (verifyConfluentArchive controls) | pass |
| E-15 | CI-20 (pin-guard red run) | pass |
| E-16 | CI-20 (pin-guard green run) | pass |
| E-17 | DEBT-14 (pin-guard red run) | pass |
| E-18 | DEBT-14 (pin-guard green run, accept-set changes) | pass |
| E-19 | DEPS-02 (step-4 suite gate) | pass |
| E-20 | SYNC-11 (merge 2 rehearsal) | pass |
| E-21 | SYNC-11 (merge 2, tail gate, self-test) | pass |
| E-22 | SYNC-12, BLD-15 (post-merge archive, dependencyInsight, advisories) | pass |
| E-23 | DEBT-15 | pass |
| E-24 | SYNC-12 (two --rerun runs, baseline tag) | pending (plan 25-08) |
| E-25 | OPS-04 (closing observation) | pending (plan 25-08) |

### E-01: OPS-04 facts at phase start (REST, GitHub clock)

Commands as run (R = `https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect`; unauthenticated, no `gh`, no token):

```bash
curl -sS "$R/labels/dependencies" -o /tmp/gsd25/exec/ops04-label.json -D /tmp/gsd25/exec/ops04-label.headers -w '%{http_code}'
curl -sS --fail "$R/labels?per_page=100" -o /tmp/gsd25/exec/ops04-labels.json
curl -sS --fail "$R/pulls?state=open&per_page=100" -o /tmp/gsd25/exec/ops04-pulls.json
curl -sS --fail "$R/issues/{4,10}/comments?per_page=100" -o /tmp/gsd25/exec/ops04-comments-{N}.json
```

```text
github_date: Wed, 21 Oct 2026 14:07:41 GMT
label_dependencies_http: 404
labels_count: 9
labels_names: bug, documentation, duplicate, enhancement, good first issue, help wanted, invalid, question, wontfix
open_prs: #10 dependabot[bot] 2026-10-20T21:37:00Z chore(deps): bump com.google.protobuf:protobuf-java from 3.25.9 to 4.36.2; #4 dependabot[bot] 2026-10-14T21:59:26Z chore(deps): bump clickHouseDriver from 0.9.6 to 0.10.0
open_dependabot_prs: 4, 10
dependabot_prs_with_labels_comment: #4: yes, #10: yes
```

Timestamp: 2026-10-21T14:07:46Z

The `dependencies` label is absent before the checkpoint, as expected; both open Dependabot PRs carry the "labels could not be found" comment.

Verdict: pass

### E-02: OPS-04 operator answer (verbatim)

Presented inline by the orchestrator in this session (plan 25-01 Task 2, `blocking-human`): the E-01 facts, the four web-UI steps for creating the `dependencies` label, and the request to answer in the user's own words. The answer below is the user's typed reply, copied unedited.

OPS-04 answer (verbatim, given directly to the orchestrator, 2026-10-21T15:11:05Z):
> just created the label

orchestrator annotation (not the user's words): no screenshot was given. The answer states no clock time; "just" is read as the instant the answer was received, which is the `ops04_created_at_user` value below. The API bounds the creation instant to the window after E-01's `404` at 2026-10-21T14:07:46Z and at or before the re-read below.

Command re-run by the orchestrator after the answer (unauthenticated, `R=https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect`, GitHub `date:` header `Wed, 21 Oct 2026 15:11:05 GMT`, `x-ratelimit-remaining: 59`):

```bash
curl -sS -D /tmp/ops04-headers.txt -o /tmp/ops04-body.json -w '%{http_code}' "$R/labels/dependencies"
```

```text
ops04_created_at_user: 2026-10-21T15:11:05Z
ops04_created_window_api: after 2026-10-21T14:07:46Z (E-01 404), at or before 2026-10-21T15:11:05Z (200)
label_dependencies_http_after: 200
label_name_after: dependencies
label_description_after: Dependency updates (Dependabot)
ops04_user_and_api_agree: yes
OPS-04 label created: pass
```

The user says the label exists, the status is 200 and the JSON `name` is exactly `dependencies`, so the agree line is `yes`. No label, PR or `dependabot.yml` change was made by an agent; no authenticated call was made. The closing observation (a Dependabot PR without the "labels could not be found" comment, or pending the next monthly run) is E-25 in plan 25-08, not this entry.

Timestamp: 2026-10-21T15:11:05Z

Verdict: pass

### E-03: DEBT-13 red run against the old ktsCode

Command as run (the three DEBT-13 tests added to `TestsWorkflowPinGuardUnitTest`, `ktsCode` still the old regex reader, nothing committed):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun > /tmp/gsd25/exec/debt13-red.log 2>&1; echo $? > /tmp/gsd25/exec/debt13-red.rc
```

```text
exit code: 1 (/tmp/gsd25/exec/debt13-red.rc)
Test result: FAILURE
Tests summary: 29 tests, 26 succeeded, 3 failed, 0 skipped
XML: /tmp/gsd25/exec/debt13-red.xml (tests="29"; the 26 existing tests pass)
```

Failing tests (exactly the predicted three), each with its fixture-level failure messages from the XML:

1. `DEBT-13: ktsCode keeps string literals, raw strings and line structure, and drops line, trailing and nested block comments` (4 failures)
   - `expected: <[val x = 1 ]> but was: <[val x = 1 // see /* here]>`
   - `expected: <[a  b]> but was: <[a  still */ b]>`
   - `expected: <8> but was: <6>` (line count of the three-line block comment plus raw string text)
   - `expected: <[val c = '"'; val d = 2 ]> but was: <[val c = '"'; val d = 2 // gone]>`
2. `fixture: reportTestForks task rule accepts the task after a slash-star inside a string, a raw string or a line comment (DEBT-13, 21-REVIEW WR-02)` (4 failures): each of the string, line-comment, trailing-comment and raw-string fixtures got `tasks.register("reportTestForks") not found in build.gradle.kts outside comments`
3. `fixture: reportTestForks task rule rejects a read inside a trailing comment and a task inside a nested block comment (DEBT-13, 21-REVIEW WR-02, WR-03)` (2 failures): `must reject ... but the rule accepted it` for both the trailing-comment read and the nested-comment task

Old versus wanted:

| Fixture | Wanted | Old result |
|---------|--------|------------|
| string `"org/apache/kafka/connect/**"` before the task, banner after | accept | rejected (phantom comment swallows the task) |
| line comment `// ... lib/* ...` before the task | accept | rejected (phantom comment) |
| trailing comment `// see /* here` before the task | accept | rejected (phantom comment) |
| raw string holding both openers before the task | accept | rejected (phantom comment) |
| read only in a trailing line comment after a constant print | reject | accepted (trailing comment survives) |
| task inside a nested block comment | reject | accepted (first closer ends the comment early) |
| ktsCode: trailing comment stripped; nested comment removed; line count kept; quote in a char literal does not open a string | as stated | all four wrong |

`build.gradle.kts`, `.github`, `gradle` and `src/main` are unchanged (`git diff --quiet HEAD` clean).

Timestamp: 2026-10-21T15:15:56Z

Verdict: pass

### E-04: DEBT-13 green run, per-file spotless and full testUnit

Commands as run (`ktsCode` replaced by the one-pass scanner; states: code, string, char, raw string, line comment, block comment with a nesting depth):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun > /tmp/gsd25/exec/debt13-green.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd25/exec/testunit-debt13.log 2>&1
git worktree add --detach /tmp/gsd25/wt-fmt HEAD   # class copied in, then:
(cd /tmp/gsd25/wt-fmt && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q spotlessApply)
cmp /tmp/gsd25/wt-fmt/src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
git worktree remove --force /tmp/gsd25/wt-fmt
```

```text
class run:  Test result: SUCCESS; Tests summary: 29 tests, 29 succeeded, 0 failed, 0 skipped (/tmp/gsd25/exec/debt13-green.xml)
full run:   Test result: SUCCESS; Tests summary: 841 tests, 839 succeeded, 0 failed, 2 skipped (/tmp/gsd25/exec/testunit-debt13.log)
spotlessApply in the worktree: exit 0, 47 files differ from HEAD = the 46 baseline files it reformats plus the class itself (copied in, so it differs from HEAD)
cmp of the worktree class against the main-tree class: identical (spotless left it unchanged)
```

The 26 existing tests keep their method names, display names and assertions; the three DEBT-13 tests pass (red in E-03). The real-file pin `reportTestForksReadsMaxParallelForksFromTheTestTask` passes with the scanner.

Research correction: research Pitfall 10 says `spotlessCheck` fails on "exactly the same 2 files". That is the truncated listing (`Violations also present in 44 other files`, `/tmp/p25/spot-w1.log` line 84), so the baseline is 46 files, as Phase 24 E-60 measured. The per-file check is therefore "the class is byte-identical after spotlessApply", not "spotlessCheck is clean".

Timestamp: 2026-10-21T15:16:39Z

Verdict: pass

### E-05: Pre-flight (refs, origin tip, latest, merge-tree)

Commands as run (repository root, branch `sync/v1.7.0`, HEAD `ade4eb98`):

```bash
git for-each-ref refs/upstream; git update-ref -d refs/upstream/main; git update-ref -d refs/upstream/v1.6.0
git for-each-ref refs/upstream; git rev-parse --verify upstream/main 2>/tmp/gsd25/exec/upstream-rp.err
git ls-remote origin custom_build; git rev-parse origin/custom_build
colima list; docker image inspect clickhouse/clickhouse-server:latest --format '{{.Id}} {{.Created}}'
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local -q "select version()"
git merge-tree --write-tree HEAD 69cd680bf548bfb0329142752d37f8a6c3190c26; echo $?
```

```text
refs_upstream_before: 789a57c24e71d972b55d3daad6f4393984251160 refs/upstream/main; 789a57c24e71d972b55d3daad6f4393984251160 refs/upstream/v1.6.0
refs_upstream_after: none
upstream_main: a6f1df839c079180bc7c343b5823f205e204dafc (stderr empty, no ambiguity warning)
ls_remote_origin_custom_build: 69cd680bf548bfb0329142752d37f8a6c3190c26
origin_custom_build_tracking_ref: 69cd680bf548bfb0329142752d37f8a6c3190c26
colima: profile vz Running
latest_image: sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b created 2026-10-14T18:05:21Z (not pulled)
latest_version: 26.9.19.9
merge_tree_m1_tree: 40488286a8831040061c90f6bed8f9ba85dc32ad
merge_tree_m1_exit: 0
```

The only uncommitted tracked file at the start was the orchestrator's `.planning/state.json`, which this plan never stages.

Timestamp: 2026-10-21T15:42:43Z

Verdict: pass

### E-06: Merge 1 rehearsal and the generated exemption rows

Rehearsal in the throwaway worktree `/tmp/gsd25/wt-m1` (detached at `ade4eb98`): `git merge --no-commit --no-ff 69cd680bf548bfb0329142752d37f8a6c3190c26` ("Automatic merge went well; stopped before committing"), the one BLD-08 row of `fork-anchors.txt` moved from `kafka = "4.1.1"` to `kafka = "4.4.0"` (exactly one replacement, no slf4j edit), merge committed.

```bash
bash /tmp/gsd25/wt-m1/.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD > m1-gate1.out 2> m1-gate1.err   # unedited gate, default env
python3 -I /tmp/gsd25/exec/gen-exemptions.py m1-gate1.err m1-exemption-rows.tsv
# rows appended under the DEPS-03 comment line, git commit --amend --no-edit, then the same gate command -> m1-gate2.{out,err}
```

```text
gate 1 last line: invariants: provenance_paths=21 anchor_commits=589 anchor_rows=69 marker_commits=636 anchor_exempt=1 result=FAIL
gate 1 stderr: 18 lines, all "ANCHOR VIOLATION: <short> <path> '<pattern>' expected N got M (<id>)"
distinct SHAs: 17 (rows: 18)
gate 1 wall time: 580 s
rehearsal merge: b87fe42e7584b2e65fecedb7f709317814202c3c (parents ade4eb98060eec661ad0c8f97fc20d91dd5ca2e0 69cd680bf548bfb0329142752d37f8a6c3190c26)
gate 2 last line: invariants: provenance_paths=21 anchor_commits=589 anchor_rows=69 marker_commits=636 anchor_exempt=19 result=PASS
gate 2 stderr: empty
gate 2 wall time: 626 s
```

Generated rows (full SHA, path, pattern, PR number; the reason field is `Dependabot #N on origin/custom_build, published before the DEPS-02 merge moved this anchor row; exempts only this row at this commit (DEPS-03)`):

| SHA | Path | Pattern | PR |
|-----|------|---------|----|
| 69cd680bf548bfb0329142752d37f8a6c3190c26 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #14 |
| af762b1b25b2107f9e7bd4a8766ea992f4632edf | gradle/libs.versions.toml | `kafka = "4.1.1"` | #15 |
| 2555fd2b33345e8a4b2f52bd12ebdf430962e337 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #15 |
| de27d2e916af7f942da5c13b72c5e8a21b59f66a | gradle/libs.versions.toml | `kafka = "4.1.1"` | #14 |
| d20bae636a55c8ffce616b945738edb291f56924 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #11 |
| 4b9ddf961ae3a028a143e6f920f2787d4be56ea4 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #12 |
| 55845c3acac2b30b9a19a940749f84a5a7c56450 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #13 |
| 83de3b78e84564328ec02a6a70b44b29d05e3d52 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #14 |
| dba83534f32dc2545ac1e1a58047755d18f9e0f6 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #14 |
| 5315776f7a150f241a0a6b469b0f74bf43c5db3a | gradle/libs.versions.toml | `kafka = "4.1.1"` | #14 |
| 71ac96600829dd3503447b1ece42f6a074dcb446 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #7 |
| 6e7fbbc79ccc021dd35ff5a8ff9a5363a0c8855a | gradle/libs.versions.toml | `kafka = "4.1.1"` | #6 |
| 75ee14b0bcbb8e21e13f09b0c73e7b0bb3c0c74a | gradle/libs.versions.toml | `kafka = "4.1.1"` | #9 |
| 6ad5b80c02024ecfe830df7a8a10bf505ecf93d1 | build.gradle.kts | `slf4j-reload4j:2.0.17` | #14 |
| 6ad5b80c02024ecfe830df7a8a10bf505ecf93d1 | gradle/libs.versions.toml | `kafka = "4.1.1"` | #14 |
| d84272d78faf137f978e1de23ce7f9d9391e7a39 | build.gradle.kts | `slf4j-reload4j:2.0.17` | #14 |
| 6d3827a24f8f36b4297c778c5ef340d47cd46ec2 | build.gradle.kts | `slf4j-reload4j:2.0.17` | #14 |
| 2ccd748678a67380d308a28c736cb7dbbab721b9 | build.gradle.kts | `slf4j-reload4j:2.0.17` | #14 |

Correction to the plan text: the gate prints the pattern wrapped in single quotes (`'<pattern>'`) for display, while an exemption row stores it bare, as the existing `6473e24f` row does. The generator therefore strips that one pair of display quotes and fails if it is absent; the path and the pattern inside the quotes are otherwise byte for byte. Counts matched the research measurement exactly (18 rows, 17 SHAs, 14 `kafka` rows and 4 `slf4j-reload4j:2.0.17` rows, `anchor_exempt=19`).

Timestamp: 2026-10-21T15:42:43Z

Verdict: pass

### E-07: Merge 1, the cumulative anchors gate and the self-test (DEPS-02, DEPS-03)

Commands as run (branch `sync/v1.7.0`; the evidence commit of E-05/E-06, `a884032d`, was the pre-merge tip):

```bash
git ls-remote origin custom_build                      # 69cd680bf548bfb0329142752d37f8a6c3190c26, read immediately before the merge
git tag pre-v1.7.0-deps                                # lightweight, local, never pushed
git merge --no-commit --no-ff 69cd680bf548bfb0329142752d37f8a6c3190c26
git checkout "$(cat /tmp/gsd25/exec/m1-rehearsal.sha)" -- .planning/scripts/fork-anchors.txt .planning/scripts/fork-anchor-exemptions.txt
git commit -F /tmp/gsd25/exec/m1-msg.txt
.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD > m1-gate.out 2> m1-gate.err          # unedited, default env
.planning/scripts/verify-merge-invariants-selftest.sh > m1-selftest.out 2>&1; echo $? > m1-selftest.rc
git worktree remove --force /tmp/gsd25/wt-m1; git worktree prune
```

```text
M1: 50f562895c7d1a2fb79c50457cb0635e9aa48f48
parents: a884032d2cae660759efebeb5c2c6afc717cd188 (= pre-v1.7.0-deps), 69cd680bf548bfb0329142752d37f8a6c3190c26 (= the ls-remote value)
git diff --name-only M1^1 M1: .planning/scripts/fork-anchor-exemptions.txt, .planning/scripts/fork-anchors.txt, build.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.jar, gradle/wrapper/gradle-wrapper.properties, gradlew.bat
git show --cc --name-only --format= M1: .planning/scripts/fork-anchor-exemptions.txt, .planning/scripts/fork-anchors.txt
M1:gradlew.bat blob == 69cd680b:gradlew.bat blob: 03754c4c3d9206f5c9ef69e41990dfe26a05251e
git diff --quiet <rehearsal> M1 -- . ':(exclude).planning/phases/25-dependency-absorption-upstream-mini-sync': exit 0 (tree equals the rehearsal outside the phase directory)
gate last line: invariants: provenance_paths=21 anchor_commits=590 anchor_rows=69 marker_commits=637 anchor_exempt=19 result=PASS
gate stderr: empty; gate wall time: 586 s
self-test: exit 0, 16 PASS cases, last line "selftest: no failures"; wall time: 723 s
verify-merge-invariants.sh and -selftest.sh: git diff --quiet pre-v1.7.0-deps HEAD exits 0 (byte-identical)
```

`grep -cF 'kafka = "4.4.0"' .planning/scripts/fork-anchors.txt` is 1. `gradlew.bat` shows modified in `git status` (CRLF in the index versus `*.bat text eol=crlf`, research Pattern 1) and was never staged. The gate and the self-test ran concurrently, each into its own log files.

X-4 correction recorded: the DEPS-03 gate is the default-env cumulative command `verify-merge-invariants.sh 54e63648 HEAD`; `pre-v1.7.0-deps` is a rollback tag only (a gate with MERGE_SHA = M1 fails invariant 1 on the legitimate bookkeeping), which supersedes the requirement's "PRE_TAG at the pre-merge rollback tag" wording. X-2: one anchor edit only (kafka 4.1.1 -> 4.4.0); the slf4j literal and its anchor row already say 2.0.20.

Timestamp: 2026-10-21T15:55:33Z

Verdict: pass

### E-08: Docker-free checks on the merged tree (DEPS-02)

Commands as run on M1 (`50f56289`) with no build-script change since:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd25/exec/testunit-m1.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain assemble --warning-mode all > /tmp/gsd25/exec/warn-m1.log 2>&1
shasum -a 256 gradle/wrapper/gradle-wrapper.jar; curl -sSL --fail https://services.gradle.org/distributions/gradle-9.8.1-wrapper.jar.sha256
git log --first-parent --merges --format=%s M1^2 ^M1^1        # keep "Merge pull request #N from ne1r0n/dependabot/"
/tmp/gsd25/exec/spotless-numstat.sh v1.7.0_gg3 > spotless-gg3.txt; /tmp/gsd25/exec/spotless-numstat.sh HEAD > spotless-m1.txt   # git archive into a fresh dir, git init, spotlessApply --offline, git diff --numstat | sort -k3
```

```text
testUnit: Test result: SUCCESS
testUnit: Tests summary: 841 tests, 839 succeeded, 0 failed, 2 skipped
assemble --warning-mode all: BUILD SUCCESSFUL, six warning kinds present:
  The 'val name by creating' property delegate syntax has been deprecated. This is scheduled to be removed in Gradle 10.
  The 'val name by extra(...)' or 'val name by extra { ... }' property delegate syntax has been deprecated. This is scheduled to be removed in Gradle 10.
  The 'val name by getting(Type::class) { }' property delegate syntax has been deprecated. This is scheduled to be removed in Gradle 10.
  The 'val name by registering(Type::class) { }' property delegate syntax has been deprecated. This is scheduled to be removed in Gradle 10.
  The com.github.ben-manes.versions plugin id is deprecated; apply io.github.ben-manes.versions instead.
  The Configuration.setVisible(boolean) method has been deprecated. This is scheduled to be removed in Gradle 11.
build.gradle.kts unchanged since M1: git diff --quiet HEAD -- build.gradle.kts exits 0
wrapper_jar_sha256: 3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98
published_sha256:   3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98
wrapper_sha256_matches: yes
distributionUrl: https\://services.gradle.org/distributions/gradle-9.8.1-bin.zip
distributionSha256Sum: absent (optional hardening, not this phase)
nine_prs: 6,7,8,9,11,12,13,14,15
spotless_numstat_lines: 46
spotless_baseline_equal: yes
```

Notes: the published checksum URL answers 301 first, so the fetch needs `-L` (the plan's command had none). The `--warning-mode all` warnings are recorded and left unfixed (DEPS-F3). Spotless is baseline-equal: the numstat of `HEAD` equals that of `v1.7.0_gg3`, byte for byte, 46 files. The full suite is not run here by design: it waits for the Shadow repair (plan 25-07, E-19).

Timestamp: 2026-10-21T16:06:49Z

Verdict: pass

### E-09: The unrepaired Shadow 9.6.1 archive on the merge-1 tree (BLD-15 before)

Tree: `6d59b920` (merge 1 `50f56289` plus evidence commits), `build.gradle.kts` still holding the two-argument `from(...)` call (the repair is `bddb838d`, below). The archive was copied to `/tmp/gsd25/exec/archive-m1-unrepaired.zip` before the next build.

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean createConfluentArchive > /tmp/gsd25/exec/archive-m1-unrepaired.log 2>&1
python3 -I - archive-m1-unrepaired.zip   # zipfile in memory: outer lib/*.jar, then the inner jar's file entries and *.jar entries
```

```text
BUILD SUCCESSFUL in 3s   (10 actionable tasks: 10 executed)
outer lib jars: 1  (clickhouse-kafka-connect-v1.7.0_gg3/lib/clickhouse-kafka-connect-v1.7.0_gg3-confluent.jar)
inner jar file entries (directories excluded): 135
nested *.jar entries: 25
  client-v2-0.9.6.jar  clickhouse-http-client-0.9.6.jar  httpclient5-5.6.5.jar  clickhouse-client-0.9.6.jar
  lz4-java-1.12.0.jar  gson-2.14.0.jar  jackson-databind-2.22.4.jar  jackson-core-2.22.4.jar
  jackson-annotations-2.22.jar  httpcore5-h2-5.4.4.jar  httpcore5-5.4.4.jar  slf4j-api-2.0.7.jar
  clickhouse-data-0.9.6.jar  commons-compress-1.28.0.jar  commons-codec-1.19.0.jar  commons-io-2.20.0.jar
  commons-lang3-3.20.0.jar  RoaringBitmap-1.0.6.jar  asm-9.7.jar  guava-33.4.6-jre.jar
  failureaccess-1.0.3.jar  listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar  jspecify-1.0.0.jar
  error_prone_annotations-2.48.0.jar  j2objc-annotations-3.0.0.jar
```

Notes: the build is green while the inner jar nests 25 dependency jars, so the worker would find no usable classes of them. Research counted 158 inner entries; that figure includes directory entries, and the 135 here are files only (the same counting the 7,376 / 7,395 figures use).

Timestamp: 2026-10-21T16:08:00Z

Verdict: pass

### E-10: The repaired archive (BLD-15, D-01..D-04)

Repair commit: `bddb838d` (`build(25-04): repair confluentJar under Shadow 9.6.1 and pin it with two anchors (BLD-15, D-01..D-04)`). It is the only non-merge first-parent commit touching `build.gradle.kts` after `test(25-02)` (`92b2be1e`, an ancestor).

```diff
 tasks.register<ShadowJar>("confluentJar") {
     archiveClassifier.set("confluent")
-    from(clickhouseDependencies, sourceSets.main.get().output)
+    // Shadow 9 copies jars given to from(...) as-is and unpacks only configurations.
+    configurations.add(clickhouseDependencies)
+    from(sourceSets.main.get().output)
+    addMultiReleaseAttribute.set(false)
+}
+
+shadow {
+    addShadowJarToAssembleLifecycle.set(false)
 }
```

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean createConfluentArchive > /tmp/gsd25/exec/archive-m1-repaired.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain assemble --dry-run > /tmp/gsd25/exec/assemble-dry.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*TestsWorkflowPinGuardUnitTest'
grep -cF 'configurations.add(clickhouseDependencies)' build.gradle.kts; grep -cF 'from(clickhouseDependencies,' build.gradle.kts
```

```text
BUILD SUCCESSFUL in 7s   (10 actionable tasks: 10 executed)
outer lib jars: 1
inner jar sha256: 72849d239fa1b4fca9668d56cd7ef3d6af1436b42ac94af0c84db23733ae65ca
inner jar file entries: 7395   nested *.jar: 0   org/apache/kafka/ entries: 0
com/clickhouse/kafka/connect/ClickHouseSinkConnector.class: present
META-INF/MANIFEST.MF: 'Manifest-Version: 1.0\r\nImplementation-Title: ClickHouse-Kafka-Connect\r\nImplementation-Version: v1.7.0_gg3\r\n\r\n'   (no Multi-Release)
assemble --dry-run tasks: :protobufDummy :extractIncludeProto :extractProto :generateProto :generateVersionFile :compileJava :processProtoResources :processResources :classes :jar :assemble   (no shadowJar)
TestsWorkflowPinGuardUnitTest: tests=29 skipped=0 failures=0 errors=0
anchor counts: configurations.add(clickhouseDependencies) = 1; from(clickhouseDependencies, = 0
fork-anchors.txt: two BLD-15 rows added in the same commit
```

Notes: D-01's primary form (Shadow `configurations.add(...)`) shipped; the zipTree fallback was not needed to get a flat jar (E-11 shows whether any difference remained).

Timestamp: 2026-10-21T16:13:00Z

Verdict: pass

### E-11: Archive compare against v1.7.0_gg3 (BLD-15, D-01, D-05)

Baseline: `git worktree add --detach /tmp/gsd25/wt-gg3 v1.7.0_gg3` (HEAD `3376a18f`), `createConfluentArchive` there (Gradle 9.6.1, Shadow 8.3.5 as of the tag), zip copied to `/tmp/gsd25/exec/archive-gg3.zip`, worktree removed and pruned. It reproduces 24-COMMAND-EVIDENCE E-04:

```text
baseline inner jar sha256: c2514401f76729d17f61bab3d405288801c2b162dba10a58eead3d20e264b36e
baseline inner jar file entries: 7376
```

Commands as run (the repaired archive is E-10's):

```bash
python3 -I .planning/scripts/compare-confluent-archives.py /tmp/gsd25/exec/archive-gg3.zip /tmp/gsd25/exec/archive-m1-repaired.zip .planning/scripts/confluent-archive-allowlist-25.txt > /tmp/gsd25/exec/cmp-m1.txt; echo $?   # 0
python3 -I ... > /tmp/gsd25/exec/cmp-m1-again.txt; cmp cmp-m1.txt cmp-m1-again.txt                                                                                   # identical
```

Allow-list rows (`.planning/scripts/confluent-archive-allowlist-25.txt`, glob then reason):

```text
com/fasterxml/jackson/core/*  ->  jackson-core 2.22.3 -> 2.22.4 (classes)
com/fasterxml/jackson/databind/*  ->  jackson-databind 2.22.3 -> 2.22.4 (classes)
META-INF/versions/*/com/fasterxml/jackson/core/internal/shaded/fdp/*  ->  jackson-core 2.22.3 -> 2.22.4 (versioned shaded fdp package renamed v2_22_3 -> v2_22_4)
META-INF/maven/com.fasterxml.jackson.core/jackson-core/*  ->  jackson-core 2.22.3 -> 2.22.4 (pom.properties and pom.xml)
META-INF/maven/com.fasterxml.jackson.core/jackson-databind/*  ->  jackson-databind 2.22.3 -> 2.22.4 (pom.properties and pom.xml)
com/google/gson/*  ->  gson 2.13.1 -> 2.14.0 (classes)
META-INF/maven/com.google.code.gson/gson/*  ->  gson 2.13.1 -> 2.14.0 (pom.properties and pom.xml)
META-INF/versions/9/module-info.class  ->  gson 2.13.1 -> 2.14.0 (module descriptor; gson is the jar whose module-info lands at this path)
org/apache/hc/client5/*  ->  httpclient5 5.6.2 -> 5.6.5 (classes)
org/apache/hc/core5/*  ->  httpcore5 and httpcore5-h2 5.4.3 -> 5.4.4 (classes)
org/publicsuffix/list/*  ->  httpclient5 5.6.2 -> 5.6.5 (bundled public suffix list data)
META-INF/DEPENDENCIES  ->  httpclient5 5.6.2 -> 5.6.5 (the DEPENDENCIES report of the jar that supplies it)
META-INF/maven/org.apache.httpcomponents.client5/httpclient5/*  ->  httpclient5 5.6.2 -> 5.6.5 (pom.properties and pom.xml)
META-INF/maven/org.apache.httpcomponents.core5/httpcore5/*  ->  httpcore5 5.4.3 -> 5.4.4 (pom.properties and pom.xml)
META-INF/maven/org.apache.httpcomponents.core5/httpcore5-h2/*  ->  httpcore5-h2 5.4.3 -> 5.4.4 (pom.properties and pom.xml)
com/google/errorprone/annotations/*  ->  error_prone_annotations 2.38.0 -> 2.48.0 (classes)
META-INF/maven/com.google.errorprone/error_prone_annotations/*  ->  error_prone_annotations 2.38.0 -> 2.48.0 (pom.properties and pom.xml)
```

Compare output without the per-entry lines (the full output, with all 441 CHANGED / ONLY-A / ONLY-B lines, is `/tmp/gsd25/exec/cmp-m1.txt`; the same 441 differences all matched a row, no UNALLOWED line, no STALE-ROW line, no outer-entry difference, and no difference under `com/clickhouse/`):

```text
VERSION at.yawk.lz4:lz4-java 1.12.0 -> 1.12.0
VERSION com.fasterxml.jackson.core:jackson-annotations 2.22 -> 2.22
VERSION com.fasterxml.jackson.core:jackson-core 2.22.3 -> 2.22.4
VERSION com.fasterxml.jackson.core:jackson-databind 2.22.3 -> 2.22.4
VERSION com.google.code.gson:gson 2.13.1 -> 2.14.0
VERSION com.google.errorprone:error_prone_annotations 2.38.0 -> 2.48.0
VERSION com.google.guava:failureaccess 1.0.3 -> 1.0.3
VERSION com.google.guava:guava 33.4.6-jre -> 33.4.6-jre
VERSION com.google.guava:listenablefuture 9999.0-empty-to-avoid-conflict-with-guava -> 9999.0-empty-to-avoid-conflict-with-guava
VERSION com.google.j2objc:j2objc-annotations 3.0.0 -> 3.0.0
VERSION commons-codec:commons-codec 1.19.0 -> 1.19.0
VERSION commons-io:commons-io 2.20.0 -> 2.20.0
VERSION org.apache.commons:commons-compress 1.28.0 -> 1.28.0
VERSION org.apache.commons:commons-lang3 3.20.0 -> 3.20.0
VERSION org.apache.httpcomponents.client5:httpclient5 5.6.2 -> 5.6.5
VERSION org.apache.httpcomponents.core5:httpcore5 5.4.3 -> 5.4.4
VERSION org.apache.httpcomponents.core5:httpcore5-h2 5.4.3 -> 5.4.4
VERSION org.slf4j:slf4j-api 2.0.7 -> 2.0.7
MANIFEST.MF: identical
totals files-a 7376 files-b 7395 only-a 51 only-b 70 changed 320 same 7005
nested jars in b: 0
kafka classes in b: 0
RESULT: PASS
```

Determinism: two runs on the same inputs are byte-identical (`cmp` silent).

Negative controls (all against the gg3 archive as A):

| Control | Inputs | Exit | Proving line |
|---|---|---|---|
| unrepaired archive as B | `archive-m1-unrepaired.zip`, real allow-list | 1 | `nested jars in b: 25`, `RESULT: FAIL` |
| one extra row | allow-list plus `com/example/never/*<TAB>control` | 1 | `STALE-ROW com/example/never/*` |
| one real row removed | allow-list without the `jackson-core ... (classes)` row | 1 | 85 `UNALLOWED` lines, first `UNALLOWED CHANGED com/fasterxml/jackson/core/filter/FilteringGeneratorDelegate.class` |
| a zip holding only a text file as B | `neg4.zip` with `note.txt` | 2 | `ERROR: .../neg4.zip: expected exactly one lib/*-confluent.jar, found 0` |

D-01: the primary form (Shadow `configurations.add(...)`) shipped. Every difference is explained by a version bump read from the VERSION lines (jackson-core and jackson-databind 2.22.3 -> 2.22.4, gson 2.13.1 -> 2.14.0, httpclient5 5.6.2 -> 5.6.5, httpcore5 and httpcore5-h2 5.4.3 -> 5.4.4, error_prone_annotations 2.38.0 -> 2.48.0), so the zipTree provider fallback was not used. `META-INF/versions/9/module-info.class` was traced to gson (the sha256 of the changed file equals the one in `gson-2.14.0.jar`, and the old one equals `gson-2.13.1.jar`'s). `META-INF/DEPENDENCIES` was traced to httpclient5 the same way.

X-1: guava 33.4.6-jre and its small dependencies are in the gg3 archive already (their VERSION lines read `x -> x`), so the allow-list holds version bumps only and no row allows an added library. `MANIFEST.MF` of the repaired inner jar is byte-identical to the gg3 one (`VERSION` unchanged in this phase). The outer entries (`manifest.json` ignoring `release_date` and `version`, `assets/logo.svg`, `doc/LICENSE`, `doc/README.md`) are equal.

Timestamp: 2026-10-21T16:25:00Z

Verdict: pass

### E-12: The production worker's Kafka API and the bless run (BLD-16, D-10..D-13)

Image: `confluentinc/cp-kafka-connect-base:7.9.7`, pulled by tag, digest recorded. The jars are untrusted downloaded data: copied with `docker create` and `docker cp` into new empty directories (`/tmp/gsd25/img/amd64/`, `/tmp/gsd25/img/native/`), hashed, and only read by `javap` and `zipfile` (the script runs as `python3 -I` from `.planning/scripts/`, never from inside the jar directories).

```text
docker pull --platform linux/amd64 confluentinc/cp-kafka-connect-base:7.9.7
Digest: sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055
docker run --rm --platform linux/amd64 --entrypoint uname ... -m      -> x86_64
docker run --rm --platform linux/amd64 --entrypoint java ... -version -> openjdk version "17.0.19" 2026-04-21 / Temurin-17.0.19+10
docker pull confluentinc/cp-kafka-connect-base:7.9.7                  (native, last, so the local tag ends native)
Digest: sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055
docker image inspect --format '{{index .RepoDigests 0}} {{.Architecture}} {{.Os}}' ...
confluentinc/cp-kafka-connect-base@sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055 arm64 linux
docker run --rm --entrypoint uname ... -m                             -> aarch64
docker run --rm --entrypoint java ... -version                        -> openjdk version "17.0.19" 2026-04-21 / Temurin-17.0.19+10
```

Jars from `/usr/share/java/kafka/` (amd64 copy and native copy hashed separately; they are byte-identical, which resolves research A3):

```text
b57546e9160d5435238fded6f38756d621fac62bd9f64737c3f68e094ae01de4  img/amd64/connect-api-7.9.7-ccs.jar
7e3071075dca79f0b3db3950be9de8d770fe5dd8649fac75ed25b2b39906eaac  img/amd64/connect-runtime-7.9.7-ccs.jar
28423086fbbf8f91b2f1d335eeeca6675916e24adf2f813b3f9c47f87b49f962  img/amd64/kafka-clients-7.9.7-ccs.jar
b57546e9160d5435238fded6f38756d621fac62bd9f64737c3f68e094ae01de4  img/native/connect-api-7.9.7-ccs.jar
7e3071075dca79f0b3db3950be9de8d770fe5dd8649fac75ed25b2b39906eaac  img/native/connect-runtime-7.9.7-ccs.jar
28423086fbbf8f91b2f1d335eeeca6675916e24adf2f813b3f9c47f87b49f962  img/native/kafka-clients-7.9.7-ccs.jar
```

Cache jars the build resolves (Gradle cache, `org.apache.kafka`):

```text
9e8fe3277d31410e3dd6c84b34967230bb5643b8424d3b96320ce1c43b4b5152  connect-api-4.1.1.jar
1f13c2617cb3ff39924a6d7a1265b3582b09f35c146c1547b38ebd308c8b61eb  connect-api-4.4.0.jar
00247122a572c3d3ae4eb60a50192a5811bc0181f51923d13018600f892e821d  kafka-clients-4.1.1.jar
1ab07317eb6633241392449d329dbdc5d9251e6ffc4926860f42dd966e88bdf9  kafka-clients-4.4.0.jar
```

Bless run (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava` first; 107 class files under `build/classes/java/main`; the compile classpath is the 4.4.0 pair):

```bash
python3 -I .planning/scripts/connect-api-refs.py --classes build/classes/java/main \
  --compile-classpath <4.4.0 connect-api>:<4.4.0 kafka-clients> \
  --reference worker=/tmp/gsd25/img/native/connect-api-7.9.7-ccs.jar:/tmp/gsd25/img/native/kafka-clients-7.9.7-ccs.jar \
  --reference 4.1.1=<4.1.1 jars> --reference 4.4.0=<4.4.0 jars> \
  --write src/test/resources/connect-api-references.txt > /tmp/gsd25/exec/bless.txt; echo $?   # 0
```

```text
SKIPPED-SUPERTYPE com/fasterxml/jackson/databind/JsonSerializer
derived classes 39 members 137 total 176
REFERENCE worker classes-missing 0 members-missing 0
REFERENCE 4.1.1 classes-missing 0 members-missing 0
REFERENCE 4.4.0 classes-missing 0 members-missing 0
WROTE src/test/resources/connect-api-references.txt
```

The one `SKIPPED-SUPERTYPE` is a Jackson type a connector class extends; `javap` cannot load it from the compile classpath and it lies outside `org/apache/kafka/`. The derived set (39 `C` plus 137 `M` = 176 entries) equals the research prototype's `/tmp/p25/derive-java.out` line for line (`diff` empty). The committed list is `src/test/resources/connect-api-references.txt` (provenance header, then 176 sorted entries).

Negative controls (`/tmp/gsd25/exec/bless-neg.txt`, no `--write`):

```text
REFERENCE ccs-api-only classes-missing 17 members-missing 37
exit=1
(research recorded 17 classes and 36 members; the live run counts 37 members, the same 17 classes)
javap -cp <worker connect-api + kafka-clients> org.apache.kafka.connect.components.ConnectPlugin
Error: class not found: org.apache.kafka.connect.components.ConnectPlugin     (exit 1)
javap -cp <4.4.0 connect-api + kafka-clients> org.apache.kafka.connect.components.ConnectPlugin
public interface org.apache.kafka.connect.components.ConnectPlugin extends org.apache.kafka.connect.components.Versioned {
```

Facts the Connect API wording needs (`javap -p -cp <connect-api jar> org.apache.kafka.connect.sink.SinkTask | grep -c onPartitionsRevoked`):

```text
connect-api-7.9.7-ccs.jar (worker, Kafka 3.9): 1   public void onPartitionsRevoked(java.util.Collection<org.apache.kafka.common.TopicPartition>);
connect-api-4.1.1.jar: 0
connect-api-4.4.0.jar: 0
worker SinkTask bytecode: close(Collection) invokes onPartitionsRevoked(Collection); open(Collection) invokes onPartitionsAssigned(Collection)
```

This contradicts the plan's expectation that the worker's `SinkTask` lacks the method: the Kafka 3.9 worker API still declares `SinkTask.onPartitionsRevoked`; it is absent from the compile-time 4.1.1 and 4.4.0 jars. The wording in the D-14 commit follows this measurement (see the plan summary's deviations).

`javap -c -p -cp /tmp/gsd25/img/native/connect-runtime-7.9.7-ccs.jar org.apache.kafka.connect.runtime.errors.DeadLetterQueueReporter` (full output `/tmp/gsd25/exec/dlq-javap.txt`):

```text
  void populateContextHeaders(org.apache.kafka.clients.producer.ProducerRecord<byte[], byte[]>, org.apache.kafka.connect.runtime.errors.ProcessingContext<org.apache.kafka.clients.consumer.ConsumerReco
     152: invokevirtual #85                 // Method java/lang/Class.getName:()Ljava/lang/String;
     182: invokevirtual #85                 // Method java/lang/Class.getName:()Ljava/lang/String;
     202: invokevirtual #90                 // Method java/lang/Throwable.getMessage:()Ljava/lang/String;
     219: invokespecial #91                 // Method stacktrace:(Ljava/lang/Throwable;)[B
  private byte[] stacktrace(java.lang.Throwable);
      26: invokevirtual #99                 // Method java/lang/Throwable.printStackTrace:(Ljava/io/PrintStream;)V
```

So `populateContextHeaders` exists in the worker's connect-runtime, calls `java/lang/Class.getName` twice (the `__connect.errors.class.name` and `__connect.errors.exception.class.name` header names), `java/lang/Throwable.getMessage` (`__connect.errors.exception.message`), and the private `stacktrace(Throwable)`, which calls `Throwable.printStackTrace(PrintStream)` (`__connect.errors.exception.stacktrace`).

Archive re-assertion (plan 25-04's `/tmp/gsd25/exec/archive-m1-repaired.zip`):

```text
inner jars ['clickhouse-kafka-connect-v1.7.0_gg3/lib/clickhouse-kafka-connect-v1.7.0_gg3-confluent.jar']
inner entries 7798 org/apache/kafka entries 0 nested jars 0
outer entries under org/apache/kafka 0
```

Container cleanup: `docker ps -a --filter name=gsd25 --format '{{.Names}}'` prints nothing.

Timestamp: 2026-10-21T16:24:57Z

Verdict: pass

### E-13: The Connect-member reference test (BLD-16, D-13)

`ConnectApiReferencesUnitTest` (package `com.clickhouse.kafka.connect.ci`, untagged, extends nothing) reads every class file under `com/clickhouse/kafka/connect/` with a `DataInputStream` constant-pool reader, produces the script's `C` and `M` strings, resolves connector-owned owners by reflection (superclass chain, then interfaces, descriptors built from the reflected types), and requires the derived set to be a subset of `src/test/resources/connect-api-references.txt` (read from the classpath as UTF-8).

Research assumption A2 was half right: on the `testUnit` runtime classpath the connector's code source is the jar `build/libs/clickhouse-kafka-connect-v1.7.0_gg3.jar` (the test-fixtures dependency on the main project), not `build/classes/java/main`. The first run failed on the plan's "must be a directory" assertion (`The connector classes must be a directory, but were loaded from file:/.../build/libs/clickhouse-kafka-connect-v1.7.0_gg3.jar`), so the test now reads the code source through `java.nio.file.Path`, opening the jar as a zip file system when it is not a directory. Both forms are covered by the same `derivedReferences(Path)`.

Red control: one `M` line removed from the working copy of the list (`M org/apache/kafka/common/config/AbstractConfig.getBoolean:(Ljava/lang/String;)Ljava/lang/Boolean;`, an entry inherited through a connector class), `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*ConnectApiReferencesUnitTest' --rerun`, XML copied to `/tmp/gsd25/exec/refs-red.xml` (3 tests, 1 failure), then `git checkout -- src/test/resources/connect-api-references.txt`:

```text
ConnectApiReferencesUnitTest > BLD-16: every org.apache.kafka class and member the compiled connector references is in the blessed worker-API list FAILED
    org.opentest4j.AssertionFailedError: The compiled connector references Kafka API entries missing from the blessed list /connect-api-references.txt. Each is a class or member the production worker's Connect API may lack:
    M org/apache/kafka/common/config/AbstractConfig.getBoolean:(Ljava/lang/String;)Ljava/lang/Boolean;
    Check each entry against the worker's connect-api and kafka-clients jars, then re-bless with: python3 -I .planning/scripts/connect-api-refs.py --classes build/classes/java/main --compile-classpath <connect-api>:<kafka-clients> --reference worker=<worker connect-api>:<worker kafka-clients> --write src/test/resources/connect-api-references.txt
Tests summary: 3 tests, 2 succeeded, 1 failed, 0 skipped
```

Equality with the script (D-11 and D-13 producers agree): the real-file test temporarily wrote its derived set to `/tmp/gsd25/exec/derived-java.txt` (the write was removed before the commit; the committed class has no `/tmp/` path):

```text
$ grep -v '^#' src/test/resources/connect-api-references.txt | diff - /tmp/gsd25/exec/derived-java.txt   # empty, exit 0
derived-java.txt: 176 lines
```

The five kind anchors are all in the blessed list and were found by the derivation without a swap: `C org/apache/kafka/connect/sink/SinkTask`, `M org/apache/kafka/connect/sink/SinkRecord.topic:()Ljava/lang/String;`, `M org/apache/kafka/connect/header/Headers.size:()I` (InterfaceMethodref), `M org/apache/kafka/connect/sink/SinkTask.context:Lorg/apache/kafka/connect/sink/SinkTaskContext;` (Fieldref), `M org/apache/kafka/common/config/AbstractConfig.getBoolean:(Ljava/lang/String;)Ljava/lang/Boolean;` (inherited through a connector class).

Green:

```text
testUnit --tests '*ConnectApiReferencesUnitTest' --rerun
Test result: SUCCESS
Tests summary: 3 tests, 3 succeeded, 0 failed, 0 skipped
testUnit --rerun
Test result: SUCCESS
Tests summary: 844 tests, 842 succeeded, 0 failed, 2 skipped
```

Spotless (per-file recipe, detached worktree `/tmp/gsd25/wt-fmt` at HEAD with the new class copied in, `spotlessApply` rewrote 47 baseline files, which shows it ran): the new class is byte-identical after `spotlessApply` (`cmp` silent). The worktree was removed and pruned.

Timestamp: 2026-10-21T16:27:52Z

Verdict: pass

### E-14: verifyConfluentArchive positive run and negative controls (CI-20, D-06..D-08)

Revision 1 shape: the archive rules exist once, in `ConfluentArchiveCheck` (JDK only, test source set, so it never ships). `verifyConfluentArchive` runs it as a `JavaExec` and `createConfluentArchive` is `finalizedBy` it; `ConfluentArchiveCheckUnitTest` (Docker-free, in `testUnit`) exercises the same class on synthetic archives.

#### Archive-check unit test (red, then green)

Red run, against an accept-all `check()` that counts the inner entries and records no violation (`run` complete):

```text
$ env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*ConfluentArchiveCheckUnitTest' --rerun   # archive-check-red.{log,rc,xml}
exit code: 1
red: 6 tests, 5 failed (the five reject tests), 1 passed (the accept test)
PASS CI-20: the archive check accepts one lib jar holding the connector and client classes and reports its inner entry count
FAIL CI-20: the archive check rejects a jar nested inside the inner jar
     expected: <[nested jar: dependency.jar]> but was: <[]>
     expected: <[nested jar: lib/dependency.jar]> but was: <[]>
FAIL CI-20: the archive check rejects Kafka Connect API and kafka-clients entries under org/apache/kafka/
     expected: <[Kafka API class: org/apache/kafka/connect/sink/SinkTask.class]> but was: <[]>
     expected: <[Kafka API class: org/apache/kafka/common/config/ConfigDef.class]> but was: <[]>
FAIL CI-20: the archive check rejects an archive that does not hold exactly one lib jar
     expected: <[expected exactly one jar under lib/ in the archive, found 0, no inner jar entries read from <tmp>/no-jar.zip, missing class: ...ClickHouseSinkConnector.class, missing class: ...Client.class]> but was: <[]>
     expected: <[expected exactly one jar under lib/ in the archive, found 2]> but was: <[]>
FAIL CI-20: the archive check rejects an inner jar that is empty or lacks the connector or client class
     expected: <[no inner jar entries read from <tmp>/empty.zip, missing class: ...ClickHouseSinkConnector.class, missing class: ...Client.class]> but was: <[]>
     expected: <[missing class: com/clickhouse/client/api/Client.class]> but was: <[]>
     expected: <[missing class: com/clickhouse/kafka/connect/ClickHouseSinkConnector.class]> but was: <[]>
FAIL CI-20: a failing archive check exits 1 with the violation and entry counts and lists at most 20 violations
     expected: <1> but was: <0>
     expected: <> but was: <verifyConfluentArchive: 27 inner entries, no nested jar, no Kafka API class>
     header plus exactly 20 listed violations ==> expected: <21> but was: <0>
```

Note: the very first red attempt used a stub that did not count the inner entries, so the accept test also failed (6 of 6, `expected: <3> but was: <0>`); that was a defect of my stub, not of the prediction. The stub was corrected to count entries as the plan describes and the red run above is the second one. No fixture was changed between the two runs.

Green, with the rules implemented (violation order: shape line, per inner entry `nested jar:` and `Kafka API class:`, empty floor, missing classes):

```text
$ ... testUnit --tests '*ConfluentArchiveCheckUnitTest' --rerun   # archive-check-green.{log,xml}
Tests summary: 6 tests, 6 succeeded, 0 failed, 0 skipped
```

#### Task, step and positive run

- `build.gradle.kts`: `finalizedBy("verifyConfluentArchive")` inside the `createConfluentArchive` block; new `tasks.register<JavaExec>("verifyConfluentArchive")` (group `verification`, archive provider taken at configuration time, `classpath = sourceSets["test"].output.classesDirs`, `mainClass.set("com.clickhouse.kafka.connect.ci.ConfluentArchiveCheck")`, a `CommandLineArgumentProvider` for the path). 14 lines added, no import, no zip-reading Kotlin.
- `.github/workflows/tests.yaml`: step `Build and verify Confluent archive` (`run: ./gradlew createConfluentArchive`) between `Compile integration-test sources` and `Run tests`, plus a three-line header comment sentence. No `uses:`, `if:`, `continue-on-error`, permission or upload added.
- Spotless (per-file recipe, detached HEAD worktree, `spotlessApply` rewrote 47 baseline files, which shows it ran): both new classes byte-identical after it (`cmp` silent).
- Commit: `18a13377` (`build(25-06): verify the Confluent archive whenever it is built...`).

```text
$ env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # verify-pos.log, exit 0
> Task :verifyConfluentArchive
verifyConfluentArchive: 7798 inner entries, no nested jar, no Kafka API class
BUILD SUCCESSFUL
$ ... testUnit --rerun --tests '*TestsWorkflowPinGuardUnitTest'   # pin-after-t1.log
Tests summary: 29 tests, 29 succeeded, 0 failed, 0 skipped
```

#### Negative controls (detached HEAD worktree `/tmp/gsd25/wt-neg`, removed afterwards)

```text
nested: confluentJar block replaced with the one from v1.7.0_gg3 (from(clickhouseDependencies, sourceSets.main.get().output), no Shadow 9 repair)
$ env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # verify-neg-nested.{log,rc}
exit code: 1
Confluent archive check failed (26 violations of 158 entries):
nested jar: client-v2-0.9.6.jar
nested jar: clickhouse-http-client-0.9.6.jar
nested jar: httpclient5-5.6.5.jar
nested jar: clickhouse-client-0.9.6.jar
(20 "nested jar:" lines listed, the cap)

kafka: clickhouseDependencies(libs.kafka.connect.api) added to the dependencies block
$ env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # verify-neg-kafka.{log,rc}
exit code: 1
Confluent archive check failed (4560 violations of 12930 entries):
Kafka API class: org/apache/kafka/connect/components/ConnectPlugin.class
Kafka API class: org/apache/kafka/connect/components/Versioned.class
Kafka API class: org/apache/kafka/connect/connector/ConnectRecord.class
```

The nested control reports 26 violations where the planning-time prototype reported 25; only the first 20 violations are listed (the cap), so the split of the 26 into nested jars and any other class is not decomposed here; the `nested jar:` lines and the non-zero exit are the contract. A first attempt at the Kafka control failed to edit the file (BSD `sed` error) and so built the unmodified tree with exit 0; that run was discarded and redone with a Python edit, which is the run above.

Timestamp: 2026-10-21T16:38:15Z

Verdict: pass

### E-15: CI-20 pin-guard red run (D-09)

Refactor first, without behaviour change: `stepBlock` and `gradlewStepProblem(lines, stepName, task)` are built from `Pattern.quote(stepName)` and `Pattern.quote(task)` in the shapes of the former `FORK_STEP` and `GRADLEW_REPORT` (both dropped); `forkCountStepProblem` delegates to it and then to `nprocProblem` with byte-identical messages. `archiveStepProblem` and `archiveTaskProblem` were accept-all stubs for this run. Four CI-20 tests were added with the exact display names of the plan; the constant `ARCHIVE_CHECK_MAIN` is `ConfluentArchiveCheck.class.getName()`.

```text
$ env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun   # ci20-red.{log,rc,xml}
exit code: 1
Tests summary: 33 tests, 32 succeeded, 1 failed, 0 skipped
failed: fixture: Confluent archive rules reject a missing or renamed step, a renamed task, an echoed call, continue-on-error, another main class and a dropped or misplaced finalizedBy (CI-20)
```

The two real-file CI-20 tests and the accept-fixture test passed against the stubs (the stubs accept everything), and all 29 pre-existing tests passed after the refactor. The reject test failed once per reject fixture (each message is "must reject '<fixture>' but the rule accepted it"):

```text
 1 missing step:      steps: / - name: Run tests / run: ./gradlew test
 2 renamed step:      steps: / - name: Build Confluent archive / run: ./gradlew createConfluentArchive
 3 renamed task:      steps: / - name: Build and verify Confluent archive / run: ./gradlew confluentJar
 4 echoed call:       steps: / - name: Build and verify Confluent archive / run: | / echo "./gradlew createConfluentArchive"
 5 continue-on-error: steps: / - name: Build and verify Confluent archive / run: ./gradlew createConfluentArchive / continue-on-error: true
 6 missing register:  tasks.register<Zip>("createConfluentArchive") { ... finalizedBy("verifyConfluentArchive") } alone (no verifyConfluentArchive register)
 7 renamed register:  the same pair with tasks.register<JavaExec>("verifyConfluentArchives")
 8 other main class:  the same pair with mainClass.set("com.clickhouse.kafka.connect.ci.OtherCheck")
 9 dropped finalizedBy: createConfluentArchive block without the finalizedBy line
10 commented finalizedBy: the line replaced by // finalizedBy("verifyConfluentArchive")
11 misplaced finalizedBy: finalizedBy inside prepareConfluentArchive, not inside createConfluentArchive
12 missing createConfluentArchive block: the verifyConfluentArchive block alone
```

Fixture 12 is one more than the plan's list of eleven; it covers the plan's "a missing createConfluentArchive block gives its own not-found message" and was seen red like the rest. The predicted red set (33 tests, exactly the reject-fixture test failing, one failure per reject fixture) is matched.

Timestamp: 2026-10-21T16:40:58Z

Verdict: pass

### E-16: CI-20 pin-guard green run, per-file spotless and full testUnit

Rules implemented. `archiveStepProblem` = `gradlewStepProblem(lines, ARCHIVE_STEP, ARCHIVE_TASK)`, then any `continue-on-error:` line in the step block whose value is not `false` is a problem. `archiveTaskProblem`: the `verifyConfluentArchive` JavaExec register must exist, its block (to the first line equal to `}`) must hold `mainClass.set("<ConfluentArchiveCheck.class.getName()>")`, and the `createConfluentArchive` block must hold `finalizedBy("verifyConfluentArchive")`; each failure has its own message.

A first green run failed one of my accept fixtures (`continue-on-error: false` was rejected): the pattern `continue-on-error:\s*(?!false\b).*` let `\s*` backtrack to zero characters so the lookahead saw the space. The pattern was changed to the possessive `\s*+`; the fixture was not touched. This is the only rule defect met in the task.

```text
$ ... testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun   # ci20-green.{log,xml}
Tests summary: 33 tests, 33 succeeded, 0 failed, 0 skipped
$ ... testUnit --rerun   # testunit-ci20.log
Test result: SUCCESS
Tests summary: 854 tests, 852 succeeded, 0 failed, 2 skipped
```

Spotless (per-file recipe, detached HEAD worktree, `spotlessApply` rewrote 47 baseline files, which shows it ran): the class is byte-identical after it (`cmp` silent). The worktree was removed.

Timestamp: 2026-10-21T16:41:09Z

Verdict: pass

### E-17: DEBT-14 pin-guard red run

Six DEBT-14 tests were added with the exact display names of the plan and no rule was touched. The sixth, "dependabot rules still reject mismatched quotes, an empty list and a list at the entry's indent", is the must-still-reject control and passes against the old rules.

```text
$ env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun   # debt14-red.{log,rc,xml}
exit code: 1
Tests summary: 39 tests, 34 succeeded, 5 failed, 0 skipped
failed (exactly the five predicted):
  fixture: runs-on rule accepts a pinned runner whose trailing comment names the floating label (DEBT-14, 21-REVIEW WR-01)
      must accept 'runs-on: ubuntu-24.04 # was ubuntu-latest' but the rule said: The floating runner label must not appear outside comments: 'runs-on: ubuntu-24.04 # was ubuntu-latest'
  fixture: gradle-step rules reject a no-op, guarded or commented-out gradle call (DEBT-14, 21-REVIEW WR-03)
      must reject '... run: | / : ./gradlew reportTestForks' but the rule accepted it
      must reject '... run: | / false && ./gradlew reportTestForks' but the rule accepted it
      must reject '... run: | / x=1 # ./gradlew reportTestForks' but the rule accepted it
      must reject '... run: | / # ./gradlew reportTestForks' but the rule accepted it
      must reject '... Build and verify Confluent archive / run: : ./gradlew createConfluentArchive' but the rule accepted it
      must reject '... Build and verify Confluent archive / run: true or-else ./gradlew createConfluentArchive' but the rule accepted it
  fixture: reportTestForks task rule rejects a task that declares the forks provider but prints something else (DEBT-14, 21-REVIEW WR-03)
      must reject 'tasks.register("reportTestForks") { ... val forks = tasks.named<Test>("test").map { it.maxParallelForks } / doLast { / println(6) ...' but the rule accepted it
      must reject '... println(other.get()) ...' but the rule accepted it
  fixture: dependabot rules accept single quotes, a trailing comment, unquoted values and list items at the key's indent (DEBT-14, 21-REVIEW IN-01)
      8 fixture failures: 'github-actions' single-quoted (ecosystem rule and exclude rule), github-actions unquoted (both), "github-actions" with a trailing comment (both), 'gradle' single-quoted (ecosystem rule), and the exclude-paths list at the key's indent (exclude rule); the reversed upstream order passed
  fixture list: the upstream workflow fixtures are the production constant (DEBT-14, 21-REVIEW IN-02)
      the accept fixtures must be built from the rule's own UPSTREAM_WORKFLOWS constant
passed (control): fixture: dependabot rules still reject mismatched quotes, an empty list and a list at the entry's indent (DEBT-14, 21-REVIEW IN-01)
```

(The true-or-else fixture is `run: true`, the shell OR operator and the call; the operator is spelled out here only to keep the Markdown table of E-18 parseable.) The predicted red set (39 tests, those five failing, the control passing) is matched.

Timestamp: 2026-10-21T16:44:36Z

Verdict: pass

### E-18: DEBT-14 green run, spotless, testUnit and accept-set changes

Rules changed: WR-01 `runsOnProblem` removes a whitespace-preceded trailing comment before it looks for the floating label; WR-03 `gradlewStepProblem` counts a line as the call only at command position after comment stripping (an optional `run:` key and an optional `name=\"$(` prefix, then `./gradlew`, and the task as a whole word); WR-03 `forkTaskProblem` captures the `val` that reads the test task's `maxParallelForks` and requires a `print` or `println` of its `.get()`; IN-01 the `github-actions` and `gradle` entries match single, double or no quotes (the same on both sides) with an optional trailing comment, and an `exclude-paths` list may start at its key's indent; IN-02 `UPSTREAM = UPSTREAM_WORKFLOWS`. The two "Missing"/"No entry" messages now say "a package-ecosystem: github-actions entry" instead of the double-quoted spelling. The class Javadoc names T-14-16, CI-11 to CI-13, CI-15, CI-18, CI-19, CI-20, DEBT-13 and DEBT-14.

```text
$ ... testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun   # debt14-green.{log,xml}
Tests summary: 39 tests, 39 succeeded, 0 failed, 0 skipped
$ ... testUnit --rerun   # testunit-debt14.log
Test result: SUCCESS
Tests summary: 860 tests, 858 succeeded, 0 failed, 2 skipped
```

Spotless (per-file recipe, detached HEAD worktree, `spotlessApply` rewrote 47 baseline files, which shows it ran): the class is byte-identical after it (`cmp` silent). The worktree was removed.

#### Accept-set changes (DEBT-14)

AGENTS.md's rule "only add new tests, never delete/weaken existing ones" and DEBT-14 (21-REVIEW WR-01, WR-03, IN-01) are the basis for every change below: no pre-existing `@Test`, `@DisplayName` or assertion changed (33 zero-argument test methods before this commit, all still present, six added), and the only widened accept sets are the rows marked `rejected` to `accepted`.

| Input | Rule | Before (E-17) | After (E-18) | Basis |
|-------|------|---------------|--------------|-------|
| `runs-on: ubuntu-24.04 # was ubuntu-latest` | runsOnProblem | rejected | accepted | DEBT-14, 21-REVIEW WR-01: the label is inside a comment |
| `- package-ecosystem: 'github-actions'` | dependabotEcosystemProblem, dependabotExcludeProblem | rejected | accepted | DEBT-14, 21-REVIEW IN-01: valid YAML spelling |
| `- package-ecosystem: github-actions` | dependabotEcosystemProblem, dependabotExcludeProblem | rejected | accepted | DEBT-14, 21-REVIEW IN-01: valid YAML spelling |
| `- package-ecosystem: "github-actions" # keeps pins current` | dependabotEcosystemProblem, dependabotExcludeProblem | rejected | accepted | DEBT-14, 21-REVIEW IN-01: trailing comment |
| `- package-ecosystem: 'gradle'` | dependabotEcosystemProblem | rejected | accepted | DEBT-14, 21-REVIEW IN-01: same pattern for the gradle entry |
| exclude-paths items at the key's indent (`    exclude-paths:` then `    - "..."`) | dependabotExcludeProblem | rejected | accepted | DEBT-14, 21-REVIEW IN-01: a sequence may start at its key's indent |
| `: ./gradlew reportTestForks` | forkCountStepProblem | accepted | rejected | DEBT-14, 21-REVIEW WR-03: a no-op is not the call |
| `false && ./gradlew reportTestForks` | forkCountStepProblem | accepted | rejected | DEBT-14, 21-REVIEW WR-03: a guarded call may never run |
| `x=1 # ./gradlew reportTestForks` | forkCountStepProblem | accepted | rejected | DEBT-14, 21-REVIEW WR-03: the call is in a comment |
| `# ./gradlew reportTestForks` in the run block | forkCountStepProblem | accepted | rejected | DEBT-14, 21-REVIEW WR-03: a commented-out call |
| `run: : ./gradlew createConfluentArchive` | archiveStepProblem | accepted | rejected | DEBT-14, 21-REVIEW WR-03: a no-op is not the call |
| `run: true` then the shell OR operator then `./gradlew createConfluentArchive` | archiveStepProblem | accepted | rejected | DEBT-14, 21-REVIEW WR-03: a guarded call |
| provider declared, `println(6)` printed | forkTaskProblem | accepted | rejected | DEBT-14, 21-REVIEW WR-03: the task must print the provider it reads |
| provider declared, `println(other.get())` printed | forkTaskProblem | accepted | rejected | DEBT-14, 21-REVIEW WR-03: the task must print the provider it reads |
| `runs-on: ubuntu-latest # x` | runsOnProblem | rejected | rejected | must-still-reject control: the floating runner is live code |
| `runs-on: ubuntu-24.04#x` | runsOnProblem | rejected | rejected | must-still-reject control: no whitespace before `#`, so a wrong runner |
| `runs-on: ubuntu-24.04` plus `- run: echo ubuntu-latest` | runsOnProblem | rejected | rejected | must-still-reject control: the label outside a comment |
| `- package-ecosystem: "github-actions'` (mismatched quotes) | dependabotEcosystemProblem, dependabotExcludeProblem | rejected | rejected | must-still-reject control, DEBT-14 IN-01 |
| an empty exclude-paths list | dependabotExcludeProblem | rejected | rejected | must-still-reject control, DEBT-14 IN-01 |
| exclude-paths items at the entry's indent | dependabotExcludeProblem | rejected | rejected | must-still-reject control, DEBT-14 IN-01 |
| exclude-paths key deeper than its items (a line below the key's indent ends the list) | dependabotExcludeProblem | rejected | rejected | must-still-reject control, DEBT-14 IN-01 |
| `echo "./gradlew reportTestForks"` | forkCountStepProblem | rejected | rejected | must-still-reject control, pre-existing fixture |
| the real fork-count call line (`./gradlew -q --console=plain reportTestForks` inside `forks="$(...)"`) | forkCountStepProblem | accepted | accepted | control: the real shape is still the call |
| `run: ./gradlew createConfluentArchive` | archiveStepProblem | accepted | accepted | control: the real shape is still the call |

Timestamp: 2026-10-21T16:45:03Z

Verdict: pass

### E-19: Step-4 suite gate on the merge-1 tree (DEPS-02)

Commands as run (repository root, branch `sync/v1.7.0`, HEAD `cff85486`, tree `3fb5ecc0dcc655289183776070e61786ef716644`; the only uncommitted tracked files were the orchestrator's `.planning/state.json` and the merge-1 `gradlew.bat`, never staged):

```bash
git status --porcelain --untracked-files=no -- . ':(exclude)gradlew.bat' ':(exclude).planning/state.json'   # empty
docker image inspect clickhouse/clickhouse-server:latest --format '{{.Id}} {{.Created}}'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileIntegrationTestJava createConfluentArchive > /tmp/gsd25/exec/step4-build.log 2>&1
rm -rf build/test-results/test
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd25/exec/suite-step4.log 2>&1
cp -R build/test-results/test /tmp/gsd25/exec/suite-step4
/tmp/gsd25/exec/spotless-numstat.sh "$PWD" v1.7.0_gg3 > /tmp/gsd25/exec/spotless-gg3-s4.txt   # SPOTLESS_NUMSTAT_AT recipe
/tmp/gsd25/exec/spotless-numstat.sh "$PWD" HEAD       > /tmp/gsd25/exec/spotless-s4.txt
```

```text
latest_image: sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b 2026-10-14T18:05:21.432177888Z (same id and date as E-05; not re-pulled)
colima: profile vz Running
build: verifyConfluentArchive: 7798 inner entries, no nested jar, no Kafka API class
build: BUILD SUCCESSFUL in 2s (compileIntegrationTestJava and createConfluentArchive)
suite: BUILD SUCCESSFUL in 2m 39s (2026-10-21T16:51:24Z start, exit 0, 22 actionable tasks: 3 executed, 19 up-to-date)
suite: Test result: SUCCESS
suite: Tests summary: 1303 tests, 1285 succeeded, 0 failed, 18 skipped
xml: files 92, tests 1303, failures 0, errors 0, skipped 18, stale files (older than the last non-.planning commit) 0
server_version: 26.9.19.9 (DEDUP_TOKEN_RETRY and JSON_AS_STRING_RESIDUAL each logged it once; exactly one distinct version)
ci.TestsWorkflowPinGuardUnitTest: 39 tests, 0 failures, 0 errors, 0 skipped
ci.ConnectApiReferencesUnitTest: 3 tests, 0 failures, 0 errors, 0 skipped
spotless: spotless-gg3-s4.txt 46 lines, spotless-s4.txt 46 lines, cmp silent (byte-equal, non-empty)
```

The 18 skips, compared as a set of class#method names (7 named methods plus 11 `ClickHouseSinkTaskWithSchemaTest#exactlyOnceStateMismatchTest` cases `[1]`..`[11]`):

```text
ClickHouseSinkTaskTest#clientNameTest()
ClickHouseSinkTaskWithSchemaTest#schemaWithNestedTupleMapArrayAndVariant()
ClickHouseSinkTaskWithSchemaTest#schemaWithTupleOfMapsWithVariantTest()
ConvertToMapOrJsonPerformanceTestJson#testDeepNestedStructPerformance()
ConvertToMapOrJsonPerformanceTestJson#testWideStructPerformance()
InsertFormatPerformanceTest#compareInsertPerformance()
JsonColumnTest#compareAllConfigurations()
ClickHouseSinkTaskWithSchemaTest#[1] .. #[11]   (the 11 exactlyOnceStateMismatchTest cases)
```

Verify command of the plan (Task 1, `STEP4-SUITE-GREEN`): see the run below.

Timestamp: 2026-10-21T16:55:00Z

Verdict: pass

### E-20: Merge 2 rehearsal (SYNC-11)

Commands as run (main tree HEAD `89900043`, branch `sync/v1.7.0`; rehearsal in the throwaway worktree `/tmp/gsd25/wt-m2`, detached at that HEAD):

```bash
git ls-remote upstream main; git rev-parse refs/remotes/upstream/main; git ls-remote --tags upstream
git merge-tree --write-tree --name-only HEAD a6f1df839c079180bc7c343b5823f205e204dafc > /tmp/gsd25/exec/m2-merge-tree.txt   # exit 1
git worktree add --detach /tmp/gsd25/wt-m2 HEAD
git -C /tmp/gsd25/wt-m2 merge --no-commit a6f1df839c079180bc7c343b5823f205e204dafc
# resolve .gitignore (union) and gradle/libs.versions.toml ([versions] hunk), git add both, git commit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava compileIntegrationTestJava   # in the worktree
# record commit in the worktree, then the unedited tail gate from the main tree:
PRE_TAG=$(git rev-parse HEAD) UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt \
  bash /tmp/gsd25/wt-m2/.planning/scripts/verify-merge-invariants.sh a9f9e8ff6275a474c09b876a71195efa30bc76be HEAD
```

```text
ls_remote_upstream_main: a6f1df839c079180bc7c343b5823f205e204dafc refs/heads/main
refs_remotes_upstream_main: a6f1df839c079180bc7c343b5823f205e204dafc
upstream_tags_newer_than_v1.7.0: none (newest tag refs/tags/v1.7.0 = 379b8d018c090ba5279c2a9f86dbfe4934a19329)
merge_tree_tree: 3360f9dcb81cdaf896bace85bd909aeae9e1d171 (exit 1)
merge_tree_conflicted: .gitignore, gradle/libs.versions.toml   (Auto-merging build.gradle.kts, no marker)
rehearsal_merge_sha: a9f9e8ff6275a474c09b876a71195efa30bc76be
rehearsal_merge_parents: 899000433021cd3e4ad5119febafd5359217fb22 a6f1df839c079180bc7c343b5823f205e204dafc
rehearsal_merge_changed_vs_first_parent: .gitignore, build.gradle.kts, gradle/libs.versions.toml (no added file)
rehearsal_compile: compileJava compileIntegrationTestJava BUILD SUCCESSFUL in 9s (15 actionable tasks: 15 executed)
rehearsal_records_sha: 4fabac3f16674fc03ec27de1c829de9be17f833b
rehearsal_gate: invariants: provenance_paths=3 anchor_commits=2 anchor_rows=71 marker_commits=14 anchor_exempt=0 result=PASS
rehearsal_gate_stderr_bytes: 0
```

The resolved `[versions]` block (identical to the resolution table in `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` Section 1a): `clickHouseDriver = "0.9.6"`, `kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)`, `gson = "2.14.0"`, `jackson = "2.22.4"`, `jacksonAnnotations = "2.22"`, `httpclient = "5.6.5"`, `lombok = "1.18.48"`, `lz4Java = "1.12.0"`, `guava = "33.7.2-jre"`; no `httpclientTest` line, no `httpclient5-test` library row.

Catalog diff, resolved file against the first parent (the fork): the `httpclientTest` line removed, the `guava` version line added, the `httpclient5-test` library row replaced by the `guava` library row (4 changed lines). The resolved catalog against the second parent (upstream) differs only in fork values the plan keeps:

```text
-kafka = "2.7.0"
-gson = "2.13.1"
-jackson = "2.22.3"
+kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)
+gson = "2.14.0"
+jackson = "2.22.4"
-lombok = "1.18.46"
-lz4Java = "1.11.4"
+lombok = "1.18.48"
+lz4Java = "1.12.0"
-commonsLang3 = "3.20.0"
-slf4j = "2.0.18"
+commonsLang3 = "3.21.0"
+slf4j = "2.0.20"
-spotless = "8.10.1"
-shadow = "8.3.5"
+spotless = "8.10.4"
+shadow = "9.6.1"
```

`build.gradle.kts` auto-merged: `implementation(libs.guava)`, `clickhouseDependencies(libs.guava) // temp to override client's with CVE`, and the integration-test `libs.httpclient5.test` became `libs.httpclient5`; the fork's Shadow repair and `verifyConfluentArchive` are untouched. `.gitignore` union against the first parent: a blank line, `# ecliose`, `.classpath`, `.project`, `.settings/` added after the fork's `/bin/`, `.vscode`, `.gsd` lines (upstream's typo kept; each non-blank line once; no conflict marker).

Timestamp: 2026-10-21T16:57:00Z

Verdict: pass

### E-21: Merge 2, the tail gate and the self-test (SYNC-11, D-15)

Commands as run (main tree, branch `sync/v1.7.0`, pre-merge tip `bb88f23e3adbb49417e73c27e561e50d6bf17a5f`):

```bash
git status --porcelain --untracked-files=no -- . ':(exclude)gradlew.bat' ':(exclude).planning/state.json'   # empty
git ls-remote upstream main    # a6f1df839c079180bc7c343b5823f205e204dafc, immediately before the merge
git tag pre-v1.7.0-tail
git merge --no-commit a6f1df839c079180bc7c343b5823f205e204dafc    # CONFLICT in .gitignore and gradle/libs.versions.toml
git checkout "$(cat /tmp/gsd25/exec/m2-rehearsal.sha)" -- .gitignore gradle/libs.versions.toml
git diff --name-only --diff-filter=U    # empty
git commit -F /tmp/gsd25/exec/m2-msg.txt
git checkout "$(cat /tmp/gsd25/exec/m2-records.sha)" -- .planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/MERGE-CHECKLIST-v1.7.0-tail.md   # then the rehearsal SHAs replaced by the real ones
git commit    # record commit R2
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt \
  .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD
.planning/scripts/verify-merge-invariants-selftest.sh
node "$HOME/.claude/gsd-core/bin/gsd-tools.cjs" query state.add-decision --phase 25 --summary-file /tmp/gsd25/exec/d15-summary.txt --rationale-file /tmp/gsd25/exec/d15-rationale.txt
git worktree remove --force /tmp/gsd25/wt-m2; git worktree prune
```

```text
tag: pre-v1.7.0-tail = bb88f23e3adbb49417e73c27e561e50d6bf17a5f (lightweight, local; git ls-remote --tags origin pre-v1.7.0-tail prints nothing)
M2: 38265d26ce00e078b9e8c7d1198ade333a34f3ac
M2 parents: bb88f23e3adbb49417e73c27e561e50d6bf17a5f (= pre-v1.7.0-tail) a6f1df839c079180bc7c343b5823f205e204dafc
git diff --name-only M2^1 M2: .gitignore, build.gradle.kts, gradle/libs.versions.toml
git diff --name-only --diff-filter=A M2^1 M2: (empty)
git diff --quiet {rehearsal a9f9e8ff6275a474c09b876a71195efa30bc76be} M2 -- . ':(exclude).planning/phases/25-dependency-absorption-upstream-mini-sync': equal
R2 (record commit after M2): 76da1b3c2509096ae9fb14eaa837b482dedf12de = .planning/MERGE-CHECKLIST-v1.7.0-tail.md, .planning/scripts/conflicted-paths-v1.7.0-tail.txt (2 files, nothing else)
tail gate: invariants: provenance_paths=3 anchor_commits=2 anchor_rows=71 marker_commits=14 anchor_exempt=0 result=PASS
tail gate stderr bytes: 0 (exit 0)
self-test: exit 0, 16 PASS lines, 0 FAIL lines, last line "selftest: no failures"
gate script and self-test vs pre-v1.7.0-deps: no diff
grep -cF httpclientTest gradle/libs.versions.toml: 0 (baseline 2)
grep -cF 'guava = "33.7.2-jre"' gradle/libs.versions.toml: 1 (baseline 0)
STATE.md: "- [Phase 25]: Standing merge gate for Phases 26-29: PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df83... CONFLICTED_PATHS_FILE=... verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD; the cumulative gate ... 54e63648 HEAD ..." added through state.add-decision (not staged here)
worktree list: only the main tree (the rehearsal worktree removed)
```

The cumulative gate was not run here (D-15: once in plan 25-03, once more at the Phase 29 release gate).

Timestamp: 2026-10-21T17:09:30Z

Verdict: pass

### E-22: The merged tree's archive, dependencyInsight and advisories (SYNC-12, BLD-15)

Commands as run (repository root, branch `sync/v1.7.0`, HEAD `d143a983`, the merge-2 tip; `state.json`, `gradlew.bat` and `milestone.lock` stayed unstaged):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean createConfluentArchive > /tmp/gsd25/exec/archive-m2.log 2>&1
cp build/confluent/clickhouse-kafka-connect-v1.7.0_gg3.zip /tmp/gsd25/exec/archive-m2.zip
python3 -I .planning/scripts/compare-confluent-archives.py /tmp/gsd25/exec/archive-gg3.zip /tmp/gsd25/exec/archive-m2.zip .planning/scripts/confluent-archive-allowlist-25.txt > /tmp/gsd25/exec/cmp-m2-first.txt   # rc 1, before the new rows
python3 -I .planning/scripts/compare-confluent-archives.py ... > /tmp/gsd25/exec/cmp-m2.txt                                                                           # rc 0, after
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --configuration runtimeClasspath --dependency com.google.guava:guava
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --configuration clickhouseDependencies --dependency com.google.guava:guava
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --configuration runtimeClasspath --dependency org.apache.httpcomponents.client5:httpclient5
curl -sS NVD cves/2.0?cveId=CVE-2026-102554 and CVE-2026-64607; OSV /v1/vulns/{GHSA-xxph-c9ww-hj94,GHSA-hjcp-jmpx-g3qm,CVE-2026-102554}; OSV POST /v1/query (Maven, 4 coordinates); GitHub REST releases/tags/v33.7.2 of google/guava
```

```text
guard: verifyConfluentArchive: 7798 inner entries, no nested jar, no Kafka API class
build: BUILD SUCCESSFUL in 7s (20 actionable tasks: 20 executed)
compare before the new rows: totals files-a 7376 files-b 7396; 1543 com/google/common, 24 com/google/j2objc, 4 com/google/thirdparty, 2 guava and 2 j2objc META-INF/maven, 4 META-INF/proguard, 1 META-INF/versions/9/OSGI-INF/MANIFEST.MF and 1 root module-info.class UNALLOWED (exit 1); every other difference already matched a merge-1 row
```

Allow-list rows added (`.planning/scripts/confluent-archive-allowlist-25.txt`; each names the bump read from a VERSION line, or traced by hash for the two files that have no VERSION line):

```text
com/google/common/*                                      guava 33.4.6-jre -> 33.7.2-jre (classes)
com/google/thirdparty/*                                  guava 33.4.6-jre -> 33.7.2-jre (publicsuffix classes bundled in guava)
META-INF/maven/com.google.guava/guava/*                  guava (pom.properties and pom.xml)
META-INF/proguard/base.pro, collect.pro, concurrent.pro  guava's ProGuard rules (3 rows)
META-INF/proguard/io.pro                                 guava (33.7.2 no longer ships it; ONLY-A)
module-info.class                                        guava (see below)
com/google/j2objc/*                                      j2objc-annotations 3.0.0 -> 3.1 (classes)
META-INF/maven/com.google.j2objc/j2objc-annotations/*    j2objc-annotations 3.0.0 -> 3.1 (pom.properties and pom.xml)
META-INF/versions/9/OSGI-INF/MANIFEST.MF                 jspecify 1.0.0 -> 1.0.1 (see below)
(2 merge-1 rows retitled)                                error_prone_annotations 2.38.0 -> 2.50.0 (the VERSION line now reads 2.50.0, not 2.48.0)
```

Two differences the research table did not list, each traced to a jar by sha256 or by listing the Gradle cache copy, and each explained by a version bump:

```text
module-info.class (jar root): A = 182 bytes, sha256 4b671aff0fc1... = jackson-annotations-2.22.jar; B = 997 bytes, sha256 3c0cb029d620... = guava-33.7.2-jre.jar.
  guava 33.4.6-jre kept its descriptor under META-INF/versions/9; 33.7.2-jre ships it at the root, so shadow's duplicate pick moved from jackson-annotations to guava. A root descriptor is ignored on the class path.
META-INF/versions/9/OSGI-INF/MANIFEST.MF (ONLY-A): sha256 a844d62b02a9... is the file inside jspecify-1.0.0.jar; jspecify-1.0.1.jar has no such entry.
  dependencyInsight (runtimeClasspath and clickhouseDependencies): org.jspecify:jspecify:1.0.0 -> 1.0.1, "By conflict resolution: between versions 1.0.1 and 1.0.0", reached through com.google.guava:guava:33.7.2-jre. jspecify has no pom.properties, so no VERSION line; org/jspecify/* is byte-identical in A and B (6 entries each).
```

The compare after the rows (`/tmp/gsd25/exec/cmp-m2.txt`, per-entry lines removed, 0 STALE-ROW):

```text
VERSION at.yawk.lz4:lz4-java 1.12.0 -> 1.12.0
VERSION com.fasterxml.jackson.core:jackson-annotations 2.22 -> 2.22
VERSION com.fasterxml.jackson.core:jackson-core 2.22.3 -> 2.22.4
VERSION com.fasterxml.jackson.core:jackson-databind 2.22.3 -> 2.22.4
VERSION com.google.code.gson:gson 2.13.1 -> 2.14.0
VERSION com.google.errorprone:error_prone_annotations 2.38.0 -> 2.50.0
VERSION com.google.guava:failureaccess 1.0.3 -> 1.0.3
VERSION com.google.guava:guava 33.4.6-jre -> 33.7.2-jre
VERSION com.google.guava:listenablefuture 9999.0-empty-to-avoid-conflict-with-guava -> 9999.0-empty-to-avoid-conflict-with-guava
VERSION com.google.j2objc:j2objc-annotations 3.0.0 -> 3.1
VERSION commons-codec:commons-codec 1.19.0 -> 1.19.0
VERSION commons-io:commons-io 2.20.0 -> 2.20.0
VERSION org.apache.commons:commons-compress 1.28.0 -> 1.28.0
VERSION org.apache.commons:commons-lang3 3.20.0 -> 3.20.0
VERSION org.apache.httpcomponents.client5:httpclient5 5.6.2 -> 5.6.5
VERSION org.apache.httpcomponents.core5:httpcore5 5.4.3 -> 5.4.4
VERSION org.apache.httpcomponents.core5:httpcore5-h2 5.4.3 -> 5.4.4
VERSION org.slf4j:slf4j-api 2.0.7 -> 2.0.7
MANIFEST.MF: identical
totals files-a 7376 files-b 7396 only-a 91 only-b 111 changed 1820 same 5465
nested jars in b: 0
kafka classes in b: 0
RESULT: PASS
```

dependencyInsight excerpts (`/tmp/gsd25/exec/insight-guava-runtime.txt`, `insight-guava-chdeps.txt`, `insight-httpclient5.txt`):

```text
runtimeClasspath:
com.google.guava:guava:33.7.2-jre
   Selection reasons:
      - By conflict resolution: between versions 33.7.2-jre and 33.4.6-jre
com.google.guava:guava:33.7.2-jre
\--- runtimeClasspath
com.google.guava:guava:33.4.6-jre -> 33.7.2-jre
\--- com.clickhouse:client-v2:0.9.6
     \--- runtimeClasspath

clickhouseDependencies:
com.google.guava:guava:33.7.2-jre   (same conflict-resolution reason)
com.google.guava:guava:33.7.2-jre
\--- clickhouseDependencies
com.google.guava:guava:33.4.6-jre -> 33.7.2-jre
\--- com.clickhouse:client-v2:0.9.6
     \--- clickhouseDependencies

httpclient5 (runtimeClasspath):
org.apache.httpcomponents.client5:httpclient5:5.6.5
   Selection reasons:
      - By conflict resolution: between versions 5.6.5 and 5.4.4
org.apache.httpcomponents.client5:httpclient5:5.6.5
\--- runtimeClasspath
org.apache.httpcomponents.client5:httpclient5:5.4.4 -> 5.6.5
+--- com.clickhouse:clickhouse-http-client:0.9.6
\--- com.clickhouse:client-v2:0.9.6
     \--- runtimeClasspath
```

Advisory facts, re-queried 2026-10-21 (NVD from 17:17:16Z, two calls 7 s apart; OSV and GitHub finished by 17:17:45Z; JSON in `/tmp/gsd25/exec/adv-*.json`). Each fact names its source. Where the live answer differs from the research table it is marked LIVE.

```text
CVE-2026-102554 = GHSA-xxph-c9ww-hj94 (guava)
  NVD    (http 200) CNA cve-coordination@google.com; published 2026-10-09T16:17:20.410, last modified 2026-10-09T18:17:01.380; vulnStatus "Awaiting Analysis"; no CPE configuration listed.
  NVD    description: CWE-770, Java object deserialization of CompactHashMap, CompactHashSet or MapMakerInternalMap, "Google Guava versions 4.0 through 33.7.1", Denial of Service via OutOfMemoryError.
  NVD    metrics: CVSS 4.0 8.2 HIGH, CVSS:4.0/AV:N/AC:L/AT:P/PR:N/UI:N/VC:N/VI:N/VA:H/SC:N/SI:N/SA:N/... (source cve-coordination@google.com, type Secondary); SSVC (CISA coordinator): exploitation poc, automatable no, technical impact partial.
  OSV    GET /v1/vulns/GHSA-xxph-c9ww-hj94 -> 404 "Vulnerability not found, but the following aliases were: CVE-2026-102554"   (LIVE: the GHSA id is not a record of its own)
  OSV    GET /v1/vulns/CVE-2026-102554 -> 200; aliases [GHSA-xxph-c9ww-hj94]; published 2026-10-09T15:07:16.130Z; a GIT range (introduced 0, fixed commit b931fe9d6d5cf00bc55714ad3308d086f71850fe); no Maven range; the listed versions are failureaccess tags, not guava versions.
  OSV    POST /v1/query Maven com.google.guava:guava 33.4.6-jre -> {} (no vulnerability); 33.7.2-jre -> {} (no vulnerability).
  Guava release notes (GitHub REST google/guava v33.7.2, published 2026-09-29T14:53:52Z): the changelog names the advisory link GHSA-xxph-c9ww-hj94 and CVE-2026-102554 and says MapMaker, CompactHashMap and CompactHashSet deserialization no longer eagerly allocate as much memory as a payload requests (a0d0c36432).

CVE-2026-64607 = GHSA-hjcp-jmpx-g3qm (httpclient5)
  NVD    (http 200) CNA security@apache.org; published 2026-07-31T11:17:11.710, last modified 2026-08-13T17:17:33.733; vulnStatus "Modified".
  NVD    description: classic I/O HttpClient fails to release the connection on an invalid or unsupported Content-Encoding value; "from 5.0-alpha1 through 5.6.2"; the async model is not affected.
  NVD    CPE configuration: cpe:2.3:a:apache:httpclient versionStartIncluding 5.0.0, versionEndExcluding 5.6.3.
  NVD    metrics: CVSS 3.1 5.3 MEDIUM CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L (source 134c704f-..., type Secondary); SSVC: exploitation none, automatable yes, technical impact partial.
  OSV    GET /v1/vulns/GHSA-hjcp-jmpx-g3qm -> 200; aliases [CVE-2026-64607]; published 2026-07-31T12:30:30Z, modified 2026-09-10T03:50:12Z; ECOSYSTEM range introduced 5.0-alpha1, fixed 5.6.3.
  OSV    POST /v1/query Maven org.apache.httpcomponents.client5:httpclient5 5.6.2 -> [GHSA-hjcp-jmpx-g3qm]; 5.6.5 -> {} (no vulnerability).
```

Differences from the research table: none that change a conclusion. Added by the live read: NVD lists no CPE configuration for the guava record (Awaiting Analysis) while its httpclient record lists `5.0.0` up to excluding `5.6.3`; OSV answers a direct GHSA lookup for the guava advisory with "not found" plus the CVE alias, and the OSV guava record carries no Maven range, which is why the Maven query for `33.4.6-jre` returns nothing. No exposure assessment is made here, and nothing is concluded about any source other than the ones named.

`docs/FORK-DELTA.md` now holds the guava and httpclient5 rows in "Adopted wholesale" with these labels, and the "Fork dependency pins" and lz4 rows are compared with upstream's tail catalog (`a6f1df83`): upstream `lz4Java` is `1.11.4`, the fork's Jackson `2.22.4` and gson `2.14.0` are past upstream's `2.22.3` and `2.13.1`, and okhttp, org.json and mockito are upstream's own values.

Timestamp: 2026-10-21T17:19:12Z

Verdict: pass

### E-23: DEBT-15 (slf4j-reload4j from the catalog)

Commands as run (branch `sync/v1.7.0`, after commit `0a7a00d9`):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight --configuration runtimeClasspath --dependency org.slf4j:slf4j-reload4j > /tmp/gsd25/exec/insight-reload4j.txt 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd25/exec/testunit-t2.log 2>&1
python3 -I -c '<mirror: for every data row of fork-anchors.txt, count lines of its path containing its pattern and compare with the count column>'
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD
```

```text
commit: 9c9b480bff0cf6971afdcf309d1b2ddafba095e3  build(25-08): move slf4j-reload4j into the version catalog on the shared slf4j key, anchor row moved with it (DEBT-15, D-16)
commit files (git diff --name-only 9c9b480b^ 9c9b480b): .planning/scripts/fork-anchors.txt, build.gradle.kts, docs/FORK-DELTA.md, gradle/libs.versions.toml
catalog: + slf4j-reload4j = { module = "org.slf4j:slf4j-reload4j", version.ref = "slf4j" }   (after the slf4j-simple row; no new [versions] key; grep -c '^slf4j-reload4j *=' = 1)
build.gradle.kts: - implementation("org.slf4j:slf4j-reload4j:2.0.20")  + implementation(libs.slf4j.reload4j)   (grep -cF 'org.slf4j:slf4j-reload4j:' = 0, was 1)
fork-anchors.txt: - build.gradle.kts<TAB>slf4j-reload4j:2.0.20<TAB>1<TAB>BLD-08
                  + gradle/libs.versions.toml<TAB>slf4j-reload4j = { module = "org.slf4j:slf4j-reload4j", version.ref = "slf4j" }<TAB>1<TAB>BLD-08
                  + build.gradle.kts<TAB>implementation(libs.slf4j.reload4j)<TAB>1<TAB>BLD-08
dependencyInsight runtimeClasspath: org.slf4j:slf4j-reload4j:2.0.20, Variant runtime, \--- runtimeClasspath
testUnit --rerun: Test result: SUCCESS, 860 tests, 858 succeeded, 0 failed, 2 skipped, BUILD SUCCESSFUL
anchors mirror: rows 72, bad [] (before the change: 71 rows)
standing tail gate on the new tip: invariants: provenance_paths=3 anchor_commits=7 anchor_rows=72 marker_commits=19 anchor_exempt=0 result=PASS
```

FORK-DELTA's `slf4j-reload4j` row now says the coordinate comes from a fork-only catalog row on the shared `slf4j` key and lists `DEBT-15`.

Timestamp: 2026-10-21T17:20:42Z

Verdict: pass
