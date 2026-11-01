---
phase: 31
slug: fork-owned-nightly-integration-tests
status: verified
verdict: SECURED
asvs_level: 1
block_on: high
threats_total: 36
threats_closed: 36
threats_open: 0
threats_open_non_blocking: 0
audited: 2026-10-26
auditor: gsd-security-auditor
---

# Phase 31 Security Verification

**Verdict: SECURED.** Every `mitigate` threat was found in the implemented code or in recorded, re-checkable evidence. Every `accept` threat is in the accepted-risks log. `threats_open: 0`.

Register: 30 numbered threats (T-31-01 to T-31-30) plus the `T-31-SC` supply-chain row in each of the six plans: 24 `mitigate`, 12 `accept`. The auditor re-ran blob/SHA-256 of `integrationTests.yaml`, pattern counts in the nightly, anchor-row recounts, a ROADMAP comparison against planning base `e3ab7a8c`, an evidence-file secret scan and read-only unauthenticated GitHub API reads on 2026-10-26. The orchestrator saved this file from the auditor's returned report (single-writer contract); the evidence column is condensed, the full per-threat evidence is in the auditor's transcript.

## Threat Verification

| Threat ID | Disposition | Status | Key evidence |
|-----------|-------------|--------|--------------|
| T-31-01 | mitigate | CLOSED | Evidence uses `expand=status` only; secret-pattern scan of `31-COMMAND-EVIDENCE.md` found 0 hits |
| T-31-02 | accept | CLOSED (AR-01) | Digests recorded at E-04 and E-18 |
| T-31-03 | mitigate | CLOSED | `docker system df` before and after (E-04, E-18); no `rmi`, `prune` or `volume rm` |
| T-31-04 | mitigate | CLOSED | Token route not taken (E-06 `fix-harness`); no `gh api`, `gh run`, `gh variable` or `git push` |
| T-31-05 | mitigate | CLOSED | E-06 records `user_answer_received_by: orchestrator (direct)` verbatim |
| T-31-06 | mitigate | CLOSED (component not built) | No skip list, no reader, no `excludeTest`; `build.gradle.kts` and `gradle/` unchanged; `--tests` banned in the nightly (guard + anchor row) |
| T-31-07 | mitigate | CLOSED (component not built) | No skip entries exist (E-06b `skip-entries: 0`) |
| T-31-08 | mitigate | CLOSED (component not built) | No reader exists |
| T-31-09 | accept | CLOSED (AR-02) | Moot, no list |
| T-31-10 | mitigate | CLOSED | `UpstreamIntegrationWorkflowBytesGuardUnitTest` pins blob `3e9b9be0`, SHA-256 `07dff3d0...5109`, 4812 bytes; red on a one-byte edit (E-10); API: `disabled_manually`, 0 runs after 2026-10-16T10:00:00Z |
| T-31-11 | accept | CLOSED (AR-03) | `.gitattributes` unchanged |
| T-31-12 | mitigate | CLOSED | API-STATE-OK requires exact id/path/state and `total_count == len`; rate limit read first |
| T-31-13 | mitigate | CLOSED | All 5 `uses:` are 40-hex SHAs with version comments, same as `tests.yaml`; `WorkflowActionPinGuardUnitTest` covers the nightly |
| T-31-14 | mitigate | CLOSED | One top-level `permissions: contents: read`; no `write`, `sudo`, `curl`, `continue-on-error` |
| T-31-15 | mitigate | CLOSED | `persist-credentials: false`; guard rule invoked on the nightly |
| T-31-16 | mitigate | CLOSED (advisory WR-02) | `escape_command` and `escape_markdown` on every untrusted value; 1000-character annotation cap; tests and a mutation check (E-14) |
| T-31-17 | mitigate | CLOSED | 0 matches for `secrets.`, `${{`, `--tests`, `Dclickhouse`, `CLUSTER_NAME`; one `vars.` (the gate) |
| T-31-18 | mitigate | CLOSED | Only `schedule` and input-free `workflow_dispatch`; gate pinned byte for byte; 7 gate and 10 trigger reject fixtures |
| T-31-19 | mitigate | CLOSED | Step 75 min inside job 85 min; `if: always()` on summary and uploads; concurrency group without cancel |
| T-31-20 | accept | CLOSED (AR-01) | Digest recorded at E-18 |
| T-31-21 | accept | CLOSED (AR-04) | 14-day retention, no secrets in the job |
| T-31-22 | mitigate | CLOSED | E-16 records the direct answer `move`; proposal commit precedes decision commit |
| T-31-23 | mitigate | CLOSED | Phase 30 section and `## Sync Mechanics` to `## Progress` byte-identical to `e3ab7a8c` |
| T-31-24 | mitigate | CLOSED | Nightly absent from `custom_build` and `origin/custom_build` (contents API 404); no push, dispatch or variable change |
| T-31-25 | mitigate | CLOSED | PHASE-STATE-OK fails closed; rate limit read first |
| T-31-26 | mitigate | CLOSED | Confluent images pinned by tag (`7.9.6`, `0.6.4-7.6.0`); worker Dockerfile has no `RUN`/download step; digests in E-17, E-18 |
| T-31-27 | mitigate | CLOSED | Error-line excerpts only; secret scan 0 hits |
| T-31-28 | mitigate | CLOSED | `ConfluentPlatform.java`: 23 lines added, 1 removed; POST assertion, timeouts and CLASSPATH untouched; `src/main` unchanged |
| T-31-29 | mitigate | CLOSED | E-06b records the direct `accept`, `skip-entries: 0`, timeouts `step=75 job=85` |
| T-31-30 | accept | CLOSED (AR-05) | Growth recorded at E-18 |
| T-31-SC (plans 01-06) | accept | CLOSED (AR-06) | No `pip`, `npm`, `apt`, `sudo`; build files unchanged; script is standard library only |

## Accepted Risks Log

| ID | Threats | Risk accepted | Rationale |
|----|---------|---------------|-----------|
| AR-01 | T-31-02, T-31-20 | Floating `clickhouse-server:latest` and tag-pinned Confluent images can drift | CONTEXT D-06 accepts `latest` drift; digests recorded; digest pinning is deferred (review IN-02) |
| AR-02 | T-31-09 | A malformed skip-list row would break every build | Moot, no list exists |
| AR-03 | T-31-11 | Byte guard fails on CRLF-converting Windows checkouts | CI and dev machine are Unix; `.gitattributes` is upstream-shared (review WR-03) |
| AR-04 | T-31-21 | Reports downloadable for 14 days | No secret in the job, synthetic data, same retention as `tests.yaml` |
| AR-05 | T-31-30 | Extra image pull and build on a 14 GB runner disk | Recorded at E-18; capacity probe and summary diagnose it |
| AR-06 | T-31-SC x6 | Package installs | None installed; actions SHA-pinned |

## Unregistered Flags

None. The phase footprint since `41d965e2` is the nightly, the summary script, `ConfluentPlatform.java` and four guard tests.

## Notes

1. **T-31-06, T-31-07, T-31-08** were closed because the skip-list component was never built (E-06b `skip-entries: 0`). If a skip list is added later (a Phase 32 gap or after), reopen and re-audit these three, with T-31-06 as a high-severity blocker.
2. **T-31-24:** the value of `NIGHTLY_ENABLED` cannot be read unauthenticated. The file is not on the default branch, so the schedule cannot fire regardless. Phase 32 must keep the variable unset until the dispatch run is recorded (ROADMAP.md Phase 32 criterion 5).
3. **Review advisories** (not counted in `threats_open`): WR-01 (all-unreadable reports print "No failed tests."; the Gradle step still fails the job), WR-02 (Markdown escaping gaps; workflow-command injection is closed), WR-03, IN-01 to IN-03.
