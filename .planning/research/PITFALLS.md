# Pitfalls Research

**Domain:** Absorbing dependency majors, a tagless upstream tail, two discovery-defect fixes and 17 review advisories into a production Kafka Connect sink fork (`ne1r0n/clickhouse-kafka-connect`, `custom_build`, `v1.7.0_gg3` to `v1.7.0_gg4`)
**Researched:** 2026-10-21
**Confidence:** MEDIUM-HIGH. Fork-history and code findings are HIGH (read from the repo, run with read-only `git merge-tree`, GitHub REST and Maven Central). Library-behaviour findings are HIGH where read from primary artifacts (Shadow `CHANGELOG.md` and `ShadowJar.kt` at tag 9.6.1, Kafka source at `4.1.1` and `4.4.0-rc4`, Maven poms) and MEDIUM where only a summary was available (Kafka 4.4.0 release notes, protobuf.dev pages). Two items are LOW and flagged: the guava CVE identity (Pitfall 4) and the Confluent 7.9.1 plus protobuf 4.x call surface (Pitfall 10).

Scope note. Nothing outside `.planning/research/` was modified and no Gradle build or test was run. `git merge-tree --write-tree` was used for the merge rehearsals; it creates unreferenced objects only and moves no ref or worktree. Anything that says "predicted" needs the measurement named beside it before it is believed.

Confidence tiers come from `classify-confidence`: web fetches score LOW and verified web searches MEDIUM by provider. Where this document says HIGH it is because the primary artifact itself was read, and it names that artifact.

---

## What CI does and does not exercise (read this first)

`tests.yaml` on `origin/custom_build` runs exactly two Gradle steps after `reportTestForks`: `./gradlew compileIntegrationTestJava` and `./gradlew test`, on ClickHouse `latest` with client V2 only (`git show origin/custom_build:.github/workflows/tests.yaml`). Consequences for this milestone:

| Surface | In CI? | Evidence |
|---|---|---|
| Unit and live tests against `latest` x V2 (1281 tests at gg3) | Yes | `tests.yaml` step "Run tests" |
| Integration test sources compile | Yes | step "Compile integration-test sources" |
| `confluentJar`, `prepareConfluentArchive`, `createConfluentArchive` (the published zip) | **No** | only `integrationTest` depends on `prepareConfluentArchive`, and CI never runs `integrationTest` (`build.gradle.kts`) |
| `integrationTest` (Confluent Platform containers, Avro/Protobuf end to end) | **No** | nightly `integrationTests.yaml` is disabled and red by inheritance (STATE.md Blockers; memory `nightly-integration-tests-inherited-red`) |
| Client V1, ClickHouse older than `latest` | No | project constraint |

CI for the Dependabot merges is **already green** for `55845c3a`, `4b9ddf96`, `d20bae63`, `af762b1b` and `69cd680b` (REST `actions/runs?branch=custom_build`, read 2026-10-21; the milestone brief said three runs were still in progress). That green run included Shadow 9.6.1, connect-api 4.4.0 and Gradle 9.8.1, and **proves nothing about the shipped jar**. The first pitfall follows from that.

---

## Critical Pitfalls

### Pitfall 1: Shadow 9 stopped unzipping `from(...)` inputs, so `confluentJar` may now be a jar of jars

**What goes wrong:**
`confluentJar` is a hand-registered `ShadowJar` whose content comes from `from(clickhouseDependencies, sourceSets.main.get().output)` (`build.gradle.kts` lines 362-365 on `origin/custom_build`). `clickhouseDependencies` is a plain `Configuration`, not the task's `configurations` property. Shadow 9.0.0 changelog: "**BREAKING CHANGE:** Align the behavior of `ShadowTask.from` with Gradle's `AbstractCopyTask.from`. In the previous versions, `ShadowTask.from` would always unzip the files before processing them ... Now it ... will not unzip the files, only copy the files as-is." The Shadow docs say the same: dependencies reached through `configurations` are "unzipped and merged"; `from("bar.jar")` copies "without unzipping". In 9.6.1 the only place that explodes jars is `addIncludedDependencies()`, which iterates `includedDependencies`, and that is built from the `configurations` property, which a custom `ShadowJar` task leaves empty. The Connect archive's `lib/*-confluent.jar` could therefore contain `httpclient5-5.6.5.jar`, `client-v2-0.9.6.jar` and so on as nested files plus the fork's own classes, instead of 7,375 exploded entries. A Connect worker's plugin classloader does not read nested jars, so the failure is `NoClassDefFoundError` at plugin load on the first deployment, not in any test.

**Why it happens:**
The plugin bump (Dependabot #11, Shadow 8.3.5 to 9.6.1) was gated by a suite that never builds the artifact. The fork's task relied on an 8.x behaviour that the 9.0.0 notes list as breaking. Status: **predicted from the changelog and the 9.6.1 source, not yet built** (no `confluent` output exists under `build/` locally; only `build/libs/clickhouse-kafka-connect-v1.7.0_gg3.jar`).

**How to avoid:**
1. First task of the dependency phase, before any other absorption work is trusted: build `createConfluentArchive` on the DEPS merge tip and on the control `v1.7.0_gg3` (or `pre-v1.7.0_gg4` tag), each in a throwaway worktree, exactly as gg3 plan 24-01 did (`24-COMMAND-EVIDENCE.md` E-04: `clean createConfluentArchive`, unpack into new empty directories, compare with `cmp.py`, Python with `-I`). Baseline to beat: **7,375 entries** in `lib/clickhouse-kafka-connect-v1.7.0_gg2-confluent.jar` and 7,376 at gg3 (one added class, `WithheldMessageException`). Each dependency PR legitimately changes some entries (gson 2.14.0, httpclient5 5.6.5, jackson 2.22.4, guava, lz4, the client's own transitive set), so the compare needs an **allow-list by path prefix** (the `artifact-diff-check.py` pattern from E-04), not "zero changed".
2. If the jar is nested, repair the task rather than pin Shadow back: register the configuration (`configurations.add(clickhouseDependencies)`, then `from(sourceSets.main.get().output)`) or wrap each file in `zipTree`. Both are candidates to prove with the same compare, not recommendations to trust. The compare must then show zero `*.jar` entries and the gg3 entry set plus only allow-listed changes.
3. After the repair, expect two further Shadow 9 jar differences and decide each on the record: (a) Shadow 9.0.0 "Inject `Multi-Release` manifest attribute if any dependency contains it" (opt-out `addMultiReleaseAttribute = false`, 9.1.0). Once `includedDependencies` is populated the manifest can gain `Multi-Release: true`, which makes the JVM honour `META-INF/versions/N/` entries in the fat jar. Behaviour preservation says set it `false` unless the compare proves the versioned entries are inert. (b) The signature-file and `module-info` excludes (`META-INF/*.SF`, `*.DSA`, `*.RSA`, `META-INF/versions/**/module-info.class`) are applied only to the plugin-registered `shadowJar` task (`registerShadowJarCommon` in `ShadowJavaPlugin`), never to a custom task, so check `unzip -l ... | grep -E 'META-INF/.*\.(SF|DSA|RSA)'` against the gg3 baseline instead of assuming.
4. Make it stand: add a CI step (or a Docker-free pin like `TestsWorkflowPinGuardUnitTest`) that runs `createConfluentArchive` and asserts "no nested `.jar`, no `org/apache/kafka/` entries, entry count within a stated band". Without it the next Dependabot Shadow bump repeats this.

**Warning signs:**
`unzip -l clickhouse-kafka-connect-*-confluent.jar | grep -c '\.jar$'` is non-zero; the entry count falls from about 7,375 to a few dozen; the file size of the archive stays similar but `com/clickhouse/client/` class entries are missing; Shadow prints duplicate-entry warnings (9.3.2 "Log warnings for duplicates in the final JAR").

**Phase to address:** Dependency absorption, as its first measured gate. Re-run in Release as the shipped-artifact check.

---

### Pitfall 2: The anchors gate fails on many Dependabot commits, not one, and `origin/custom_build` has already moved

**What goes wrong:**
`.planning/scripts/fork-anchors.txt` row 48 pins `gradle/libs.versions.toml` `kafka = "4.1.1"` (BLD-08). Dependabot #9 changed it to `4.4.0`. `verify-merge-invariants.sh` invariant 3 checks **every commit in the range against its own copy** of the anchors file (script lines 240-252), so every commit from the #9 bump through all later merge commits that carries the anchors file and `kafka = "4.4.0"` is a violation, not just the tip. gg3 needed one exemption row (`6473e24f`, `fork-anchor-exemptions.txt`); this milestone can need a dozen, and each must be a full 40-hex SHA of an already-published commit with the pattern spelled as **that commit's** anchors copy spells it (21-RESEARCH Pitfall 7). Separately, `origin/custom_build` is now `69cd680b`, two merges past the brief's `d20bae63` (jackson 2.22.4 #14, slf4j-simple 2.0.20 #15); only PRs #4 and #10 remain open. It will keep moving.

**Why it happens:**
The gate was built for "one Dependabot tip" (gg3). The milestone start state is "seven-plus Dependabot merges on origin", and the user merges directly on origin (memory `dependabot-merges-on-custom-build`). Also the merge commit itself must satisfy its own anchors (21-RESEARCH Pitfall 1: apply the D-03 bookkeeping edits in the **same** `git commit` of the merge, with no GSD tool call between `--no-commit` and `commit`, Pitfall 2).

**How to avoid:**
- Re-read `git ls-remote origin custom_build` immediately before the merge and record it (gg3 E-03 did; ls-remote and fetch must agree).
- Rehearse in a throwaway worktree, then run the **unmodified** gate first and generate the exemption rows from its `ANCHOR VIOLATION: <sha>` lines (gg3 E-04 order: gate before exemption). Do not edit the script (D-02). Keep a count: expect a violation line per commit after #9 on the path.
- In the merge commit move the anchors and docs together: `fork-anchors.txt` row 48 to `kafka = "4.4.0"`, `docs/FORK-DELTA.md` rows that name `4.1.1` (lines 34 and 68), and any other anchored literal Dependabot moved. Also update the stale `4.1.1` prose in `InMemoryDLQ`, `ThrowableTexts`, `ValueFreeDlqHeadersUnitTest` and the `ClickHouseSinkTask.onPartitionsRevoked` comment only if re-verified (Pitfall 9).
- Add anchors for lines that matter and are currently unanchored: `lz4Java = "1.12.0"` is **not** in `fork-anchors.txt` (grep of the file shows only `withCapability("org.lz4:lz4-java")`), see Pitfall 3.
- The release gate must re-fetch and re-merge if `origin/custom_build` moved again (Pitfall 14).

**Warning signs:**
`invariants: ... result=FAIL` with `expected 1 got 0` lines naming Dependabot SHAs; a merge commit that violates its own anchors; an exemption row rejected as malformed (not 40-hex, wrong tab count).

**Phase to address:** Dependency absorption (DEPS-01 merge), re-checked at Release.

---

### Pitfall 3: The tagless mini-sync conflicts on the whole `[versions]` block, and a wrong-side resolution is a silent multi-dependency downgrade

**What goes wrong:**
`git merge-tree --write-tree origin/custom_build refs/remotes/upstream/main` reports conflicts in exactly two files, `.gitignore` and `gradle/libs.versions.toml`; `build.gradle.kts` **auto-merges** (guava lines and `libs.httpclient5` in the integrationTest suite arrive with no marker). The toml conflict is one hunk covering `kafka` through `lz4Java`. Taking upstream's side of that hunk would silently revert `kafka` 4.4.0 to 2.7.0, `gson` 2.14.0 to 2.13.1, `jackson` 2.22.4 to 2.22.3, `lombok` 1.18.48 to 1.18.46 and `lz4Java` 1.12.0 to **1.11.4** (upstream's tail value). Taking only the fork side drops `guava = "33.7.2-jre"`, which fails loudly (`libs.guava` unresolved). The `lz4Java` literal and the `kafka` row are the dangerous ones: lz4 1.12.0 was chosen over upstream (`PROJECT.md`: "lz4-java went past the planned 1.10.4 to an advisory-free 1.12.0"; FORK-DELTA records floor 1.11.4), and no anchor pins it. Upstream's tail also deletes the `httpclientTest` catalog entry and the `httpclient5-test` alias; the fork's toml keeps both, so the merged tree has a dead alias plus a build that now uses `libs.httpclient5` for integration tests (harmless but must be an explicit decision recorded in FORK-DELTA).

The tail itself is small (`git diff v1.7.0 refs/remotes/upstream/main --stat`: `.gitignore`, `build.gradle.kts`, `gradle/libs.versions.toml`, 16 changed lines), so the risk is not volume, it is that "tiny" invites a hand merge outside the sync discipline.

**Why it happens:**
Both sides edited adjacent lines of the same block for the same dependencies (jackson, httpclient5, mockito, okhttp, org.json, lz4), so git cannot split the hunk. The Sync Mechanics rule "no dependency bump rides inside the sync commit" collides with a sync whose entire content is dependency bumps.

**How to avoid:**
- Follow the recipe: scratch branch, rollback tag, **one mechanical merge commit** (conflict markers committed as-is, as `54e63648` did), then a separate resolution commit. Resolve to the explicit target table: fork values everywhere upstream's are older or equal, plus the `guava` version line and alias, plus upstream's `.gitignore` additions. Re-assert with `./gradlew dependencyInsight` (below) rather than reading the diff.
- Order: DEPS-01 merge of `origin/custom_build` first. `git merge-tree` shows that merge is clean against `sync/v1.7.0`, and doing the mini-sync second means its conflicts are exactly the two above.
- `verify-merge-invariants.sh` requires the second parent to equal `UPSTREAM_REF` (`^{commit}`-resolved). There is no tag, so pass the full SHA `a6f1df839c079180bc7c343b5823f205e204dafc` (current `refs/remotes/upstream/main`) and a new `PRE_TAG`; record conflicted paths in a new `CONFLICTED_PATHS_FILE`. Never write bare `upstream/main`: `git rev-parse upstream/main` prints "refname 'upstream/main' is ambiguous" today because the stale local `refs/upstream/main` still points at `789a57c2` (the v1.6.0 merge). Delete that ref first (it is in scope) and use `refs/remotes/upstream/main` in every command.
- Anchor `lz4Java = "1.12.0"` (and `gson`, `jackson` if the user wants) in `fork-anchors.txt` in the same commit that resolves the toml, and prove the anchor red by resolving the wrong way in a throwaway worktree (seen-red).

**Warning signs:**
`dependencyInsight --dependency lz4-java` shows `1.11.4`; `kafka` shows `2.7.0` and compile errors on `SinkTask`; FORK-DELTA pin rows no longer match the catalog; `git diff pre-tag..HEAD -- gradle/libs.versions.toml` removes the `# FORK OVERRIDE` comment.

**Phase to address:** Mini-sync (second step of the dependency phase).

---

### Pitfall 4: "Closes a guava CVE" has no CVE identifier, so the record cannot claim it

**What goes wrong:**
The brief and `PROJECT.md` plan to close "the guava CVE on the record". Upstream's commit `30521f74` says only "Overriden version of guava to fix CVE", and its code comment says `clickhouseDependencies(libs.guava) // temp to override client's with CVE`. No ID is given. Checks run for this research:
- GitHub advisories for `com.google.guava:guava` list only CVE-2023-2976, CVE-2020-8908 and CVE-2018-10237, all fixed at or below 32.0.0 (`api.github.com/advisories?ecosystem=maven&affects=com.google.guava:guava`).
- `client-v2 0.9.6`'s pom already depends on `guava 33.4.6-jre` (compile scope), which is past every listed fix.
- OSV (`api.osv.dev/v1/query`) returns no vulnerability for `33.4.6-jre`, `33.4.8-jre` or `33.7.2-jre`.
So the claim is unverifiable from public databases today (LOW confidence on what the CVE is; it may live in a scanner feed that the public APIs do not carry). Writing "closes CVE-X" in the rollout note without an ID repeats the unsupported-quantifier defect class that cost gg3 three review rounds (RETROSPECTIVE gg3, Key Lesson 4).

**Why it happens:**
The fork inherits upstream's wording for a change it cannot independently confirm. Also the explicit guava lands inside the shipped jar (`clickhouseDependencies`), so it is a content change either way.

**How to avoid:**
- Ask the user or upstream for the advisory ID before the rollout note is drafted. If none arrives, write the **measured** fact: the shipped guava moves from 33.4.6-jre (client-v2 transitive) to 33.7.2-jre, shown by `dependencyInsight --dependency guava --configuration runtimeClasspath` and by the jar compare (guava entries changed, no unrelated entries). Label the CVE claim "per upstream commit message, not independently identified".
- Check the test classpath too: Confluent 7.9.1's parent pins `guava.version` 32.0.1-jre (`common-parent-7.9.1.pom`), so `testRuntimeClasspath` will resolve the highest, 33.7.2; ClickHouse-backed tests passing proves only that.

**Warning signs:**
A rollout sentence containing "CVE" with no ID; a requirement worded "CVE closed".

**Phase to address:** Mini-sync (decide the wording) and Release (note text).

---

### Pitfall 5: The 999.4 rule is wider than `shouldSkipDescribeRow`, and a wrong rule fails in both directions

**What goes wrong:**
Three separate facts make the "keep a declared element named `size`/`size0`" fix easy to get wrong.

1. **Two code paths drop the element, not one.** `shouldSkipDescribeRow` (`ClickHouseHelperClient` line 473) skips any subcolumn row ending `.null`, `.size0` or `.size`. Upstream's `Table.SIZE_FIELD_MATCHER` (`.+\.size[0-9]+$`, `Table.java` line 24) additionally returns early for `size<digits>` children of ARRAY and MAP parents. FORK-DELTA Known costs item 10 and the gg3 rollout note (item 2) say an element named `size<digits>` under an `Array` is also dropped, and two of the four measured failing shapes are `Array(Tuple(size1 ...))` and `Array(Tuple(v String, size1 ...))`. The milestone text and the backlog title name only `size` and `size0`. A fix confined to `shouldSkipDescribeRow` leaves the `size1` shapes failing, and touching `Table.java` (currently upstream-identical, "`Table.java` is unchanged" in FORK-DELTA row 51) adds a fork delta in a file the next sync will conflict on.
2. **Too narrow: still dropped.** The element stays out, the insert still fails with code 32/33 (`CANNOT_READ_ALL_DATA`, `ATTEMPT_TO_READ_AFTER_EOF`; the 23-EVIDENCE R-01 capture and 24-COMMAND-EVIDENCE E-15), the WARN keeps firing and the D-04 pin never flips.
3. **Too wide: orphan or corrupting Columns.** `Table.updateParent`'s TUPLE branch ends `parent.getTupleFields().add(child)` with no name check (`Table.java`), so a structural row that gets through with a Tuple parent becomes a Tuple field and the writer, which iterates `getTupleFields()`, writes an extra element. Concretely `d.null` (parent `d` is `Nullable(Tuple)`) would be appended as a `UInt8` field. Rows under a `JSON`/`Dynamic` ancestor that leak through become orphan Columns plus an ERROR log (P3-WR-01, closed by DESC-05); there `ClickHouse latest` was not observed emitting that shape, so the regression would be silent in the existing live tests.

Rows from the live capture to keep dropping (`23-EVIDENCE.md` R-01): `c.name.size` (parent `c.name` is a String), `d.null`, `d.other.size`, `e.size0` (the array's **offsets** row under `Array(Tuple)`), `e.v.size`. Rows that must be kept: `c.size`, `d.size0`, `e.size`. The ROADMAP sketch says "parent-is-a-Tuple alone is not enough for `Array(Tuple)`": confirmed by the capture, where `e.size0` (UInt64, offsets) and an element named `size0` would share a name.

**Why it happens:**
The skip is name-suffix based, a heuristic designed when no one had a Tuple element called `size`. The DESCRIBE output has no flag that separates a structural subcolumn from an element, and the Table routing is parent-type-driven.

**How to avoid:**
- **Spike before design** (the gg2 lesson; `Retrospective` gg2 Key Lesson 1). Capture raw `DESCRIBE ... describe_include_subcolumns=1` for the shapes the fix must separate before writing the rule, including the ones not yet captured: `Array(Tuple(size0 ...))` (both the offsets row and the element row can carry `e.size0`), `Array(Array(Tuple(size ...)))` (`.size0` and `.size1` offsets), `Map(String, Tuple(size ...))`, `Nullable(Tuple(size ...))`, `Tuple(x Tuple(size ...))`, and wrappers (`LowCardinality`, `Nullable`, `SimpleAggregateFunction`, `Nested` with `flatten_nested=0`) around them. Hypothesis to test, not assume: offsets rows are exactly `UInt64`, and element rows under an Array parent are `Array(...)`.
- A principled alternative to heuristics is to use the root's **declared type** (already parsed by `DeclaredNullability` via `ClickHouseColumn.of`) to know which element paths exist, and keep a `.size`/`.size0` row only if it is a declared element path. Decide in the spike, and keep `.null` unconditionally skipped: the server rejects a Tuple element named `null` (code 36, R-01 and E-15), so it is always structural.
- Acceptance fixtures already exist: the 13 affected tables (`s4_a01`..`s4_a13`), 19 look-alikes and the over-flag `s4_o01` of E-15, plus the oracle (the WARN fires on exactly the 13). Reuse them: every affected table must insert and read back equal; every look-alike and `s4_o01` must be byte-for-byte unchanged in the Column list.
- Do not "tidy" `.size1`: today `.size1` rows are not skipped by `shouldSkipDescribeRow`, `Table.addColumn` registers them (`registerValidColumn` runs before `handleNonRoot`) and `updateParent` then ignores them via `SIZE_FIELD_MATCHER`. That is existing behaviour the DESC pins depend on.
- Keep the DESC-05 pins passing unchanged (`rowsAtAnyDepthBelowJsonOrDynamicAreSkipped`, `rowsWithoutJsonOrDynamicAncestorAreNotSkipped`, the Map `.keys`/`.values` and `tuple_col.a` pins in `ClickHouseHelperClientForkUnitTest`), keep the ancestor walk independent of the new element rule, and add rows for `tj.jj.size`-style cases rather than editing existing ones.
- Fix 23-REVIEW WR-02 with it: the pin exercises a **copy** of the DESCRIBE loop (`discoverAsDescribeTableV2`). Extract the loop into one static method used by both `describeTableV1` and `describeTableV2` and have the test call it. Prove it with a mutation of the production loop that turns the pin red. Client V1's path (`ClickHouseRecord`-based) was never measured for 999.4 (FORK-DELTA item 10), so state that in the evidence.
- Both describe call sites change together (`describeTableV1` and `describeTableV2` are copy-pasted), and the FORK-DELTA row (47), an anchors row and the D-04 WARN pin flip land in the same commit.

**Warning signs:**
A previously-passing DESC-05 or `TableTest` assertion changes; a look-alike table's Column count differs from the gg3 capture; an ERROR log "Got non-root column, but its parent was not found" or "Ignoring complex parent type" for a fixture that was quiet; the live round trip passes for `size` but `size1` under `Array` still fails.

**Phase to address:** 999.4 phase (spike first, then fix). Release must re-state the measured shapes.

---

### Pitfall 6: Any change to the Column list or to field lookup changes the bytes a retried batch sends, which is the exactly-once invariant

**What goes wrong:**
`AGENTS.md` requires that after `BEFORE` is set the retried batch is byte-identical. RowBinary inserts carry **no column list** (`client.insert(table.getName(), dataWriter, format, settings)`, `ClickHouseWriter` lines 1311 and 1733), so the Column list from `describeTable` **is** the wire format. 999.4 changes it for affected tables; 999.5 changes which Kafka field a Column reads, so the same record writes different values.
- Upgrade, tables the gg3 note measured failing (code 32/33, no rows stored): an in-flight range holds no row, and the retry under the same `insert_deduplication_token` lands once. Safe without a clean stop.
- Upgrade, tables where the old shorter write happened to parse (R-01's `abc U+0003 xyz` row stored `c = (1667391751,'xyz')`): a wrong row exists, and `DedupTokenRetryLiveTest` (gg2) showed that on a deduplicating engine a retry with different bytes under the same token is **dropped when the earlier attempt landed**. The wrong row persists and the correction never arrives.
- Rollback to gg3: the same tables fail with code 32/33 again, and a range left `BEFORE_PROCESSING` by gg4 can be stuck even though its rows landed. This is exactly gg3 rollout item 4 for the 14 tables of the backslash-n shape.
- 999.5: a root column whose natural field name stops being ignored now stores the value where gg3 stored `DEFAULT`; a retry of the same range by the other release writes different bytes (same stuck-range logic, in the opposite direction).

**Why it happens:**
The connector's idempotence rests on "same input gives same bytes". Discovery and name matching are inputs to that function, and they are rarely thought of as such.

**How to avoid:**
- Treat each behaviour change as a **ledger row** with a pin (the L-nn practice, 18/19/22/23 behaviour ledgers), and write rollout items that mirror gg3 items 3 and 4: what changed, an "am I affected" query, and a rollback section stating whether a clean stop is required, with the measured basis. Re-use gg3's measurement method: run both releases' real table-description code over a fixture set on the same server and diff the Column descriptions (E-04/item 4 did this over 44 fixtures; extend it with the 999.4 and 999.5 fixtures). Bytes can differ only through the Column description, so a description diff scoped to the fixtures bounds the clean-stop advice.
- Measure the unmeasured: gg3 left "the other nine affected shapes", client V1, and the explicit `insertFormat=json` route unmeasured. Run the 13 shapes through insert and read-back on both releases and record the outcome per shape, so the gg4 note can use "measured" instead of "likely".
- Update the gg3 greppable instruction in the gg4 note: after the fix the unpaired-element WARN disappears for fixed shapes, so "no WARN in the log" no longer says "no affected table" on a mixed fleet.
- Never reorder `BEFORE`/`AFTER` state handling, never touch `Processing` or `KeeperStateProvider` in this milestone (AGENTS.md high-risk files); the `ClickHouseWriter` name-lookup change in 999.5 is also in the high-risk list.

**Warning signs:**
A rollout note that says "no clean stop needed" without a Column-description diff; a live test that stores rows and reads back only on gg4 (no gg3 control).

**Phase to address:** 999.4 and 999.5 phases (measure), Release (note and rollback).

---

### Pitfall 7: `fromJsonRow`'s doubling is load-bearing for type parsing, not only for names

**What goes wrong:**
`ClickHouseFieldDescriptor.fromJsonRow` does `json.replace("\\", "\\\\").replace("\n", "\\n")` before Jackson parses the **whole row**, so `name` and `type` (and every other field) keep JSON escapes as text. Everything downstream is written against that representation: `DeclaredNullability.normalise` reads "a lone backslash-n outside quotes" as the server's line break (`run == 1`), "even run lengths inside quotes" (2 for a real newline, 4 for a literal backslash-n) and closes a quote only when `(run / 2) % 2 == 0`; its Javadoc says outright "`fromJsonRow` keeps JSON escapes as text". `Column.extractColumn` parses multi-line Tuple `type` strings in that same raw form for every table with a multi-line Tuple type, not only tables with odd names. 23-EVIDENCE R-02 table: with the current code a `Nullable(Tuple(`a\\nb` Int32, d String))` stores correctly only because the sub-column name (`c.a\\nb`, doubled) equals the element name that `ClickHouseColumn.of` produces by unescaping one level of the raw type text.

If the 999.5 fix decodes only the sub-column `name`, the Column name has one backslash while `ClickHouseColumn.of` still yields two, so `DeclaredNullability` stops pairing and `Nullable(Tuple)` with backslash names falls back to the gg2 failure (code 33), re-breaking the gg3 L-50 fix and logging the fallback WARN. If it decodes `type` too, every multi-line Tuple type changes (the structural line break becomes a real LF, which `normalise` does not replace and `ClickHouseColumn.of` may not parse), and the 39-test `DeclaredNullabilityTest` fixtures plus `DeclaredNullabilityFixtures.BACKSLASH_N_NAME_RAW` (all written in the doubled form) describe a representation that no longer exists.

**Why it happens:**
The "bug" looks like a one-line string hack, but two later fixes (COLS-12 and the quote-aware `normalise`) were deliberately built on its output.

**How to avoid:**
- Decide first which form is the contract (Pitfall 8), then change the narrowest thing: the Column **name** (and the parsed element names compared against it), not the raw `type`. Re-key the pairing so it compares like with like, and keep `normalise`'s premises untouched, or change both in one commit with the full matrix.
- Acceptance matrix, live and Docker-free: the 10 escaped-name fixtures (E-16 `e5_e01`..`e5_e11`), the 14 gg3 backslash-n `Nullable(Tuple)` fixtures, the 8 look-alikes `e5_l01`..`e5_l08`, plus plain multi-line Tuple types (no odd names) which must be byte-for-byte unchanged in Column description. Compare gg3 vs gg4 descriptions as in Pitfall 6.
- 23-REVIEW WR-01 (the escaped-quote branch of `normalise` has no test) must land **with** this change, as the milestone says: add the pin that mutates the `(run / 2) % 2` arithmetic red *before* touching the representation, then keep it green.
- Include the JSON escapes beyond backslash: ClickHouse's JSON output also escapes `/` as `\/`, tab, line feed and `"`. E-16: `a/b` becomes Column name `a\/b`, a double quote leaves the table **unmapped** (`describeTable` returns null), and a `Nullable(Tuple)` with a slash fails with code 33. Decide, per character, between "decoded" and "documented residual" and test each.
- The test JVM runs `file.encoding=windows-1252` on purpose (STATE.md). Build non-ASCII names with `\uXXXX` escapes (E-16 look-alike `e5_l05` uses `é` and `日本`), read all ClickHouse output with explicit UTF-8, and assert on Column names with a non-ASCII case so a platform-charset reader cannot sneak in.
- Audit every consumer of `Column.getName()` before changing it. The RowBinary insert uses no name (positional), the lookup `record.getJsonMap().get(colName)` and `cleanupExtraFields` do, and `Utils.escapeName` only strips backticks (no backslash escaping), so today's doubled form is accidentally also the SQL-escaped form for a backquoted identifier; a decoded name with a real backslash is not. Check each place a Column name reaches SQL text (the JSON routes' column lists, `alterTableAddColumns`) with a live test, not a read.

**Warning signs:**
The fallback WARN ("Could not take element nullability ... (structure mismatch)") appears for a fixture that was quiet; a `Nullable(Tuple)` backslash-n fixture returns to code 33; `DeclaredNullabilityTest` needs its fixtures rewritten (a sign the representation changed).

**Phase to address:** 999.5 phase, with WR-01 inside it.

---

### Pitfall 8: The 999.5 fix flips which Kafka field name silently works

**What goes wrong:**
Today (identical on gg2 and gg3, E-16): for a root column with `\`, `\n`, a real line break, `/` or tab in its name, a record using the **natural** server name is silently ignored and the column's `DEFAULT` is stored with no error; the connector's doubled form stores the value. For an element inside a Tuple the natural name makes the record fail. After a correct fix the natural name works and the doubled form (which production field names may have been adapted to, as the backlog entry warns) is silently ignored or fails. A fix is correct and a breaking change for the operators who adapted.

**Why it happens:**
Escaped matching was never a documented contract, so operators who hit it worked around it in their producers or SMTs.

**How to avoid:**
- This is a blocking-human decision, like R-01 in gg1: present the user the three options (decode only; decode and accept the legacy doubled field names as a fallback with a one-time WARN per column; leave as documented residual) with the E-16 table and the hot-path cost of a dual lookup (AGENTS.md: avoid per-record allocation in `ClickHouseWriter`). The fork "runs in production", so do not pick for them.
- Whatever is chosen: ship a rollout item with the E-16 detection query (already written and run for gg3), the per-character table of old and new field names, and a statement of what a rollback does to a range left in flight (Pitfall 6).
- Pin both forms live on a real server, both directions, plus the "neither form" control (stores `DEFAULT`).

**Warning signs:**
A silent `DEFAULT` value in a column after the upgrade for a table the detection query lists.

**Phase to address:** 999.5 phase (decision checkpoint first), Release (note).

---

### Pitfall 9: connect-api 4.4.0 is compile and test only; the worker supplies the runtime

**What goes wrong:**
The shipped jar does not contain connect-api or kafka-clients: `confluentJar` is built from the explicit `clickhouseDependencies` list (httpclient5, client, client-v2, lz4, http-client, gson, jackson), which has no Kafka artifact. So the bump to `4.4.0` changes what tests and `javac` see, while production behaviour comes from whatever Kafka Connect worker loads the plugin. Three consequences:
1. **Test and production skew.** `ConfigDef` lives in `kafka-clients`. Kafka 4.2.0 upgrade notes (KIP-1161): LIST-type configs now reject most null values and, for most configs, duplicate entries; empty lists are rejected for some configs. The fork declares LIST configs in `ClickHouseSinkConfig` (`clickhouseSettings`, `topic2TableMap`, `dateTimeFormats`, each defaulting to `""`) and in `HeaderToValue` (`fields`, `headers`, no default). The tests now exercise 4.4 semantics; a worker on 3.x/4.0/4.1 behaves differently, and a worker on 4.2 or later may reject an operator config (duplicate `topic2TableMap` entries, an empty list) that older workers accepted. The connector cannot influence this, but the rollout note must say so.
2. **Compile-time API creep.** Compiling against 4.4.0 can silently use a method that older workers lack (`NoSuchMethodError` only at runtime). The fork already compiles against 4.1.1 (FORK-DELTA line 34: 4.1.1 removed `SinkTask.onPartitionsRevoked`, hence the annotation-less method kept in `ClickHouseSinkTask`). 4.1.1 to 4.4.0 adds a new range of surface.
3. **Emulations pinned to 4.1.1.** `InMemoryDLQ.connectHeaders`, `ThrowableTexts` and `ValueFreeDlqHeadersUnitTest` hand-render the DLQ headers and task-status trace "the way Kafka Connect 4.1.1 renders them"; `connect-runtime` is not on the classpath, so no test can notice a runtime change. Checked now: `DeadLetterQueueReporter.java` at tag `4.1.1` vs `4.4.0-rc4` differ only in an import (`RecordBatch` moved to `common.record.internal`) and `singleton`/`Set.of`; header names and the `printStackTrace` rendering are identical (HIGH, raw source diff). The 4.4.0 release notes list no Connect `SinkTask`, `Converter`, `ConfigDef` or error-handling change (MEDIUM: the notes page was read through a summarising fetch). The unchecked part is the Worker's task-status trace rendering, which `ThrowableTexts` also emulates.

A separate integration hazard: connect-api 4.4.0 pulls `kafka-clients 4.4.0` (compile scope, `connect-api-4.4.0.pom`) onto the same test classpath as the Confluent 7.9.1 test stack, which was built against Kafka `7.9.1-ccs` (about 3.9) and guava 32.0.1 (`common-parent-7.9.1.pom`). Kafka 4.4.0 moves "internal" classes (`org.apache.kafka.common.record.RecordBatch` to `...record.internal`, per the DLQ reporter diff and the release-notes entry KAFKA-19414 "Removes public APIs from releases 4.1 through 4.5"). The fork's own imports are safe (`TimestampType` stays public at `common/record/TimestampType.java` in both versions; only tests import it), and `./gradlew test` is green on this combination. What is not exercised is any Confluent serializer or converter path that touches a moved class, which would appear only as a `NoClassDefFoundError` in `integrationTest`.

**How to avoid:**
- Keep it that way: a jar-content assertion that no `org/apache/kafka/` entry is in the confluent jar (part of Pitfall 1's standing check).
- Record, in the rollout note, the minimum worker version the connector is tested against (none is stated today) and the KIP-1161 behaviour for the five LIST configs. Ask the user which worker version production runs before writing it.
- Re-verify and then update the three emulation comments to say 4.4.0, or leave them and say which version they were verified at. Compare the Worker's task-status rendering source at `4.4.0-rc4` for `ThrowableTexts`.
- Run `./gradlew dependencyInsight --dependency kafka-clients` for `runtimeClasspath` and `testRuntimeClasspath`, and `--dependency connect` to see whether any `connect-json`/`connect-transforms` copy at 3.9 sits beside connect-api 4.4.0.
- One optional local smoke, evidence only, not a CI gate: run `integrationTest` once at gg3 and once at the DEPS tip, and compare the failing set. The nightly is red by inheritance (upstream 43/43), so only a before/after comparison is meaningful; it needs Docker (colima profile `vz`) and depends on Pitfall 1's archive being sound.

**Warning signs:**
`NoSuchMethodError`/`NoClassDefFoundError` naming `org.apache.kafka.*` in the integration run; `org/apache/kafka/` entries in the confluent jar; DLQ-header unit tests that still say 4.1.1 after the bump.

**Phase to address:** Dependency absorption; the note text in Release.

---

### Pitfall 10: protobuf-java 4.x moves the runtime and `protoc` together, and only part of the surface is in CI

**What goes wrong:**
`libprotobuf` is one catalog value used for both the runtime (`protobuf-java`) and the generator (`protoc.artifact = "com.google.protobuf:protoc:${libs.versions.libprotobuf.get()}"`, `build.gradle.kts` line 472). Dependabot #10 (3.25.9 to 4.36.2, still open; both artifacts exist on Central, HTTP 200) therefore regenerates all `src/testFixtures/proto` classes with protoc 4.36.2 **and** bumps the runtime. The "testFixtures only" description is true of the shipped jar (protobuf is not in `clickhouseDependencies`), but it also changes every test that uses protobuf.

Facts: protobuf's cross-version guarantee is that code generated for major V works with runtimes V and V+1, with the Java example "gencode 3.0.0 works with runtimes 3.0.0 to 4.x.y" (protobuf.dev `cross-version-runtime-guarantee`), and poison pills were added in 26.0 so newer gencode against an older runtime fails with a clear error. The 26.x plan listed removal of deprecated APIs (`PARSER` field, `TextFormat` print methods, descriptor syntax APIs) and renaming `GeneratedMessageV3` to `GeneratedMessage` (protobuf.dev news 2023-12-05; planned changes, "can and do change"). Confluent 7.9.1's provider is built against protobuf `3.25.5` (`common-parent-7.9.1.pom`, `protobuf.version`), so it sits at the supported V boundary but calls into 3.x-era API. I found no public report of `kafka-protobuf-serializer` 7.9.x failing on 4.x, and no statement that it works (LOW).

Where it shows: `ProtobufIngestTest`, `ColumnTest` and `ClickHouseSinkTaskWithSchemaTest` use protobuf and live in `src/test`, so `./gradlew test` covers them, unlike the packaged-archive path (Pitfall 1) and `integrationTest`'s `clickhouse_sink_protobuf.json` flow. The risk is therefore less "CI is blind" and more "CI only sees it if #10 is merged before the final gate", and that the Confluent-converter-in-a-worker path is not run at all.

**How to avoid:**
- Settle #10 up front: ask the user to merge or close it before the DEPS merge so the base does not move after the gate (Pitfall 14). The brief expects it merged first.
- After merge, check `dependencyInsight --dependency protobuf-java` on `testRuntimeClasspath` and `integrationTestRuntimeClasspath` (a lower transitive version winning would trip gencode 4.x's runtime check), confirm zero `com/google/protobuf` entries in the confluent jar, and run the three protobuf test classes with `--rerun`.
- If a Confluent-side `NoSuchMethodError` appears, the fix space is: keep runtime 3.25.x and pin `protoc` separately (split the catalog entry), or move the Confluent test stack (7.9.10 and 8.x exist on `packages.confluent.io`). Moving Confluent is a separate decision for the user (AGENTS.md: do not upgrade dependencies without asking).
- A split catalog entry (`libprotoc` beside `libprotobuf`) is worth proposing regardless, so Dependabot cannot move gencode and runtime in one PR.

**Warning signs:**
`UnsupportedOperationException` or `ProtobufRuntimeVersionException`-style messages naming "Detected incompatible Protobuf Gencode/Runtime versions" (protobuf.dev describes poison pills, not the exact text); `NoSuchMethodError` in `io.confluent.kafka.schemaregistry.protobuf.*`.

**Phase to address:** Dependency absorption.

---

### Pitfall 11: The pin guard misreads `build.gradle.kts` the moment the Shadow repair adds a glob

**What goes wrong:**
`TestsWorkflowPinGuardUnitTest.ktsCode` strips block comments over the whole file with `(?s)/\*.*?\*/` before looking for the `reportTestForks` registration (21-REVIEW WR-02, deferred with the re-open trigger "any `build.gradle.kts` edit that adds a string or a `//` comment containing `/*`"). The likely Pitfall 1 repair adds exactly such a string: `exclude("META-INF/*.SF")`, `exclude("META-INF/versions/**/module-info.class")`. A `/*` inside a string literal opens a phantom comment that runs to the next `*/`, can swallow the `reportTestForks` task, and `testUnit` goes red for a reason unrelated to the change. The same file is edited for the `jacocoForkReport` class list (carried item), so two planned edits hit one fragile reader.

**How to avoid:**
Fix WR-02 (a string- and line-comment-aware scanner, with fixtures for `/*` in a string and in a `//` comment) **before** the first `build.gradle.kts` edit of the milestone, as its own commit with its own gate. Then WR-01 and WR-03 together, with fixtures proving both directions.

**Warning signs:**
`TestsWorkflowPinGuardUnitTest` fails with "reportTestForks ... not found outside comments" right after a `build.gradle.kts` change that did not touch that task.

**Phase to address:** Advisory closure for WR-02, sequenced **before** the Shadow repair. This is the one ordering constraint that crosses the "dependency first" convention; the roadmap should either put WR-01..03 first, or make the Shadow repair avoid `/*` in strings.

---

### Pitfall 12: Closing advisories means editing existing tests, and the repo rule says never weaken one

**What goes wrong:**
At least these carried advisories require editing an existing test: 21-REVIEW WR-01..03 and IN-01/IN-02 (`TestsWorkflowPinGuardUnitTest`), 22-REVIEW IN-04 (`ClickHouseSinkTaskWithSchemaTest.schemaWithFixedStringMismatchTest`, assertions only in a `catch`), 23-REVIEW IN-03 (`NullableTupleMatrixLiveTest`), 24-REVIEW IN-03 (`JsonAsStringKeyResidualLiveTest`, which would re-open its live run and FORK-DELTA's raw numbers). AGENTS.md: "Existing tests: only add new tests, never delete/weaken existing ones". Specific traps:
- WR-01 loosens a guard (accept `# was ubuntu-latest` after the pin) while WR-03 tightens others. Loosening without a fixture that still rejects the live floating label is a weakening.
- Adding `fail()` after the call in a catch-only test can turn a test that was green only because nothing was asserted into a red one, or hide that the throw is route-dependent.
- Removing planning identifiers (`D-03`, `22-RESEARCH Pitfall 8`) from `@DisplayName` text changes test IDs that other documents cite (`FORK-DELTA` and the ledgers name tests).

**How to avoid (precedent in `PROJECT.md` Key Decisions):**
- Use the recorded patterns: Phase 23 D-19 "Option B" (keep every existing name and assertion, add rows, move root-only inputs to sibling tests) and Phase 22 L-47 (flip one test to an equivalent-strength assertion with the user's approval at UAT). Ask the user at a checkpoint for any flip that changes what a test asserts.
- Show the fix was seen red: for each edited guard, run the original test against a mutated target and the edited test against the same mutation (the 12 fail-first self-tests of the anchors exemption and the D-17 mutation runs in 23-EVIDENCE are the template). Record the red and green output in the phase evidence, with the mutation reverted via `git checkout --`.
- One commit per advisory with exactly its files (gg3's disposition gate checked that each row's commit touches exactly its finding's files). Never reuse a review ID; number new findings past the old ones (RETROSPECTIVE gg2 Key Lesson 2).

**Warning signs:**
A test method disappears or its `@DisplayName` changes without a ledger row; a diff that shortens an assertion list.

**Phase to address:** Advisory closure.

---

### Pitfall 13: 22-REVIEW WR-01 touches the retry classification that gg3 pinned with 184 cases

**What goes wrong:**
`passesThroughUnchanged` passes any exception whose **cause chain contains an `IOException`** straight through (`ClickHouseWriter` lines 637-653), because retry classification needs to see stream and timeout failures (`Utils.handleException` treats an `IOException` root cause whose message starts with the client read or write timeout text, and `SocketTimeoutException`, as `RetriableException`). `JsonProcessingException` extends `IOException`, which is why a Jackson reference chain can name a Map key on the opt-in JSON-as-string route (measured by `JsonAsStringKeyResidualLiveTest`). The advisory's fix, "exclude `JsonProcessingException` from the `IOException` pass-through", has two failure modes:
- Excluding the **whole chain** when its head is a `JsonProcessingException` would also wrap a genuine `SocketTimeoutException` nested inside it, changing a retriable failure into a non-retriable `DataException`.
- Excluding by `instanceof` on the wrong link changes the exception type the Connect task sees (the existing pass-through yields a `RuntimeException("Number of records: n", e)` wrapper through `handleException`; a `DataException` is rethrown as-is when `errorsTolerance` is off).

**How to avoid:**
Test **per link**: `t instanceof IOException && !(t instanceof JsonProcessingException)` inside the chain walk, so a timeout beneath a Jackson exception still passes through. Extend `WriterFailureClassificationUnitTest` by **adding** rows (JPE alone; JPE caused by `SocketTimeoutException`; JPE caused by a timeout-text `IOException`; plain IOException) and show the existing 184 rows byte-identical in outcome before and after, as WRT-14 did (green on the unchanged writer first). Then flip `JsonAsStringKeyResidualLiveTest` deliberately (Map key no longer reaches the three destinations), update FORK-DELTA Known costs items 6, and note the DLQ/status wording change in the rollout.

**Warning signs:**
Any previously green classification row changes; a timeout test goes non-retriable.

**Phase to address:** Advisory closure.

---

### Pitfall 14: Dependabot keeps merging while the milestone runs, and the published tree must be the tested tree

**What goes wrong:**
The base already moved after the brief (`d20bae63` to `69cd680b`). The gg3 milestone ran under a "Dependabot freeze" that ended at its publication ("The Dependabot freeze is over", STATE.md Blockers). This milestone has none, PR #10 and #4 are open, and the `gradle` ecosystem has produced bursts of PRs on 2026-10-19, -20 and -21. Each merge on origin changes `libs.versions.toml` or `build.gradle.kts`, can move an anchored line (Pitfall 2) and, if it lands between the rehearsal and the publication merge, means the published SHA is a tree no run ever tested. Publication is a `--no-ff` merge onto whatever `custom_build` is then.

**How to avoid:**
- Ask the user for a freeze from the DEPS merge to publication (no merges on origin), or plan a short DEPS-02 pass immediately before the rehearsal. Decide this at roadmap time, not at the gate.
- At the release gate: `git ls-remote origin custom_build` right before the rehearsal push and right before the merge; if it moved, merge, re-gate (anchors, `testUnit`, two `--rerun` runs) and re-rehearse. The rehearsal SHA must equal the SHA that is merged, or the one-run-per-SHA claim is false.
- Standing rules still apply: one branch per SHA (two branches at one SHA cancel each other's run), rollback tag `pre-v1.7.0_gg4` before the merge, tags only after the SHA's own run is green, scratch branch deleted last.

**Warning signs:**
`git rev-list --left-right --count origin/custom_build...HEAD` shows a non-zero left count at the gate; a new Dependabot comment on a PR you thought was closed.

**Phase to address:** Dependency absorption (decision) and Release (gate).

---

### Pitfall 15: Release process defects that recurred in every recent milestone

**What goes wrong:**
- **Operator docs reviewed inline, not independently.** Recurred in Phase 15 and Phase 24 despite a memory note (RETROSPECTIVE gg1 Key Lesson 1, gg3 Key Lesson 1; memory `gsd-phase15-review-gate-gotchas`). gg3's inline review closed at `open: 0` and an orchestrator-spawned reviewer then found five warnings after publication. This milestone's note is large: 999.4, 999.5, connect-api, guava, Shadow jar content.
- **Unsupported quantifiers.** "The four inserts that were run" vs "all"; "safe to roll back" for connectors with three or more topics or `enableDbTopicSplit=true`. The two published corrections requested (gg1 and gg2 notes get the corrected `enableDbTopicSplit=true` key rule and state-table query) are themselves operator instructions that destroy state if wrong: the gg3 reviewer's own Fix text for WR-10 would have reintroduced a false "safe" (RETROSPECTIVE gg3, Key Lesson 2).
- **Verification staleness** (five of five milestones closed `override_closeout`) and the audit detour: gg3 started the close before the audit.
- **`latest` drift.** The suite runs on a floating image (26.9.19.9 at gg3; 26.9.12.8 at the first sync). A red suite on unchanged code: check the image date first (Sync Mechanics).
- **`VERSION`/MBean identity.** `VERSION=v1.7.0_gg4` needs a `clean` build before the generated `Version` constant agrees, and the anchors `VERSION` row must change in the same commit (FORK-DELTA "release-identity traps"); the MBean `version=` segment and client name change again (gg3 note item 5), so the note needs the item again.

**How to avoid:**
- Orchestrator-spawned, fresh-context `gsd-code-reviewer` on every operator document **before** the rehearsal/push gate, repeated until a round finds nothing, never-reused IDs, disposition gate with counts (gg3 pattern). An executor cannot satisfy this.
- Every command and query in a note is run live (gg2/gg3), and every quantifier matches the measured count. A statement derived from reading code is labelled "read from the connector code, not measured".
- Published docs (gg1/gg2 rollout notes) are corrected through a docs-only `--no-ff` merge with a revision notice linking exactly the items that differ from the tagged copy; the tagged trees do not change, so say so (rollback tags and `git read-tree` rollback in gg3 item 7 depend on it).
- Run `/gsd-audit-milestone` before `/gsd-complete-milestone`; plan the override closeout for stale digests, or re-verify at close.
- Two full `--rerun` runs at the release tip on `latest` x V2; record the image version in the evidence.
- Confirm the user-side items on the record: the `dependencies` label exists (the Dependabot "labels could not be found" comment should stop on the next PR), the nightly `integrationTests.yaml` is still disabled and no `schedule` run followed (re-check with the Phase 21 E-13 commands), and the stale `refs/upstream/main` is deleted.

**Warning signs:**
A review round that "found nothing" with no spawned reviewer; a note sentence with "all", "every" or "safe" and no matching measurement.

**Phase to address:** Release.

---

## Moderate Pitfalls

### Pitfall 16: New Shadow 9 defaults that bite outside `confluentJar`

**What goes wrong:** 9.0.0 "Let `assemble` depend on `shadowJar`" (and 9.4.0 added the opt-out `addShadowJarToAssembleLifecycle = false`) means `./gradlew build`/`assemble` now also produce a runtimeClasspath fat jar `*-all.jar`, which, unlike `confluentJar`, includes connect-api and kafka-clients. `createConfluentArchive` copies only `${project.name}-${project.version}-confluent.jar`, so it is not picked up there, but anything that globs `build/libs` would. 9.1.0 adds a `shadowRuntimeElements` variant to the java component (`addShadowVariantIntoJavaComponent`), relevant because `maven-publish` is applied and testFixtures are published locally for the benchmark project. Shadow 9.5.0 raised the minimum Gradle to 9.2.0 (satisfied by 9.8.1, but this couples the two bumps: do not roll back the wrapper alone).
**How to avoid:** list `build/libs` after `assemble` in the evidence; decide per option (leave, or opt out); keep the wrapper and Shadow bumps in one decision record.
**Phase to address:** Dependency absorption.

### Pitfall 17: The Gradle wrapper bump ships a binary

**What goes wrong:** #8 changes `gradle/wrapper/gradle-wrapper.jar` (48,462 to 48,749 bytes) and rewrites `gradlew.bat` (194 lines). A binary in a merge is unreviewable by diff; the properties file has no `distributionSha256Sum`.
**How to avoid:** verify the wrapper jar SHA-256 against Gradle's published release checksums (`https://gradle.org/release-checksums/`) and record it in the DEPS evidence; consider adding `distributionSha256Sum` as a user decision.
**Phase to address:** Dependency absorption.

### Pitfall 18: Standing environment traps

**What goes wrong:** run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (the shell sets the literal string `"null"`, breaking Ryuk); a two-run gate needs `--rerun` (a bare second run echoes the first); Docker is colima profile `vz`; `./gradlew test` exit 255 can be an intermittent OpenJ9 GPF in Mockito inline mocking (delete the multi-GB core dump, re-run once; memory `openj9-mockito-test-jvm-crash`); never inline-mock `ClickHouseHelperClient` in new tests (use the `CapturingClickHouseWriter`/`CountingHelperClient` pattern); `spotlessCheck` is red tree-wide at baseline (46 files, import order only), so never run a tree-wide `spotlessApply`; format a new file via a throwaway worktree (21-RESEARCH Pitfall 3); do not interleave a GSD state commit with an in-progress merge.
**Phase to address:** every phase (gate hygiene).

### Pitfall 19: `.size1` and friends: preserve, do not fix, existing quirks

**What goes wrong:** while in `shouldSkipDescribeRow`, "improving" other behaviour (for example dropping `.size1`/`.size<digits>` rows before `Table`, or treating `.keys`/`.values`) changes the registered Column set, hence `allColumnsList`, with no failing test. Map `.keys`/`.values` are deliberately un-skipped so `Table.updateParent` can consume them (DESC-03 pin).
**How to avoid:** the 999.4 diff should change the decision for the declared-element cases only, shown by the look-alike Column-list comparison of Pitfall 5.
**Phase to address:** 999.4.

### Pitfall 20: Messages and strings operators grep must not change

**What goes wrong:** several advisories edit greppable text: 22-REVIEW IN-03 (share the "Failed to write column " prefix constant) and IN-01/IN-02 (enum label NPE and "No content in enum" texts), 23-REVIEW IN-02 (null-name text in the fallback WARN). The rollout notes tell operators to grep `Failed to write column ` and `have no matching DESCRIBE sub-column`, and the 184-case matrix plus the WARN pins assert current text.
**How to avoid:** refactors must keep rendered text byte-identical (pin before the refactor, as WRT-14 did). Any intended text change is a ledger row and a rollout line, and the note's grep strings are re-run live.
**Phase to address:** Advisory closure.

---

## Minor Pitfalls

- **jacoco class list.** `jacocoForkReport` lists 15 classes and omits `DeclaredNullability` and `WithheldMessageException`; adding them edits `build.gradle.kts` (see Pitfall 11 ordering) and only changes the coverage report. The FORK-DELTA jacoco row must change in the same commit.
- **`slf4j-reload4j` literal vs catalog (21-REVIEW IN-04).** The literal in `build.gradle.kts` and the catalog `slf4j` entry are both 2.0.20 now, but the anchor row pins the literal text; a Dependabot bump of either moves an anchor (Pitfall 2). Closing IN-04 by moving the literal into the catalog changes an anchored line, so do it in the DEPS-style commit that also moves the anchor.
- **Dependabot housekeeping.** PRs #4 (client 0.10.0) stays open and unignored (user decision); if the user adds `ignore` entries for #4's dependency and `connect-api` to `dependabot.yml`, the pin-guard test reads that file (21-REVIEW IN-01, indent style) and the file layout must keep matching its reader.
- **The 12-commit upstream tail also carries upstream's own bumps** (jackson 2.22.3, okhttp 5.5.0, org.json 20260814, mockito 5.24.0) which the fork took in Phase 14; the merge should produce zero net change for them. A non-zero diff there means a conflict resolution mistake (Pitfall 3).
- **Stale doc facts.** `.claude/CLAUDE.md` says "Gradle 7.4.2" and "Kafka Connect API 4.1.1"; update only if the user asks (it is project-instruction text, not a deliverable of this milestone).

---

## Phase-Specific Warnings

| Phase topic | Likely pitfall | Mitigation |
|---|---|---|
| Dependency absorption, step 0 | Base moved again (P14); `origin/custom_build` is `69cd680b` not `d20bae63` | `git ls-remote` + record; decide the freeze with the user |
| Dependency absorption, DEPS-01 merge | Anchors gate fails on many Dependabot SHAs (P2); merge must satisfy its own anchors in one commit | rehearse in a worktree, generate exemption rows from the unmodified gate, D-03 edits inside the merge commit |
| Dependency absorption, build verification | `confluentJar` may be nested (P1); `assemble` builds `-all.jar` (P16); wrapper binary (P17) | build the archive on tip and control, allow-list compare, jar standing assertion |
| Dependency absorption, connect-api | compile/test only (P9); KIP-1161; emulations at 4.1.1 | `dependencyInsight`, no `org/apache/kafka/` in jar, re-verify emulations, record the supported worker version |
| Dependency absorption, protobuf (#10) | gencode and runtime move together; Confluent 7.9.1 at 3.25.5 (P10) | merge or close #10 first; run the three protobuf test classes with `--rerun`; consider splitting the catalog entry |
| Mini-sync | `[versions]` conflict hides downgrades (P3); guava CVE has no ID (P4); ambiguous `upstream/main` | full SHA as `UPSTREAM_REF`, new `PRE_TAG`, anchor `lz4Java`, `dependencyInsight` after resolution, delete `refs/upstream/main` first |
| Advisory closure (first commits) | pin-guard `ktsCode` vs globs added by the Shadow repair (P11) | WR-02 before the first `build.gradle.kts` edit |
| Advisory closure (rest) | editing existing tests (P12), classification of JPE (P13), greppable text (P20) | Option B / L-47 precedent, seen-red mutation evidence, per-link chain test, byte-identical text pins |
| 999.4 | rule wider than one function (P5); Column list is the wire format (P6); preserve `.size1` (P19) | spike over 13 + 19 + `s4_o01` fixtures, declared-type or type-based discrimination, real-loop pin (WR-02), gg3 vs gg4 description diff |
| 999.5 | `fromJsonRow` hack is load-bearing (P7); the silent direction flips (P8); `normalise` WR-01 | user decision checkpoint, name-only decode, pairing re-keyed, WR-01 pin first, UTF-8 and `\u` escapes under `windows-1252` |
| Release | docs review not independent (P15); published SHA differs from tested SHA (P14); `VERSION` traps | orchestrator-spawned reviewer before the gate, re-fetch and re-gate, rehearsal SHA equals merged SHA, tags after green |

## Research Flags For The Roadmap

- **Needs a measured spike before planning:** 999.4 (DESCRIBE capture of the collision shapes and a rule decision), 999.5 (decision and the per-character matrix), Shadow repair (build and compare). These are the three places where an assumption in this document could be wrong.
- **Standard patterns, unlikely to need research:** DEPS-01 merge (gg3 recipe, with the multi-SHA exemption scale-up), advisory closures (precedent decisions exist), release gating.
- **Open questions only the user can answer:** which Connect worker versions production runs; whether to freeze Dependabot merges; what to do with #10; the 999.5 name-form decision; whether the guava advisory ID exists.

## Sources

Fork history (all HIGH, read in the repo):
- `.planning/PROJECT.md` (Current Milestone, Key Decisions rows for Phase 22 L-47 and Phase 23 D-19), `.planning/STATE.md` (Accumulated Context, Blockers), `AGENTS.md`
- `.planning/RETROSPECTIVE.md` (gg1 to gg3 Key Lessons), `.planning/ROADMAP.md` (Sync Mechanics, Phases 999.4 and 999.5)
- `.planning/milestones/v1.7.0_gg3-hardening-phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md` (E-03, E-04), `21-RESEARCH.md` (Common Pitfalls 1-8), `21-REVIEW.md`
- `.../23-declared-nullability-hardening-review-closure/23-EVIDENCE.md` (R-01, R-02)
- `.../24-release-publication/24-COMMAND-EVIDENCE.md` (E-04 jar compare, E-15, E-16), `24-MILESTONE-CLOSE-RECORD.md`
- `docs/FORK-DELTA.md` (rows 47, 51, 68; Known costs 10 and 11; "For the next syncer"), `docs/ROLLOUT-v1.7.0_gg3.md` (items 2, 3, 4, 6, 7)
- `.planning/scripts/verify-merge-invariants.sh`, `fork-anchors.txt`, `fork-anchor-exemptions.txt`
- Code: `ClickHouseHelperClient.shouldSkipDescribeRow` and describe loops, `ClickHouseFieldDescriptor.fromJsonRow`, `Table.java`, `DeclaredNullability.normalise`, `ClickHouseWriter.passesThroughUnchanged`, `Utils.handleException`, `build.gradle.kts` and `gradle/libs.versions.toml` on `origin/custom_build` (`69cd680b`)
- `git merge-tree --write-tree` rehearsals of `sync/v1.7.0` and `origin/custom_build` against `origin/custom_build` and `refs/remotes/upstream/main`; `git diff v1.7.0 refs/remotes/upstream/main`
- GitHub REST, unauthenticated: `actions/runs?branch=custom_build`, `pulls?state=open` (2026-10-21)

External (tier by provider: webfetch LOW, verified websearch MEDIUM; items below marked HIGH were read as primary artifacts):
- HIGH. Shadow `CHANGELOG.md` (9.0.0 to 9.6.1): https://raw.githubusercontent.com/GradleUp/shadow/main/CHANGELOG.md
- HIGH. Shadow 9.6.1 source, `ShadowJar.kt` and `ShadowJavaPlugin.kt`: https://raw.githubusercontent.com/GradleUp/shadow/9.6.1/src/main/kotlin/com/github/jengelman/gradle/plugins/shadow/tasks/ShadowJar.kt
- HIGH. Shadow docs, dependencies and `from` semantics: https://raw.githubusercontent.com/GradleUp/shadow/9.6.1/docs/configuration/dependencies/README.md
- HIGH. Kafka `DeadLetterQueueReporter.java` at `4.1.1` and `4.4.0-rc4`: https://raw.githubusercontent.com/apache/kafka/4.4.0-rc4/connect/runtime/src/main/java/org/apache/kafka/connect/runtime/errors/DeadLetterQueueReporter.java
- HIGH. Maven Central metadata and poms: `connect-api` 4.1.1/4.4.0, `client-v2` 0.9.6 (guava 33.4.6-jre), `protoc` and `protobuf-java` 4.36.2 (HTTP 200), `kafka-protobuf-serializer` versions; Confluent `common-parent-7.9.1.pom` (`protobuf.version` 3.25.5, `guava.version` 32.0.1-jre, `kafka.version` 7.9.1-ccs): https://packages.confluent.io/maven/io/confluent/common-parent/7.9.1/common-parent-7.9.1.pom
- HIGH. GitHub advisories API for `com.google.guava:guava`: https://api.github.com/advisories?ecosystem=maven&affects=com.google.guava%3Aguava ; OSV: https://api.osv.dev/v1/query
- MEDIUM. Kafka upgrade notes 4.1.0 to 4.3.0 (KIP-1161 LIST validation, KIP-1205, KIP-1188): https://kafka.apache.org/43/getting-started/upgrade/
- MEDIUM. Kafka 4.4.0 release notes (no Connect `SinkTask`/`Converter`/`ConfigDef` items; KAFKA-19414): https://downloads.apache.org/kafka/4.4.0/RELEASE_NOTES.html . There is no 4.4 upgrade page yet (HTTP 404 at `/44/getting-started/upgrade/`, 2026-10-21; Maven Central shows 4.4.0 published 2026-10-20).
- MEDIUM. protobuf cross-version guarantee: https://protobuf.dev/support/cross-version-runtime-guarantee/ ; planned 26.x Java changes: https://protobuf.dev/news/2023-12-05
- LOW. Confluent `kafka-protobuf-serializer` 7.9.x with protobuf-java 4.x: no public report found either way.
