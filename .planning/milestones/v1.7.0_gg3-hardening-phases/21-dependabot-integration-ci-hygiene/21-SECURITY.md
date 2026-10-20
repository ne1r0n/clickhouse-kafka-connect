---
phase: "21"
slug: "dependabot-integration-ci-hygiene"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-19"
---

# Phase 21 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| User's browser and chat -> evidence file | Operator facts cross only as the user's typed words or a screenshot | Operator answers (non-secret) |
| Public GitHub REST API -> evidence file and closing verdict | Unauthenticated JSON is the only machine view of the repository settings and of whether the nightly ran | Public repository metadata |
| macOS Keychain -> shell | A secret store, queried only for presence | Exit status only; never the token |
| origin/custom_build -> sync/v1.7.0 | Published commits made outside the GSD flow (the user's Dependabot merges) enter the working branch | Source and build config |
| Maven repositories -> build classpath | The bumped coordinates are resolved and loaded by Gradle | Third-party jars |
| Working tree -> merge commit | Unrelated dirty files could be folded into a permanent commit | Planning state files |
| Exemption data file -> gate policy | A data row decides whether a real anchor violation is reported | Gate policy data |
| Self-test -> repository refs | The self-test synthesizes commits in the live repository | Git objects and refs |
| Workflow and build text -> CI behaviour | tests.yaml and build.gradle.kts decide what CI runs and with which token scope | CI config |
| Guard regexes -> accepted configurations | A regex change can silently widen what the guard accepts | CI config verdicts |
| Local clock vs GitHub clock | The window rule must use GitHub's Date header | Timestamps |
| Local branch -> origin | A push here would publish an ungated tree | Commits |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-21-01 | Spoofing | E-02 answers | medium | mitigate | 21-EVIDENCE.md E-02: every recorded answer (7 lines) is marked "given directly to the orchestrator" with a timestamp. The screenshot-only replies were not counted as the user's reading, including the 2026-10-19 OPS-02 follow-up. | closed |
| T-21-02 | Information Disclosure | Keychain token, evidence file | medium | mitigate | 21-EVIDENCE.md: 0 matches for `gh[pousr]_` / `github_pat_` token shapes. Every `find-generic-password` call discards output (`>/dev/null 2>&1`), and 0 uses of `-w`/`-g`. | closed |
| T-21-03 | Elevation of Privilege | GitHub repository settings | medium | mitigate | 21-EVIDENCE.md: 0 `Authorization:` headers and 0 `gh api/run/pr` calls. Every API command is an unauthenticated `curl -sS --fail` GET. | closed |
| T-21-04 | Repudiation | OPS-01 disable date | low | mitigate | E-02 records `ops01_dates_agree: no` with the user's verbatim resolution "OPS-01: time is right" and `ops01_disable_instant_resolved: 2026-10-16T09:38:23Z`. E-13 derives `cutoff_u: 2026-10-16T09:38:23Z` from them. | closed |
| T-21-05 | Tampering | merge of origin/custom_build | high | mitigate | E-03: `remote_tip` 6473e24f (ls-remote and fetch agree). The rehearsal staged 2 files with no unmerged paths. The numstat against the first parent is 4 files, 1/1, 1/1, 2/2, 2/2. | closed |
| T-21-06 | Tampering | merge commit contents | medium | mitigate | `git show --stat 82524e12`: 4 files changed. `.planning/state.json` is not in the merge commit. | closed |
| T-21-07 | Repudiation | anchors invariant past 6473e24f | medium | mitigate | E-04: the unmodified gate's stderr is exactly one line, at 6473e24f (BLD-08). No line names the merge commit 82524e12. | closed |
| T-21-08 | Tampering | Lombok 1.18.48 annotation processing | medium | mitigate | E-06: 0 "ambiguous" lines in the clean compile. The control without `lombok.config` shows the Ambiguous warning. | closed |
| T-21-09 | Denial of Service | published history | medium | mitigate | E-12: `git ls-remote origin` has 0 refs at HEAD's SHA. Re-checked 2026-10-19: origin `custom_build` is still 6473e24f, and no `sync/*` branch exists on origin. | closed |
| T-21-10 | Tampering | fork-anchor-exemptions.txt used to hide a new anchor violation | high | mitigate | The single exemption row is keyed on the full 40-hex SHA, path and pattern. `verify-merge-invariants.sh:277` matches with `grep -qxF`. The self-test has ST6-other-row, ST6-other-commit, ST7-other-bld08-row, ST8-* and ST9-exact-row. E-08 records `anchor_exempt=1`. | closed |
| T-21-11 | Tampering | verify-merge-invariants.sh weakened by the patch | high | mitigate | E-09: the SHA-only mutant run fails 2 cases (14 PASS, 2 FAIL), proving an over-broad lookup goes red. | closed |
| T-21-12 | Repudiation | self-test passing vacuously | medium | mitigate | E-09: the old-gate run fails exactly 7 cases (9 PASS, 7 FAIL), including the ST9-exact-row positive control. | closed |
| T-21-13 | Tampering | live repository refs | low | mitigate | The self-test creates only dangling objects (temporary `GIT_INDEX_FILE` with `read-tree`/`write-tree`). 0 `git stash` uses. E-09: the `git for-each-ref` snapshot before and after is byte-identical (145 lines, `cmp` clean). | closed |
| T-21-14 | Tampering | write-scope and runs-on rules | high | mitigate | `TestsWorkflowPinGuardUnitTest` keeps must-reject fixtures for `contents: write`, `write-all`, quoted `"write"`/`'write'`, `ubuntu-latest # x` and `ubuntu-24.04#x`. E-11 has the `#### Accept-set changes (D-10)` table. The class is green at the tip (26/26, re-run by the verifier). | closed |
| T-21-15 | Spoofing | CI-18 checks satisfied by a mention | medium | mitigate | `ktsCode()` (line 305) strips comments before the register search. `GRADLEW_REPORT` (lines 56-57) needs `./gradlew ... reportTestForks` and refuses lines starting with echo/printf. The verifier's mutation runs turned the real-file test red on both CI-18 losses. | closed |
| T-21-16 | Repudiation | D-07 red proof | medium | mitigate | E-10: 26 tests, 20 succeeded, 6 failed before the fix, with names and messages recorded. | closed |
| T-21-17 | Tampering | guarded files edited to make the guard pass | medium | mitigate | `git diff 82524e12 HEAD -- .github build.gradle.kts gradle src/main` is empty, and the working tree is clean for those paths. | closed |
| T-21-18 | Repudiation | E-13 recorded before the window | medium | mitigate | E-13 `github_date: Sun, 18 Oct 2026 17:11:13 GMT`, after 2026-10-17T12:00:00Z, taken from GitHub's Date header. | closed |
| T-21-19 | Tampering | REST filter silently matching nothing | medium | mitigate | E-13: the control query since 2026-10-01 returns 2 runs. Both workflow-scoped and repo-wide counts are recorded (0 and 0). | closed |
| T-21-20 | Elevation of Privilege | unmaintained nightly running with repository secrets | medium | mitigate | E-13: `workflow_state: disabled_manually`, `workflow_updated_at` 2026-10-16T09:38:23Z, matching E-01. The file is byte-identical to upstream v1.7.0. Phase 24 re-checks OPS-01. | closed |
| T-21-21 | Repudiation | phase closed on a stale gate | medium | mitigate | E-12 at the tip: the gate exits 0 with empty stderr, the self-test has no failures, and testUnit and spotless baseline-equal were run. The no-push check uses `git ls-remote origin`. | closed |
| T-21-22 | Repudiation | E-13 cutoff not traceable to the user's disable date | low | mitigate | E-13 re-derives `cutoff_u` from E-01 `workflow_updated_at` and E-02 `ops01_disable_instant_resolved` (equal, 2026-10-16T09:38:23Z). The disagreement is resolved by the user's verbatim answer. | closed |
| T-21-SC (21-02) | Tampering | dependency bumps (lombok, spotless, slf4j-reload4j) | medium | mitigate | E-05 dependencyInsight: slf4j-reload4j 2.0.17 -> 2.0.20, lombok 1.18.48. The bumps arrive only through the user's own published Dependabot merges. No catalog edit by the phase beyond the merge. | closed |
| T-21-SC (21-01, 21-03, 21-04, 21-05) | Tampering | npm/pip/cargo installs | low | accept | No package install and no dependency change in these plans. See R-21-01. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-21-01 | T-21-SC (21-01, 21-03, 21-04, 21-05) | These plans install no package and change no dependency, so nothing reaches a package manager. The only dependency change in the phase is the 21-02 merge, which is mitigated separately. | Plan threat models (accept disposition at plan time) | 2026-10-19 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-19 | 24 | 24 | 0 | gsd-secure-phase (L1 short-circuit: register authored at plan time, all mitigations evidenced) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-19
