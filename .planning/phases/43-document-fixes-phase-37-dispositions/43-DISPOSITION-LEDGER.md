# Phase 43: Disposition ledger of the rows Phase 37 left open

This ledger disposes of every row that Phase 37 left open, as of the head `5f32e312000dbaa058bc8331c46694c545cd7686` of `sync/v1.7.0` (HEAD when plan 43-03 wrote it). It holds 18 rows for 17 findings: the header comment of `increment-version.yml`, the five advisory rows that needed the user's approval (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01 and 28 IN-05(b)), the broad allow-list globs (UF-37-3), the six Infos of `37-REVIEW.md` and the three of `37-REVIEW-UF.md`, and row 30-IN-04, whose Phase 37 owner was a phase that has finished. Review IN-05 needed two decisions, so it is split into rows with a letter in parentheses, one status per part. The sources are under `.planning/milestones/v1.7.0_gg6-maintenance-phases/37-ci-hardening-advisory-debt/`: `37-ADVISORY-LEDGER.md`, `37-REVIEW.md`, `37-REVIEW-UF.md`, `37-SECURITY.md` and `37-RELEASE-RECORD.md`.

Status vocabulary: `fixed` means a fix landed in Phase 43 and the pointer is the 40-hex commit that carries it, read from `43-COMMAND-EVIDENCE.md`; `dispositioned` means the row is deliberately left as it is, the pointer is `-`, and the row carries the owner `fork maintainer (the user)` and a re-open trigger. No row changed a file under `src/main`: the user approved none of the four `src/main` rows.

## The user's answers (2026-11-02)

The user gave these answers to the orchestrator during `/gsd-autonomous`; they are recorded in `43-RESEARCH.md` at `f8ff8fc3` (section "User Decisions Recorded").

- `src/main` rows 26 IN-02, 27 WR-01, 27 IN-01 and 28 IN-01: "Defer all four (Recommended)".
- Assertion rows: "Approve 37 IN-04, defer 28 IN-05(b) (Recommended)".
- FUT-05 placement: "gg6 note C2 + gg7 handoff (Recommended)".

## Header comment

| ID | Status | Pointer | Owner | Re-open trigger | Note |
|----|--------|---------|-------|-----------------|------|
| 37-HDR | fixed | 566f92e4f612dc4a33eebd3683ea04643e15daa9 | - | - | `.github/workflows/increment-version.yml`: the header names `IncrementVersionWorkflowGuardUnitTest`, the class that reads the file (the open item of `37-RELEASE-RECORD.md`); the six workflow guard classes passed on the commit (E-02) |

## Advisory rows that needed the user's approval

| ID | Status | Pointer | Owner | Re-open trigger | Note |
|----|--------|---------|-------|-----------------|------|
| 26-IN-02 | dispositioned | - | fork maintainer (the user) | The next edit of the `shouldSkipDescribeRow` anchor row or of the describe code of `ClickHouseHelperClient`, or an upstream sync that touches that method; removal needs the user's approval and an anchors edit. | `src/main`, `ClickHouseHelperClient.java`: both `shouldSkipDescribeRow` overloads have no caller in `src/main` and back an anchor row; removal retargets 17 test sites; the user's answer of 2026-11-02: "Defer all four (Recommended)" |
| 27-WR-01 | dispositioned | - | fork maintainer (the user) | An operator report that confuses the withheld text with a defect, the user's decision on a pass-through exception type, or the next edit of `Column.convertEnumValues`. | `src/main`, `Column.java`: an unknown enum label throws `IllegalArgumentException`, which the writer reports with a withheld cause; the reviewed fix (a `DataException`) changes two existing assertions and an operator-visible text; the user's answer of 2026-11-02: "Defer all four (Recommended)" |
| 27-IN-01 | dispositioned | - | fork maintainer (the user) | The next edit of the Map, Array or Tuple write lambdas or of `passesThroughUnchanged` in `ClickHouseWriter`. | `src/main`, `ClickHouseWriter.java`: `passesThroughUnchanged` returns before its Jackson check, so the exclusion holds through the call sites; the fix is one Javadoc sentence; the user's answer of 2026-11-02: "Defer all four (Recommended)" |
| 28-IN-01 | dispositioned | - | fork maintainer (the user) | The next edit of `DeclaredNullability` or its fixtures; narrowing the constant needs the user's approval because tests in two packages read it. | `src/main`, `DeclaredNullability.java`: `ESCAPED_NEWLINE` is public and only tests read it; moving it into `DeclaredNullabilityFixtures` contradicts that class's rule that tests read the production constant; the user's answer of 2026-11-02: "Defer all four (Recommended)" |
| 28-IN-05(b) | dispositioned | - | fork maintainer (the user) | The user's sign-off to tighten the null assertion, the next edit of `DescribeUnreadableEnumMockServerTest` or of the `ColumnTest` enum pins, or an `Enum16` entry added to the matrix. | `DescribeUnreadableEnumMockServerTest.java`: tightening changes an existing assertion; `ColumnTest` already pins the same type text at `Column.extractColumn`; the user's answer of 2026-11-02: "Approve 37 IN-04, defer 28 IN-05(b) (Recommended)" |

## Broad allow-list globs

| ID | Status | Pointer | Owner | Re-open trigger | Note |
|----|--------|---------|-------|-----------------|------|
| UF-37-3 | dispositioned | - | fork maintainer (the user) | The authoring of the next allow-list, or a generated allow-list row whose wildcard follows fewer than two literal path segments. | `compare_confluent_archives.py` `load_allowlist` (`37-SECURITY.md` UF-37-3): a row with a short literal prefix such as `com/*` loads; none of the four shipped lists has a wildcard row with fewer than two literal leading segments (measured in Phase 43 research); a script rule would change the release path, which no later phase may repair; Phase 44 checks the generated gg7 list |

## Open review Infos

| ID | Status | Pointer | Owner | Re-open trigger | Note |
|----|--------|---------|-------|-----------------|------|
| 37-IN-01 | dispositioned | - | fork maintainer (the user) | The next edit of `release.yaml` or `pre-release.yaml`, or a `VERSION` that reads `_gg0` or a gg number with a leading zero. | `release.yaml` and `pre-release.yaml`: the workflows accept `_gg[0-9]+` while the scripts need `_gg[1-9][0-9]*`, and the failure branch of the baseline step names the wrong cause for a grammar error; the run still fails closed |
| 37-IN-02 | dispositioned | - | fork maintainer (the user) | The next edit of `load_allowlist`, or an allow-list whose row hides an added or a removed entry. | `compare_confluent_archives.py`: a row matches on the path alone, so it cannot say whether it allows a changed, an added or a removed entry; a kind column would change the list format on the release path |
| 37-IN-03 | dispositioned | - | fork maintainer (the user) | The next edit of `load_inner`, or a CI log that shows a traceback from the compare. | `compare_confluent_archives.py` `load_inner`: a corrupt archive escapes as a traceback with exit 1 instead of exit 2; it fails closed, because the workflow fails the step on every non-zero exit |
| 37-IN-04 | fixed | 33d079a986b04ade5c878caa0d3aa392915c38a8 | - | - | `ReleaseWorkflowGuardUnitTest.java`: `CHAIN` holds `TAG_GATE_STEP` between the tests gate and the publish step, with a new fixture test seen red first (E-03); approved by the user's answer of 2026-11-02: "Approve 37 IN-04, defer 28 IN-05(b) (Recommended)" |
| 37-IN-05(a) | fixed | 103221a459dcd742a7404d3ea8eceefa17493a29 | - | - | the first line of `confluent-archive-allowlist-v1.7.0_gg4.txt` and `confluent-archive-allowlist-v1.7.0_gg5.txt`, the two lists the review names, names `.github/scripts/compare_confluent_archives.py`; `CompareConfluentArchivesScriptUnitTest` passed on it (E-05) |
| 37-IN-05(b) | dispositioned | - | fork maintainer (the user) | The next release that touches the allow-lists; the gg6 list is not rewritten, so it stays identical to its copy in tag `v1.7.0_gg6`. | `confluent-archive-allowlist-25.txt` and `confluent-archive-allowlist-v1.7.0_gg6.txt` carry the same stale script name in their first line; the gg6 list belongs to the published tag `v1.7.0_gg6`, so neither is rewritten in Phase 43 and the tag copy never diverges (plan-check decision of 2026-11-02) |
| 37-IN-06 | fixed | 7796ad0ab338ca623f41901dcb36935c49226a40 | - | - | `ConfluentArchiveCheck.java`: the lib jars line and its stale-directory hint print only for more than one jar, with a new zero-jar test seen red first (E-04) |
| UF-IN-01 | dispositioned | - | fork maintainer (the user) | The next edit of `UNSAFE_CHARS`, or a compare report line that misleads a reader. | `37-REVIEW-UF.md` IN-01, `compare_confluent_archives.py`: zero-width, format and bidirectional characters pass through the report unescaped; cosmetic, they cannot start a workflow command |
| UF-IN-02 | dispositioned | - | fork maintainer (the user) | The next edit of `load_inner`, or a CI log that shows a traceback from the compare. | `37-REVIEW-UF.md` IN-02: the same defect as 37-IN-03 (a traceback with exit 1); no archive entry name reaches it today because the zip library quotes names |
| UF-IN-03 | dispositioned | - | fork maintainer (the user) | The next edit of `release.yaml` or `pre-release.yaml`. | `37-REVIEW-UF.md` IN-03: the `set -o pipefail` comment cites a runner log without a pointer and is 173 columns; comment-only, but it edits the publish path in both twins and a fixture string for no operator value |

## Re-owned row

| ID | Status | Pointer | Owner | Re-open trigger | Note |
|----|--------|---------|-------|-----------------|------|
| 30-IN-04 | dispositioned | - | fork maintainer (the user) | A publish whose only green `tests.yaml` run came from a branch whose workflow files differ from `custom_build`'s; the publication record of each release states the run. | `release.yaml`: re-owned from Phase 38, a finished phase, to the fork maintainer; for `v1.7.0_gg6` the merged SHA `72e8ccff` has exactly one `tests.yaml` run, `41297502026`, a successful push run on `custom_build` (E-06) |

## Carried unchanged from the Phase 37 ledger

The other twelve `dispositioned` rows of `37-ADVISORY-LEDGER.md` are carried unchanged, one line each, with the owner and the re-open trigger copied verbatim from that ledger:

- 31-IN-02: owner fork maintainer (the user); re-open trigger: A nightly red traced to an image pull failure or Docker Hub throttling, or the next edit of the image tags in `ConfluentPlatform`; digest pinning changes harness behaviour.
- 31-IN-03(b): owner fork maintainer (the user); re-open trigger: A developer machine that cannot run testUnit for lack of `python3`, or an edit of the launcher of `FailedTestsSummaryScriptUnitTest`; the hard failure on a missing `python3` is declared intentional in its Javadoc.
- 25-WR-02(b): owner fork maintainer (the user); re-open trigger: A change of the Connect API pin or a bump of the Confluent test stack, because the derivation resolves members on the test classpath whose Kafka version can differ from the compile pin.
- 25-WR-03: owner fork maintainer (the user); re-open trigger: A connector change that uses a Kafka type only in a signature or an annotation, which is not reachable today.
- 25-IN-02: owner fork maintainer (the user); re-open trigger: A Shadow bump that changes how multi-release entries are written, or an edit of `ConfluentArchiveCheck`; the new compare also reports nested jars.
- 25-IN-03: owner fork maintainer (the user); re-open trigger: A Kotlin DSL rewrite of the archive task (DEPS-F3), or a connector change that stops using one of the five anchor members.
- 25-IN-04(a): owner fork maintainer (the user); re-open trigger: A client bump that changes the guava pin or a CVE on guava; the lines are upstream-owned and Phase 36 E-09 measured them as redundant on 0.11.0.
- 25-IN-04(c): owner fork maintainer (the user); re-open trigger: The next Gradle wrapper bump, or a decision to add `gradle/verification-metadata.xml`; adding a wrapper checksum is a build change that touches every wrapper update (O-3).
- 27-IN-02: owner fork maintainer (the user); re-open trigger: The next edit of `JsonAsStringKeyResidualLiveTest` or of the FORK-DELTA rows that name it; the finding's own alternative is taken, the Javadoc intro was re-read and states the present behaviour (E-15), and the stale class name is accepted.
- 27-IN-03: owner fork maintainer (the user); re-open trigger: An upstream sync whose comment trips `SourcePlanningIdGuardUnitTest`, or the next edit of that test.
- 28-IN-02: owner fork maintainer (the user); re-open trigger: The next edit of `jacocoForkReport` or `ForkCoverageScopeUnitTest`, or a new fork class.
- 28-IN-04(b): owner fork maintainer (the user); re-open trigger: The next edit of `RawNameMatrix` or the `ColumnTest` enum pins, or a server release that changes the escapes measured in Phase 29 E-24; a live matrix entry needs a Docker run.

## Other Phase 37 carried items

UF-36-4 (no `gradle/verification-metadata.xml`, and no `zstd-jni` row in the compare's version table) stays open with the fork maintainer as FORK-DELTA item 18. The Phase 36 follow-ups F-1, F-2 and F-3 were closed by Phases 40, 41 (on client V2; its limits stay open) and 39, as FORK-DELTA items 13, 14 and 15 state. The ADV-03 caveat closed with publish run `41298074815` (FORK-DELTA item 22). The open item on the header comment of `increment-version.yml` is row 37-HDR.

## Handoff to Phase 44

1. The gg7 rollout note states "FUT-05 stays open" in its RSK-03 section (the user's answer "gg6 note C2 + gg7 handoff (Recommended)"); RLS-03's review checks it there.
2. The reviewer brief of RLS-03 covers the published documents edited in Phase 43: `docs/ROLLOUT-v1.7.0_gg1.md` and `docs/ROLLOUT-v1.7.0_gg6.md` (commit `965e646f`) and `docs/FORK-DELTA.md` (commit `870fe9d8` and the commit that added this ledger), and says that FUT-05 in row C2 and in the correction notice of the gg6 note is deliberate, and that the section "The bypass of errors.tolerance" of the gg6 note (about line 121) describes the same redelivery without the ID by the user's choice (FUT-05 is named only in row C2 and the notice).
3. If Phase 44 corrects the gg6 note's sentence on the insert timeout with `async=false` (assigned to Phase 44 by `41-03-SUMMARY.md`), it extends the gg6 correction notice to name that row too.
4. Raw numbers: Phase 43 repointed only the gg6 reproduction commands and the gg5-to-gg6 paragraph to tags; the `HEAD`-relative commands of the gg4-to-gg5 paragraph (`git diff --shortstat v1.7.0_gg4 HEAD`) and of the import-order recount (`git diff --quiet v1.7.0 HEAD`) are left for Phase 44's one raw-numbers refresh, with the new gg7 row.
5. UF-37-3 and 37-IN-05(b): the verify of the generated gg7 allow-list checks that every wildcard row follows at least two literal path segments (true for all four shipped lists, measured in Phase 43 research), and its first line names `.github/scripts/compare_confluent_archives.py`.
6. Carried from Phase 38 as information: the SAME first-chunk case is unstated (`38-03-SUMMARY.md`, "Issues Encountered"), and section B of the gg6 note lists configurations it does not characterise.
7. FORK-DELTA item 20 names this ledger by file name; when the milestone is archived, the Phase 43 directory moves under `.planning/milestones/`.
