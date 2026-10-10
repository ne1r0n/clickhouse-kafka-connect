---
phase: "15"
slug: "suite-gate-publication-rollout"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-10"
---

# Phase 15 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

The register below was built from the `<threat_model>` blocks of plans 15-01 to 15-08, which were written when the plans were made. The `gsd-security-auditor` checked each mitigation against the published merge commit `f328fd21` and the rehearsed tree `ae8520ed`, which are byte-identical. It used read-only commands and returned `## SECURED`, 29 of 29.

The orchestrator then reopened T-15-14 on the evidence of 15-REVIEW WR-01, checked against the source (see the T-15-14 row).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Kafka record -> JSON request body | Untrusted Debezium values become numbers in the JSONEachRow body sent to ClickHouse | record values (untrusted) |
| ClickHouse catalog -> connector log | Table and column names from DESCRIBE are written into a WARN line | catalog metadata |
| Review finding -> production edit | A finding can pull in edits outside the approved scope | source changes |
| VERSION -> shipped jar | The file feeds the MBean `version=` segment and the client name every request carries | build identity |
| Records -> public documents | The ledger and dispositions are copied into CHANGELOG, FORK-DELTA and the approval bundle | planning records |
| Audited code -> public fork | Phase 13 and 14 changes ship on publication | source |
| OSV API -> audit record | External advisory data read unauthenticated | advisory data |
| Private deployment knowledge -> public documents | The fork is public; the documents are read by anyone | deployment names (must not cross) |
| Rollout doc -> operator action on exactly-once state | An operator clears or queries KeeperMap state by following the doc | operator procedure |
| Fork knowledge -> public FORK-DELTA | Read by anyone building from or syncing this fork | fork description |
| Local Docker daemon -> gate evidence | A running daemon would make a Docker-free claim vacuous | test evidence |
| Local repository -> public origin | The rehearsal push publishes code and planning history for the first time beyond 54b5ce2f | source, planning history |
| GitHub REST API -> gate evidence | Unauthenticated, rate-limited, and the evidence the human decides on | CI results |
| Approved evidence -> published artifact | What the human approved must be what is published | release tree |
| Local custom_build -> public fork | A non-force push to a protected, published branch is only revertible forward | release branch |
| Published tags -> downstream consumers | A consumer may fetch a tag before it could be deleted | release tags |
| Human -> orchestrator -> commands | Only a direct human answer authorises the post-gate steps | approval |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-15-01 | Tampering | MetadataFreeJsonWriter Timestamp/NanoTimestamp encoding | high | mitigate | `MetadataFreeJsonWriter.java:225-242` writes through `gen.writeNumber(BigDecimal.valueOf(long, MILLIS_SCALE/NANOS_SCALE))`, never string formatting. Unit pins cover the edge values 0, 1 and -1. The live parity test `debeziumTimestampAndNanoTimestampReachRowBinaryParityOnTheMetadataFreeRoute` passes, and the pins were seen RED before the fix. | closed |
| T-15-02 | Information Disclosure | D-02 WARN line | medium | mitigate | `ClickHouseHelperClient.java:492-495`: the format string takes four `{}` parameters, all metadata (database, table, column, raw type). `:525` is the single WARN site, fed by the DESCRIBE row. | closed |
| T-15-03 | Tampering | log injection through catalog names in the WARN | low | accept | SLF4J parameters (`:525`). The names are already logged at DEBUG (`:533`, `:560`, `:574`). See the Accepted Risks Log. | closed |
| T-15-04 | Denial of Service | WARN repetition | medium | mitigate | `:518` returns early for sub-column and unflagged rows. `:498` is a static `ConcurrentHashMap.newKeySet()` keyed on `db.table.col:rawType`. Pins: `warnOnceDedupesPerColumnAndRawType`, `subColumnsAndUnflaggedTypesNeverWarn`, and the live `describeTableWarnsOnceForNullableEnumOrTupleColumns`. The batch path can also reach `describeTable` (`ClickHouseWriter.java:1201`, `:1756`), but the dedup is inside the method. | closed |
| T-15-05 | Tampering | exactly-once retry re-encoded by the new encoding | medium | mitigate | `ROLLOUT-v1.7.0_gg1.md` item 1 (lines 28-71): a route table marking the L-22/L-20 route Required under `exactlyOnce=true`, with the state query and the stop/upgrade procedure. | closed |
| T-15-06 | Elevation of Privilege | scope creep into other src/main files | high | mitigate | `git diff --name-only e4755e38 f328fd21 -- src/main` shows exactly MetadataFreeJsonWriter.java and ClickHouseHelperClient.java. Only commits 981a89d6 and 4a91b92c touch src/main. No `fix(15-02)` commit exists. | closed |
| T-15-07 | Repudiation | review finding silently dropped | medium | mitigate | 15-02 review: 0/0/6 with an outcome table. IN-01..06 are carried into the 15-07 bundle item 6 and recorded `deferred` in 15-REVIEW-DISPOSITION.md. | closed |
| T-15-08 | Tampering | stale generated Version.java | high | mitigate | `git show f328fd21:VERSION` reads `v1.7.0_gg1`. Gate Run 1 executed `:clean` and `:generateVersionFile`, and the generated file reads `v1.7.0_gg1`. CI is green on M. | closed |
| T-15-09 | Repudiation | dispositions disagree between YAML and table | medium | mitigate | DISPOSITIONS-OK re-run: 9/9 rows agree for Phase 13 and 6/6 for Phase 14, with `open: 0`. | closed |
| T-15-10 | Tampering | anchors file pinning the old VERSION | medium | mitigate | Commit c750ed1a touches exactly VERSION and fork-anchors.txt. 43/43 anchor rows match at f328fd21. The invariant script reported `result=PASS` (Fact 38). | closed |
| T-15-11 | Information Disclosure | open high threat from Phase 13/14 published | high | mitigate | 13-SECURITY.md and 14-SECURITY.md both report `threats_open: 0`, with all high rows closed. The audit commits are ancestors of T0 and in M. | closed |
| T-15-12 | Repudiation | audit verdicts cited, not measured | medium | mitigate | Every audit row cites a file:line, a test or a command result. The OSV re-query is recorded with positive controls (14-SECURITY.md:31-48). | closed |
| T-15-13 | Information Disclosure | internal names in ROLLOUT/CHANGELOG | high | mitigate | ROLLOUT uses placeholders only. The auditor re-ran the public-content scan on M's ROLLOUT and CHANGELOG section: clean. Both files were in the 44-file pre-push secret scan. | closed |
| T-15-14 | Tampering | operator follows a wrong exactly-once procedure | high | mitigate | Verified: the query columns match `KeeperStateProvider.java:79`, the default `connect_state` is at `ClickHouseSinkConfig.java:315`, the key format at `TopicPartitionContainer.java:22`, and the backfill steps match the ledger verbatim. **Gap, from 15-REVIEW WR-01, checked against the source:** the KeeperMap table is named by `zkDatabase` (default `connect_state`), lives at `zkPath` (default `/kafka-connect`) and is keyed only by `<topic>-<partition>`, so with default settings every `exactlyOnce` connector on a database shares it. The published ROLLOUT item 2 offers `TRUNCATE TABLE <zkDatabase>` as "clear every state row of this connector's table". On a shared table that wipes other connectors' exactly-once state, and their next batches are then treated as new, which risks duplicates. Item 1's "repeat until empty" can also never finish while another connector is active, and the `DELETE` example omits the `<db><dbTopicSplitChar>` key prefix used under `enableDbTopicSplit=true`. **Fixed in 505e4981** (docs only, on local `sync/v1.7.0`): item 1 scopes the state query to the connector's topics, matching the exact topic after stripping `-<partition>` and including the db prefix. It also says what to do with orphan rows. Item 2 replaces `TRUNCATE` with key- and topic-scoped `DELETE`s and warns against truncating the shared table. The 15-05 checks (content, backfill-verbatim, public scan) still pass. The published `v1.7.0_gg1` copy keeps the old text until the next approved publish. | closed |
| T-15-15 | Information Disclosure | understating the Nullable Enum/Tuple defect | high | mitigate | ROLLOUT:162 says "may be stored as NULL or shifted with no error". The self-check at :174-178 matches top-level columns. The WARN constant (`ClickHouseHelperClient.java:494`) uses the same wording. | closed |
| T-15-16 | Repudiation | FORK-DELTA claims untrue of the published tree | high | mitigate | RAW-NUMBERS-MATCH re-run against `v1.7.0..v1.7.0_gg1`: 4/4. Skip basis 18, equal to the gate skip set. The T0..M diff outside `.planning/` is empty. | closed |
| T-15-17 | Information Disclosure | internal names in FORK-DELTA | medium | mitigate | Public-content scan of `f328fd21:docs/FORK-DELTA.md`: clean. The file was in the pre-push scan. | closed |
| T-15-18 | Information Disclosure | secrets in commits first published by the rehearsal push | high | mitigate | Re-scan of the 44 files in 54b5ce2f..ae8520ed found no key header, token or credential URL. The only hits are `ClickHouseHelperClient.java:91`/`:863`, which are variable assignments. | closed |
| T-15-19 | Spoofing | gate evidence from cached results or a stale version | high | mitigate | run1.log shows `:clean` with 23/23 tasks executed. run2.log shows `:test` re-executed. Both exit 0, and the counts are 69/830/0/0/18. | closed |
| T-15-20 | Spoofing | testUnit measured with Docker running | medium | mitigate | `docker info` returned rc=1 (the vz socket was absent) at 11:17Z. testUnit XML at 11:17:43–46Z: 454 tests, 0 failures. | closed |
| T-15-21 | Tampering | rehearsal push publishing tags or other refs | high | mitigate | Tag snapshots before and after the push are byte-identical. Only `sync/v1.7.0` moved. `push.followTags` is unset. | closed |
| T-15-22 | Spoofing | CI evidence from a rate-limit body or another run | medium | mitigate | API re-query: exactly 1 tests.yaml run for ae8520ed (38048114845), push, success, 391 s against the 1350 s limit. | closed |
| T-15-23 | Elevation of Privilege | publication without a recorded direct approval | high | mitigate | Task 1 is `gate="blocking-human"`, and Tasks 2-3 carry preconditions. The verbatim `publish` is recorded in Fact 40. Tasks 2-3 ran in the orchestrator. | closed |
| T-15-24 | Tampering | fast-forward or force push replacing the merge commit | high | mitigate | M has two parents, c61d90ce (first) and ae8520ed. c61d90ce is an ancestor of M. No force flag was used. | closed |
| T-15-25 | Tampering | publishing a tree other than the rehearsed one | high | mitigate | M^2 = ae8520ed, and `git diff --quiet M X` holds. X ran green on CI. | closed |
| T-15-26 | Information Disclosure | upstream or local-only tags reaching origin | high | mitigate | `git ls-remote origin` shows exactly 4 tags at the expected targets. | closed |
| T-15-27 | Spoofing | CI-08 evidence missing a tag-triggered or cancelled run | medium | mitigate | f328fd21 has exactly 1 run (38050860929, success), none cancelled and none with head_branch `v1.7.0_gg1`, re-queried hours after the tag push. | closed |
| T-15-28 | Denial of Service | stale scratch branch on the public fork | medium | mitigate | Origin has no `refs/heads/sync/v1.7.0`. It was deleted only after M was green and the tags were re-checked (Fact 40). | closed |
| T-15-SC | Tampering | package installs (all eight plans) | low | accept | `git diff e4755e38 f328fd21` changes no build, gradle or settings file and no package manifest. See the Accepted Risks Log. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-15-01 | T-15-03 | Catalog names come from the server's own DESCRIBE and are already logged at DEBUG. The WARN uses SLF4J parameters, not a concatenated format string. | plan 15-01 threat model | 2026-10-10 |
| AR-15-02 | T-15-SC | No package is installed or changed in any phase 15 plan. The build files are unchanged between e4755e38 and f328fd21. | plans 15-01..15-08 threat models | 2026-10-10 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-10 | 29 | 29 | 0 | gsd-security-auditor (opus), read-only, against f328fd21 / ae8520ed |
| 2026-10-10 | 29 | 28 | 1 | orchestrator: T-15-14 reopened on 15-REVIEW WR-01, checked against `KeeperStateProvider.java` and `ClickHouseSinkConfig.java:314-315` |
| 2026-10-10 | 29 | 29 | 0 | orchestrator: T-15-14 closed by 505e4981 after the user chose "Fix the doc locally"; topic-scoped SQL tested in clickhouse-local, 15-05/15-06 automated checks re-run green |

Auditor notes, all non-blocking:
- **Scan type:** the pre-push scan was a secret scan, not the IPv4/hostname scan. The auditor re-scanned the published docs and they are clean.
- **Run 2 evidence:** the Run 2 XML was overwritten by a later green full run. Run 2 now rests on `run2.log` and Fact 38.
- **Approval bundle:** 15-08 Task 1 presented a condensed bundle with the full one referenced, not the verbatim bundle the plan asked for.
- **Local paths:** some published planning files contain local developer filesystem paths, which are not credentials. The pattern predates this phase.

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-10. T-15-14 is fixed on the local tail. Publishing that fix to `custom_build` is a separate, approved step.
