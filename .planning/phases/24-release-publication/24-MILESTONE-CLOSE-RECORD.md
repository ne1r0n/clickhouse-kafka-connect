# Phase 24: Milestone close record

This file holds everything the `v1.7.0_gg3-hardening` milestone leaves open once `v1.7.0_gg3` is published. `/gsd-complete-milestone` reads it for `MILESTONES.md` Known gaps and for the Active list in `PROJECT.md`. It was written by plan 24-05 (D-06, D-11, 20-D-12).

Only decisions are written here. No record value, host, credential or token appears in it.

## Carried review advisories

The 17 findings below come from `21-REVIEW.md`, `22-REVIEW.md` and `23-REVIEW.md`. Each is carried for the reason D-06 gives: Phase 24 is a release phase, and it makes no `src/main` edit and no edit of an existing test. Each row in `21-REVIEW-DISPOSITION.md`, `22-REVIEW-DISPOSITION.md` and `23-REVIEW-DISPOSITION.md` is `deferred` and points here. The two findings of those reviews that Phase 24 did fix are in the next section.

### 21-REVIEW WR-01: runs-on rule rejects a pinned runner whose trailing comment mentions the floating label

- **Reason:** `TestsWorkflowPinGuardUnitTest` scans every non-comment line for the floating runner label, so a pinned `runs-on:` line with a trailing comment that names the label fails the test. It is a false failure of a CI guard, never a missed regression, and it is not reachable today because the real workflow carries no such comment. Fixing it is a test edit, which D-06 forbids in the release phase.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `TestsWorkflowPinGuardUnitTest`, or a trailing comment added to a `runs-on:` line in `tests.yaml`.
- **Source:** 21-REVIEW.md

### 21-REVIEW WR-02: `ktsCode` comment stripping is not string- or line-comment-aware

- **Reason:** The helper removes block comments over the whole text, so a `/*` inside a string or a `//` comment of `build.gradle.kts` could hide the `reportTestForks` registration and make the rule report live code as missing. The file has only balanced real block comments today, so the rule passes. The risk is a false failure after a future edit of the build script.
- **Owner:** the next milestone
- **Re-open trigger:** any `build.gradle.kts` edit that adds a string or a `//` comment containing `/*`, or the next edit of the pin-guard test.
- **Source:** 21-REVIEW.md

### 21-REVIEW WR-03: forkTaskProblem and the fork-count step rule accept text that is not live code

- **Reason:** The fork-count rules are satisfied by substring presence, so a commented-out `maxParallelForks` read or a guarded `./gradlew reportTestForks` call would be accepted. The guard could therefore miss a regression of CI-18. No such edit exists today, and tightening the rules is a test edit that the release phase does not make.
- **Owner:** the next milestone
- **Re-open trigger:** any edit of the `reportTestForks` task, of the fork-count step in `tests.yaml`, or of the pin-guard test.
- **Source:** 21-REVIEW.md

### 21-REVIEW IN-01: dependabot parsing is stricter than YAML and silently depends on one indent style

- **Reason:** The `exclude-paths` reader needs the list indented deeper than its key and the exact double-quoted `package-ecosystem` text. A valid YAML layout in another style would fail as missing. These are false failures only, and the real file uses the style the test reads.
- **Owner:** the next milestone
- **Re-open trigger:** a change of the `dependabot.yml` layout or quoting, or the next edit of the pin-guard test.
- **Source:** 21-REVIEW.md

### 21-REVIEW IN-02: Fixture list duplicates the production constant

- **Reason:** The test's fixture list of the four upstream workflows is built separately from the rule's constant, and the class Javadoc names only part of what the class pins now. If the excluded list changes, the fixture must be edited by hand. No behaviour is affected.
- **Owner:** the next milestone
- **Re-open trigger:** a fifth upstream workflow added to the `exclude-paths` list, or the next edit of the pin-guard test.
- **Source:** 21-REVIEW.md

### 21-REVIEW IN-04: slf4j-reload4j is versioned independently of the slf4j catalog entry

- **Reason:** `slf4j-reload4j` is a literal in `build.gradle.kts` (2.0.20) while the catalog entry for slf4j is 2.0.18, so the binding and the API drift apart. Gradle resolves the API to the higher version, and the plan 21 anchor exemption already covers the literal. FORK-DELTA records the DEPS-01 runtime-classpath-only statement for it.
- **Owner:** the next milestone
- **Re-open trigger:** the next Dependabot bump of the literal, or a slf4j major release that makes the binding and the API diverge.
- **Source:** 21-REVIEW.md

### 22-REVIEW WR-01: The blanket `IOException` pass-through lets Jackson's `JsonMappingException` carry record text unsanitized

- **Reason:** On the opt-in JSON-as-string route (`input_format_binary_read_json_as_string` set to `1` or `true` in `clickhouseSettings`) a Jackson failure is an `IOException` subtype and passes through, so its reference chain can name a Map key. The residual was accepted in 22-UAT test 3 and in 22-SECURITY R-22-02. Phase 24 measured it with `JsonAsStringKeyResidualLiveTest` on server 26.9.19.9 with client V2 (E-03, the D-04 branch 1 result): a Map key reaches the task status, the DLQ stack-trace header and the `Error inserting records` throwable; a Map value, a Struct string or a List element reaches none of them. The rollout note, the CHANGELOG and FORK-DELTA Known costs item 6 state the exception.
- **Owner:** the next milestone
- **Re-open trigger:** `JsonAsStringKeyResidualLiveTest` going red, a Map value appearing in any of the three destinations, or a report of record text in a Connect task status on that route. The fix is to exclude `JsonProcessingException` from the retry-relevant `IOException` test in `ClickHouseWriter`.
- **Source:** 22-REVIEW.md

### 22-REVIEW IN-01: An unknown Enum label is reported as `java.lang.NullPointerException`

- **Reason:** The operator sees the exception class of a lookup miss, so the status names a connector bug instead of the cause. The text is value-free, the failure is non-retriable as intended, and the fix is a `src/main` edit that the release phase does not make.
- **Owner:** the next milestone
- **Re-open trigger:** a report that confuses this message with a connector defect, or the next edit of `convertEnumValues` or its call sites.
- **Source:** 22-REVIEW.md

### 22-REVIEW IN-02: The fixed `No content in enum` text drops the column name, and the exact-`RuntimeException` pass-through delivers it as it is

- **Reason:** Only a hand-built `Column` reaches the text, and the column is named in the preceding ERROR log. The effect is a less specific status line, not a leak.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `Column.convertEnumValues`, or a report of the bare text in a task status.
- **Source:** 22-REVIEW.md

### 22-REVIEW IN-03: The metadata-derivation code is duplicated between the `ClassCastException` branch and the new helper

- **Reason:** The source type and value class are computed in two places, and the older message does not use the new shared prefix constant. Nothing keeps the two in step except the grep prefix FORK-DELTA documents. No behaviour is affected.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of either message in `ClickHouseWriter`, or a change of the shared `Failed to write column` prefix.
- **Source:** 22-REVIEW.md

### 22-REVIEW IN-04: `schemaWithFixedStringMismatchTest` passes vacuously when `put` does not throw

- **Reason:** The assertions sit in a `catch` block with no `fail` after the call, a gap that existed before Phase 22. If the mismatch were ever accepted, the test would stay green. An existing test is not edited in the release phase.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `ClickHouseSinkTaskWithSchemaTest`, or a change of how a FixedString length mismatch is handled.
- **Source:** 22-REVIEW.md

### 22-REVIEW IN-05: Dead counter and planning-ID noise

- **Reason:** `CountingHelperClient.useClientV2Calls` is counted but never asserted, and comments in `src/main` cite planning-only identifiers. Neither affects behaviour, and removing them is a `src/main` and test edit.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `CountingHelperClient`, `ClickHouseWriter` or `WithheldMessageException`, or an upstream sync that touches those comments.
- **Source:** 22-REVIEW.md

### 23-REVIEW WR-01: The escaped-quote branch of `normalise` is not pinned by any test

- **Reason:** No test covers a backslash run before a quote character that keeps the quote open, so a mutation of that arithmetic would pass the suite. The behaviour is correct under the stated premise, and a wrong result fails open to the nullability `DESCRIBE` reported with the metadata-only WARN. Adding the pin is a test edit that the release phase does not make. The escaped-name limitation is documented in the rollout note.
- **Owner:** the next milestone
- **Re-open trigger:** any edit of `DeclaredNullability.normalise`, or a report of the fallback WARN for a table whose element name or enum label contains an escaped quote. Backlog 999.5 (backslashes in names come out doubled) touches the same escaping and should land together with the pin.
- **Source:** 23-REVIEW.md

### 23-REVIEW WR-02: `discoverAsDescribeTableV2` copies the production DESCRIBE loop into the test

- **Reason:** The 999.4 pin in `ClickHouseHelperClientForkUnitTest` exercises a copy of the describe loop, so a production change of that loop would not flip it. The pin documents a known defect and the live tests cover the real path. Extracting the loop needs a `src/main` edit.
- **Owner:** the next milestone
- **Re-open trigger:** the 999.4 fix (a Tuple element named `size` or `size0`), or any edit of `describeTableV1` or `describeTableV2`.
- **Source:** 23-REVIEW.md

### 23-REVIEW IN-01: `apply` Javadoc still says "all or none"

- **Reason:** The method comment predates the partial case, in which a Tuple with unpaired declared elements still applies to the elements that paired. The class comment is correct. It is wording inside `src/main`.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `DeclaredNullability.apply`.
- **Source:** 23-REVIEW.md

### 23-REVIEW IN-02: Null-name check sits inside the `try` and logs a null column slot

- **Reason:** A root with a null name logs the literal text `null` in the column slot of the fallback WARN, which a reader may take for a column named "null". The case is not reachable from the one caller, and a test asserts the current text.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `DeclaredNullability.apply`, or any path that can build a Column without a name.
- **Source:** 23-REVIEW.md

### 23-REVIEW IN-03: Live-test premise loops can pass vacuously, and lookups can NPE instead of failing with a message

- **Reason:** `NullableTupleMatrixLiveTest` checks its premise only if the described row is found, and two lookups throw a bare `NullPointerException` when discovery drops the column. A pairing regression would still fail the test, with a poorer message. An existing test is not edited in the release phase.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `NullableTupleMatrixLiveTest`, or a failure of that test that shows a bare `NullPointerException`.
- **Source:** 23-REVIEW.md

## Fulfilled in Phase 24

### 21-REVIEW IN-03: FORK-DELTA BLD-14 evidence not re-established for Lombok 1.18.48

- **Status:** fulfilled
- **Evidence:** E-04 and E-05 in `24-COMMAND-EVIDENCE.md`. E-05 lifts the javac warning cap and compiles the `v1.7.0_gg2` tree (Lombok 1.18.46) and the gg3 tip (Lombok 1.18.48): 273 warnings each and no `Ambiguous: Jackson2 and Jackson3` warning, while a control without the `lombok.jacksonized.jacksonVersion += 2` line prints it once (274 warnings). E-04 finds the Lombok-generated class bytes identical between the two releases. FORK-DELTA's `lombok.config` row (BLD-14, row 67) carries the re-established wording.
- **Commit:** `177f247c` (plan 24-04, "FORK-DELTA rows and known costs for v1.7.0_gg3").

### 23-REVIEW IN-04: The unpaired-element WARN repeats at every refresh for a known, unfixed defect

- **Status:** fulfilled
- **Evidence:** `docs/ROLLOUT-v1.7.0_gg3.md` item 2, "The unpaired-element WARN and Tuple elements named size", says what the WARN marks (a Tuple element named `size` or `size0` that the table description drops, backlog 999.4), that it repeats at every describe, and what to do about it. The WARN text is unchanged; the note carries the consequence the finding asked for. Item 3 and FORK-DELTA Known costs item 10 say the same.
- **Commit:** `9d4cb9b2` (plan 24-03, "rollout items 2 and 3, the known defects and the backslash change").

## Other carried items

### The `jacocoForkReport` class list omits two fork classes

- **Reason:** `jacocoForkReport` in `build.gradle.kts` lists 15 classes and omits `DeclaredNullability` and `WithheldMessageException` (research R6.2). Adding them is a `build.gradle.kts` change that Phase 24 does not make. FORK-DELTA's jacoco row records the gap, so the coverage report understates the fork's coverage and nothing else.
- **Owner:** the next milestone
- **Re-open trigger:** the next edit of `jacocoForkReport`, or the next milestone that reports fork coverage.

## Open user items after publication

These three items are for the user in the GitHub web UI of `ne1r0n/clickhouse-kafka-connect` (D-11). No agent changes repository settings, and nothing in the release waits on the answers.

### 1. The `dependencies` label

- **Steps:** open the repository on GitHub, then Issues, then Labels, then New label. Name it `dependencies` and save. The alternative is to drop the two `labels:` requests from `.github/dependabot.yml` in a later milestone.
- **Why it matters:** both `dependabot.yml` entries request the label, the repository has none, and every Dependabot PR gets a "labels could not be found" comment. Updates still run.

Outcome: reminder only; nothing waits on it (D-11)

### 2. Pull requests #4 and #9: the fork-only pins

- **Steps:** open Pull requests, then #4 (ClickHouse client 0.9.6 to 0.10.0) and #9 (`connect-api` 4.1.1 to 4.4.0). Close each, or leave it open. Consider adding `ignore` entries for both dependencies to `.github/dependabot.yml` in a later milestone so the PRs do not return.
- **Why it matters:** both bump versions this fork pins on purpose. #4 is out of scope for the fork, and #9 is a fork-only pin. They hold two of the five open-PR slots.

Outcome: reminder only; nothing waits on it (D-11)

### 3. Pull requests #6, #7 and #8: the dependency freeze ends with the tag

- **Steps:** open Pull requests, then #6 (commons-lang3 3.21.0), #7 (ben-manes versions plugin 0.66.0) and #8 (Gradle wrapper 9.8.1). Merge or close each when ready.
- **Why it matters:** the dependency freeze ends once `v1.7.0_gg3` is published. Any merge on `origin/custom_build` diverges `sync/v1.7.0`, and the next milestone opens with a DEPS-01-style merge of `origin/custom_build` before it starts work.

Outcome: reminder only; nothing waits on it (D-11)

## Milestone audit

Run `/gsd-audit-milestone` after Phase 24 verification and before `/gsd-complete-milestone` (20-D-12), so that `v1.7.0_gg3-hardening` closes with an audit instead of an override closeout. The audit reads this file and the four `*-REVIEW-DISPOSITION.md` files, all of which read `open: 0`.

## Housekeeping

- Remove `/tmp/gsd24`, `/tmp/gsd24-scripts` and any leftover worktree of the probe (`git worktree list` shows them).
- If plan 24-06 or 24-07 used a new read-only CI-log token, revoke it on GitHub under Settings, Developer settings, Personal access tokens, and delete its Keychain item.
- Backlog 999.4 (discovery drops a Tuple element named `size` or `size0`) and 999.5 (backslashes in names come out of `fromJsonRow` doubled) stay open. They are recorded in FORK-DELTA Known costs items 10 and 11.
- Milestone tags stay local.
