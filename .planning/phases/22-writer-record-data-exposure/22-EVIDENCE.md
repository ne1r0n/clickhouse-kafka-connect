# Phase 22 Evidence

- Phase: 22 (Writer Record Data Exposure)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for HARN-08 and WRT-13..WRT-15, read by the verifier together with 22-BEHAVIOUR-LEDGER.md. The file holds commands, counts, display names and message formats only, never a sentinel or record value. Nothing here was pushed.
- Entry format: one `### E-NN: {title}` section per entry, with `key: value` lines for the measured facts. Long output stays in `/tmp/gsd22/exec/`.

## Index

| ID | Plan | What it records | Status |
|----|------|-----------------|--------|
| E-01 | 22-01 | Spotless baseline (D-16), anchors mirror, HARN-08 before and after runs, F7 reading | recorded |
| E-02 | 22-02 | WRT-14 pin on the unchanged writer | pending |
| E-03 | 22-03 | WRT-13 red-before and green-after, D-03 result, WRT-15 commit | pending |
| E-04 | 22-04 | Live put() evidence and DLQ headers | pending |
| E-05 | 22-05 | Phase close gate and SC1..SC4 readings | pending |

### E-01: Spotless baseline, anchors mirror and the HARN-08 migration (22-01)

Recorded 2026-10-19T20:07:27Z. Scratch output is in `/tmp/gsd22/exec/` (`harn08-before.log`, `harn08-before.xml`, `harn08-after.log`, `harn08-after.xml`, `testunit-22-01.log`, `anchors-22-01.txt`, `spotless-gate.sh`).

spotless_base_rev: fefd8648524f8b906a22f47540391f170e2357af
spotless_base_files: 46
spotless_base_md5: f2e32c7bc069ecd155830f82d26285d6
spotless_baseline_red_touched_by_phase: ClickHouseWriter.java 9/10, Column.java 13/14, ClickHouseSinkTaskWithSchemaTest.java 27/28
spotless_after_22_01: SPOTLESS-BASELINE-EQUAL
anchors_mirror_22_01: rows=59 bad=0
harn08_before: tests=5 failures=0 mockito_lines=5
harn08_after: tests=5 failures=0 mockito_lines=0
testunit_22_01: Tests summary: 577 tests, 575 succeeded, 0 failed, 2 skipped
harn08_commit: 84e773be9d20bbe67a8850c05904a099636239e1

The class XML reports `tests="5" skipped="0" failures="0" errors="0"` both before the edit and after it. The D-16 gate (whole-tree `spotlessApply` numstat on a throwaway `git archive` copy of HEAD plus the working tree's changed and untracked files) printed SPOTLESS-BASELINE-EQUAL after the edit, so the new fixture and the edited test are spotless-clean on their own.

(a) D-10 constructor facts. With a dummy builder (`127.0.0.1:8123`, no proxy, no credentials) the real `ClickHouseHelperClient` constructor opens no connection and owns no thread, as measured in 22-RESEARCH ("HARN-08 facts"). `CountingHelperClient` is therefore a plain subclass overriding `isUseClientV2()`, `pinReplica()` and `unpinReplica()`, and each test closes it with try-with-resources (the inherited `close()` closes the V2 client). Nothing is bytecode-instrumented, so the Phase 18 deferred item (OpenJ9 `retransformClasses` crash risk in ClickHouseWriterNullMessageUnitTest) is closed by HARN-08. Other Mockito use in the suite stays out of scope (22-CONTEXT domain). `git diff --quiet fefd8648 HEAD -- src/main` holds.

(b) F7 reading of HARN-08 and ROADMAP SC3, stated for the verifier: the operative clauses are "no longer inline-mocks ClickHouseHelperClient with Mockito" and "closes the Phase 18 OpenJ9 retransformClasses deferred item". The phrase "overrides one level up, in the CapturingClickHouseWriter pattern" is met by the locked D-10 design, a hand-written Mockito-free subclass written in CapturingClickHouseWriter's no-Mockito style. `doInsertRawBinary` calls `chc.isUseClientV2()`, `chc.pinReplica()` and `chc.unpinReplica()` directly, so no writer-level override can count them without a production change, which D-10 forbids.

D-16 plan-time import probe, relied on by plan 22-03: a new `org.apache.kafka.connect.errors.*` import in ClickHouseWriter.java moves its numstat from 9/10 to 10/11, because the `org.*` block is the block spotless moves. An import placed right after `com.clickhouse.kafka.connect.util.Utils` keeps it at 9/10.
