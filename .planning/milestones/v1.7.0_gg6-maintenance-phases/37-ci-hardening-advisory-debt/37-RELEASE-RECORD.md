# Phase 37: Release record (ADV-03)

This record states what is known about the real `v1.7.0_gg5` release, the deviation from the ROADMAP wording of Phase 37 criterion 5, the items that sit outside the 29 ledger rows, and what Phase 38 inherits. Every value in the first section is a plain read of the GitHub REST API taken in plan 37-02 and recorded in `37-COMMAND-EVIDENCE.md` entry E-08 (entry E-17 cites it); nothing was written to GitHub. The 29 review rows are in `37-ADVISORY-LEDGER.md`.

## The real v1.7.0_gg5 release (ADV-03)

| Field | Value |
|-------|-------|
| Release id | 426690002 |
| Tag | `v1.7.0_gg5` |
| Draft, prerelease | false, false |
| Target | `custom_build` |
| Author, type | `ne1r0n`, User |
| Created, published | 2026-10-26T22:56:14Z, 2026-10-28T13:16:09Z |
| Releases in the repository | 1 |
| Tag ref, object type | `refs/tags/v1.7.0_gg5`, commit (not an annotated tag) |
| Tag commit | `3a3a15a9ae064ffe09c696b8bb87bce23419d463` (equals the local tag) |
| Tag commit parents | `21c51598b06b6f5af58430765d57b1e5f6747c5a` (the `v1.7.0_gg4` tag), `41e7d5465e8353abb43a5f56dcf9f697c96b537a` |
| Tag commit date, subject | 2026-10-26T22:56:14Z, `Merge sync/v1.7.0: release v1.7.0_gg5` |
| Asset id | 685055288 |
| Asset name | `clickhouse-kafka-connect-v1.7.0_gg5.zip` |
| Asset size | 12453556 bytes |
| Asset digest | `sha256:4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767` |
| Asset uploader, type | `ne1r0n`, User |
| Asset created | 2026-10-28T13:15:52Z |
| Downloaded copy | 12453556 bytes, SHA-256 `4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767`; both equal the API's size and digest |
| Download counter | 0 before and 0 after the API download |
| Green `tests.yaml` run on the tag SHA | run 40416215635, event `push`, branch `custom_build` |

The three `release.yaml` runs (`pre-release.yaml` has one run in total, read as a count only):

| Run id | Event | Branch | Short SHA | Conclusion | Title |
|--------|-------|--------|-----------|------------|-------|
| 40693904004 | workflow_dispatch | `custom_build` | `3a3a15a9` | success | Release custom_build (dry run) |
| 40474419526 | workflow_dispatch | `custom_build` | `3a3a15a9` | cancelled | Release custom_build (publish) |
| 40296481765 | workflow_dispatch | `sync/v1.7.0` | `b847fbdb` | success | Release sync/v1.7.0 (dry run) |

The publish-mode run 40474419526 was cancelled during the step `Build and verify Confluent archive`, and its step `Publish release` was skipped (as was every later step). The dry run 40693904004 also skipped `Publish release`; it created, checked and deleted its own draft, and its annotation reports an archive of 12453661 bytes with SHA-256 `d61a69324393f961410b35a861551f3f6783a77b957262516cb813efb264fa99`.

**Finding.** The release was created outside any workflow run. The keys read in E-08 are `created_by_workflow_run: no` and `publish_path_exercised: no`. The release author and the uploader of the asset are the user's own account, not a workflow bot; the asset was uploaded about four and a half minutes after the successful dry run 40693904004 finished (13:11:25Z to 13:15:52Z). The only publish-mode run ended cancelled before its publish step, and the later success was a dry run that deletes its own draft. So no workflow run created the release and no run took `release.yaml` through its publish step.

**The archive.** The asset's SHA-256 differs from the archive the dry run built on the runner (12453556 against 12453661 bytes, different digests). The cause cannot be determined from the record, because the dry run deleted its draft and the runner's archive no longer exists. A rebuild from the tag, made locally in plan 37-02, has the asset's size (12453556 bytes) but a different digest (`efa6cca4f30d182eea52565f331581f2c4ccee30c99e91ce0f053b341629e0fe`, not byte-identical), and it compares PASS against the published asset under an empty allow-list (`changed 0 same 7399`, E-08): the compare ignores the build date of `manifest.json`, which the research found to be the only differing file of an extracted copy (E-08 measured the entries, not the extracted bytes).

**How to read the same values again** (every call is a plain GET; `<id>` values come from the table above):

1. `gh api repos/ne1r0n/clickhouse-kafka-connect/releases` for the release, its flags and its assets.
2. `gh api repos/ne1r0n/clickhouse-kafka-connect/git/ref/tags/v1.7.0_gg5` for the tag ref and its object.
3. `gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/release.yaml/runs?per_page=10'` for the runs.
4. `gh api repos/ne1r0n/clickhouse-kafka-connect/actions/runs/<run id>/jobs` for the steps and their conclusions.
5. `gh api repos/ne1r0n/clickhouse-kafka-connect/check-runs/<job id>/annotations` for the archive annotation of a run.
6. `gh api repos/ne1r0n/clickhouse-kafka-connect/releases/assets/<asset id> -H 'Accept: application/octet-stream' > <new empty directory>/asset.zip`, then `shasum -a 256` on the file; keep the file out of any directory that holds a script, and never extract or run it.

## Deviation from the ROADMAP wording (O-6)

Phase 37 criterion 5 reads: "The real `v1.7.0_gg5` GitHub release of 2026-10-28 is recorded as evidence, read from the REST API. The record gives the release, the workflow run that created it, its tag commit, and the attached archive's name, size and SHA-256. This closes the 'no run exercised the publish path' caveat (ADV-03)."

The record gives the release, the tag commit and the archive's name, size and SHA-256 as measured. It cannot give a creating run, because none exists. And it does not close the caveat: the facts show that no run exercised the publish path, so a record of them cannot change that. The caveat stays open until Phase 38 publishes through release.yaml from custom_build (RLS-05) and that run is recorded.

The ROADMAP wording is not edited here. The orchestrator decides whether to reword criterion 5 to what the facts allow.

## Items carried outside the 29 rows

| Item | Disposition | Owner | Re-open trigger |
|------|-------------|-------|-----------------|
| 36-SECURITY UF-36-4: no `gradle/verification-metadata.xml`, and no zstd-jni row in the compare's VERSION table | not added (O-3); the zstd natives are caught as UNALLOWED by the compare (E-07 run B lists three `libzstd-jni-1.5.7-23` natives), because that jar ships no `pom.properties` | fork maintainer (the user) | Any dependency bump that adds a native-bearing jar, a Dependabot gradle pull request, or a decision to verify dependency checksums; it is a build change that touches every dependency update. |
| F-1: `ClickHouseWriter.stop()` never closes its `ClickHouseHelperClient` | carried, not fixed | fork maintainer (the user) | A report of growing connections or memory on a worker that restarts or rebalances tasks often, or the next edit of `ClickHouseWriter.stop`. |
| F-2: the 4-minute insert timeout has no effect with `async=false` | carried, not fixed | fork maintainer (the user) | An insert that blocks on a stalled server, or the next edit of `awaitInsertResponse` or of the client timeout options. |
| F-3: Datagen completion counting spans all connectors outside `generateData` | carried, not fixed | fork maintainer (the user) | A nightly red traced to Datagen completion counting, or the next edit of a `ConfluentPlatform` helper that counts across `getConnectors()`. |

F-1 to F-3 are `src/main` or harness behaviour, were observed on both client lines (0.9.6 and 0.11.0), are not Phase 37 requirements and are carried by disposition, not fixed; they come from `36-FOLLOWUPS.md`.

## Open item: the header comment of increment-version.yml

The header comment of `.github/workflows/increment-version.yml` says that `ReleaseWorkflowGuardUnitTest` pins the file. The class that pins it is `IncrementVersionWorkflowGuardUnitTest`; `ReleaseWorkflowGuardUnitTest` pins the two release workflows. The text is wrong but harmless: no test reads that sentence, and the replacement would move no anchor count.

Fixing it means editing `.github/workflows/increment-version.yml`, which plan 37-05 prohibits (plan 37-03 left it for the same reason). Disposition: not fixed. Re-open trigger: the next edit of `increment-version.yml`. Owner: the fork maintainer (the user), because no planned phase may do it: Phase 38 may not edit `.github/` (the Phase 29 D-05 rule that the ROADMAP restates for Phase 38), so the owner is not Phase 38. The fix is one comment word, to be made in a phase that may touch `.github/` (a gap plan of Phase 37, if the orchestrator opens one) or together with any later edit of the workflow. It is carried to Phase 38 as information in the handoff note after item 6.

## Handoffs to Phase 38

1. **Bump the version with the script.** Bump `VERSION` to `v1.7.0_gg6` with `python3 -I .github/scripts/bump_version.py gg` from the repository root; it moves the `VERSION` row of `.planning/scripts/fork-anchors.txt` in the same step. Commit both files together. Phase 38 edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file, and `VERSION` and the anchors file are not among them. (Source: plan 37-01, E-02, E-03.)
2. **Write the gg6 allow-list last.** Write `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt` last, from the final tree, with the baseline `v1.7.0_gg5` (the script `baseline_tag.py` selects it), never copied from the gg5 list. Prove it with the local recipe of E-07 run A: the baseline is rebuilt from its tag in a worktree with the tag's own wrapper, the candidate is built from the tip, and `python3 -I .github/scripts/compare_confluent_archives.py <baseline archive> <candidate archive> .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt` exits 0 with no STALE-ROW. The gg5 list names only the release identity; the research's gg4-to-tip delta (E-07 run B) is a different comparison. (Source: plan 37-02, E-06, E-07.)
3. **The first CI exercise of the compare chain.** The release dry run of RLS-05 is the first CI exercise of the compare chain unless the user ran the handoff `37-USER-DISPATCH-DRYRUN.md` (E-12, expected RED at `VERSION` `v1.7.0_gg5`; the user's report would be E-19). A CI-only defect found by Phase 38 has no phase that may edit `.github/` (Phase 29 D-05), so it returns to Phase 37 as a gap. (Source: plan 37-03, E-11, E-12.)
4. **The ADV-03 caveat.** The caveat closes only with the gg6 publish through `release.yaml` from `custom_build` and its recorded run (RLS-05 and the ROADMAP research flag on where the tag and release come from). Until then the record above stands as written. (Source: this record, E-08, E-17.)
5. **The anchors file.** The anchors file now holds `rows=172`; the `VERSION` row and the release rows of `fork-anchors.txt` are the first things the cumulative gate checks. (Source: plans 37-01 to 37-04, E-18.)
6. **The compare is strict on a dry run too.** The compare step has no `if:` and no `continue-on-error`, so a green dry run means a passing compare. (Source: plan 37-03, E-09, E-10.)

Carried alongside the six, as information and not as a seventh action: the header comment of `increment-version.yml` (the section above) is open with the fork maintainer, with the trigger "next edit of increment-version.yml"; UF-36-4 and F-1 to F-3 stay with their owners as in the table above; and the four `src/main` ledger rows (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01) and the null assertion of 28 IN-05 are dispositions that need the user's approval if he wants them fixed.

## What this phase did not do

- No `src/main` change: the four `src/main` rows of the ledger are dispositions.
- No push, no workflow dispatch or re-run and no mutating `gh` call; no pull-request action.
- No provisional allow-list: the gg6 list belongs to Phase 38.
- The CI dry run is pending with the user (E-12, expected RED); it is not counted as a pass of any criterion.
