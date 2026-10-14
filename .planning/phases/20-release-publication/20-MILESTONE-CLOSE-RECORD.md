# Phase 20: Milestone close record

This file holds everything the `v1.7.0_gg2-hardening` milestone leaves open or still to check once `v1.7.0_gg2` is published. `/gsd-complete-milestone` reads it for `MILESTONES.md` Known gaps and for the Active list in `PROJECT.md`. It was started by plan 20-04 (D-08, D-11, D-12); plan 20-08 fills the three manual-check outcomes.

Only decisions are written here. No record value, host, credential or token appears in it.

## Carried review advisories

The IDs in this section and in the 19-REVIEW IN-04 section are those of `19-REVIEW.md` (report `7d50944b`). An earlier Phase 19 review (report `68355de6`, recorded by `ea20cf0a`) used some of the same IDs for different findings; the next section explains the one that matters. Each advisory below is carried for the reason D-08 gives: no code churn between Phase 19's green gate and the release. Each row in `19-REVIEW-DISPOSITION.md` is `deferred` and points here.

### 19-REVIEW WR-01: The totality guarantee covers only the body of `apply`, not its handler or its caller

- **Reason:** `DeclaredNullability.apply` and `containsTuple` can throw a `NullPointerException` outside the `try` for a null root or a Column with a null type. Neither is reachable today, because `Column.extractColumn` always sets a type and the one caller null-checks the root first. Hardening it means a production change to the declared-nullability pass after its last green gate, so it waits.
- **Re-open trigger:** a new caller of `DeclaredNullability.apply` or `containsTuple`, any path that can build a Column without a type, or the next change to the pass.
- **Source:** 19-REVIEW.md

### 19-REVIEW WR-02: Several WARN-content assertions are vacuous and cannot fail

- **Reason:** The tests that should pin "the fallback WARN names the column" use a column named `c`, which also appears in the fixed text (`Could`), so they pass even if the column were dropped from the line. It is a weakness of four test assertions, not a defect in the shipped behaviour, and fixing it is a test-only change that was deferred to keep the tree frozen after the last green gate.
- **Re-open trigger:** any edit to the fallback WARN text or its arguments, or the next edit of the two test classes that hold the assertions. The fix is a column name that cannot appear in the fixed text, asserted in a delimited form.
- **Source:** 19-REVIEW.md

### 19-REVIEW IN-01: The backslash-n constant is copied into three test classes

- **Reason:** `DeclaredNullability.ESCAPED_NEWLINE` is package-private, so three tests re-declare the same literal. No behaviour is affected.
- **Re-open trigger:** a fourth copy of the literal, or a change of what the server returns for a multi-line type.
- **Source:** 19-REVIEW.md

### 19-REVIEW IN-02: Brittle and indirect test pins

- **Reason:** One test provokes a `NullPointerException` through a Column with a null type, one helper is a one-line pass-through and one constant sits in the middle of a class. They are style and brittleness issues in tests, with no effect on behaviour.
- **Re-open trigger:** `DeclaredNullability.collect` gaining a defensive null check (the NPE-provoking test would then fail although the contract holds), or the next edit of those test classes.
- **Source:** 19-REVIEW.md

### 19-REVIEW IN-03: `normalise` rewrites every backslash-n pair, including ones inside names and enum labels

- **Reason:** A literal backslash-n inside a backtick-quoted element name or an enum label is rewritten like the server's newline between elements, so pairing fails and the root falls back, with the metadata-only WARN, to the nullability `DESCRIBE` reported. It fails open, is rare, and has no test.
- **Re-open trigger:** a report of the fallback WARN for a table whose Tuple element name or enum label contains a backslash-n, or a change in how `ClickHouseFieldDescriptor.fromJsonRow` escapes multi-line types.
- **Source:** 19-REVIEW.md

## Carried obligation fulfilled in Phase 20

### L-38: a single-field Nullable(Tuple) with a null field now fails with a DataException

- **Status:** fulfilled
- **Evidence:** `docs/ROLLOUT-v1.7.0_gg2.md` item 2, which carries the "(ledger L-38)" tag, and the `1.7.0_gg2` section of `CHANGELOG.md` (Important Changes) both state that the `Nullable(Tuple)` limitation is closed and its startup WARN is gone, that rows stored by earlier releases are not repaired, and that a single-field `Nullable(Tuple(a T))` with a null field now fails with a `DataException` that stops the task under `errors.tolerance=none`. Both were written in Phase 20 (plans 20-03 and 20-04) and ship inside the merged tree.
- **Label:** ROADMAP's Phase 20 entry and STATE.md list this item as "WR-02". That is an ID from the earlier Phase 19 review (report `68355de6`, recorded by `ea20cf0a`), which the current `19-REVIEW.md` reused for the vacuous WARN-assertion finding in the previous section. So "19-REVIEW WR-02 deferred" does not concern this item, and this item is not deferred. This entry is not an advisory: it has no reason or trigger to carry.

## 19-REVIEW IN-04

**A Tuple Column that lacks a declared element still "applies".** A shortfall needs a `Table` pairing that drops a declared element, and no `DESCRIBE` shape that does so was found: the matrix, the Docker-free pins and the live test for the `SimpleAggregateFunction` fix pair every element. Logging or failing open on it needs a decision on whether legitimately unpaired elements exist.

- **Owner:** the next milestone, which inherits it through this record.
- **Trigger to re-open:** a clickhouse-data version change, or any `DESCRIBE` shape observed to drop a declared Tuple element.
- **Pin:** `missingDeclaredElementDoesNotShiftPairing` holds the current behaviour until then.

## Manual checks after publication

These three checks are for the user in the GitHub web UI of `ne1r0n/clickhouse-kafka-connect` (D-11). No agent changes repository settings. Plan 20-08 presents each one after the tag is pushed, waits for the user's answer in the session and records it on the `Outcome` line.

### 1. Dependabot version updates: enabled or not

- **Steps:** open the repository on GitHub, then Settings, then Code security (called "Code security and analysis" or "Advanced Security" in some versions of the interface). Find "Dependabot version updates" and read whether it is Enabled or Disabled.
- **Why it matters:** version updates on a fork stay off until someone enables them here, so the `github-actions` entry that keeps the SHA pins current (and the `gradle` entry) does nothing while it is off.

Outcome: enabled (recorded 2026-10-14, given directly to the orchestrator)

### 2. The default branch

- **Steps:** Settings, then General. Under "Default branch" read the branch name. It was `main` at Phase 17 (a 2025 upstream mirror), not `custom_build`. Record the user's intent: keep `main`, switch to `custom_build`, or undecided.
- **Why it matters:** Dependabot reads `.github/dependabot.yml` from the default branch only, and scheduled workflows also run from the default branch, so on `main` neither sees the fork's files.

Outcome: changed to custom_build (recorded 2026-10-14, given directly to the orchestrator)

- **Consequence for the next milestone:** with `custom_build` the default branch, Dependabot reads the fork's `.github/dependabot.yml` and scheduled workflows run from `custom_build`, so the Phase 17 F1 concern ("neither sees the fork's files on `main`") is closed. The one-branch-per-SHA CI rule and the branch-triggered `tests.yaml` are unaffected. The user made this change themselves in the web UI; no agent touched a setting.

### 3. `dependabot.yml`'s `exclude-paths` key did not invalidate the configuration

- **Steps:** Insights, then Dependency graph, then the Dependabot tab. Check that no configuration error is shown and that the `gradle` entry still shows a recent "Last checked" time. This can only be observed once the default branch carries the file, so if check 2 says `main` stays the default and `main` has no `dependabot.yml` with the `github-actions` entry, record "not observable" and why.
- **Why it matters:** the `github-actions` entry's `exclude-paths` support was confirmed from Dependabot's source and never observed in a run (17-REVIEW WR-01). If Dependabot rejects the key, the whole file could be invalid and stall the `gradle` entry as well.

Outcome: no errors, I see only [screenshot: Insights > Dependency graph > Dependabot showing the actions entry "Running version update job now" and Recent jobs "Version update 1627650108, Running now"] (recorded 2026-10-14, given directly to the orchestrator)

- **Reading of the screenshot (orchestrator's annotation, not the user's words):** check 2's switch to `custom_build` made this check observable at once (assumption A18's "not observable yet" path did not apply). The Dependabot tab shows no configuration error, so `exclude-paths` did not invalidate the file. At the moment of the check the first version-update job on the fork's config was still running, so the `gradle` entry's "Last checked" time was not yet visible — only the `actions` entry was listed. Carried forward: next milestone, glance at the same tab to confirm the `gradle` entry appears with a recent "Last checked" time once job 1627650108 and its successors finish (closes 17-REVIEW WR-01 fully).

## Milestone audit

Run `/gsd-audit-milestone` after Phase 20 verification and before `/gsd-complete-milestone` (D-12). It replaces the stale, untracked `.planning/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md` snapshot of 2026-10-10, which is not committed, so that `v1.7.0_gg2-hardening` closes clean instead of as a third override closeout.

## Housekeeping

- Revoke the read-only GitHub token (Actions: Read, single repository) that is stored in the macOS Keychain item `gsd17-gh-actions-read`, on GitHub under Settings, Developer settings, Personal access tokens. Then delete the Keychain item with `security delete-generic-password -s gsd17-gh-actions-read`. It was made for the Phase 17 job-log evidence and is not needed once Phase 20 has read its last run.
- Backlog 999.3 stays open: a rethrown write-failure message can still carry a record value into the task status, the dead letter queue headers and two `Error inserting records` log lines. It is recorded in FORK-DELTA Known costs item 6.
