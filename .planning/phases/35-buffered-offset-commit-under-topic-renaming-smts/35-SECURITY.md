---
phase: "35"
slug: buffered-offset-commit-under-topic-renaming-smts
status: verified
threats_open: 0
asvs_level: 1
created: "2026-10-29"
---

# Phase 35 — Security

> Plan-time threat registers (35-01..35-03) classified at evidence depth against `35-COMMAND-EVIDENCE.md` (E-01..E-09), the three SUMMARYs, `35-REVIEW.md`/`35-REVIEW-FIX.md` and the committed tree. No auditor spawned: all threats mitigated or accepted, ASVS 1. Re-read live at close: origin `custom_build` = `3a3a15a9`; `git ls-remote --exit-code` for `refs/tags/pre-v1.7.0-tail-2` and `refs/heads/sync/v1.7.0` exits 2; `src/main` differs from the phase start only in `ChunkFlusher.java`, `AtLeastOnceBufferStrategy.java`, `ExactlyOnceBufferStrategy.java`; the `processing` and `state` packages are unchanged.

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Connect runtime -> buffered sink offsets | The keys and offsets the task returns from `preCommit` decide what the group commits | Topic, partition, offset |
| Original vs transformed record position | SMT output carries both; the fix selects the original | Record metadata |
| Test harness -> repository | Probes and mutations run only in removed worktrees | Source experiments |
| local repo -> origin | Not crossed (no push) | None |
| Rollout guidance -> operators | First-start and reset guidance | Documentation |

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-35-01 | Tampering | `ChunkFlusher.flush` committed offsets | high | mitigate | Offsets from flushed records only, `originalKafkaOffset()+1`; exact-map pins red then green, mutation-proven (E-02..E-04) | closed |
| T-35-02 | Tampering | strategy revoke/grouping keys | high | mitigate | Revoked-partition pins for both strategies; review WR-01 added the partition-half pin, proven red by mutation (`b50b8452`) | closed |
| T-35-03 | Tampering | table routing | high | mitigate | Edit confined to three offset sites; routing pin; gate scope check on `src/main` (E-03, E-09) | closed |
| T-35-04 | Tampering | `connect-api-references.txt` (BLD-16) | high | mitigate | Re-bless only after 0 missing on 7.9.7/4.1.1/4.4.0; diff = date line + 3 `M` lines (E-03) | closed |
| T-35-05 | Repudiation | red/green evidence | medium | mitigate | JUnit XML via helper; exact red-set names; live git re-checks | closed |
| T-35-06 | Tampering | origin branch/tags | high | mitigate | No push; ls-remote exit 2; origin tip unchanged (E-01, E-09; re-read at close) | closed |
| T-35-07 | Information Disclosure | evidence, fixtures, tests | low | mitigate | Synthetic ids; writer records no values; secrets and hygiene scans | closed |
| T-35-08 | Repudiation | live pin emulates Connect's filter | medium | accept | Documented contract applied and asserted to drop nothing; limit recorded in E-05 | closed |
| T-35-09 | Tampering | Processing / state key | high | mitigate | No repo edit (user decision); counterfactual and mutation only in detached worktrees; `processing`/`state` diff empty | closed |
| T-35-10 | Repudiation | BUF-03 evidence drift | medium | mitigate | Probes re-run on the tip; committed tripwire re-checks state key on every `testUnit` (E-06, E-07) | closed |
| T-35-11 | Tampering | probe/mutation leaking into repo | medium | mitigate | Probe class absent from main tree; no worktree remains | closed |
| T-35-12 | Information Disclosure | live-test logs, record | low | mitigate | Synthetic ids, angle-bracket values, scans | closed |
| T-35-13 | Information Disclosure | rollout item/evidence naming deployments | medium | mitigate | Hygiene helper clean over phase files and FORK-DELTA | closed |
| T-35-14 | Tampering | first-start / `--reset-offsets` guidance | high | mitigate | Labelled "derived from Kafka consumer-group semantics (not measured)"; reset flagged unmeasured | closed |
| T-35-15 | Tampering | origin at phase gate | high | mitigate | Gate re-read and ls-remote exit 2 (E-09) | closed |
| T-35-16 | Repudiation | phase-gate evidence | medium | mitigate | `--rerun` suite, named skips, Docker-only delta = 4 live tests (E-09) | closed |
| T-35-17 | Tampering | scope creep at the gate | high | mitigate | Gate requires exactly the three fix files in `src/main`; build/release/workflow untouched (E-09) | closed |
| T-35-SC | Tampering | package installs | low | accept | No package installed, no dependency change | closed |

## Review Linkage

Code review (`35-REVIEW.md`): 0 critical, 2 warnings (WR-01 partition pin, WR-02 old-runtime failure wording; both fixed, `35-REVIEW-FIX.md`), 5 info open (IN-01..IN-05). IN-04 (`onPartitionRemoved` keyed by original partitions against transformed-topic state) shares the root cause of OI-35-1 and is carried to the milestone close.

## Accepted Risks / Open Items

- T-35-08, T-35-SC accepted.
- OI-35-1..3 (Direct-path `reportInsertedOffsets`, many-to-one state key, exactly-once under a partition-changing SMT) are recorded in `35-BUF03-RECORD.md` for the milestone close; any fix touching `Processing.java` or the state key stops for a user decision.
- An at-least-once worker below the supported Connect floor could duplicate its first chunk before failing (review WR-02); guarded by BLD-16 in CI and documented in the rollout item.
