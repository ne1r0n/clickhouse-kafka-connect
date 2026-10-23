# Merge Resolution Checklist — upstream tail a6f1df83 (tagless mini-sync)

This document is the checked-in resolution record for merge 2 of Phase 25 on branch `sync/v1.7.0`: the merge commit M2 = `38265d26ce00e078b9e8c7d1198ade333a34f3ac` takes upstream's untagged tail `a6f1df839c079180bc7c343b5823f205e204dafc` (upstream PR #859's merge, 12 commits past `v1.7.0`, no tag) onto the merge-1 tip. The rollback tag is the local, never-pushed `pre-v1.7.0-tail` = `bb88f23e3adbb49417e73c27e561e50d6bf17a5f` (M2's first parent); `pre-v1.7.0_gg4` stays reserved for the release. `UPSTREAM_REF` is the full SHA above, never `upstream/main`. Per research correction X-3 the record lands in the commit after M2, because a new file inside the merge commit would fail the tail gate's provenance invariant. It sits beside `.planning/MERGE-CHECKLIST-v1.7.0.md` (Recorded Facts 17-41).

Three rules govern it:

1. Mechanical merge, per-hunk resolution: Section 1 is generated from `.planning/scripts/conflicted-paths-v1.7.0-tail.txt` (the committed output of `git merge-tree --write-tree --name-only` over M2's two parents) and Section 2 from `git diff --name-only v1.7.0 a6f1df839c079180bc7c343b5823f205e204dafc`. Neither is hand-transcribed.
2. No dependency bump rides inside M2 beyond what upstream's tail carries, and no `.planning` commit happens between `git merge --no-commit` and M2.
3. Recorded Facts continue the global numbering at 42, because the v1.7.0 checklist ends at 41, so a citation "Recorded Fact N" is never ambiguous.

## Section 1 — Conflicted

| path | predicted class | actual class | hunks | resolution shape | owning phase | fork behaviour at risk | adjudicating test | status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| .gitignore | CONFLICT content | conflict | 1 | union of both sides: the fork's `/bin/`, `.vscode`, `.gsd` lines (with their blank lines) first, then upstream's `# ecliose`, `.classpath`, `.project`, `.settings/`; upstream's typo kept so every line exists in a parent; each line once | 25 | the fork's local-tooling ignore lines (`/bin/`, `.vscode`, `.gsd`) | tail gate invariants 1 and 2 (no authored line, no marker, `provenance_paths=3`) | done |
| gradle/libs.versions.toml | CONFLICT content | conflict | 1 | the `[versions]` block takes the fork's value for every shared key, adds upstream's `guava = "33.7.2-jre"` and drops `httpclientTest` (Section 1a); the `[libraries]` hunk auto-merged | 25 | the Kafka `4.4.0` override, `jackson 2.22.4`, `gson 2.14.0`, `lombok 1.18.48`, `lz4Java 1.12.0` and `httpclient 5.6.5` pins | BLD-08 `kafka = "4.4.0"` anchor in fork-anchors.txt, `compileJava` and `compileIntegrationTestJava` (no orphan accessor), the tail gate | done |

## Section 1a — [versions] resolution table

| key | fork value | upstream value | resolution | reason |
| --- | --- | --- | --- | --- |
| clickHouseDriver | 0.9.6 | 0.9.6 | `clickHouseDriver = "0.9.6"` | identical on both sides |
| kafka | 4.4.0  # FORK OVERRIDE (upstream 2.7.0) | 2.7.0 | `kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)` | the fork's Connect API override, comment form kept (BLD-08) |
| gson | 2.14.0 | 2.13.1 | `gson = "2.14.0"` | fork is newer |
| jackson | 2.22.4 | 2.22.3 | `jackson = "2.22.4"` | fork is newer |
| jacksonAnnotations | 2.22 | 2.22 | `jacksonAnnotations = "2.22"` | identical |
| httpclient | 5.6.5 | 5.6.5 | `httpclient = "5.6.5"` | identical on both sides |
| httpclientTest | 5.6.5 | (deleted) | line removed | upstream deleted it with its only consumer, the `httpclient5-test` library row; `build.gradle.kts` now uses `libs.httpclient5` |
| lombok | 1.18.48 | 1.18.46 | `lombok = "1.18.48"` | fork is newer |
| lz4Java | 1.12.0 | 1.11.4 | `lz4Java = "1.12.0"` | fork is newer |
| guava | (absent) | 33.7.2-jre | `guava = "33.7.2-jre"` | added by upstream: overrides client-v2's transitive `33.4.6-jre` (research X-1) |

## Section 2 — Every path upstream changed

| path | upstream change | predicted class | actual class | fork-touched | review | owning phase | status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| .gitignore | M | CONFLICT content | conflict | yes | resolved in Section 1 as a union | 25 | done |
| build.gradle.kts | M | auto-merged both sides | auto-merged | yes | three upstream hunks, all read: `implementation(libs.guava)`, `clickhouseDependencies(libs.guava) // temp to override client's with CVE`, and the integration-test `libs.httpclient5.test` becoming `libs.httpclient5`. The fork's Shadow repair and `verifyConfluentArchive` are untouched | 25 | done |
| gradle/libs.versions.toml | M | CONFLICT content | conflict | yes | resolved in Section 1 and Section 1a; the `[libraries]` hunk (the `httpclient5-test` row replaced by the `guava` row) auto-merged and was confirmed | 25 | done |

## Gate commands (D-15)

Standing tail gate for Phases 26-29 (the unedited script; run it with each gate):

```bash
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD
```

Cumulative gate (default environment; it ran once in Phase 25 for DEPS-03 and runs once more at the Phase 29 release gate):

```bash
.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD
```

Self-test (expected last line `selftest: no failures`):

```bash
.planning/scripts/verify-merge-invariants-selftest.sh
```

## Recorded Facts

42. **Merge-2 conflict list — `observed`, 2026-10-21, plan 25-07.** `git merge-tree --write-tree --name-only HEAD a6f1df839c079180bc7c343b5823f205e204dafc` printed exactly `.gitignore` and `gradle/libs.versions.toml` as conflicted (exit 1); `build.gradle.kts` auto-merged. `git ls-remote upstream main` printed `a6f1df839c079180bc7c343b5823f205e204dafc` immediately before the merge, and `git ls-remote --tags upstream` has no tag newer than `v1.7.0`.

43. **Merge-2 resolution — `observed`, 2026-10-21, plan 25-07.** Resolved as Section 1 and Section 1a record. The resolution was proved by `compileJava` and `compileIntegrationTestJava` (BUILD SUCCESSFUL, so `libs.guava` resolves and no `httpclient5.test` accessor remains) and by the unedited tail gate ending `result=PASS` with `provenance_paths=3` (the real run is evidence entry E-21). M2 changes exactly `.gitignore`, `build.gradle.kts` and `gradle/libs.versions.toml` and adds no file.
