---
phase: "34"
slug: upstream-tail-merge
status: verified
threats_open: 0
asvs_level: 1
created: "2026-10-29"
---

# Phase 34 — Security

> Plan-time threat registers (34-01..34-03) classified at evidence depth against `34-COMMAND-EVIDENCE.md` (E-01..E-11), the three SUMMARYs, `34-REVIEW.md`/`34-REVIEW-FIX.md` and the committed tree. No auditor spawned: all threats mitigated or accepted, ASVS 1. Re-read live at close: origin `custom_build` = `3a3a15a9`; `git ls-remote --exit-code` for `refs/tags/pre-v1.7.0-tail-2` and `refs/heads/sync/v1.7.0` exits 2; M3 `adaf8c4e` carries no `.planning` path; `ProxySinkTask` has exactly 3 constructors, two of them package-private.

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| upstream repo -> local scratch branch | Upstream tail merged as a re-read full SHA | Source commits |
| agent merge resolution -> M3 | Hand resolution of conflicted paths | Source lines |
| local repo -> origin | Not crossed (no push) | None |
| connector config -> ProxySinkTask / ClickHouseSinkConfig | Connector name and `additionalRetriableErrorCodes` are operator input | Config strings |
| test seam -> production class | Package-private constructors | None outside package |

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-34-01 | Tampering | M3 mechanical merge | high | mitigate | Unedited tail gate PASS (E-03, E-11); no `.planning` path in M3 | closed |
| T-34-02 | Spoofing | upstream commit | high | mitigate | Upstream re-read before merge, second parent = `9bed6f79` (E-02); no newer release tag (E-11) | closed |
| T-34-03 | Tampering | origin branch/tags | high | mitigate | No push; tag local; ls-remote exit 2; origin tip unchanged (E-01, E-11) | closed |
| T-34-04 | Repudiation | evidence E-01..E-04 | medium | mitigate | Counts from JUnit XML via helper; helper history single-commit | closed |
| T-34-05 | Tampering | ORCH-02 JMX `connector=<name>` | high | mitigate | 8 anchor rows (rows=141 bad=0); constructor test asserts MBean name; mutation-proven (E-05, E-08) | closed |
| T-34-06 | Tampering | `connect-api-references.txt` | high | mitigate | Blessed only via `--write` after 0 missing on 7.9.7/4.1.1/4.4.0; diff = date line + one entry (E-07; review confirmed) | closed |
| T-34-07 | Elevation of Privilege | package-private seams | low | mitigate | Reflection test pins modifiers and 3-ctor set; javap on built class agrees | closed |
| T-34-08 | Information Disclosure | evidence/test output | low | mitigate | Evidence holds commands, hashes, counts; value-free test messages; secrets scan before commits | closed |
| T-34-09 | Denial of Service | non-transient code in `additionalRetriableErrorCodes` | medium | mitigate | Empty default pinned as no-op; malformed values rejected; FUT-05 operator warning handed to Phase 38 (E-10) | closed |
| T-34-10 | Tampering | `DEFAULT_RETRIABLE_ERROR_CODES` drift | medium | mitigate | 15-code oracle, unset-key sweep, anchor row; mutation dropping 159 fails the pins (E-09) | closed |
| T-34-11 | Tampering | config input validation | medium | mitigate | Parse/validate matrix on ConfigDef and constructor paths (E-09) | closed |
| T-34-12 | Repudiation | phase-gate evidence | medium | mitigate | `--rerun` full suites x2, 2028 tests / 0 failures / 18 named skips; cumulative gate PASS (E-11) | closed |
| T-34-13 | Tampering | origin at phase gate | high | mitigate | Gate re-read of origin tip, tag, branch (E-11); re-read at close | closed |
| T-34-14 | Information Disclosure | test logs | low | mitigate | Constant messages; no credentials in new test files (grep clean) | closed |
| T-34-SC | Tampering | package installs | low | accept | No package installed, no dependency change | closed |

## Review Linkage

Code review (`34-REVIEW.md`): 0 critical, 1 warning (WR-01, MBean leak in seam tests, fixed in `ad38ab27`), 3 info open (IN-01..IN-03, none security-relevant).

## Accepted Risks

- T-34-SC: no installs this phase.
- FUT-05 behaviour (redelivery of an operator-listed code bypasses `errors.tolerance`/DLQ) is upstream behaviour, documented for Phase 38.
