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
| E-05 | DEPS-02 (pre-flight) | pending (plan 25-03) |
| E-06 | DEPS-03 (merge 1 rehearsal and exemption rows) | pending (plan 25-03) |
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
