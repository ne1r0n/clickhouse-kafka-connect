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
| E-07 | DEPS-02, DEPS-03 (merge 1, cumulative gate, self-test) | pending (plan 25-03) |
| E-08 | DEPS-02 (merged-tree Docker-free checks) | pending (plan 25-03) |
| E-09 | BLD-15 (unrepaired archive) | pending (plan 25-04) |
| E-10 | BLD-15 (repair) | pending (plan 25-04) |
| E-11 | BLD-15 (compare against v1.7.0_gg3) | pending (plan 25-04) |
| E-12 | BLD-16 (worker jars and bless run) | pending (plan 25-05) |
| E-13 | BLD-16 (reference test) | pending (plan 25-05) |
| E-14 | CI-20 (verifyConfluentArchive controls) | pending (plan 25-06) |
| E-15 | CI-20 (pin-guard red run) | pending (plan 25-06) |
| E-16 | CI-20 (pin-guard green run) | pending (plan 25-06) |
| E-17 | DEBT-14 (pin-guard red run) | pending (plan 25-06) |
| E-18 | DEBT-14 (pin-guard green run, accept-set changes) | pending (plan 25-06) |
| E-19 | DEPS-02 (step-4 suite gate) | pending (plan 25-07) |
| E-20 | SYNC-11 (merge 2 rehearsal) | pending (plan 25-07) |
| E-21 | SYNC-11 (merge 2, tail gate, self-test) | pending (plan 25-07) |
| E-22 | SYNC-12, BLD-15 (post-merge archive, dependencyInsight, advisories) | pending (plan 25-08) |
| E-23 | DEBT-15 | pending (plan 25-08) |
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
