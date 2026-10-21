---
phase: "26"
slug: "size-named-tuple-elements-in-discovery"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-22"
---

# Phase 26 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| ClickHouse server output -> committed fixture files (26-01) | The server's DESCRIBE text becomes a test oracle in the repository | DESCRIBE rows (names and types), server version, image id |
| Developer shell -> capture harness (26-01) | A hand-run JavaExec writes into the source tree | Fixture files below `src/test/resources/describe-fixtures/` |
| ClickHouse DESCRIBE response -> connector discovery (26-02) | Server text becomes the Column list, which is the RowBinary wire format | Column names and types |
| ClickHouse DESCRIBE response -> Column list -> RowBinary bytes (26-03) | Server text decides which Columns exist; the writer iterates them to build every row's bytes | Column names, types, record bytes |
| Upstream sync -> fork-modified Table.java (26-03) | A wholesale take of upstream's file would silently revert the fork line | Source text |
| Kafka record -> ClickHouse row through the sink task (26-04) | The test writes synthetic records and reads them back typed | Synthetic test values, one crafted control-character String |
| Evidence and rollout facts -> Phase 29 operator note (26-04) | Recorded facts become operator guidance | Measured and assumed facts |
| Gate output -> phase completion (26-05) | The recorded results decide whether Phase 27 starts from this tree | Test results, gate output |
| Local branch -> origin (26-05) | Nothing may cross it in this phase | Commits, tags |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-26-01 | Tampering | `DescribeFixtureCapture` writing `src/test/resources` | low | mitigate | `run()` exits 2 when `isCloud()` or `isCluster()`; container and both clients are in try-with-resources; `deleteStaleFixtureFiles` removes only non-catalog `*.jsonl`/`*.columns.txt`; `SERVER_VERSION.txt` records version and image id; E-01 records two runs with `diff -r` silent | closed |
| T-26-02 | Tampering | committed fixtures going stale when `latest` moves | medium | mitigate | `DescribeFixtureDriftLiveTest extends ClickHouseBase` asserts, per fixture with the key in the message, the live DESCRIBE lines and the production Column snapshot equal the committed files (94 tests green in E-03, E-07, E-12); re-capture is one documented Gradle command | closed |
| T-26-03 | Repudiation | the "no behaviour change" claim of the extraction commit | medium | mitigate | `DescribeFixtureSnapshotUnitTest` compares the builder's snapshot with the committed pre-extraction snapshot for all 47 `DescribeFixtureCatalog.FIXTURES`; E-04 records the full suite on the extraction commit `9c186046`: 1543 tests, 0 failed, 18 skipped | closed |
| T-26-04 | Information disclosure | log lines moved into `DescribeTableBuilder` | low | mitigate | The builder's logger is `LoggerFactory.getLogger(ClickHouseHelperClient.class)`; its DEBUG and WARN lines carry column names, types, database and table only; the 26-02 SUMMARY threat flags confirm no new text or logger | closed |
| T-26-05 | Tampering | `DescribeTableBuilder.decide` keeping a structural row | high | mitigate | `decide` fails closed on a `RuntimeException` from the parent-type parse (D-03) and keeps a row only when the parsed parent Tuple declares its key; `DescribeTableBuilderUnitTest` (120 rows) asserts the DDL oracle for all 47 fixtures; the look-alike snapshots stayed byte-identical (E-06); `TupleSizeNamedElementRoundTripLiveTest` reads 46 shapes back typed with zero WARN (E-09) | closed |
| T-26-06 | Information disclosure | `UNREADABLE_PARENT_WARNING` | medium | mitigate | The WARN text names raw type, parent column, database, table and the exception's simple class name; the "D-03, D-04: an unreadable parent fails closed with one metadata-only WARN" test asserts its content through `LogCapture` | closed |
| T-26-07 | Denial of service | `ClickHouseColumn.of` on server-supplied type text | low | mitigate | The parse runs only for a size-named row whose parent text contains the segment, once per row per DESCRIBE pass; a parser `RuntimeException` is caught and fails closed; the inputs are the same type strings `DeclaredNullability` already parses per root | closed |
| T-26-08 | Tampering | a later upstream sync reverting `Table.java`'s fork line | medium | mitigate | `fork-anchors.txt` rows for `Table.java`: `!child.isDeclaredElement()` count 1 and upstream's pre-fix condition count 0 (DESC-08); the tail gate ends `anchor_rows=77 result=PASS` (E-07, E-12); FORK-DELTA's "For the next syncer" section names the line | closed |
| T-26-09 | Repudiation | the rollback and block-deduplication rollout fact | medium | transfer | E-11 fact (2) is marked assumed (research A5) with its reasoning; Phase 29 measures it or words it as a precaution and must not cite it as measured | closed |
| T-26-10 | Information disclosure | test output and evidence carrying the crafted control-character value | low | mitigate | `TupleSizeNamedElementRoundTripLiveTest` prints key, outcome, server code name and row count only; the crafted String is compared through `hex(tupleElement(c, 'v'))` against `CRAFTED_STRING_HEX`; E-08 and E-09 quote hex and synthetic constants only | closed |
| T-26-11 | Repudiation | stale or cached gate results | medium | mitigate | E-12: `testUnit --rerun`, `rm -rf build/test-results/test` before `test --rerun`, every XML checked against the recorded start by `suite-check.py`; self-test and tail gate ran on the same HEAD `1f2f1561` with no commit in between; the three OpenJ9 crashes and the HotSpot 21 run that counts are all recorded | closed |
| T-26-12 | Tampering | an accidental push or a moved `origin/custom_build` | high | mitigate | No push in any plan; E-12 reads `git ls-remote origin custom_build` = `69cd680b`, no `sync/v1.7.0` branch and no `baseline-v1.7.0_gg4` tag on origin; re-read at this audit: `69cd680b` unchanged, no local-only tags on origin | closed |
| T-26-SC | Tampering | npm/pip/cargo installs | low | accept | No plan installs a package or changes a dependency: `gradle/libs.versions.toml` and `settings.gradle.kts` are unchanged against `baseline-v1.7.0_gg4`, and the only `build.gradle.kts` change is the `captureDescribeFixtures` JavaExec on the existing test runtime classpath | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-26-01 | T-26-SC | Phase 26 changes no dependency and installs no package; the supply-chain threat has no surface in this phase | plan author (26-01 to 26-05 threat models) | 2026-10-21 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-22 | 13 | 13 | 0 | execute-phase orchestrator (L1 grep-depth short-circuit: threats_open 0, register authored at plan time, ASVS 1) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-22
