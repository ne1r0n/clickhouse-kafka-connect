# Phase 24: Release & Publication - Pattern Map

**Mapped:** 2026-10-20
**Files analyzed:** 14 new/modified (1 new test, 1 optional twin, 4 docs/config, 6 planning artifacts, 2 scratch scripts)
**Analogs found:** 14 / 14 (all tracked; verified with `git ls-files`)

All analog paths below are git-tracked source. `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/*` files are tracked. Scratch scripts live under `/tmp` and are not committed. The `src/main` tree is untouched in this phase (D-06).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/test/java/com/clickhouse/kafka/connect/sink/<D-03 live test>.java` (suggest `JsonAsStringKeyResidualLiveTest`) | test (live) | request-response (put -> ClickHouse -> failure) | `src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java` | exact |
| optional Docker-free twin (same package `...sink.db`) | test (unit) | transform | `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java` (`unsupportedJsonFieldTypePassesThrough`, line 414) + `.../sink/ValueFreeDlqHeadersUnitTest.java` | role-match |
| `docs/ROLLOUT-v1.7.0_gg3.md` | doc (operator checklist) | n/a | `docs/ROLLOUT-v1.7.0_gg2.md` | exact |
| `CHANGELOG.md` (new `1.7.0_gg3` section on top) | doc | n/a | `CHANGELOG.md` `1.7.0_gg2` section (lines 1-72) | exact |
| `docs/FORK-DELTA.md` (rows, Known costs 6 + new 999.4/999.5 items, raw numbers) | doc | n/a | its own tables and `## Known costs and open items` (line 135) | exact |
| `VERSION` + `.planning/scripts/fork-anchors.txt:41` | config | n/a | current `VERSION` (`v1.7.0_gg2`) and row `VERSION<TAB>v1.7.0_gg2<TAB>1<TAB>BLD-07` | exact |
| `24-COMMAND-EVIDENCE.md` | planning evidence | n/a | `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-COMMAND-EVIDENCE.md` | exact |
| `24-MILESTONE-CLOSE-RECORD.md` | planning record | n/a | `.../20-release-publication/20-MILESTONE-CLOSE-RECORD.md` | exact |
| `24-REVIEW-DISPOSITION.md` | planning record | n/a | `.../20-release-publication/20-REVIEW-DISPOSITION.md` | exact |
| `21-/22-/23-REVIEW-DISPOSITION.md` row flips | planning record | n/a | their own front-matter plus table rules; flipped example in 19's disposition | exact |
| rollback script (scratch, `/tmp/gsd24/rollback-demo.sh`) | script | batch | E-31 `rollback-demo.sh` in 20-COMMAND-EVIDENCE.md (lines 579-637) | exact |
| artifact compare script (scratch `/tmp/gsd24-scripts/cmp.py`) | script | transform | 24-RESEARCH "Code Examples" `cmp.py` | exact |
| 999.4 / 999.5 detection SQL (embedded in note, proven in evidence) | doc/SQL | request-response | 24-RESEARCH R5.2 and R5.4 queries | exact |
| Publication steps (no file) | procedure | n/a | 20-COMMAND-EVIDENCE E-60..E-72 | exact |

## Pattern Assignments

### New D-03 live test (test, live, request-response)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java` (458 lines; package `com.clickhouse.kafka.connect.sink`, `extends ClickHouseBase` so the `clickhouse` tag keeps it out of `testUnit`).

**Imports** (lines 3-40), reuse the subset needed:
```java
import com.clickhouse.kafka.connect.sink.db.ClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import org.apache.kafka.connect.data.{Schema,SchemaBuilder,Struct};
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.{DisplayName,Test,TestInfo};
```

**Constants pattern** (lines 52-73): class-level `private static final String` for DDL, `SENTINEL`, `INSERT_FAILURE_MESSAGE = "Error inserting records"`, `ERROR_LEVEL = "ERROR"`, `ERRORS_TOLERANCE_ALL`, `CONNECTOR_NAME_PROP`. Use DDL `"CREATE TABLE %s (id Int32, j JSON) ENGINE = MergeTree ORDER BY id"` and three distinct sentinels (Map key, sibling Map value, Struct string field). The Json-as-string setting constant is `"input_format_binary_read_json_as_string=1"` (WriterValueFreeFailureUnitTest line 71), set through `props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, ...)`.

**Core harness: copy `putBatch` (lines 274-323) with the writer swap removed** (failure == null keeps the real writer):
```java
Map<String, String> props = getBaseProps();
props.put(CONNECTOR_NAME_PROP, uniqueConnectorName(testInfo));
if (toleranceAll) { props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, ERRORS_TOLERANCE_ALL); }
ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
String topic = createTopicName(TOPIC_PREFIX);
ClickHouseTestHelpers.dropTable(chc, topic);
createTable(chc, topic, tableDdl);          // table BEFORE task.start()
ClickHouseSinkTask task = new ClickHouseSinkTask();
if (reporter != null) { task.setErrorReporter(reporter); }
try {
    task.start(props);
    try { task.put(batchForTopic.apply(topic)); return null; } catch (Exception thrown) { return thrown; }
} finally {
    task.stop();
    try { assertEquals(0, ClickHouseTestHelpers.countRows(chc, topic), "a failed batch must write no rows"); }
    finally { ClickHouseTestHelpers.dropTable(chc, topic); }
}
```
`createTable` helper is lines 163-169 (`chc.queryV2(String.format(ddl, table))` in try-with-resources).

**Record building** (lines 199-210 `uuidBatch` shape): `SchemaBuilder.struct()` + `new Struct(schema).put(...)` + `new SinkRecord(topic, 0, null, null, schema, struct, offset)`. Per research R2.1, `Struct.put` validates, so build the Map entry with an unserializable value by mutating the Map AFTER both `put` calls: `((Map) in.get("m")).put(KEY, new Object())`.

**Assertions: copy from the tolerance=none test** (lines 405-444):
```java
try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
    Throwable thrown = putBatch(...);
    assertNotNull(thrown, "put must fail when errors.tolerance=none");
    ThrowableTexts.allTexts(thrown).forEach(text -> assertFalse(text.contains(valueSentinel), "..."));
    String rendered = ThrowableTexts.printedStackTrace(thrown);   // approximates Connect task status
    List<LogCapture.CapturedEvent> insertErrors = log.events(ERROR_LEVEL).stream()
            .filter(e -> INSERT_FAILURE_MESSAGE.equals(e.message()) && e.hasThrowable()).toList();
    ... String.join("\n", e.throwableTexts()) ...
}
```
Here invert for the key sentinel: `assertTrue(rendered.contains(KEY))` pins the measured residual (R2.2), `assertFalse` for value sentinels, and for the `Error writing value of` line neither appears.

**DLQ (tolerance=all) pattern** (lines 446-457):
```java
InMemoryDLQ dlq = new InMemoryDLQ();
Throwable thrown = putBatch(testInfo, ddl, true, dlq, null, batch);
assertNull(thrown, "put must return when errors.tolerance=all");
```
then `dlq.connectHeaders` for message and stacktrace headers; message header must match `ValueFreeDlqHeadersUnitTest.QUERY_ID_MESSAGE` (package-private, line 38; same package `sink`, so accessible). Stacktrace header contains key, not values.

**Also add** (R2.4): list-index variant (no sentinel), control with setting absent (`Writing JSON in binary is not supported yet...`), and log `SELECT version()` the way `DedupTokenRetryLiveTest` does (it logs `DEDUP_TOKEN_RETRY server version=`).

**Conventions:** `@DisplayName("D-03: ...")` descriptive names; no skip annotations (keep the 18-skip inventory unchanged); read-back/zero-row assertion on failure is mandatory (AGENTS.md).

---

### Optional Docker-free twin (test, unit, transform)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java`. It extends nothing, so it stays in `testUnit`.

**Core pattern** (lines 414-422):
```java
Map<String, String> props = minimalProps();
props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, JSON_AS_STRING_SETTING);
Captured captured = writeColValue(
        new ClickHouseSinkConfig(props), column(JSON_COLUMN, Type.JSON), new Data(Schema.INT32_SCHEMA, 1));
assertEquals(RuntimeException.class, captured.thrown.getClass());
```
Drive `writeColValue(config, column(JSON), data)` with a Struct holding a mutated Map instead, assert key present and value absent in `captured.thrown` chain (rendered with `ThrowableTexts`). The class-level doc contract (lines 39-55) says both directions are asserted and the raw-message embeds the value (non-vacuous check).

---

### `docs/ROLLOUT-v1.7.0_gg3.md` (doc)

**Analog:** `docs/ROLLOUT-v1.7.0_gg2.md`.

**Opening** (lines 1-5): title `# Rollout checklist: \`v1.7.0_gg2\` to \`v1.7.0_gg3\``; first line points operators from older releases at `ROLLOUT-v1.7.0_gg2.md` (and through it gg1); states both releases share upstream base `1.7.0`; changes listed in `../CHANGELOG.md`, `FORK-DELTA.md`.

**Placeholder + three-part vocabulary** (lines 7-17): placeholders only (`<db>`, `<table>`, `<column>`, `<topic>`, `<connector-name>`, `<alert-rule-directory>`; add `<connector-config-directory>`, `<connector-log-file>`); each item has **What changed** / **Am I affected?** (with what an empty result means) / **What to do**; ledger tags like `(ledger L-42)` as trailing tags only; name the ClickHouse server version on every measured claim (26.9.19.9, client V2).

**Item 4 model (alert-rule grep self-check), lines 257-305:**
```sh
grep -rnF -e 'is not nullable and no value is provided' -e 'Error parsing date time string:' <alert-rule-directory>
```
Followed by: **Empty result:** ... **Non-empty result:** ... plus the sentence that `-F` compares literal text so escaped-regex matchers are not listed, and "Run on a scratch directory with one file per old text, the command listed each of them once" (E-30). Reuse for gg3: `grep -rnF -e 'Invalid UUID string' -e 'For input string' <alert-rule-directory>`, `grep -rnF 'input_format_binary_read_json_as_string' <connector-config-directory>` (the D-14 self-check; there is no `binaryFormatWrtiteJsonAsString` connector key, derived at `ClickHouseSinkConfig.java:355-356`), `grep -F 'have no matching DESCRIBE sub-column' <connector-log-file>`.

**Item 5 model (MBean and client name), lines 310-327:** bullet the MBean `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg3` (+ `,topic=<topic>`), client name `clickhouse-kafka-connect/v1.7.0_gg3`; Am I affected points at gg1 note item 7 (`ROLLOUT-v1.7.0_gg1.md#item-7-jmx-mbean-name-and-client-name`) query, unchanged; What to do: change matchers, wildcard forms keep working.

**Item 6 model (rollback), lines 329-345:** "What changed" says nothing in data format is migrated; names `pre-v1.7.0_gg3` -> `6473e24f` (actual pre-merge tip at gate time). Steps:
1. `git fetch --tags origin`, `git checkout custom_build`, `git merge --ff-only origin/custom_build`, `git status --porcelain` prints nothing (commit or `git stash -u`; warn the next command discards changes).
2. `git read-tree -u --reset pre-v1.7.0_gg3`, `git commit -m "Roll back v1.7.0_gg3"`; `git diff --quiet HEAD pre-v1.7.0_gg3 && echo ok`. Explain why not `git revert -m 1`.
Then `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg3.md` to read the note after rollback. Add "Rolling back brings back" list = 24-RESEARCH R7.2 items 1-8, and the artifact wording D-07 (operator's own `./gradlew createConfluentArchive` at tag `v1.7.0_gg2`; the build of `pre-v1.7.0_gg3` has the same zip bytes; no GitHub Release exists).

**Item 1 model (in-flight batches), lines 28-92:** reuse its clean-stop procedure and KeeperMap self-check only for the D-15 shape (`Nullable(Tuple)` with backslash-n or line-break element name, rollback direction), prove live per 20-D-03. Every other change gets one sentence "no clean stop is needed" each way. Do not tell `TRUNCATE` of the shared KeeperMap table (Phase 15 finding).

**New items (no gg2 analog, use item format):** value-free error text (R1 L-42..L-48, with the D-14 headline exception), 999.4 (D-01/D-02/D-12: two-predicate query R5.2 with its one over-flag stated, no workaround, code 32 or 33), 999.5 paragraph beside COLS-13 (D-05/D-13, R5.3 table, R5.4 query plus `/` and tab predicates only after live proof), dependency item (R4: all three bumps leave the shipped zip unchanged; `slf4j-reload4j` is runtimeClasspath only).

---

### `CHANGELOG.md` `1.7.0_gg3` section (doc)

**Analog:** `CHANGELOG.md` lines 1-72 (`# 1.7.0_gg2, 2026-10-14`). Section layout: H1 header with date, intro paragraph (lines 3-7: "fork release built on the same upstream base ... lists only the fork's own changes since ... See FORK-DELTA ... work through docs/ROLLOUT-... before upgrading"), then `## Important Changes`, `## Bug Fixes` (line 44), `## Build and CI` (line 55) with `*` bullets wrapped at ~120 chars, plain language, no planning IDs. Add the 999.4 Known-issue line (D-01, widened per D-12) as a `Known Issues` subsection or in Important Changes. No CHANGELOG line for 999.5 (D-05/D-13). Pre-PR obligation from AGENTS.md: VERSION bump plus this entry.

Example bullet shape to copy (lines 9-15): `* \`Nullable(Enum8)\`... values are now stored as the record says ... Rows that earlier releases already stored wrongly are not repaired by the connector; the rollout checklist has queries that list the suspects.`

---

### `docs/FORK-DELTA.md` (doc)

**Analog:** itself. Table rows with last column = requirement IDs; per R6.2 close these gaps: row 68 catalog (add lombok 1.18.48, spotless 8.10.4, DEPS-01), row 60 Pinned-by (CI-18, CI-19), new Test harness row (HARN-08 fixtures), row 63 VERSION and row 33 MBean (`v1.7.0_gg2` -> `v1.7.0_gg3`), BLD-14 row at `:65`/`:67` (re-establish for Lombok 1.18.48, closes 21-IN-03), intro lines 5 and 7, "For the next syncer" gg3 paragraph plus exemption-list sentence, ledger pointer at line 25 (add `L-42..L-48` via `22-BEHAVIOUR-LEDGER.md`, `L-49..L-51` via `23-BEHAVIOUR-LEDGER.md`).

**Known costs** (`## Known costs and open items`, line 135): numbered items, each "closed with what closed it" or "knowingly open". Item 6 replaces "was not measured" with the R2.2 measurement (Map key only, 26.9.19.9); add new items for 999.4 (D-12 scope) and 999.5 (D-13 scope). Raw numbers: follow E-40 (20-COMMAND-EVIDENCE) method, add `after gg3` rows for scope `src build.gradle.kts gradle .github` and for `src/main`; orientation figures at `644b1df8`: 129/22476/534 and 33/2245/145.

Cross-document consistency check (Pitfall 9, Phase 20 review WR-02): after writing, grep that the three docs agree on every shared number and sentence.

---

### `VERSION` and `.planning/scripts/fork-anchors.txt:41` (config)

**Analog:** current files. `VERSION` content: `v1.7.0_gg2` (single line). Anchor row, tab-separated (line 41):
```
VERSION	v1.7.0_gg2	1	BLD-07
```
Change both in ONE commit to `v1.7.0_gg3`. A subsequent `clean` build is required before grepping `ARTIFACT_VERSION = "v1.7.0_gg3"` in `build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java` (not a task input). Do not edit `verify-merge-invariants.sh` or its exemption list unless another anchored line gets bumped (then add a SHA-pinned row to `fork-anchor-exemptions.txt`).

---

### `24-COMMAND-EVIDENCE.md` (planning evidence)

**Analog:** `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-COMMAND-EVIDENCE.md`.

**Header pattern** (lines 1-8): `# Phase 24: Command evidence (D-03, D-04)`, bullets for base commit (`P24_BASE`), shell (macOS zsh, BSD grep; Gradle always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`), date UTC, and the Rule: "a `Matches text: no` verdict means the doc text gets fixed, never the evidence"; entries backing no doc text carry `Matches text: n/a (gate check)`.

**Entry pattern** (E-01, E-31): `### E-nn: title`, `Backs: <doc>, <item> (written in plan 24-0N)`, a fenced `sh` block with the exact command, a fenced `text` block with the output, prose on deviations, last line `Matches text: yes|no|n/a (gate check)`. Evidence quotes sentinels only, never real data. Entries needed: D-03 probe (with `SELECT version()` line from the test log), D-08 artifact diffs (zip SHA-256, `cmp.py` output, `-Xmaxwarns 100000` build with negative control), D-09 byte matrix, 999.4/999.5 query proofs on fixtures plus look-alikes, MBean/client-name probe (E-19 recipe), alert-grep scratch-dir run (E-30), rollback demo (E-31 adapted), raw numbers (E-40..E-42), suite gate (E-60), pre-push checks (E-61), rehearsal (E-62), rollback tag (E-70), merge (E-71), tag (E-72).

---

### Rollback script (scratch, not committed; text goes into evidence)

**Analog:** E-31 `rollback-demo.sh` (20-COMMAND-EVIDENCE.md lines 579-637). Adaptation points only:
- `pre-v1.7.0_gg2` -> `pre-v1.7.0_gg3` (local lightweight tag at the actual pre-merge tip, today `6473e24f`), at the `git worktree add ... 'pre-v1.7.0_gg2^{commit}'` line and every `git read-tree`/`diff` use.
- `BR=gg2-rollback-demo-$$` -> `gg3-rollback-demo-$$`; commit message `"Roll back v1.7.0_gg3"`.
- doc path `docs/ROLLOUT-v1.7.0_gg2.md` -> `docs/ROLLOUT-v1.7.0_gg3.md` (two uses: `ls-files --error-unmatch` and `git show "$RB^:..."`, plus the `grep -c '^## Item 6: Rollback'` heading, which must match the new note's item title).
- Usage `rollback-demo.sh <repo> <target-tip> <worktree-path>`; dry-run: `git worktree add --detach /tmp/gsd24/dry 6473e24f`, then `git -c user.name=demo -c user.email=demo@example.invalid merge --no-ff --no-edit <X>` giving D, print `MERGE-DRY-RUN-OK` when 2 parents, `D^1`=pre-merge tip, `D^2`=X, `git diff --quiet D X`. Run the script twice (dry-run merge, then the real M before the push). Record the stand-in-branch deviation (a linked worktree cannot check out `custom_build`).

---

### `24-MILESTONE-CLOSE-RECORD.md` (planning record)

**Analog:** `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md`.

**Structure to copy:** intro paragraph ("Only decisions are written here. No record value, host, credential or token appears in it."), then
- `## Carried review advisories`, one `### <NN>-REVIEW <ID>: title` per finding with `- **Reason:**`, `- **Re-open trigger:**`, `- **Source:**` (17 findings per D-06; 22-WR-01 row points at the D-03 test);
- a "fulfilled in Phase 24" section for 21-IN-03 and 23-IN-04;
- `## Manual checks after publication` with numbered `### n.` items each having `- **Steps:**`, `- **Why it matters:**`, `Outcome: ...` (D-11 reminders: `dependencies` label, PRs #4/#9, PRs #6-#8);
- `## Milestone audit` (run `/gsd-audit-milestone` after verification, before `/gsd-complete-milestone`);
- `## Housekeeping`. Per-finding variant used for 19-IN-04: `**Owner:**`, `**Trigger to re-open:**`, `**Pin:**`. D-06 asks for reason, owner and trigger.

---

### `24-REVIEW-DISPOSITION.md` and the 21/22/23 row flips (planning record)

**Analog:** `20-REVIEW-DISPOSITION.md` and the three live files.

**File shape:** YAML front matter then a table.
```yaml
---
phase: 24
review: 24-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "..."
open: 0
total: N
recorded: <ISO>
---
# Phase 24: Code Review Disposition
| Finding | Severity | Disposition | Source |
| WR-01 | warning | fixed | 54474bf5 (docs only, Phase 24; <what changed>; evidence E-nn) |
```
Allowed values: `open`, `fixed`, `skipped`, `deferred`; set `deferred` by hand with the reason in the Source cell (pointing at `24-MILESTONE-CLOSE-RECORD.md`), `|` inside a reason is escaped by the gate. A reviewer appending to an existing REVIEW.md must keep `### <CR|BL|WR|IN>-NN: title` headings.

**Flip rows in place** (both the YAML `disposition:` and the `open:` count line AND the table row, as the 21 file shows: `open: 7`, rows `| WR-01 | warning | open | - |`):
- 21: IN-03 -> `fixed` (Source: D-08 artifact build evidence), WR-01..03, IN-01, IN-02, IN-04 -> `deferred` (-> `24-MILESTONE-CLOSE-RECORD.md`).
- 22: WR-01 -> `deferred`, row points at the D-03 live test and the measured result; IN-01..05 -> `deferred`.
- 23: IN-04 -> `fixed` (closed through docs, the rollout 999.4 item); WR-01, WR-02, IN-01..03 -> `deferred`.
Precedent for wording of a flipped row from a prior release: 19's WR-01 row, "`<sha>` (what changed); previously deferred (20-MILESTONE-CLOSE-RECORD.md)".

---

### Publication procedure (no file; orchestrator inline)

**Analog:** 20-COMMAND-EVIDENCE.md E-60..E-72 (see 24-RESEARCH R9 for the extraction). Concrete commands and checks to copy:
- `git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0` (explicit refspec, scratch branch; exactly one run for X; never two branches at one SHA).
- `git push origin refs/tags/pre-v1.7.0_gg3:refs/tags/pre-v1.7.0_gg3` first, wait ~62 s, confirm the run count on the pre-merge SHA is unchanged.
- Merge in a throwaway worktree with `merge --no-ff -F <msg file>`; `MERGE-SHAPE-OK` = exactly two parents, `M^1` = pre-merge tip, `M^2` = X, `git diff --quiet M X`, X0..X planning-only, worktree clean; re-run rollback script on M.
- `git push origin refs/heads/custom_build:refs/heads/custom_build`, poll REST `actions/runs?head_sha=<sha>` and `/jobs` at 60-90 s (unauthenticated, 60/hour), late re-check (one run, none cancelled).
- Only after green: `git tag v1.7.0_gg3 <M>`, push by name, wait ~62 s, delete scratch branch, `FINAL-REFS-OK`.
- Anchors invariant: `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`, about 10 minutes, run in background.

## Shared Patterns

### Measured, not cited
**Source:** 20-COMMAND-EVIDENCE.md header rule; 24-RESEARCH Pattern 2.
**Apply to:** rollout note, CHANGELOG, FORK-DELTA. Every quoted command/SQL is run live with placeholders substituted; every measured statement names server version 26.9.19.9 and client V2; avoid the Phase 20 overclaims ("byte-identical", "new", "measured" where evidence showed "changed", "existing", "simulated").

### Gradle invocation and suite tally
**Source:** project MEMORY + 24-RESEARCH R8.
**Apply to:** every build/test step.
```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...
```
`testUnit` run with Docker stopped via `colima stop --profile vz` / `colima start --profile vz` (never bare). Tally `build/test-results/test/*.xml`; baselines: testUnit 838/836 passed/2 skipped, full 1277/1259/0/18 skipped (expect more tests, same 18 skips). OpenJ9 exit 255: delete core dumps individually, re-run once.

### Sentinel-only test and evidence text
**Source:** `WriterErrorPathLiveTest.SENTINEL = "SENTINELq7Zx41kV"`; `WriterValueFreeFailureUnitTest` (numeric sentinel at most 9 chars because BigInteger's NumberFormatException truncates longer values).
**Apply to:** the D-03 test, all evidence. No real data, host, credential or token in any committed file.

### Docs-only edits in the merged tree
**Source:** 24-CONTEXT locked block.
**Apply to:** rollout, CHANGELOG, FORK-DELTA. They ship in M; no `src/main` or existing-test edit; `.planning/` may change after the gate (X0..X planning-only).

### ROADMAP scripted edits
**Source:** MEMORY "Phase 11 orchestration gotchas".
**Apply to:** any scripted ROADMAP edit: slice to `### Phase 24:` first (`**Wave N**` headings repeat).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| 999.4 / 999.5 rollout items' prose | doc | n/a | New content; use item format of gg2 and queries/measurements from 24-RESEARCH R5 (re-prove live; `/` and tab predicates unproven) |
| D-15 shape-limited clean-stop paragraph | doc/procedure | n/a | Adapt gg2 item 1 (lines 28-92) and its KeeperMap query; prove the query live on a connector-created state table as E-17 did |
| Coverage-audit and XML-tally scripts | script | transform | Phase 20 scratch scripts are gone; re-write from E-41 text and R6.2/R8 rules |

## Metadata

**Analog search scope:** `src/test/java/com/clickhouse/kafka/connect/sink/**`, `docs/`, `CHANGELOG.md`, `VERSION`, `.planning/scripts/`, `.planning/phases/2[123]-*`, `.planning/milestones/v1.7.0_gg2-hardening-phases/20-release-publication/`.
**Files scanned:** about 20 read in part or whole.
**Tracked-source gate:** `git ls-files` confirmed for VERSION, CHANGELOG.md, docs/FORK-DELTA.md, docs/ROLLOUT-v1.7.0_gg2.md, fork-anchors.txt, the Phase 20 evidence and close-record files, and the test classes and helpers (`ClickHouseBase`, `LogCapture`, `ThrowableTexts`, `InMemoryDLQ`).
**Pattern extraction date:** 2026-10-20
