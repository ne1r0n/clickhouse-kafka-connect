---
phase: "11"
slug: "full-suite-ci-publication"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-07"
---

# Phase 11 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: authored at plan time. All eight of `11-01-PLAN.md` to `11-08-PLAN.md` carry a parseable `<threat_model>` block. This audit checks that the declared mitigations exist. It does not scan for new threats. `T-11-SC`, which is repeated in every plan, is recorded once.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| local working tree → public fork `origin` | The scratch push (11-05) and the `custom_build` merge plus tag pushes (11-06) publish the whole tree, including `.planning/` and the new `docs/FORK-DELTA.md`. A non-force push to a published branch can only be reverted forward | full source tree and planning documents (must be credential-free) |
| git history → `verify-merge-invariants.sh` | The script is the only automated guard against a conflict marker or an unreviewed third-party change reaching the published branch | commit contents |
| `.github/workflows/tests.yaml` → GitHub Actions runner | Defines what runs on hardware the developer does not control, on every push | merged code, test fixtures |
| GitHub REST API → evidence bundle | CI conclusion, timing and artifact facts come from an unauthenticated third-party endpoint (60 req/h) | run metadata (public) |
| Gradle build cache / Docker daemon → test evidence | A cached `UP-TO-DATE` result or a live daemon during `testUnit` would pass for fresh, Docker-free evidence | test result XML |
| approved evidence → published artifact | What the human approves at the 11-06 `blocking-human` gate must be what gets published | publication decision |
| connector → ClickHouse server (11-08) | One connector-default server setting (`network_compression_method=lz4`) on every outbound client request | server settings |
| `.planning/` bookkeeping → `docs/` and requirement rows | Facts and closures move into documents a stranger or the next syncer will trust without re-deriving them | bookkeeping claims |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-11-03 | Tampering | `verify-merge-invariants.sh` Invariant 2 | high | mitigate | Three-way exit-status branch: `grep_rc=$?` / `case "${grep_rc}"` with `*) violation="search failed with exit status ${grep_rc} …"` (`.planning/scripts/verify-merge-invariants.sh:47-51`), so a swallowed tool error is reported as a violation | closed |
| T-11-04 | Repudiation | `MERGE-CHECKLIST.md` status cells | medium | mitigate | Flipped rows carry the measured shortstat / resolved-diff evidence inline (`MERGE-CHECKLIST.md`) rather than a citation | closed |
| T-11-06 | Information Disclosure | Phase 07/09 production surface | high | mitigate | Retroactive audits exist: `07-SECURITY.md` and `09-SECURITY.md` both `threats_open: 0`, `status` verified | closed |
| T-11-07 | Elevation of Privilege | `src/main/` edit under an audit mitigation | high | mitigate | `git log 6ac2d20c..HEAD -- src/main`: the only Phase 11 commit touching `src/main` is `87fa1e78` (11-08 D-17 fix); none from 11-01 | closed |
| T-11-08 | Spoofing | `tests.yaml` job env vs local default | high | mitigate | `tests.yaml` sets neither `CLICKHOUSE_VERSION` nor `CLIENT_VERSION`; `ClickHouseVersionDefaultUnitTest` is the single source of truth and runs in both places | closed |
| T-11-09 | Repudiation | `ClickHouseTestHelpers` D-04 Javadoc | medium | mitigate | Javadoc now reads "CI **used to** set `CLICKHOUSE_VERSION: latest`" (past tense), so no comment claims a pin that no longer exists | closed |
| T-11-10 | Tampering | scope creep into trigger / timeout / coverage | medium | mitigate | `on: [push]`, `timeout-minutes: 45`, `jacoco_summary.py`, both `upload-artifact@v4` steps (`jacoco-coverage-reports`) all still present in `tests.yaml` | closed |
| T-11-11 | Elevation of Privilege | `src/main/` edit under the testFixtures change | high | mitigate | Same `git log -- src/main` check as T-11-07: no 11-02 commit touches `src/main` | closed |
| T-11-02 | Information Disclosure | published tree with no statement of R-01 | high | mitigate | `docs/FORK-DELTA.md:110-120` `## Known costs and open items` leads with R-01, including the trigger, the loss, and the caveat that the operator fact behind accepting it does not transfer to other deployments | closed |
| T-11-12 | Repudiation | ledger rows recorded from intention | high | mitigate | 11-03 SUMMARY: every ledger row derived from `MERGE-CHECKLIST.md` then "confirmed by a grep or git diff against the merged tree, never transcribed" | closed |
| T-11-13 | Information Disclosure | scope-switched delta metric | medium | mitigate | `docs/FORK-DELTA.md` `## The raw numbers` publishes all twelve scope-by-endpoint figures. Re-measured in 11-07 (84/9476/451, 30/1368/98). The mixed result was put to the user as UAT test 3 and recorded `pass` | closed |
| T-11-14 | Information Disclosure | secrets in the new public document | high | mitigate | 11-03 SUMMARY: pattern scan of `FORK-DELTA.md` found no host, credential, token or connection string. Covered again by the 11-05 pre-push scan (T-11-01) | closed |
| T-11-15 | Tampering | `AGENTS.md` edits beyond the link line | medium | mitigate | `git show --numstat 623b8829`: `AGENTS.md` 1/0, `README.md` 1/0, which is exactly two added lines and zero deletions | closed |
| T-11-16 | Spoofing | Gradle `UP-TO-DATE` cache echoing a prior run | high | mitigate | 11-04 SUMMARY records the exact cache-busting commands: `./gradlew test --rerun` and `./gradlew testUnit --rerun` | closed |
| T-11-17 | Repudiation | skip-set substitution behind an unchanged count | high | mitigate | 11-04 SUMMARY D2: 18 (class, method) pairs derived from result XML are set-identical to Phase 10's named 18. Compared as sets, never by count | closed |
| T-11-18 | Spoofing | `testUnit` measured against a live daemon | medium | mitigate | 11-04 SUMMARY: `docker info rc=1 (socket absent)` observed before `testUnit --rerun` → `TESTUNIT_CONTRACT_HOLDS` | closed |
| T-11-19 | Tampering | red test absorbed by re-running until green | high | mitigate | 11-04 gate green on the recorded run (`BUILD SUCCESSFUL … GATE_TOTALS_OK`); no blind re-run and no re-baseline recorded | closed |
| T-11-20 | Denial of Service | server-global state across 25 classes in one fork | low | accept | Accepted — see AR-01 | closed |
| T-11-01 | Information Disclosure | tree published by the scratch push | high | mitigate | 11-05 SUMMARY `### Secret scan, run before the push (T-11-01)`. The only literal-value hit (`MaskTest.java:14`) is byte-identical in upstream `v1.6.0` | closed |
| T-11-21 | Spoofing | run lookup resolving to the wrong / an ambiguous run | high | mitigate | 11-05: `head_sha` + workflow `path` → exactly one run (37617508949). On the published SHA the plain locator returned two runs (one cancelled by the tag-push concurrency group). The narrowed locator (`conclusion != cancelled`) → exactly one (37619434848) was accepted by the user in UAT test 1 (`pass`) | closed |
| T-11-22 | Tampering | red run laundered by re-running | high | mitigate | 11-05 run metadata: `run_attempt` = 1; run green on first attempt | closed |
| T-11-23 | Repudiation | threshold comparison that cannot fail | medium | mitigate | Threshold 1350 s derived from `timeout-minutes: 45` read at execution time. Measured 350 s (11-05) and 386 s (11-07) | closed |
| T-11-24 | Denial of Service | REST rate limit exhausted mid-poll | low | accept | Accepted — see AR-02 | closed |
| T-11-34 | Information Disclosure | published tree (publication half of T-11-01) | high | mitigate | The planned "byte-identical to the scanned tree" proof no longer holds literally, because the approved F-1 fix landed between scan and publish. Re-verified in this audit: `git diff --stat 64289171 c61d90ce -- . ':!.planning'` = `docs/FORK-DELTA.md` only, 5+/5− (raw-number rows). A secret-pattern scan of the full diff's added lines found no credential (the only hits are the 11-05 SUMMARY's prose describing its own scan) | closed |
| T-11-35 | Information Disclosure | publishing a known data-loss path undisclosed | high | mitigate | R-01 disclosure (T-11-02) is in the published tree at `c61d90ce`. The 11-06 bundle re-confirmed the open-item lists (F-2 recorded) | closed |
| T-11-25 | Tampering | fast-forward replacing the merge commit | high | mitigate | `c61d90ce` has two parents (`6ac2d20c`, `586af775`); local `custom_build` resolves to a two-parent commit | closed |
| T-11-26 | Tampering | force flag on published branch or rollback tag | high | mitigate | `pre-v1.6.0-sync` still resolves to `6ac2d20c`. 11-06 records push commands with no force flag and origin `custom_build` = local `c61d90ce` (verified via `git ls-remote`) | closed |
| T-11-27 | Spoofing | CI-06 evidence taken from the rehearsal | high | mitigate | CI-06 evidence is run 37619434848 on the published merge SHA `c61d90ce`, distinct from scratch run 37617508949 | closed |
| T-11-28 | Repudiation | publishing without recorded human approval | high | mitigate | 11-06 SUMMARY records the user's answer "Fix F-1, then publish (Recommended)" given directly to the orchestrator on 2026-10-07. The user confirmed it in UAT test 2 (`pass`) | closed |
| T-11-29 | Denial of Service | stale scratch branch | medium | mitigate | 11-06: `git push origin --delete sync/v1.6.0` after green; absence verified by `git ls-remote`. No `origin/sync/v1.6.0` ref locally | closed |
| T-11-30 | Repudiation | requirement closures outrunning evidence | high | mitigate | 11-07 re-ran every closure command at execution time; a fresh run on tree `4a9fc072` (equal to `c61d90ce` outside `.planning/`) is recorded as a measurement, not a citation | closed |
| T-11-31 | Tampering | unapproved push to the published branch | high | mitigate | 11-07 `DIVERGENCE_ZERO`: local `custom_build` = origin = `c61d90ce`. Bookkeeping commits sit on `sync/v1.6.0` and nothing was pushed. The deviation from the plan's expected divergence is stated in the SUMMARY | closed |
| T-11-32 | Information Disclosure | probe row silently dropped | medium | mitigate | 11-07: probe accounting closed at 11 of 11 (9 explicit + 2 backstop + 0 unresolved). Both backstops were routed to UAT tests 4–5 (`pass`) | closed |
| T-11-33 | Elevation of Privilege | `src/main/` edit invalidating approved evidence | high | mitigate | `git log 6ac2d20c..HEAD -- src/main`: `87fa1e78` (11-08, D-17) is the only Phase 11 commit; it predates the 11-06 approval and is covered by the published-SHA CI run | closed |
| T-11-36 | Denial of Service | connector vs ClickHouse 26.9+ (ZSTD default) | high | mitigate | `ClickHouseHelperClient.java:107-116,187` forces `network_compression_method=lz4` on V1 and V2 clients. Pinned by `NetworkCompressionPinUnitTest` and `NetworkCompressionPinLiveTest` (round-trip against `latest`) | closed |
| T-11-37 | Tampering | operator server settings overridden by connector default | medium | mitigate | Default applied before operator options (V2 `serverSetting` before the `jdbcConnectionProperties` loop; V1 merged by client). The unit test asserts the operator value wins, and the live tests prove it reaches the server on both clients | closed |
| T-11-38 | Denial of Service | profile with `readonly=1` / settings constraint rejecting the setting | low | accept | Accepted — see AR-03 | closed |
| T-11-SC | Tampering | npm/pip/cargo installs (supply chain) | n/a | accept | Accepted — see AR-04 | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on (`high`) count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

**Summary threat flag carried forward (not a new Phase 11 threat):** 11-01 flagged the client-v2 `0.9.6` unquoted splice of `jsonInsertColumns` into `INSERT INTO t (cols)` (`ClickHouseWriter.java:1391`). It is operator-supplied config, already accepted as AR-04 / T-09-16 in `09-SECURITY.md`.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-11-20 | Server-global state can accumulate across the 25 classes sharing one container fork. This is an accepted cost recorded in `MERGE-CHECKLIST.md` Recorded Fact 16. A clean full-suite run detects it, and 11-04 took exactly that run. | plan 11-04 | 2026-10-07 |
| AR-02 | T-11-24 | Polling the unauthenticated REST API stays well inside the 60 req/h budget (~90 s interval, ≤45 min). Exhaustion would delay the measurement, not corrupt it. | plan 11-05 | 2026-10-07 |
| AR-03 | T-11-38 | The connector user must write, so `readonly=1` is impossible. A settings constraint on `network_compression_method` is non-standard. Recorded in 11-08 SUMMARY as the residual risk to check before a prod rollout. | plan 11-08 | 2026-10-07 |
| AR-04 | T-11-SC | No plan in the phase runs a package-manager install or changes a dependency, so the package-legitimacy gate has no surface. | plans 11-01 to 11-08 | 2026-10-07 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-07 | 38 | 38 | 0 | /gsd-secure-phase (orchestrator, ASVS L1 short-circuit — register authored at plan time, grep-depth verification; T-11-34 re-scanned against the scanned-vs-published diff) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-07
