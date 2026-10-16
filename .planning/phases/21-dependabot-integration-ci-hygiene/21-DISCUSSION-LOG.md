# Phase 21: Dependabot Integration & CI Hygiene - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-16 to 2026-10-17
**Phase:** 21-dependabot-integration-ci-hygiene
**Areas discussed:** Anchors past 6473e24f, Pin-guard red proof, Pin-guard scope, OPS timing & evidence

Facts presented before the area selection:
- The public API showed `integrationTests.yaml` already `disabled_manually` since 2026-10-16 09:38 UTC.
- Changing `PRE_TAG` alone cannot exclude `6473e24f` from the anchors range. Invariant 0 requires `PRE_TAG` to be an ancestor of `54e63648`'s first parent.

---

## Anchors past 6473e24f

### How should the anchors check in Phases 22–24 get past 6473e24f?

| Option | Description | Selected |
|--------|-------------|----------|
| SHA-pinned exemption (Recommended) | A recorded exemption naming exactly commit `6473e24f` and row BLD-08, with the reason. The full range is still checked, the gate command is unchanged, and any other violation still fails. A few lines of script. | ✓ |
| Later start for the anchors range | A new variable such as `ANCHORS_FROM` starts invariant 3 at the DEPS-01 merge. Published history is no longer re-checked. | |
| Separate anchors script | Leave `verify-merge-invariants.sh` untouched and add an anchors-only script. Two scripts to keep in sync. | |

**User's choice:** SHA-pinned exemption

### Where should the BLD-08 row and the FORK-DELTA version rows move?

| Option | Description | Selected |
|--------|-------------|----------|
| Inside the merge commit (Recommended) | `git merge --no-commit`, edit the rows, commit. Only `6473e24f` needs exempting. | ✓ |
| First commit after the merge | The merge stays mechanical, but the merge commit also fails and needs a second exemption. | |

**User's choice:** Inside the merge commit

### One-off or reusable exemption?

| Option | Description | Selected |
|--------|-------------|----------|
| Reusable list, one row now (Recommended) | A file beside `fork-anchors.txt` with commit SHA, row and reason per exemption. Phase 24 adds a row instead of editing the script. | ✓ |
| Hard-coded one-off | Written into the script. A future anchored bump needs another script edit. | |

**User's choice:** Reusable list, one row now
**Notes:** While writing CONTEXT, the scout found that `BLD-08` backs two anchor rows (`slf4j-reload4j` and `kafka = "4.1.1"`). The exemption therefore keys on the exact row, not the backs-id (CONTEXT D-02).

### How is the exemption proven narrow?

| Option | Description | Selected |
|--------|-------------|----------|
| New self-test cases (Recommended) | Fail-first cases in `verify-merge-invariants-selftest.sh` in the ST1–ST4 style, which stay and run with each gate. | ✓ |
| One-off red/green runs in evidence | Recorded once and not committed. Nothing guards the exemption later. | |

**User's choice:** New self-test cases

---

## Pin-guard red proof

### Committed fixture tests or one-off?

| Option | Description | Selected |
|--------|-------------|----------|
| Committed fixture tests (Recommended) | Each rule runs on accept and reject fixtures in `testUnit`. The only standing guard for the IN-02 fixes. | ✓ |
| One-off fixture runs in evidence | The Phase 17 D-09 habit. The IN-02 fixes would have no standing guard. | |

**User's choice:** Committed fixture tests

### How should the rule logic be shared?

| Option | Description | Selected |
|--------|-------------|----------|
| Extract rules, keep tests (Recommended) | Static line-list rule methods in the same class. The existing five tests keep their names, DisplayNames and messages. No duplicated regex. | ✓ |
| Separate fixture class | Leaves the existing class untouched but duplicates every regex (a DRY violation, and the copies can drift). | |
| You decide | Leave it to planning, with no weakening and no duplication. | |

**User's choice:** Extract rules, keep tests

### How is red then green shown?

| Option | Description | Selected |
|--------|-------------|----------|
| Red run in evidence, one green commit (Recommended) | Run the fixtures against the old regexes and record the failures, then commit the tests and the fix together. | ✓ |
| Red commit, then fix commit | The red state is visible in history, but `testUnit` fails at one commit. | |

**User's choice:** Red run in evidence, one green commit
**Notes:** The fixture format was left to Claude, with a lean toward inline Java text blocks.

---

## Pin-guard scope

### How tight is CI-18's check on `tests.yaml`?

| Option | Description | Selected |
|--------|-------------|----------|
| Gradle call in that step + no nproc formula (Recommended) | Require the `./gradlew … reportTestForks` call in the "Report test fork count" step, and reject `nproc` except on the one message line. | ✓ |
| Gradle call in that step only | No rule on `nproc`. | |
| Word anywhere in the file | The `::error::` line alone would satisfy it. | |

**User's choice:** Gradle call in that step + no nproc formula

### How does CI-18 check the task exists?

| Option | Description | Selected |
|--------|-------------|----------|
| Register line + reads test forks (Recommended) | A text check for `tasks.register("reportTestForks")` and for reading the test task's `maxParallelForks`. Docker-free, no new dependency. | ✓ |
| Register line only | Misses a task that prints a constant. | |
| Run Gradle to confirm the task | Strongest, but needs TestKit (a new dependency) and slows `testUnit`. | |

**User's choice:** Register line + reads test forks

### Add the WR-01 `exclude-paths` guard half here?

| Option | Description | Selected |
|--------|-------------|----------|
| Add it here (Recommended) | Assert the four upstream workflows appear under `exclude-paths` and that each listed path exists. With OPS-02, WR-01 ends `fixed`. | ✓ |
| Leave it; close with a reason | DEBT-12 records why. Entries stay unguarded. | |

**User's choice:** Add it here
**Notes:** A constraint was recorded before moving on: the narrowed write-scope rule must still reject `contents: write`, `permissions: write-all` and a job-level write block, each as a must-reject fixture.

---

## OPS timing & evidence

Facts presented:
- `integrationTests.yaml` has been `disabled_manually` since 2026-10-16 09:38:23 UTC. Its scheduled runs started at 09:39 and 09:32 UTC, about 4.5 h after the cron time.
- Dependabot gradle-ecosystem PRs #1–#5 were opened on 2026-10-14 between 21:55 and 21:59 UTC.
- The Keychain item `gsd17-gh-actions-read` is already absent.

### When does OPS-01's window count as passed?

| Option | Description | Selected |
|--------|-------------|----------|
| After 12:00 UTC on 10-17 (Recommended) | An API check at or after 2026-10-17 12:00 UTC, with margin past the observed delay. | ✓ |
| Any time after 05:00 UTC on 10-17 | The literal roadmap wording. A delayed run could still appear after the check. | |
| Two nights | After 2026-10-18 12:00 UTC. Stronger, but a day longer. | |

**User's choice:** After 12:00 UTC on 10-17

### Does Phase 22 wait for OPS-01?

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 22 waits (Recommended) | Phase 21 closes only after the window. Strict sequencing, with a delay of a few hours at most. | ✓ |
| Phase 22 can start | DEPS-01 is the only code gate, and Phase 24 re-checks OPS-01. Two phases would be open at once. | |

**User's choice:** Phase 22 waits

### When and how are the OPS confirmations given?

| Option | Description | Selected |
|--------|-------------|----------|
| Free text at execution start (Recommended) | Orchestrator-inline, with exact web-UI steps. The user's own words are recorded verbatim, and a screenshot is fine. | ✓ |
| At the end of Phase 21 | The same form, asked just before verification. | |

**User's choice:** Free text at execution start

---

## Claude's Discretion

- The exemption file's name, columns and read source. Lean: `RANGE_END`'s tree, TAB-separated, full 40-hex SHA.
- Whether to rehearse the merge and the gate in a throwaway worktree first. Lean: yes.
- The fixture format. Lean: inline Java text blocks.
- The exact regexes and the step-block parsing (line-level, no YAML library).
- The plan split, commit order (within the fixed DEPS-01-first order) and evidence layout.

## Deferred Ideas

- Aligning the catalog's `slf4j = "2.0.18"` with the `slf4j-api` that `slf4j-reload4j:2.0.20` pulls in. That is a dependency bump and needs approval. Research records the resolved versions only.
- PR #4 (client `0.10.0`) is already a Future Requirement.
