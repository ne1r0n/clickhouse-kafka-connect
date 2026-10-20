# Pitfalls Research

**Domain:** Follow-up hardening and CI hygiene on a production fork of the ClickHouse Kafka Connect sink (`custom_build`, `v1.7.0_gg1` -> `v1.7.0_gg2`)
**Milestone:** `v1.7.0_gg2-hardening`
**Researched:** 2026-10-10
**Confidence:** HIGH for the writer, exactly-once, Gradle and CI findings (read from this repo, measured on ClickHouse `26.9.14.10`, or read from the GitHub REST API on 2026-10-10). MEDIUM for the Ubuntu 26 migration dates and the Lombok key semantics (web sources, cross-checked once). The unauthenticated web tier is LOW by the `classify-confidence` seam, so nothing below rests on it alone.

Phase labels used in this file (the roadmapper will renumber them, probably 16-20):

| Label | Scope |
|-------|-------|
| **PUB-1** | Early publication: docs-only `--no-ff` merge of `sync/v1.7.0` into `custom_build`, CI green on that SHA |
| **WRITER** | 999.1 Nullable Enum/Tuple, empty-CDC skip on `bypassRowBinary=true`, WR-03, WR-04, T-13-19 |
| **DEBT** | Lz4 response-leg assertion, 999.2 duplicate lz4, Lombok warning, IN-01..IN-10 triage |
| **CI** | `tests.yaml`: Node 24 majors, `ubuntu-24.04`, SHA pins, `permissions:`, fork-count step |
| **REL** | `VERSION`, CHANGELOG, rollout note, rollback tag, second `--no-ff` merge |

First-hand evidence collected for this file (so the roadmapper does not have to redo it):

- **DESCRIBE lies about `Nullable(Tuple)` elements.** On `26.9.14.10`, `DESCRIBE ... SETTINGS describe_include_subcolumns=1` for `t Nullable(Tuple(a Int32, b String))` returns `t.null UInt8`, `t.a Nullable(Int32)`, `t.b Nullable(String)`. The wire format has no per-element markers: a row `((7,'x'), Enum16 'b'=300)` serialises as `00 | 07 00 00 00 | 01 78 | 00 | 2c 01`, and a NULL tuple followed by a NULL enum is `01 | 01` with no payload.
- **Response-leg compression is visible only as `NetworkSendBytes`.** For one `SELECT` of 24,320,383 result bytes: `compress=0` -> `NetworkSendBytes` 24,200,790; `compress=1`+lz4 -> 183,708; `compress=1`+zstd -> 51,901. No `Compressed*` ProfileEvent appeared for the response.
- **Annotated tag trap.** `gradle/actions` tag `v6.4.0` resolves through `git/ref/tags/v6.4.0` to a tag object (`b9bee63e...`), not a commit; the commit is `3f5f9ada...`. The `checkout`, `upload-artifact` and `setup-java` tags are lightweight.

---

## Critical Pitfalls

### Pitfall 1: Fixing 999.1 by propagating `isNullable` to Tuple writes an extra marker and desyncs the stream

**What goes wrong:**
The obvious fix is `builder.type(Type.TUPLE)...isNullable(isNull)` in `Column.extractColumn`. For `Nullable(Tuple(...))` the tuple's fields are then built from DESCRIBE sub-column rows, and those rows report **every element as `Nullable`** whatever the declared element type is (`t.a Nullable(Int32)` for `Tuple(a Int32)`). The writer then emits `[tuple NonNull marker][field marker][field value]`, but ClickHouse expects `[tuple NonNull marker][field value]`. The server reads the extra `00` as data, so the next column is shifted or the insert fails with `ATTEMPT_TO_READ_AFTER_EOF` / `CANNOT_READ_ALL_DATA`.

The single-field case is the dangerous one. Today `Array(Nullable(Tuple(a Int32)))` round-trips `[(5), NULL]` only by coincidence (the field's marker doubles as the tuple's null flag, 13-RESEARCH spike b3). A naive 999.1 fix **breaks the one shape that currently works** and turns it into silent garbage.

**Why it happens:**
`Table.updateParent` fills `parent.getTupleFields()` from DESCRIBE child rows, and `Column.extractColumn` takes nullability from the row's type string. `describe_include_subcolumns` output is a *subcolumn* view, not the stored type. Whether sub-columns of a `Nullable(Tuple)` come back `Nullable` also depends on `allow_nullable_tuple_in_extracted_subcolumns`, which ClickHouse documents as read **at server startup only**, so a production server may differ from the test container.

**How to avoid:**
- Derive element nullability from the **parent's type string** (split the tuple body with a splitter that respects both parentheses and quoted enum members), never from the DESCRIBE sub-column rows of a `Nullable(Tuple)`. Parse once when the `Column` is built, not per record (hot path, AGENTS.md).
- Land Enum and Tuple as **two commits**: Enum first (a one-line `isNullable(isNull)` in the Enum branch), Tuple second. Narrow the `hasNullableEnumOrTuple` regex in each commit so the WARN never claims more or less than the code supports.
- The proof must be a live twin-table matrix that compares against the server, not hand-written expectations (the pattern that found the Debezium `Timestamp` gap): `Nullable(Tuple)` with 1 field and with 3 fields, elements `Int32`, `Nullable(Int32)`, `String`, `Enum8`, `Nullable(Enum8)`, nested `Tuple`, in the positions top level, `Array` element, `Map` value, tuple field, `Array(Array(...))`; each with a null and a non-null value; each both with and without a `DEFAULT` column in the table (that switches the writer to the `RowBinaryWithDefaults` branch of `doWriteCol`).
- Add Docker-free `Column`/`Table` tests that feed **both** DESCRIBE shapes (`t.a Nullable(Int32)` and `t.a Int32`) and assert the same writer bytes, so the fix does not depend on the server's startup setting.
- Run a seen-red mutation: re-introduce the DESCRIBE-derived nullability and confirm the matrix goes red.

**Warning signs:**
`ATTEMPT_TO_READ_AFTER_EOF`, `CANNOT_READ_ALL_DATA`, or a value appearing in the *next* column; any test that passes only for 1-field tuples; a fix that touches `Table.java` less than it touches the parsing of the type string.

**Phase to address:** WRITER (deepest research flag of the milestone; plan the Tuple half as its own plan).

---

### Pitfall 2: The Enum fix changes stored values, flips a validation gate, and makes four pins and a WARN stale

**What goes wrong:**
`Nullable(Enum8)` is written without its null marker today. For Enum8 the value byte is read as the null flag, so a real value is stored as `NULL` (no error). For Enum16 the low byte is the flag and the high byte shifts the following columns. After the fix, the same input stores the real value. Four consequences are easy to miss:

1. **Stored values change for running pipelines.** Historical rows stay `NULL`/shifted, new rows are correct. Materialized views, alerts and queries that filtered `IS NULL`, and anything keyed on that column, change behaviour at the cut-over.
2. **`validateDataSchema` flips.** It tests `!isNullable && !hasDefault`; a record that omits a `Nullable(Enum)` column used to fail validation ("Table column name [x] was not found") and will now be accepted and write `NULL`. That is a second, silent behaviour change.
3. **Pins that document the defect go red.** `NestedNullRowBinaryLiveTest` (D-16, around line 417) and `NestedNullableCompositeWriteTest` (D-16, around line 264) assert a `DataException` "because Column reports the Tuple non-nullable". The WARN pins (`ClickHouseHelperClientForkUnitTest`, `ClickHouseHelperClientForkLiveTest`) assert a warning that is no longer true. AGENTS.md forbids deleting or weakening tests.
4. **Comments and messages go stale.** The TUPLE case comment "Column reports a Tuple non-nullable even for Nullable(Tuple(...)) (D-16)" and `renderColumnType` (which now prints `Nullable(...)`, changing the message text any pin matches).

**Why it happens:**
The defect was documented as an accepted cost, so five artefacts (code comment, WARN, pins, FORK-DELTA Known costs item 1, rollout item 4) now describe it. Fixing code without touching all of them leaves the repo contradicting itself.

**How to avoid:**
- Flip each defect-pinning test to the new truth with a dated rationale comment (the R-01 precedent), never delete it. Grep list: `Nullable(Enum`, `Nullable(Tuple`, `999.1`, `D-16`, `hasNullableEnumOrTuple`, `L-11`, `L-13`, `L-21`.
- Decide the WARN explicitly: remove it only if no shape remains unsupported. If any shape stays unsupported (for example an exotic nesting), narrow the regex and the text instead. Either way update `.planning/scripts/fork-anchors.txt`: it has a row for `static boolean hasNullableEnumOrTuple(` (backs-ID PUB-02). A commit that removes the method without the row change fails the anchor grep.
- Record a new behaviour-ledger entry (L-23 onwards) for the value change and the validation flip, and write the rollout prose from it.
- Keep enum member names simple in the new matrix. `Column.extractEnumValues` cuts the member list at the first `)` (`indexOf(")")`), so a member such as `'a)b'` is mis-parsed. That is a separate pre-existing bug; a matrix that trips it will be blamed on 999.1.

**Warning signs:**
A green suite with an unchanged WARN test; `git grep 999.1` still returns "accepted cost" wording after the fix; FORK-DELTA Known costs still lists item 1 as open.

**Phase to address:** WRITER (code and pins), REL (ledger, FORK-DELTA, rollout, CHANGELOG).

---

### Pitfall 3: Writer changes alter the bytes for an offset range that the old jar may already have sent

**What goes wrong:**
Exactly-once relies on a retry sending the same block. Three things in this milestone change the bytes for an unchanged offset range: the 999.1 marker bytes, the empty-CDC skip (rows removed from the block), and WR-03/WR-04 (only failure paths). A connector that crashed after `BEFORE_PROCESSING` under `v1.7.0_gg1` and restarts under `v1.7.0_gg2` retries a different block.

The mechanism matters for the rollout wording. `QueryIdentifier.getDeduplicationToken()` returns `topic-partition-minOffset-maxOffset` and every V2 insert path (RowBinary and both JSON paths, `ClickHouseWriter` around lines 1239, 1490, 1638) sets it as `insert_deduplication_token`. So the retry is deduplicated by **token, not by block hash**: if the first attempt landed, the new block is dropped and the **old encoding stays** (stale `NULL`s, or phantom `{}` rows); if it did not land, the new block lands. `docs/ROLLOUT-v1.7.0_gg1.md` item 1 describes the failure as "rows can be written twice" on the basis of block hashing, an inference its own text marks as unmeasured. Do not copy that sentence into the gg2 note without re-deriving it.

**Why it happens:**
The invariant is stated in AGENTS.md ("batch must be byte-identical") and is easy to read as "within one process". It also has to hold across a version change, which only an operator procedure can guarantee.

**How to avoid:**
- Make the rollout note's first item the same drain procedure as gg1 item 1 (stop the connector, confirm no `BEFORE_PROCESSING` rows for *its own topics* in the shared KeeperMap table, then upgrade), and list the gg2 routes that re-encode: RowBinary with `Nullable(Enum|Tuple)` columns, and `bypassRowBinary=true` with Debezium CDC records.
- State the dedup consequence as "token-based" and mark it measured or unmeasured honestly; if it is stated as fact, probe it on `latest` first.
- Do not invent a new in-process rule: keep every skip/encoding decision a pure function of the record and the table metadata, never of retry state, time or config that can change between attempts.

**Warning signs:**
A rollout note that says "no drain needed" for the writer changes; a skip predicate that reads mutable state.

**Phase to address:** WRITER (determinism), REL (rollout wording).

---

### Pitfall 4: The empty-CDC skip is implemented at the wrong layer or with the wrong predicate

**What goes wrong:**
Three variants of the same mistake:

1. **Filtering the `Record` list before `Processing`.** The offset range, the `BEFORE`/`AFTER` flags, the dedup token and `getInsertedOffsetsSnapshot()` (what `preCommit` returns when `reportInsertedOffsets` is on) are derived from the records. Dropping records earlier changes `minOffset`/`maxOffset`, so the batch composition differs on retry, and a partition whose whole batch was filtered never advances its committed offset (permanent lag, redelivery after restart).
2. **Using "the cleaned map is empty" as the predicate.** On the `bypassRowBinary=true` route `cleanupExtraFields` can empty a legitimate schemaless record whose fields are all unknown; the server then writes a defaults-only row, which is intended behaviour. The skip must key on the **CDC record itself** (`SchemaType.DEBEZIUM_CDC` and an empty `getJsonMap()`, i.e. `op=t` or a null before/after struct), evaluated **before** cleanup.
3. **Forgetting the all-skipped batch.** In `doInsertJsonV2` and `V1` the HTTP insert is started before `writeRecordsJson` runs, so a batch where every record is skipped sends an empty body. Nothing in the current tests pins that case (the metadata-free skip has no all-skipped test).

**Why it happens:**
The metadata-free branch already has a `continue` inside `writeRecordsJson`, so the hoist looks like a three-line move (13-REVIEW WR-01 even suggests the snippet). The surrounding contract is invisible from that function.

**How to avoid:**
- Hoist the guard inside `writeRecordsJson` (after the range is fixed), keyed as in variant 2, and keep the metadata-free branch behaving byte-identically.
- Pin, live: (a) a mixed batch (real, `op=t`, real) lands exactly the real rows; (b) an all-skipped batch completes with zero rows and no exception; (c) with `exactlyOnce=true` the state ends `AFTER_PROCESSING` with the full range, and with `reportInsertedOffsets` the skipped offsets are reported; (d) the same batch retried produces an identical body (assert on the serialised bytes, not on row counts); (e) a schemaless record whose fields are all unknown still lands a defaults-only row.
- If the empty-body insert misbehaves on client V1 or V2, fix it as a separate, explicitly approved change (short-circuit before opening the request); do not fold it into the skip silently.
- Remember the asymmetry the skip creates is deliberate and already documented for RowBinary versus JSON (FORK-DELTA row for the nested-null split); update the FORK-DELTA row for the metadata-free path (L-06) so it no longer says "on this route".

**Warning signs:**
Offsets for a partition stop advancing in `reportInsertedOffsets` mode; `Written Rows: [0]` followed by a retry loop; a diff that touches `ProxySinkTask`, `Processing` or `Record` for a writer-level fix.

**Phase to address:** WRITER.

---

### Pitfall 5: WR-03/WR-04 are "just messages" but they sit in classification code

**What goes wrong:**
`Utils.handleException` classifies by exception **type, ClickHouse error code and root cause**, not by message, so WR-03 (`RuntimeException()` -> a message) cannot flip retriable status by itself. Two message-dependent spots exist, and both interact with these fixes:

- `ClickHouseWriter.doInsertRawBinary` (V1 branch, line ~1177): `e.getMessage().contains(code)` runs **before** the null check and matches `"ClickHouseException: Code: 33"` / `"Code: 131"` as substrings. WR-04 makes it null-safe. Exceptions with a null message previously died with an NPE inside the catch (non-retriable, original error lost). After the fix they reach `Utils.handleException`, where a null-message `SocketTimeoutException` is now correctly **retriable**. That is the right outcome and still an operator-visible classification change; record it.
- `Utils.handleException`, `IOException` branch: `rootCause.getMessage()` is dereferenced with `msg.indexOf(...)` and has the same latent NPE for a null-message `IOException`. Fixing WR-04 and leaving this one gives a half-fixed family.

For WR-03 the new text must be data-free and must not collide with the substring matchers. The failing path is `fieldExists == false` (a missing non-nullable field with no default), a different branch from the top-level "null in non-nullable column" `RuntimeException` that L-12/D-11 pin; do not unify them. `Processing.doInsert` wraps every failure in `RuntimeException(queryId, e)`, so `e instanceof DataException` in `handleException` is rarely true; tests must walk the cause chain, as the Phase 13 tests do.

**How to avoid:**
- Use `DataException` with a constant format (column name and type only; schema, not data), mirroring `buildNestedNullMessage`, and drop the duplicate `LOGGER.error` + throw pair if the message now carries the information.
- Pin through a full `ClickHouseSinkTask.put()`: message visible in the thrown chain, `errors.tolerance=all` routes the batch to the DLQ, `errors.tolerance=none` stops the task, and a null-message `SocketTimeoutException` is now retriable.
- Add the null-message hardening for the `IOException` branch in the same commit or record why not.

**Warning signs:**
A column literally named like an error code appearing in a message that is fed to a `contains` matcher; a test that asserts `instanceof DataException` on the top-level thrown exception.

**Phase to address:** WRITER; CHANGELOG entry in REL.

---

### Pitfall 6: Masking the DateTime log loses diagnosability, or masks only half the leak (T-13-19)

**What goes wrong:**
There are two upstream-identical ERROR logs in `doWriteDates`. Line ~461 logs `value.getObject()`. Line ~515 logs `value.getObject()` **and** `e.getMessage()`, and a `DateTimeParseException` message contains the offending text (`Text '...' could not be parsed at index 0`). Masking only the first argument leaves the value in the log. After masking, the only other message is the `DataException` "(Potentially) Not implemented conversion from STRING to DateTime", which names **no column**, even though `doWriteDates` receives the column name. An operator is left with an ERROR that says nothing.

**How to avoid:**
- Mask both arguments at both sites. Log: column name, target type, source Connect type, topic/partition/offset where available, the exception **class** (never its message), and the value **length** only (no prefix or suffix: a prefix is still data).
- Prove it with a log-capture test using a unique sentinel string as the field value; assert the sentinel is absent from every captured event (message, throwable message and the stack's `getMessage`), and assert the column name is present. A negative-only test passes when the log line is simply deleted.
- Keep the patch to the two lines plus one helper, add a `fork-anchors.txt` row, and add a FORK-DELTA row (these lines are byte-identical to upstream `v1.7.0`, so this is new fork delta by definition).
- `grep` `ClickHouseWriter` for other `LOGGER.(error|warn)` calls that interpolate `value.getObject()` or `getMessage()` of a parse exception before closing the threat; do not widen the fix beyond what is found without asking.

**Warning signs:**
The ERROR line becomes shorter but the DLQ header or Connect status still carries the text (the `DataException` message is data-free today; keep it so).

**Phase to address:** WRITER.

---

### Pitfall 7: A response-leg Lz4 assertion that mirrors the request-leg one is vacuous, and a version-pinned one flakes on `latest` drift

**What goes wrong:**
The natural copy of `CompressedReadBufferBlocks > 0` for the response is `CompressedWriteBufferBlocks`, but on the HTTP response path ClickHouse `26.9.14.10` records **no `Compressed*` ProfileEvent** (measured). An assertion on it is either always zero (red) or, if written as `>= 0`, always green. Reading the data back also proves nothing: it succeeds with `compressServerResponse(true)` silently ignored. `latest` has moved `26.9.12.8` -> `26.9.13.15` -> `26.9.14.10` during the last two milestones, so anything keyed on a server version or a default (the 26.9 ZSTD default broke client `0.9.6` once already) will flake.

**How to avoid:**
- Assert on **wire size versus result size** for the `SELECT`'s own query id: `ProfileEvents['NetworkSendBytes']` from `system.query_log` against `result_bytes`. The test payload is highly compressible (repeated lorem ipsum, about 4.8 MB), so a loose bound such as `NetworkSendBytes < result_bytes / 4` is stable across versions (measured ratios: plain 0.995, lz4 0.0076, zstd 0.0021).
- Set an explicit query id on the read (the current test sets one only on the insert), assert exactly one `QueryFinish` row, and keep the existing `getSetting('network_compression_method') = 'lz4'` assertion. Ratio alone cannot tell lz4 from zstd; if that distinction matters, a counting proxy can check the method byte that follows each 16-byte block checksum (0x82 = LZ4, 0x90 = ZSTD; this is ClickHouse's block format, not measured here).
- Do not assert a server version, `Content-Encoding`, or an absolute byte count.
- Seen-red mutation: set `compressServerResponse(false)` and confirm the new assertion fails.
- When this class goes red on unchanged code, check the `latest` image date and `SELECT version()` before reading the diff (memory: `gsd-phase11-orchestration-gotchas`).
- Update the class Javadoc and `@DisplayName` ("on both legs") only once the assertion exists.

**Warning signs:**
An assertion that passes in the first run without a red step; a bound that is tight against today's ratio.

**Phase to address:** DEBT.

---

### Pitfall 8: The 999.2 exclusion is applied too widely, or leaves the test harness order-dependent

**What goes wrong:**
`dependencyInsight` on `testRuntimeClasspath` shows `org.lz4:lz4-java:1.8.0` arriving through `org.apache.kafka:kafka-clients:7.9.1-ccs` (the Confluent test stack), beside `at.yawk.lz4:lz4-java:1.12.0`; both provide `net.jpountz.lz4`, so which one loads depends on classpath order. (The same graph resolves `kafka-clients` to `7.9.1-ccs` although `connect-api:4.1.1` asks for `4.1.1`; do not touch that here.) Two ways to get it wrong:

- A global `configurations.all { exclude(group = "org.lz4", module = "lz4-java") }` also hits `runtimeClasspath`, the `clickhouseDependencies` configuration behind `prepareConfluentArchive`/`confluentJar`, and the `integrationTest` suite, none of which need to change. The integration tests that need Confluent are not run in CI (cloud/Confluent infrastructure), so a break would be invisible until someone runs them.
- Excluding without proving that `at.yawk` stays: the fork must remain on `testRuntimeClasspath` (it comes from `implementation(libs.lz4.java)`), otherwise `net.jpountz.lz4` vanishes entirely.

**How to avoid:**
- Scope the exclusion to `configurations.testRuntimeClasspath` only.
- Before/after evidence, committed in the plan summary: `dependencyInsight --dependency lz4` for `testRuntimeClasspath`, `integrationTestRuntimeClasspath`, `runtimeClasspath` and `clickhouseDependencies`; the last three must be byte-identical, and `testRuntimeClasspath` must show only `at.yawk`.
- `compileIntegrationTestJava` stays in the gate; `Lz4RoundTripLiveTest.lz4FactoryLoadsFromTheMaintainedFork` must still pass and now passes deterministically, so remove the Javadoc sentence about the old artifact (its "Pitfall 2" reference) in the same change.
- Full suite twice (`clean test`, then `test --rerun`; Gradle's up-to-date check otherwise replays run 1).

**Warning signs:**
`NoClassDefFoundError: net/jpountz/lz4/...` in a Kafka-backed test; a changed `confluentJar` contents list.

**Phase to address:** DEBT.

---

### Pitfall 9: The Lombok config edit silently changes the generated Jackson code or drops the JaCoCo filter

**What goes wrong:**
Lombok `1.18.44+` warns "Ambiguous: Jackson2 and Jackson3 exist" for `@Jacksonized` until `lombok.jacksonized.jacksonVersion` is configured. Exactly one class uses it: `ClickHouseFieldDescriptor`, which parses every DESCRIBE row with a Jackson **2** `ObjectMapper`. If the key is set to `3` (or the warning is "fixed" by switching the annotation), Lombok generates `tools.jackson` builder annotations, the class still compiles, and at runtime Jackson 2 finds no creator, so **every table discovery fails**. Two further traps: rewriting `lombok.config` instead of appending drops `lombok.addLombokGeneratedAnnotation = true`, which the JaCoCo reports rely on to exclude generated code (fork-class coverage numbers shift), and `config.stopBubbling = true` must stay.

**How to avoid:**
- Append only, using the list-key form: `lombok.jacksonized.jacksonVersion += 2`.
- Verify by diffing `javap -v -p` of `ClickHouseFieldDescriptor` and its builder before and after: the annotations on the current build are `com.fasterxml.jackson.databind.annotation.JsonNaming` and `JsonPOJOBuilder`/`JsonDeserialize`; they must be identical.
- Add or confirm a Docker-free test that `ClickHouseFieldDescriptor.fromJsonRow` parses a representative row including `is_subcolumn`.
- Re-run `jacocoForkReport` and compare the 15-class table to the previous run.

**Warning signs:**
The warning disappears but `ClickHouseHelperClientForkUnitTest` or any `describeTable` test fails; the coverage summary's fork-class percentages move without a code change.

**Phase to address:** DEBT.

---

### Pitfall 10: SHA pinning done wrong (tag object versus commit, no update path, wrong scope)

**What goes wrong:**
- **Tag-object SHA.** `gradle/actions` `v6.4.0` is an annotated tag: `git/ref/tags/v6.4.0` returns the tag object `b9bee63e...`; the commit is `3f5f9ada...` (read from `git/tags/<sha>`). Pinning the former fails at job start with an action-not-found error, and only on the run that first uses the workflow. The other three actions use lightweight tags, so the same procedure yields correct values for them and a wrong one here.
- **Mutable-major comfort.** Pinning "whatever `v4` points at today" makes the next upgrade a silent surprise. Pin a concrete release and keep the version in a trailing comment so a bot can bump it.
- **No update path.** `.github/dependabot.yml` has only the `gradle` ecosystem. Without `github-actions`, pins rot and the Node deprecation annotation recurs in two years. The file is upstream's, so adding the ecosystem grows FORK-DELTA by one row; that is the right trade, but make it consciously.
- **Scope creep.** Five other workflows exist (`release`, `pre-release`, `increment-version`, `integrationTests`, `test-head`). Only `tests.yaml`, `pre-release.yaml` and `test-head.yaml` are registered on the fork (`test-head` is `disabled_inactivity`; it also calls `tests.yaml` as a reusable workflow with inputs `tests.yaml` does not declare, so it cannot work). Editing upstream-owned workflows buys nothing and adds rebase conflicts.
- **Impostor commits.** A SHA can exist in the repository's fork network without being on the release. Check the SHA against the tag, not just that it resolves.

**How to avoid:**
- Pin `tests.yaml` only; state in FORK-DELTA that the other workflows are intentionally untouched.
- Resolve every pin the same way and record the table in the plan: for each action, `GET /repos/{o}/{r}/git/ref/tags/{tag}`; if `object.type == "tag"`, follow `git/tags/{sha}` to the commit. As of 2026-10-10 the latest releases are `actions/checkout` v7.0.1 (`3d3c42e5...`), `actions/upload-artifact` v7.0.2 (`cf430e03...`), `actions/setup-java` v6.0.1 (`de7274f0...`), `gradle/actions` v6.4.0 (`3f5f9ada...`); re-resolve at implementation time. `action.yml` of all four now declares `using: node24` (read from the API); only `checkout@v4` and `upload-artifact@v4` are still Node 20.
- Add a Docker-free guard (a few lines in the existing invariants tooling or a unit test over the YAML): every `uses:` line in `tests.yaml` matches `@[0-9a-f]{40}` and carries a `# vX.Y.Z` comment.
- Rehearse the changed workflow on a pushed scratch branch **before** the release merge, because errors in `uses:` only appear on the first run.

**Warning signs:**
"Unable to resolve action" on the first run; a pin whose comment version does not match `git/ref/tags/<version>`.

**Phase to address:** CI.

---

### Pitfall 11: The `permissions:` block, runner pin and fork-count step break something unexpected

**What goes wrong:**
- `permissions: contents: read` is sufficient for this workflow (checked against the `setup-gradle` docs: only the PR-comment feature needs `pull-requests: write` and dependency submission needs `contents: write`; both are off by default). Step summaries and `::notice` annotations are file/stdout commands, and artifact upload and the Gradle cache use the runtime token, so none depends on `GITHUB_TOKEN` scopes. The mistake is adding `checks: write` "just in case", or omitting the block on a repository whose default token is read-write.
- `runs-on: ubuntu-24.04` is a **no-op today** (`ubuntu-latest` is Ubuntu 24.04 until the gradual switch to 26.04 between 2026-10-19 and 2026-11-19), so it cannot be verified as a change; confirm it from the run's "Set up job" log (`Image: ubuntu-24.04`). CI uses the runner's native Docker, not colima, so local `vz` quirks (`DOCKER_HOST`, never a bare `colima start`) do not transfer in either direction.
- The "Test fork count" step is a **prediction** (`nproc` in shell, before Gradle runs), not what Gradle uses (`Runtime.availableProcessors()`, cgroup-aware, evaluated in `build.gradle.kts`), and it duplicates the formula. Replacing it with a Gradle log line fails the original purpose (CI-09): job logs are not readable without admin rights, so the value must still reach an **annotation** (`::notice ...` printed by Gradle on stdout is parsed by the runner) or the step summary, and it must be printed by the `test` task only (the `withType<Test>` block also configures `testUnit` and `integrationTest`). If the print is placed in a closure that captures `project`, check the build still works under the configuration cache if anyone enables it.

**How to avoid:**
- Put `permissions: contents: read` at workflow level, nothing else; confirm the run's "Set up job" log lists `Contents: read` and `Metadata: read`.
- Keep the notice annotation; read the value from Gradle (`maxParallelForks` of the `test` task) and emit it once.
- Verify with the same locator as before: pushed commit `head_sha` + workflow `path`, exactly one run, none cancelled, both artifacts present, annotation present.

**Warning signs:**
A missing "Test fork count" annotation on the run; `nproc` and Gradle disagreeing in the new output (the point of the change).

**Phase to address:** CI.

---

### Pitfall 12: Publication mechanics: CI cancellations, rollback tag and the unpublished tail

**What goes wrong:**
- **Same SHA, two refs.** The concurrency group is `workflow-<sha>` with `cancel-in-progress`. Two branches at one SHA cancel each other; tag pushes no longer start runs, **but only if the workflow at the tagged commit has the branch-only trigger**. The observed run `38051333950` was started by pushing `pre-v1.7.0-sync` (`c61d90ce`), which predates CI-08. Any new rollback tag must point at a commit that contains the branch-only trigger (`f328fd21` or later), or it will run CI.
- **Wrong verification tool.** `.planning/scripts/verify-merge-invariants.sh` is the **upstream-sync** gate: it requires two parents with the second equal to an upstream release tag. It will fail on a docs-only or hardening merge and must not be "fixed". The equivalent checks for this milestone are: two parents, first parent = `origin/custom_build`, merged tree equals the tip of the merged branch, and `git diff --name-only origin/custom_build <tip>` limited to the expected paths (PUB-1: only `.planning/` and the two docs; today the only non-`.planning` paths are `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg1.md`).
- **Local noise in the merge.** `.planning/state.json` is modified and `.planning/.gsd-ws-arg` is untracked; neither belongs on `origin` (the previous publication excluded the latter on purpose).
- **Unpublished tail again.** `custom_build` is protected, so bookkeeping commits made after the merged tree stay local. Last time that stranded `505e4981`. This time the rollout note and CHANGELOG must be inside the merged tree, i.e. written and reviewed before the publish gate.
- **Rollback point.** The gg2 rollback tag must be created before the second merge and point at the first merge `M1` (not at `f328fd21`, which the PUB-1 merge will already have moved past). The `git revert -m 1` consequence (re-merge brings nothing back until the revert is reverted, 15-REVIEW IN-08) belongs in the rollout note.
- **CI evidence.** `gh` is not installed; the unauthenticated REST API allows 60 requests per hour, so verification scripts should fetch once and tolerate a 403.

**How to avoid:**
- Push exactly one ref at the published SHA; push tags only after that SHA's own run is green; then re-list runs for the SHA.
- Write a small milestone-specific invariant check (above) and run it before the push; keep the sync script untouched.
- Treat approvals as non-relayable: the executor refuses a human gate answered through another agent (memory), so the orchestrator runs post-gate tasks inline. Re-force the dispatch-isolation sentinel to `none` as the last action before every executor dispatch and never query `--json` afterwards (memory: sentinel clobber, 10-minute staleness).
- When editing `ROADMAP.md` with scripts, slice to the target `### Phase N:` section first; the `**Wave N**` anchors repeat per phase and truncated the file once (memory).
- Reviewers appending to an existing `*-REVIEW.md` must use `### ID: title` headings or the disposition step drops findings silently (memory: 6 of 13 recorded last time).

**Warning signs:**
Two runs on one SHA; a cancelled sibling; a merge diff touching anything outside the allow-list; `git status` showing `.planning/state.json` in the merge.

**Phase to address:** PUB-1 (first merge), REL (second merge).

---

## Moderate Pitfalls

### Pitfall 13: The info-finding triage (IN-01..IN-10) grows the fork delta for no benefit

**What goes wrong:**
Fixing every Info item mechanically touches upstream-identical lines (for example the `catch (Exception e)` in `doWriteColValue`, the `writeRecordsJson` loop shape, constants near upstream code) and rewrites published release text. FORK-DELTA stands at 95 files wide / 31 in `src/main`, +13,587 / +1,826 insertions at `v1.7.0_gg1`; every added upstream-identical hunk is rebase cost.

**How to avoid:** Apply one rule: fix an item only if it is in fork-owned code or documentation and does not touch a line byte-identical to upstream `v1.7.0`; otherwise close it with a written reason in the `REVIEW-DISPOSITION.md` (set `deferred` by hand and put the reason in the Source cell). A suggested split for the 15-REVIEW set:

| Item | Suggested disposition |
|------|----------------------|
| IN-01 (WARN fires on routes that cannot hit the defect), IN-02 (V1 hook unexercised), IN-03 (warned-key set), IN-04 (wording not pinned), IN-05 (constants mid-class) | Close as **obsolete by 999.1** if the WARN is removed; if any residual shape keeps the WARN, fix IN-01/IN-03/IN-04 in that code and close IN-02/IN-05 with the existing reasons |
| IN-06 (edge-value pins) | Fix: add one `@CsvSource` row such as `io.debezium.time.NanoTimestamp, 123, 1.23E-7`; test-only, additive |
| IN-07, IN-08, IN-09, IN-10 | Fix in the **gg2** rollout/FORK-DELTA/CHANGELOG, and run every command quoted in them (`grep -rc` prints per-file counts, not totals) |

Do not rewrite the published `1.7.0_gg1` CHANGELOG section; correct forward in the `1.7.0_gg2` section. Compare the four FORK-DELTA `git diff --shortstat` numbers before and after and explain every delta row in the summary.

**Phase to address:** DEBT (triage), REL (docs).

### Pitfall 14: Docs-only first merge accidentally carries code or leaves the dangerous text live

**What goes wrong:** If `sync/v1.7.0` has gained anything outside `.planning/` and the two docs, the "docs-only" claim is false and CI should not be the only check. Conversely, publishing the corrected rollout doc but leaving `TRUNCATE` guidance anywhere (CHANGELOG, FORK-DELTA, a planning copy linked from the published docs) keeps the hazard reachable.

**How to avoid:** Run the allow-list diff above; `grep -rn "TRUNCATE" docs CHANGELOG.md` on the merged tree; execute the item 1 and item 2 SQL against a real server once more (the 15-REVIEW reviewer did this and found the shared-table problem); read the diff as an operator would before the approval gate.

**Phase to address:** PUB-1.

### Pitfall 15: `hasDefault` and `RowBinaryWithDefaults` hide a second marker layout

**What goes wrong:** Tables with any `DEFAULT` column switch the connector to `RowBinaryWithDefaults`, where top-level columns get an extra "use default" byte. Matrix cells written only for plain tables prove nothing for these.

**How to avoid:** Each 999.1 matrix cell runs against a table that has a `DEFAULT` column and one that does not.

**Phase to address:** WRITER.

### Pitfall 16: Release bump is itself an operator-visible change

**What goes wrong:** `VERSION` feeds `Version.ARTIFACT_VERSION`, which is both the JMX `version=` segment and the client name `clickhouse-kafka-connect/<version>`. Bumping to `v1.7.0_gg2` changes every MBean name and the HTTP user agent again, exactly the issue gg1 item 7 describes. `generateVersionFile` is not an input of the task, so building after editing `VERSION` needs `clean`, and the `VERSION` row in `fork-anchors.txt` moves in the same commit.

**How to avoid:** Restate the item 7 advice (match `version=` by pattern; use the regex form `version=[^,]*` for regex matchers, the `version=*,*` form only for `ObjectName` patterns) in the gg2 note; run `clean` before the version gate; update the anchor row in the bump commit.

**Phase to address:** REL.

### Pitfall 17: `latest` drifts mid-milestone

**What goes wrong:** ClickHouse `latest` moved `26.9.12.8` -> `26.9.13.15` -> `26.9.14.10` in the last two milestones. The Nullable(Tuple) matrix relies on `enable_nullable_tuple_type` being `1` by default (it is on `26.9.14.10`; the old name `allow_experimental_nullable_tuple_type` is an alias), and the DESCRIBE shape depends on a startup-loaded setting. A future `latest` can change either and turn a correct fix red.

**How to avoid:** Record `SELECT version()` in every gate; do not set either setting name in test DDL; when a gate is red on unchanged code, check the image creation date first; keep skips at the named 18 (no `Assumptions` escape hatch for "server too new").

**Phase to address:** WRITER, DEBT, REL.

---

## Minor Pitfalls

### Pitfall 18: Spotless and imports on touched files
`./gradlew spotlessApply` rewrites whole files if it was not clean before; on upstream-identical files that produces diff noise. Run `spotlessCheck` first and apply only to fork-owned or already-clean files, or hand-format the hunk.

### Pitfall 19: Stale Javadocs after each fix
Last milestone found four stale Javadocs after the fact. Grep each changed behaviour's old wording (`D-16`, "accepted cost", "both legs", "also carries the old") and fix them in the same commit.

### Pitfall 20: Reviews run after the gate
Release documents got their first review after publication last time. Put a docs review of the rollout note and CHANGELOG into the plans before the approval gate, and spell out the `### ID: title` heading rule in the review prompt.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Propagate `isNullable` to Tuple from DESCRIBE rows | Two-line fix | Stream desync; breaks the one working single-field shape | Never |
| Delete the D-16 pins instead of flipping them | Green suite faster | Violates AGENTS.md; erases the record of why behaviour changed | Never |
| Keep the WARN after 999.1 "to be safe" | No test edits | Misleading warning on every startup; stale rollout text | Only for a shape that is genuinely still unsupported |
| Skip `Record`s in `ProxySinkTask` rather than in the writer | Simple filter | Changes offset range and dedup token | Never |
| `configurations.all { exclude(...) }` for 999.2 | One line | Changes shipped/Confluent classpaths unseen | Never |
| Pin actions by moving major tag | No lookup | Silent action changes, defeats T-14-16 | Never |
| Pin by `git/ref` SHA without checking `object.type` | Fast | Annotated-tag SHA fails at runtime (real for `gradle/actions`) | Never |
| Edit upstream workflows to pin them too | Uniform repo | Rebase conflicts; those workflows do not run on the fork | Only if a later sync makes them live |
| Fix an Info finding on an upstream-identical line | Tidier code | Permanent rebase cost | Only for a security/data-correctness item with a FORK-DELTA row (T-13-19) |
| Re-run a flaky Lz4 test until green | Quick | Hides a vacuous assertion | Never |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| ClickHouse DESCRIBE with sub-columns | Trusting `Nullable` on sub-column rows of a `Nullable(Tuple)` | Use the parent's declared type string; test both DESCRIBE shapes |
| ClickHouse RowBinary | Assuming element markers inside `Nullable(Tuple)` | Wire is `flag + raw elements`; element markers only where the element type is itself `Nullable` |
| ClickHouse dedup | Describing retry dedup as block-hash based | The connector sets `insert_deduplication_token = topic-partition-min-max` on all V2 inserts |
| ClickHouse `query_log` | Mirroring request-leg ProfileEvents for the response | Use `NetworkSendBytes` against `result_bytes` for the read's own query id |
| Kafka Connect `preCommit` | Filtering records before `Processing` | Skip inside the writer so ranges, state and reported offsets still advance |
| Gradle capability resolution | Assuming the capability rule removes the POM-only `org.lz4` artifact | Scope an explicit exclusion to `testRuntimeClasspath` and prove the other configurations unchanged |
| Lombok `@Jacksonized` | Choosing Jackson 3 because it is "newer" | Match the runtime mapper: `+= 2` |
| GitHub REST API | Treating `git/ref/tags/<t>` as a commit SHA | Check `object.type`; dereference annotated tags |
| GitHub Actions concurrency | Pushing a branch and a tag/second branch at one SHA | One ref per SHA; tags after the run |
| GitHub Actions tag pushes | Assuming branch-only trigger covers old tags | The workflow at the tagged commit decides; tag a post-CI-08 commit |
| Colima / Testcontainers | Using `colima start` or `colima status` without `-p vz` | Profile is `vz`; `DOCKER_HOST` is already exported |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Parsing the Tuple type string per record | Throughput drop in RowBinary path | Parse once when building `Column` | Any large batch (hot path in AGENTS.md) |
| Per-record log formatting for the DateTime masking helper | GC churn on error floods | Log only on the failure path; no string building before the check | Streams with many unparseable timestamps |
| Empty-body insert per all-skipped batch | Extra HTTP round trips for truncate-heavy topics | Pin that it succeeds; short-circuit only with approval | Topics dominated by `op=t` |
| Fork-count print placed in every `Test` task | Triplicate output / annotations | Emit from the `test` task only | Every CI run |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Masking only `value.getObject()` and not `e.getMessage()` | Record data still logged at ERROR | Mask both; sentinel-based log-capture test |
| Logging a prefix or suffix of the masked value | Partial disclosure | Log length only |
| New `DataException` messages built from record data | Data in Connect status and DLQ headers | Column and type names only (metadata) |
| Over-broad `permissions:` | Larger token blast radius | `contents: read` only |
| Pinning a SHA not on the release | Supply-chain substitution | Compare to `git/ref/tags/<tag>` (and dereference) |
| Rollout SQL that mutates shared state | Other connectors' exactly-once state erased (the gg1 `TRUNCATE` finding) | Run every operator command on a real server; scope by topic; review the doc before the gate |

## UX Pitfalls (operators)

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Value change for `Nullable(Enum)` with no announcement | Dashboards and MVs change meaning silently | Dedicated rollout item with an "Am I affected?" query and a before/after example |
| Old rows not repaired | Operators assume history is fixed | State plainly that historical `NULL`/shifted rows are not rewritten |
| WARN text contradicting the fix | Confusion on upgrade | Remove or retarget the WARN in the same release |
| Release bump changes MBean/UA again | Blank dashboards | Repeat the pattern-matching guidance; use the regex vs `ObjectName` split |
| CHANGELOG using planning IDs (`R-01`, `WR-02`) | Unreadable to outsiders | Plain descriptions; ledger path stated once |

## "Looks Done But Isn't" Checklist

- [ ] **999.1 Enum:** Often missing the validation-gate flip and the stale pins; verify `git grep -n "D-16\|999.1\|accepted cost"` returns only intentional history.
- [ ] **999.1 Tuple:** Often only 1-field tested; verify a 3-field `Nullable(Tuple)` with a `Nullable` element and a plain element, null and non-null, in all five positions, with and without a DEFAULT column.
- [ ] **999.1 matrix:** Often hand-written expectations; verify read-back is compared to a twin table filled by SQL, and a mutation turned it red.
- [ ] **WARN removal:** Often leaves the anchor row; verify `.planning/scripts/fork-anchors.txt` and its grep pass at HEAD.
- [ ] **Empty-CDC skip:** Often misses the all-skipped batch and exactly-once state; verify the five live pins in Pitfall 4.
- [ ] **WR-03/WR-04:** Often misses the `IOException` null-message sibling in `Utils`; verify through `ClickHouseSinkTask.put()` with both tolerance settings.
- [ ] **T-13-19:** Often masks one of four arguments; verify sentinel absent from message and throwable, column name present.
- [ ] **Lz4 response leg:** Often a vacuous assertion; verify red under `compressServerResponse(false)`.
- [ ] **999.2:** Often not proven for the other configurations; verify four before/after `dependencyInsight` outputs.
- [ ] **Lombok:** Often only the warning is checked; verify `javap` of `ClickHouseFieldDescriptor*` is unchanged and `lombok.addLombokGeneratedAnnotation` is still present.
- [ ] **SHA pins:** Often one annotated tag wrong; verify each pin against the tag API and the first CI run.
- [ ] **`permissions:`:** Verify the "Set up job" log shows only `Contents: read` / `Metadata: read` and the summary, annotation and both artifacts are still produced.
- [ ] **Runner pin:** Verify the log says `ubuntu-24.04`.
- [ ] **Publication 1:** Allow-list diff, no `.gsd-ws-arg`, no `state.json`, one run on the SHA, none cancelled, tags pushed only after green.
- [ ] **Release:** `clean` build, `VERSION` anchor row, CHANGELOG `1.7.0_gg2` section, rollout note reviewed before the gate, rollback tag on `M1` pointing at a commit with the branch-only trigger.
- [ ] **Docs commands:** Every command quoted in the rollout note and FORK-DELTA was executed and its output matches the text.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Tuple desync shipped | HIGH | Revert the Tuple commit (Enum commit stays); rollback tag; operators drain state, re-run with the WARN restored; backfill affected rows from the source |
| Enum change surprises a pipeline | MEDIUM | Operators compare `countIf(e IS NULL)` per day around the upgrade; backfill from source; no connector rollback needed |
| Block differs after upgrade (exactly-once) | HIGH | Identify ranges that were `BEFORE_PROCESSING` at upgrade; compare against source; delete only that connector's state rows (never `TRUNCATE`); replay from offsets |
| Wrong skip layer (offsets stuck) | MEDIUM | Revert the skip commit; reset consumer group offsets for the stuck partitions |
| Masked log too thin | LOW | Add the missing metadata field; no data fix needed |
| Bad SHA pin | LOW | Correct the SHA; push a branch (not a tag) to re-run |
| Wrong Lombok version key | MEDIUM | Revert the key; `clean` build; confirm `fromJsonRow` test |
| Exclusion hit the wrong configuration | LOW-MEDIUM | Revert the exclusion; compare `confluentJar` listing to the previous release |
| Two refs at one SHA cancelled the run | LOW | Delete the extra ref on `origin`, push an empty-diff commit only with approval, or re-run the surviving run |
| Merged corrected doc still wrong | LOW | Docs-only follow-up merge (costs one more CI run on `custom_build`) |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1 Tuple desync | WRITER | Twin-table matrix green, seen-red mutation, both DESCRIBE shapes Docker-free |
| 2 Enum value change, stale pins | WRITER, REL | Pins flipped with rationale, anchors pass, ledger entry, rollout item |
| 3 Cross-version bytes | WRITER, REL | Rollout drain item; skip/encoding is a pure function of record + metadata |
| 4 Empty-CDC skip layer | WRITER | Five live pins; no diff outside `ClickHouseWriter` |
| 5 WR-03/WR-04 classification | WRITER | `put()`-level pins for both tolerance modes and null-message timeout |
| 6 T-13-19 masking | WRITER | Sentinel log-capture test; FORK-DELTA row; anchor row |
| 7 Lz4 response leg | DEBT | Ratio assertion red under mutation; no version/absolute-byte assertion |
| 8 999.2 exclusion | DEBT | Four `dependencyInsight` diffs; `compileIntegrationTestJava`; two full runs |
| 9 Lombok | DEBT | `javap` identical; coverage table stable |
| 10 SHA pins | CI | Tag API table in the plan; YAML guard; first CI run green |
| 11 permissions/runner/fork count | CI | Job log lines; annotation present once; one run, both artifacts |
| 12 Publication mechanics | PUB-1, REL | Allow-list diff; one run per SHA; rollback tag semantics |
| 13 IN triage | DEBT | `REVIEW-DISPOSITION` has a written reason per item; shortstat compared |
| 14 Docs-only merge | PUB-1 | `grep TRUNCATE`; SQL executed |
| 15 Defaults layout | WRITER | Each cell run with and without a DEFAULT column |
| 16 Version bump effects | REL | `clean` build; anchor row; item 7 text |
| 17 `latest` drift | all | `SELECT version()` recorded per gate |

### Ordering consequences for the roadmap

1. PUB-1 first and alone: it should run the **unchanged** workflow so a CI failure cannot be blamed on the CI changes.
2. WRITER before CI and REL: its tests define the behaviour ledger the rollout note is written from. Within WRITER: Enum, then Tuple, then the empty-CDC skip, then WR-03/WR-04/T-13-19 as separate commits.
3. DEBT (Lz4, 999.2, Lombok) is independent of WRITER and can be parallel; both touch only tests and `build.gradle.kts`/`lombok.config`.
4. CI last before REL, rehearsed on a pushed scratch branch, so the release merge's own run is not the first run of the new workflow.
5. REL last: docs reviewed before the approval gate, one run on the published SHA, tags after green.

### Research flags for phases

- **WRITER (Tuple half):** needs its own research/spike: choose the type-string splitting approach and confirm behaviour for `LowCardinality`, `SimpleAggregateFunction`, nested tuples and quoted enum members. HIGH priority.
- **WRITER (all-skipped batch on V1/V2):** needs a short spike to confirm an empty-body insert is accepted by both clients.
- **CI:** standard patterns; re-resolve the pin table at implementation time.
- **DEBT (Lz4):** small spike to confirm the `NetworkSendBytes` read path works through client-v2 `queryAll` with an explicit query id.
- **PUB-1, REL:** repeat of the proven recipe; the new parts are the allow-list check and the rollback-tag choice.

## Sources

First-hand (HIGH):
- Repository: `Column.java`, `Table.java`, `ClickHouseWriter.java` (`doWriteCol`, `doWriteColValue`, `writeRecordsJson`, `doInsertRawBinary`), `Utils.handleException`, `QueryIdentifier`, `ClickHouseHelperClient` (WARN hook), `build.gradle.kts`, `gradle/libs.versions.toml`, `lombok.config`, `.github/workflows/tests.yaml`, `.github/dependabot.yml`, `.planning/scripts/*`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg1.md`, `CHANGELOG.md`.
- Planning: `.planning/PROJECT.md`, `RETROSPECTIVE.md`, `MILESTONES.md`; `milestones/v1.7.0_gg1-sync-phases/13-REVIEW.md`, `13-RESEARCH.md` (spikes b2, b3, Pitfall 8), `14-REVIEW.md`, `15-REVIEW.md`, `15-REVIEW-DISPOSITION.md`, `13-SECURITY.md` (T-13-19), `14-SECURITY.md` (T-14-16).
- User memory: `gsd-dispatch-isolation-sentinel-clobber`, `gsd-phase11-orchestration-gotchas`, `gsd-phase15-review-gate-gotchas`, `colima-vz-profile`.
- Live probes on `clickhouse/clickhouse-server:latest` = `26.9.14.10` (throwaway containers, removed afterwards): DESCRIBE sub-column shapes, RowBinary hex dumps, `system.settings` for the nullable-tuple settings, HTTP `compress=0/1` response sizes with `system.query_log` ProfileEvents.
- `./gradlew --offline dependencyInsight --configuration testRuntimeClasspath --dependency lz4`; `javap -v -p` of the built `ClickHouseFieldDescriptor` classes.
- GitHub REST API, 2026-10-10: releases and `git/ref/tags` for `actions/checkout`, `actions/upload-artifact`, `actions/setup-java`, `gradle/actions`; `action.yml` `runs.using`; the fork's workflow and run lists (`ne1r0n/clickhouse-kafka-connect`); upstream issue search (no open upstream fix for Nullable Enum/Tuple).
- `gradle/actions` `docs/setup-gradle.md` (v6.4.0), permission requirements.

Web (MEDIUM, cross-checked against the repo or the API):
- [Ubuntu 26 generally available and latest migration (GitHub changelog)](https://github.blog/changelog/2026-09-17-ubuntu-26-generally-available-and-latest-migration): `ubuntu-latest` moves from 24.04 to 26.04 between 2026-10-19 and 2026-11-19; pin `ubuntu-24.04` to stay.
- [Project Lombok changelog](https://projectlombok.org/changelog): v1.18.44 adds Jackson 3 support in `@Jacksonized` and warns until `lombok.jacksonized.jacksonVersion` is configured; v1.18.46 refines it. The generated-annotation claim was confirmed with `javap` on this build.

Not verified (LOW): ClickHouse compressed-block method bytes (0x82 LZ4, 0x90 ZSTD) are quoted from the ClickHouse block format, not measured here; the Docker Hub `latest` tag may already be newer than `26.9.14.10`.
