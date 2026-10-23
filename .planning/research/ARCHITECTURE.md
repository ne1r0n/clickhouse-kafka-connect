# Architecture Research: v1.7.0_gg4-hardening Integration

**Domain:** Production Kafka Connect sink fork (`ne1r0n/clickhouse-kafka-connect`, branch `custom_build`), subsequent milestone
**Researched:** 2026-10-21
**Scope:** how dependency absorption, a tagless upstream mini-sync, backlogs 999.4 and 999.5, 17+2 carried advisories and the `v1.7.0_gg4` release integrate with the existing code, invariants and proven phase shapes. Nothing in the connector's data path (`Kafka -> ClickHouseSinkTask -> ProxySinkTask -> Processing -> ClickHouseWriter -> ClickHouse`) changes shape. All new work lives in the **build layer**, the **table-discovery layer** (`ClickHouseHelperClient -> ClickHouseFieldDescriptor -> Column -> Table -> DeclaredNullability`), the **writer failure path**, the **test guards** and the **release documents**.
**Overall confidence:** HIGH for sequencing and conflict surface (measured with git), MEDIUM for the 999.4/999.5 designs (read from code, not yet run against a live DESCRIBE), LOW for two named unknowns in "Gaps" at the end.

## 0. Bottom line

1. **Two merges, Dependabot first, upstream tail second.** Measured with `git merge-tree`: merging `origin/custom_build` into `sync/v1.7.0` is conflict-free; merging `upstream/main` (`a6f1df83`) afterwards conflicts in exactly two files (`.gitignore`, `gradle/libs.versions.toml`), in one hunk each. The reverse order conflicts in the same two files and then conflicts in the catalog a second time.
2. **Build order:** absorption (merge 1, merge 2, one baseline) -> 999.4 -> writer residual + enum advisories -> 999.5 -> test/build hygiene (jacoco list last) -> release. Reasoning in section 7.
3. **Three corrections to the milestone brief** that change plans (details in the sections named):
   - `origin/custom_build` is `69cd680b` now, not `d20bae63`: two more Dependabot merges landed (#14 jackson 2.22.4, #15 slf4j-simple 2.0.20). 18 commits past `6a2a87a5`. (section 1)
   - 999.4 has **two** drop points, not one. `Table.SIZE_FIELD_MATCHER` (in `Table.java`, byte-identical to upstream) also drops `size<digits>` children of an `Array` parent. A helper-only fix cannot cover `Array(Tuple(size1 ...))`. (section 2)
   - The stale local ref `refs/upstream/main` is not hygiene, it is a **prerequisite** of the mini-sync: bare `upstream/main` currently resolves to `789a57c2` (the v1.6.0 merge), so `git merge upstream/main` would report "Already up to date". (section 1)

## 1. Dependency absorption and the tagless mini-sync

### 1.1 Measured state (read-only git, 2026-10-21)

| Fact | Value | Command |
|------|-------|---------|
| Local branch | `sync/v1.7.0` = `73cb565d`, 5 commits not on origin (all `.planning`) | `git rev-list --count origin/custom_build..sync/v1.7.0` |
| Origin | `origin/custom_build` = `69cd680b`; 18 commits past published `6a2a87a5`, 20 not in `sync/v1.7.0` | `git rev-list --count sync/v1.7.0..origin/custom_build` |
| Merge base | `a403cc1d` (not `6a2a87a5`: that docs-only merge exists only on origin; its docs equal `8fdfcbfd`'s) | `git merge-base sync/v1.7.0 origin/custom_build` |
| Code/build delta sync vs origin | 5 files: `build.gradle.kts` (1 line, ben-manes), `gradle/libs.versions.toml` (16 lines), `gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradlew.bat` (194 lines, wrapper regeneration) | `git diff sync/v1.7.0...origin/custom_build --stat` |
| Upstream delta | `v1.7.0..upstream/main` = 3 files: `.gitignore` (+5), `build.gradle.kts` (+4/-1), `gradle/libs.versions.toml` (+8/-8). **Zero `src/` changes.** | `git diff v1.7.0..refs/remotes/upstream/main --stat` |
| Merge 1 trial (`sync` + `origin`) | clean, no conflicts | `git merge-tree --write-tree --name-only sync/v1.7.0 origin/custom_build` |
| Merge 2 trial (result of 1 + `upstream/main`) | CONFLICT in `.gitignore` and `gradle/libs.versions.toml`; `build.gradle.kts` auto-merges | same, on a dangling `commit-tree` of merge 1 |
| Reverse order | same two conflicts in step 1, then `gradle/libs.versions.toml` conflicts again in step 2 | same |
| Stale refs | `refs/upstream/main` and `refs/upstream/v1.6.0` both at `789a57c2`; `git rev-parse upstream/main` warns "ambiguous" and prints `789a57c2` | `git rev-parse upstream/main` |

(The trial merges wrote only loose tree/commit objects, no refs, no working-tree change.)

Origin's 18 commits are nine Dependabot PRs (each a bump commit plus a merge): #8 Gradle wrapper 9.6.1 -> 9.8.1, #9 `connect-api` 4.1.1 -> 4.4.0, #6 commons-lang3 3.21.0, #7 ben-manes versions 0.66.0, #13 gson 2.14.0, #12 httpclient5 5.6.5 (both `httpclient` and `httpclientTest`), #11 Shadow 8.3.5 -> 9.6.1, #15 slf4j (catalog) 2.0.18 -> 2.0.20, #14 jackson 2.22.3 -> 2.22.4. Open and not on origin: #10 (protobuf) and #4 (client 0.10.0, out of scope).

### 1.2 Which of upstream's 12 commits the fork already has by content

| Upstream commit | Content | Fork state | Net effect of merging |
|-----------------|---------|------------|-----------------------|
| `166ec6f6` (+merge `74887551`) | jackson 2.22.3 | fork was 2.22.3 (Phase 14), origin now **2.22.4** | no-op; keep fork 2.22.4 |
| `eed5ea26` (+merge `291320e3`) | org.json 20260814 | have | no-op |
| `f890d0a5` (+merge `5dfbfe02`) | mockito 5.24.0 | have | no-op |
| `3570aaf6` (+merge `f4952f38`) | okhttp 5.5.0 | have | no-op |
| `bc896506` | httpclient5 5.6.5, and **removes** `httpclientTest` + the `httpclient5-test` alias; `integrationTest` uses `libs.httpclient5` | `httpclient` already 5.6.5 via Dependabot #12 | the alias removal is real new content; must also delete the fork-side `httpclientTest = "5.6.5"` line |
| `fc82b263` | lz4Java 1.10.4 -> 1.11.4 | fork **1.12.0** (ahead, advisory-free floor is 1.11.4) | keep fork 1.12.0; FORK-DELTA "upstream value" text becomes 1.11.4 |
| `30521f74` | `guava = "33.7.2-jre"`, `implementation(libs.guava)`, `clickhouseDependencies(libs.guava)` ("temp to override client's with CVE"), `.gitignore` eclipse lines | absent | **the only substantive new content**, plus the `.gitignore` union |
| `a6f1df83` | merge of PR #859 | - | the second parent |

Net: the "mini-sync" is a catalog/build/`.gitignore` merge. It cannot change connector behaviour. Its value is the guava pin, the closed `httpclientTest` alias, and the next tagged sync starting from `a6f1df83`.

### 1.3 The proven shape (DEPS-01, Phase 21) and what is reused

DEPS-01 (`82524e12`, plans 21-02/21-03) = a two-parent `--no-ff` merge of `origin/custom_build` into the sync branch, with the **bookkeeping edits made inside the merge** (`git merge --no-commit`, edit `fork-anchors.txt` + FORK-DELTA rows, then commit; D-03) so the merge commit satisfies its own anchors file; an SHA-pinned exemption row for the one published Dependabot commit that predates the anchor move (D-01/D-02); a self-test of the exemption mechanism; `testUnit` + four-source-set compile + `dependencyInsight` + Lombok-warning check + spotless baseline-equal as evidence (E-03..E-08). Reuse all of it. What differs is scale.

**Measured: the gate is not green on the raw merge.** Running the *unmodified* gate against a dangling rehearsal merge of `sync/v1.7.0` + `origin/custom_build` (`verify-merge-invariants.sh 54e63648 <rehearsal>`, ~10 minutes, 621 commits in `pre-v1.7.0-sync..`) gives 19 `ANCHOR VIOLATION` lines and `anchor_exempt=1`:

| Violation | Commits | Cause | Resolution |
|-----------|---------|-------|------------|
| `gradle/libs.versions.toml 'kafka = "4.1.1"' (BLD-08)` | 15: 14 published origin SHAs + the merge itself | `connect-api` 4.4.0 (#9) reached every later origin commit | merge commit: move the row to `kafka = "4.4.0"` (D-03). 14 SHA-pinned rows in `fork-anchor-exemptions.txt` |
| `build.gradle.kts 'slf4j-reload4j:2.0.17' (BLD-08)` | 4: `6ad5b80c`, `d84272d7`, `6d3827a2`, `2ccd7486` | Dependabot branches cut from `6473e24f`, whose own anchors copy still says 2.0.17 but whose tree has 2.0.20 | 4 more SHA-pinned rows (`6ad5b80c` needs both a `kafka` and a `slf4j` row: 18 rows total, 17 distinct SHAs) |

Rows are mechanical: generate them from the gate's own stderr, assert "exactly these and no other violation", never edit the script (D-02). A future Dependabot bump of any anchored line produces the same class of violation, so budget this as routine.

### 1.4 Recommendation: two merges, in this order

**Merge 1 (DEPS-02): `origin/custom_build` into `sync/v1.7.0`.** Conflict-free; every red result afterwards is attributable to the nine Dependabot bumps and nothing else (no guava, no alias removal confounding). Steps:

1. Pre-flight, before any merge: delete the two stale refs (`git update-ref -d refs/upstream/main`, `refs/upstream/v1.6.0`; user-visible repo-local ref cleanup, no remote effect); `git fetch origin`; re-check whether #10 or any further Dependabot PR landed. If `origin/custom_build` moved past `69cd680b`, merge the actual tip. Record `SELECT version()` of the `latest` image.
2. Local rollback tag at the pre-merge tip (local only until release, per "milestone tags stay local").
3. `git merge --no-ff --no-commit origin/custom_build`; in the same commit: `kafka = "4.4.0"` anchor row, FORK-DELTA rows (pins row 68: kafka 4.4.0, gson, jackson 2.22.4, httpclient 5.6.5, commons-lang3, ben-manes, Shadow 9.6.1, Gradle 9.8.1; row 65 slf4j wording; row 34 `onPartitionsRevoked` "re-verify if the Connect API pin changes" -> record the 4.4.0 result), 18 exemption rows.
4. Gate: `testUnit` (Docker-free), `clean compileJava compileTestJava compileIntegrationTestJava compileTestFixturesJava`, the anchors gate (cumulative form, 18 exemptions), then **one full suite run** (`latest` x V2; CI shows ~390 s so this is cheap). Treat the two production-reaching majors explicitly:
   - **Shadow 9.6.1** reaches the shipped artifact (`confluentJar` is a hand-registered `ShadowJar` fed by `from(clickhouseDependencies, sourceSets.main.get().output)`). Use the Phase 24 E-04 method: build `createConfluentArchive` in throwaway worktrees at the pre-merge tip and the merge, diff the jar entry lists, `META-INF/services/*` (the fork's `Transformation` service file registers `ConvertToMapOrJson`, `HeaderToValue`, `HeadersToValue`, `TableOperationFilter`, plus upstream's `ExtractTopic`, `KeyToValue`), the manifest, and per-dependency versions. Expected: only version changes.
   - **connect-api 4.4.0** is `api(...)` and is *not* in the archive (the worker provides it), so it can only change compile-time linkage. Prove it the same way E-04 proved Lombok: compare compiled `src/main` class bytes (or `javap -c` constant-pool references to `org.apache.kafka.*`) between the 4.1.1 and 4.4.0 builds. Byte-identical = no operator effect, and the rollout item becomes one sentence. The FORK-DELTA row for `onPartitionsRevoked` (no `@Override`, kept because Connect 4.1.1 removed it from `SinkTask`) must be re-verified against 4.4.0 by compile.
5. **PR #10 (protobuf 4.36.2)** is *more* than "testFixtures only": the single catalog line `libprotobuf` feeds both `protobuf-java` (testFixturesApi) **and** `protoc` (`build.gradle.kts:472`, `com.google.protobuf:protoc:${libs.versions.libprotobuf.get()}`), so the generated test classes change too, and Confluent 7.9.1 serializers were built against 3.25. It is the likeliest red in the batch. Keep it a separate, revertable step: if the user has merged it on origin before merge 1 starts, it is in the batch (and its red is attributable via the one-line catalog diff); if not, do not wait for it. Treat "PR #10 stays open/closed" as an acceptable outcome, not a milestone failure.

**Merge 2 (SYNC-TAIL): `a6f1df839c079180bc7c343b5823f205e204dafc` into the post-merge-1 tip.** Local rollback tag first (name it distinctly from the release rollback tag, e.g. `pre-v1.7.0-tail`; `pre-v1.7.0_gg4` is reserved for the release-time `custom_build` tip). Resolve:

| File | Hunk | Resolution |
|------|------|-----------|
| `gradle/libs.versions.toml` `[versions]` (single hunk) | fork side: `kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)`, gson 2.14.0, jackson 2.22.4, httpclient 5.6.5, **httpclientTest 5.6.5**, lombok 1.18.48, lz4Java 1.12.0. Upstream side: kafka 2.7.0, gson 2.13.1, jackson 2.22.3, httpclient 5.6.5, lombok 1.18.46, lz4Java 1.11.4, **guava 33.7.2-jre** | keep fork side for every shared key; **add** `guava = "33.7.2-jre"`; **delete** `httpclientTest` (its alias is already gone from `[libraries]` in the auto-merged result). `provenance` invariant holds: every kept line exists in one parent |
| `.gitignore` | fork appended `/bin/`, `.vscode`, `.gsd`; upstream appended `# ecliose`, `.classpath`, `.project`, `.settings/` | union, fork lines first |
| `build.gradle.kts` | auto-merges: `implementation(libs.guava)`, `clickhouseDependencies(libs.guava)`, `integrationTest` -> `libs.httpclient5` | no action; verify |

Gate for merge 2 (this is the shape `verify-merge-invariants.sh` was written for):

```
PRE_TAG=pre-v1.7.0-tail \
UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc \
CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt \
bash .planning/scripts/verify-merge-invariants.sh <merge-2-sha> HEAD
```

- `UPSTREAM_REF` must be a ref that resolves to the second parent. **Use the full SHA** (or `refs/remotes/upstream/main` fetched and then compared to the SHA); never bare `upstream/main` (stale ref). There is no tag, so ROADMAP "Sync Mechanics: upstream ref is the tag" is amended to "a pinned SHA when untagged".
- `PRE_TAG` must be an ancestor of merge 2's first parent; a tag at the post-merge-1 tip satisfies that and **excludes the 17 Dependabot SHAs from invariants 2 and 3**, which is what keeps this gate free of the 18 exemptions. (Invariant 0 requires exactly two parents and the second parent == `UPSTREAM_REF`, so this shape cannot validate merge 1; merge 1 uses the cumulative form of 1.3.)
- `CONFLICTED_PATHS_FILE` is cross-checked against `merge-tree` of the two parents, so a new file listing `.gitignore` and `gradle/libs.versions.toml` must be committed in the same commit (else `CONFLICTED-PATHS MISMATCH`). Do the same D-03 trick: write it between `--no-commit` and commit.
- Add a short `MERGE-CHECKLIST-v1.7.0-tail.md` (Section 1: the three paths above; Section 2: the 3 upstream-changed paths). Do not extend `MERGE-CHECKLIST-v1.7.0.md`.
- Evidence for the guava claim: `dependencyInsight --configuration clickhouseDependencies --dependency guava` before and after, plus the shipped-archive diff. **Whether guava is in the archive today is unmeasured** (the cached client poms show no guava; the Confluent test stack carries 32.1.3/33.4.6 in the Gradle cache). Do not write "guava is added" or "CVE fixed" in the release documents until this evidence exists, and upstream names no CVE id (PR #859 text); the advisory id must be identified by us.

Why this order, in one paragraph: (a) merge 1 is clean and carries the only changes that can reach production (Shadow, connect-api), so isolating it keeps the first gate attributable; (b) merge 2 has the only conflicts and no `src/` content, so its gate is cheap and the resolution is one hunk; (c) the reverse order resolves the catalog hunk twice and mixes guava into the first gate; (d) the gates' `PRE_TAG` mechanics favour a tag between the two; (e) "every later gate runs on the post-absorption build" is satisfied by the full-suite baseline after merge 2.

### 1.5 Operator and bookkeeping items inside this phase

- The user creates the `dependencies` label (UI action, D-11 pattern: recorded in the user's own words, never relayed). The "labels could not be found" comment only reappears or stops on the **next** Dependabot PR (monthly schedule), so the milestone can verify the label exists, not that the comment stopped. Define the success criterion accordingly.
- PR #4 stays open and unignored (user decision).
- Skip the Dependabot-ignore edits for #4/#9 unless the user asks (the close record suggests them as optional).

## 2. Backlog 999.4: Tuple element named `size` / `size0` (and `size<digits>` under Array)

### 2.1 Today's flow

```
DESCRIBE TABLE db.t FORMAT JSONEachRow, describe_include_subcolumns=1
  -> describeTableV1 (response.records(), v.asString())   \  same loop body,
  -> describeTableV2 (BufferedReader.readLine(), UTF-8)   /  duplicated
     per row:  ClickHouseFieldDescriptor.fromJsonRow
               numColumns++ (root rows only, before any skip)
               alias/materialized/ephemeral -> skippedCols
               parent in skippedCols        -> skip
               shouldSkipDescribeRow(row, columnsByName) -> skip     <- fork (HLP-02, DESC-05)
               declaredTypes.put(name, rawType) (root rows only)     <- fork (COLS-10)
               Column.extractColumn -> columns, columnsByName
     buildDescribedTable(...) -> new Table(...)  (upstream-identical; routes sub-columns to parents)
                              -> DeclaredNullability.apply(root, declaredTypes[root]) per Tuple-bearing root
```

`shouldSkipDescribeRow` (package-private static, `ClickHouseHelperClient.java:473`): non-sub-columns never skip; names ending `.null`, `.size0`, `.size` skip; then the **DESC-05 ancestor walk** (`for dot = lastIndexOf('.'); dot > 0; dot = lastIndexOf('.', dot-1)`) skips any row with a `JSON`/`DYNAMIC` ancestor registered in `columnsByName`. DESCRIBE lists parents before children, so ancestors are present.

### 2.2 The second drop point

`Table.updateParent`, `ARRAY` case: `if (SIZE_FIELD_MATCHER.test(childName) || childName.endsWith(".null")) return;` with `SIZE_FIELD_MATCHER = ".+\\.size[0-9]+$"`. `Table.java` is *adopted wholesale* (`git diff --quiet v1.7.0 HEAD -- Table.java` exits 0; MAP-03). So for `Array(Tuple(size1 UInt16, v String))` the `r.size1` row passes `shouldSkipDescribeRow` today (it only checks `.size0`/`.size`) and is then silently discarded by `Table`. FORK-DELTA Known costs item 10 and the 24-COMMAND-EVIDENCE E-15 fixtures (`s4_a08` `Array(Tuple(size1 ...))`, `s4_a09` `size12`, `s4_a10`, `s4_a11`) list these shapes; the ROADMAP backlog text and the milestone brief say "only `size` and `size0`", which is narrower than the measured defect. **Decision needed at plan time: fix `size<digits>` too (requires a one-line patch to an adopted-wholesale file) or scope to `size`/`size0` and keep a documented residual.** Recommendation: fix it. A Tuple element named `size1` is no rarer than `size`, and the two-patch mechanism below is small.

### 2.3 Where the declared element list is available

- At loop time `columnsByName` holds `Column`s whose `tupleFields` is **empty** (`Column.extractColumn` builds `new ArrayList<>()`; `Table` fills it later). The Column cannot tell which elements exist.
- `declaredTypes` holds the raw DESCRIBE type of **root** rows only.
- The parent *row's* own raw type is available for every registered row (including `m.values`, `r.x` sub-columns) if the loop records it (it currently throws it away for sub-columns).
- `DeclaredNullability.collectTuple` already derives per-Tuple element keys from `ClickHouseColumn.getNestedColumns()` (`getColumnName()`, or `String.valueOf(i + 1)` for unnamed). That key function is the single definition of "element name" in the codebase.

### 2.4 Proposed design (per-row rule, no path reconstruction)

For a sub-column row `P.x` whose name ends in `size`, `size0` or `size<digits>`: **keep it iff** the parent row `P` was registered, its raw type read through `Array`/`Nullable`/`SimpleAggregateFunction`/`LowCardinality` (not through `Map`; a map value is its own `m.values` row) is a `Tuple`, and that Tuple declares an element whose key (the `DeclaredNullability` key function) equals `x`. Disambiguation of the one real collision, `Array(Tuple(size0 ...))` where the server emits both the structural `P.size0` (`UInt64`) and the element `P.size0` (`Array(..)`): the structural row has type exactly `UInt64` and an Array-wrapped parent's element rows are `Array(..)`; apply the rule on `(parent raw type, segment, row type)`. A `String` parent's `P.size` (UInt64) fails the Tuple test, so structural rows stay dropped (`c.name.size`, `d.null`, `e.size0` under `Array`).

Fail-open: if the parent's raw type does not parse, behave as today (skip). Do not log: `DeclaredNullability.apply` logs the one metadata-only WARN for the same condition.

Second patch (only if `size<digits>` is in scope): mark kept elements (`Column` gains a boolean such as `declaredTupleElement`, set by the loop; `Column` is already a fork-edited file with `@Setter`) and change `Table.updateParent`'s `ARRAY` case to `(SIZE_FIELD_MATCHER.test(childName) && !child.isDeclaredTupleElement())`. That moves `Table.java` out of "Adopted wholesale" (FORK-DELTA row 104 changes, a fork-anchors row for the new line, new tests in a **new** `TableSizeNamedElementTest`, leaving upstream's `TableTest` byte-identical).

### 2.5 WR-02: extract the DESCRIBE loop (is it a safe `src/main` edit?)

**Yes, with these conditions; it should be its own refactor plan *before* the fix.**

- New fork-owned class in `sink/db/helper`, e.g. `DescribedTableBuilder` (mutable, one per DESCRIBE): `boolean accept(ClickHouseFieldDescriptor row)` (false = "unable to handle column", caller returns `null` exactly as today) and `Table build(database, tableName)`. It owns: `numColumns`, `hasDefaults`, `skippedCols`, `columnsByName`, `declaredTypes`/raw-type map, the alias/materialized/ephemeral skip, the parent-skipped skip, the structural skip, `Column.extractColumn`, then calls the existing static `buildDescribedTable`. `describeTableV1`/`V2` keep only I/O, `fromJsonRow` and their own exception handling, and stream rows into `accept` (streaming preserved).
- `shouldSkipDescribeRow` and `buildDescribedTable` stay where they are with their current signatures (keep a 2-argument overload that delegates), so the ~20 existing call sites in `ClickHouseHelperClientForkUnitTest` compile unchanged: additive, rebasable.
- Safe because: it runs once per DESCRIBE (periodic `TableMappingRefresher` and `ClickHouseWriter.start`), never per record; it does not touch `Processing`, `KeeperStateProvider`, batch composition or the `BEFORE`/`AFTER` sequence; the result `Table` is built from the same inputs in the same order. Its behaviour is pinned by `ClickHouseHelperClientForkLiveTest`, `NullableTupleMatrixLiveTest`, `SimpleAggregateTupleRoundTripLiveTest` and `TableTest` on the real server.
- Costs to name: (1) `describeTableV1` is **not exercised by CI** (suite is `latest` x V2 only). The extraction makes both paths share one tested unit, but record one manual `CLIENT_VERSION=V1` run of the two helper live tests as evidence. (2) The two methods stop matching upstream's text, so a future upstream edit of those loops conflicts; FORK-DELTA's "for the next syncer" section must say "re-apply per-row changes in `DescribedTableBuilder`". (3) The fork-anchors row `ClickHouseHelperClient.java 'shouldSkipDescribeRow' 3 HLP-02` drops to 1 (definition only); add rows for the builder file (`shouldSkipDescribeRow(` 1) and for the two `new DescribedTableBuilder(` call sites (2). Move the anchors in the same commit.
- Lighter fallback if the user prefers fewer upstream-region changes: extract only the fork-added state (`columnsByName`, raw-type map, skip decision) into a small helper and leave upstream's `skippedCols`/`numColumns` lines inline. WR-02 is then only partly closed (the test copy shrinks to upstream-only lines). Not recommended.

### 2.6 Interaction with COLS-14 and the D-04 pin

- The unpaired-element WARN (`DeclaredNullability.UNPAIRED_ELEMENT_WARNING`, `collectTuple`) stays. After the fix it stops firing for `size`/`size0`/`size<digits>` elements because the element now pairs; it still fires for any future unpaired cause. Its independent pins (`tupleRootRowAloneLogsOneUnpairedElementWarning`, `repeatedDiscoveryOfARootRowAloneWarnsEveryTime`, D-19) do not depend on the size defect, so they stay green and prove the WARN still works.
- The D-04 test `sizeNamedTupleElementDroppedByDiscoveryIsNamedInOneWarning` (3 cases from 23-EVIDENCE R-01) currently asserts the *defect* through `discoverAsDescribeTableV2` (a copy of the loop). It must flip: element present as a Column, zero WARNs, every field carries its declared nullability (the `size0` case keeps `other` non-nullable although DESCRIBE reported `Nullable(String)`), and the driver becomes the real `DescribedTableBuilder`. "Existing tests: never delete/weaken" applies in spirit: record the flip in the plan as a deliberate expectation change tied to the backlog, as 19-10/23 did for D-11.
- Add the other E-15 shapes as unit cases (`Map(String, Tuple(size ...))`, `Tuple(x Tuple(size ...))`, `Tuple(x Array(Tuple(size0 ...)))`, `Array(Tuple(size12 ...))`, `Array(Array(Tuple(size1 ...)))`) and keep the look-alikes (`Array(Map(String, Tuple(size1 ...)))` is a known over-flag of the *suspects query*, not of the connector).

### 2.7 Deliverables checklist (the roadmapper should make each a plan task)

| Item | File(s) |
|------|---------|
| Live DESCRIBE matrix spike (tracer): dump rows for the E-15 shapes on `latest` | scratch, evidence file; follows the Phase 19 spike / Phase 23 D-03 precedent |
| Characterization first: behaviour-preserving extraction, all existing tests unchanged and green | `DescribedTableBuilder` (new), `ClickHouseHelperClient` (modified) |
| Fix: declared-element rule | `DescribedTableBuilder`, `DeclaredNullability` (one public key helper), optionally `Column` + `Table` |
| Live round-trip pin: insert and read back with `assertEquals` for the matrix on client V2 | new `TupleSizeNamedElementRoundTripLiveTest` (Docker, `latest` x V2); includes a DESC-05 regression row (JSON/Dynamic descendants still skipped) |
| Unit pins | `ClickHouseHelperClientForkUnitTest` flip; new `DescribedTableBuilderUnitTest`; `TableSizeNamedElementTest` if `Table` is patched |
| FORK-DELTA | row 47 (`shouldSkipDescribeRow` sentence about size elements removed), row 51 (the 999.4 clause), new row for `DescribedTableBuilder`, Known costs item 10 closed, "Adopted wholesale" row 104 if `Table` patched, raw numbers at release only |
| `fork-anchors.txt` | HLP-02 count, new builder rows, `Table` row if patched, in the same commits |
| Operator note | behaviour change: tables that failed with code 32/33 now insert; WARN disappears for these shapes; rows not repaired; rollback to `v1.7.0_gg3` reintroduces the failure |

## 3. Backlog 999.5: `fromJsonRow` doubles escaped name characters

### 3.1 Data flow

```
server JSONEachRow row                       {"name":"a\\b","type":"Tuple(`a\\\\b` String)", ...}
  ClickHouseFieldDescriptor.fromJsonRow(json)   json.replace("\\","\\\\").replace("\n","\\n")  then Jackson
      => every JSON escape survives as literal text: name  a\\b (2 backslashes), a/b -> a\/b, TAB -> a\tb, LF -> a\nb
      => a double quote becomes \\" which ends the JSON string: JsonProcessingException -> describeTable returns null -> table unmapped
  Column.extractColumn(descriptor)               Column.name = the doubled form; extractEnumValues strips quotes from labels in the same domain
  Table.handleNonRoot                            parent routing by substring(0, lastIndexOf('.')) over the doubled names
  DeclaredNullability.apply                      normalise(type) turns the structural line break (backslash-n outside quotes) into a space;
                                                 byKey = ClickHouseColumn element names vs Column.getName().substring(prefix)
  ClickHouseWriter                               record field name must equal Column.name: doWriteCol / doWriteColValue (tuple fields use the
                                                 last '.'-segment of the Column name), cleanupExtraFields (m.containsKey(c.getName()) drops
                                                 non-matching fields -> DEFAULT stored, no error), getMissingColumns (auto.evolve, not adopted)
```

The doubling is **load-bearing**: it is what puts DESCRIBE row names and `ClickHouseColumn.of` element names into the same escaped domain so `DeclaredNullability.collectTuple` can pair them by string equality, and why `normalise` needs its quote-aware run-length arithmetic ("the JSON layer doubles every backslash inside a quoted name"). This is inferred from reading the code and the rollout item 3 table (measured on 26.9.19.9); it has **not** been re-run for this research.

`MetadataFreeJsonWriter` is **not** a consumer: it never calls DESCRIBE and writes record keys as given (`writeUTF8String` escaping only), so it already uses the *natural* name form. That is a consistency argument for the natural form: the RowBinary route is the odd one out.

### 3.2 Single decode point

`ClickHouseFieldDescriptor.fromJsonRow` (byte-identical to upstream `v1.7.0` today, so the fix is a new fork delta; its only callers are the two helper-client loops, and tests `NullableRoundTrip`, `DeclaredNullabilityTest`, `DeclaredNullabilityFixtures`). The decode must be applied to JSON string values generally (name, type, comment, expressions), which also changes:

1. The **type** string: the structural multi-line break becomes a real LF instead of the two characters backslash-n, so `ESCAPED_NEWLINE`, `normalise` and its WR-01 branch largely collapse (a real LF cannot occur inside a quoted name because the server prints `\n` there; a blanket LF -> space is then correct). Every test using `DeclaredNullability.ESCAPED_NEWLINE` (3 live tests, `DeclaredNullabilityFixtures`, ~7 sites in `DeclaredNullabilityTest`) is an existing pin of the old domain and must be re-based deliberately.
2. The **pairing key** in `collectTuple`: `ClickHouseColumn.of` keeps names in type-text escape form (`a\nb` as 2 chars). After a natural decode the row names are natural (LF), so the key function needs an unescape of CH identifier escapes before comparing. This is the same helper section 2.4 reuses, which is the architectural reason to land 999.4 first with the key function public and unit-tested.
3. **Enum labels** (`Column.extractEnumValues` strips quotes from the type text, no unescape): labels containing backslash change form. Include a backslash enum label in the live matrix (related advisory 22 IN-01, unknown label reported as NPE).

### 3.3 Decision to take before coding (blocking-human)

| Option | Effect | Recommendation |
|--------|--------|----------------|
| A. Natural names (decode in `fromJsonRow`, fix pairing key) | Kafka fields use the server's real name; RowBinary matches the JSON route; quote-in-name tables become mapped; breaking for operators who adopted the doubled form | **Recommended**, with a rollout item |
| B. Keep doubled form as the contract; fix only the `\"` crash; pin | no behaviour change except the crash | only if the user wants zero operator impact |
| C. Accept both forms | needs a second lookup in the writer hot path | reject |

The rollout item can reuse the gg3 note's item 3 detection query (it lists exactly the affected tables) and the forms table (backslash, backslash-n, LF, slash, tab); the double-quote row changes from "table unmapped" to "maps".

### 3.4 Test shape that pins both forms

- Docker-free: new `ClickHouseFieldDescriptorTest` over `fromJsonRow` for `\\`, `\"`, `\/`, `\t`, `\n`, `é`, raw multi-byte, a multi-line type, an empty-quote name. Plus the WR-01 pin (backslash run before a quote keeping it open) written against the **current** `normalise` and landed green *before* the change (precedent: Phase 22 WRT-14 landed its classification pin on the unchanged writer first). If option A deletes that branch, the pin moves to the new key function rather than being deleted.
- Live (`latest` x V2): `NameEscapeRoundTripLiveTest`: root and Tuple-element names with a backslash, backslash-n, LF, slash, tab, backtick, double quote and a Unicode letter, plus an enum label with a backslash. For each: a record using the **natural** name stores and reads back `assertEquals`; a record using the **doubled** name is asserted to behave as the new truth (root column: field ignored, DEFAULT stored; Tuple element: record fails) so a later regression in either direction is visible. Run the same matrix on the unchanged code first to record the baseline, then flip.
- Spike first (go/no-go on the design): a live DESCRIBE dump of the name matrix, as Phase 19 and Phase 23 D-03 did. Names containing `.` break `Table.handleNonRoot` and the writer's `split("\\.")` independently of this fix; list them as a known adjacent limitation, not in scope.

## 4. Advisory closure: grouping by component

17 carried review findings (`24-MILESTONE-CLOSE-RECORD.md`) + 2 other carried items = 19. Counts reconcile: 21-REVIEW 6 (WR-01..03, IN-01, IN-02, IN-04), 22-REVIEW 6 (WR-01, IN-01..IN-05), 23-REVIEW 5 (WR-01, WR-02, IN-01..IN-03) = 17.

| Component | Items | Overlap with 999.4/999.5 | Notes |
|-----------|-------|--------------------------|-------|
| `TestsWorkflowPinGuardUnitTest` (test only, `src/test/.../ci/`) | 21 WR-01 (runs-on trailing comment), WR-02 (`ktsCode` not string/`//` aware), WR-03 (fork-count rules accept non-live text), IN-01 (dependabot parsing), IN-02 (fixture list duplicates the production constant) | none | one file; edit once. Reads `build.gradle.kts` and `tests.yaml`; do it after absorption (build file changed: Shadow line, ben-manes) |
| `build.gradle.kts` + catalog | 21 IN-04 (`slf4j-reload4j` literal vs catalog); the `jacocoForkReport` class list | jacoco: touches the same list 999.4/999.5 want extended | IN-04 is currently drift-free (catalog `slf4j` and the literal are both 2.0.20 after #15); closure = declare `slf4j-reload4j` in the catalog with `version.ref = "slf4j"` and move the BLD-08 anchor/FORK-DELTA row (row 65). **Do the jacoco list edit once, in the last code phase**, so one edit adds `DeclaredNullability`, `WithheldMessageException`, `DescribedTableBuilder`, `ClickHouseFieldDescriptor` and any other new fork class; its comment currently says `ClickHouseFieldDescriptor` can "never be pulled in" and must be updated. Class entries use exact names, so a new top-level class is **not** covered by `ClickHouseHelperClient\$*.class` |
| `ClickHouseWriter` (high-risk file) | 22 WR-01 (exclude `JsonProcessingException` from the `IOException` pass-through, with a sentinel Map-key test), 22 IN-03 (duplicated metadata derivation between the `ClassCastException` branch and the helper), 22 IN-05 (planning ids in `src/main` comments) | none (no discovery code) | one phase, classification pin green on the unchanged writer first (WRT-14 pattern). WR-01 **flips** `JsonAsStringKeyResidualLiveTest` from "Map key reaches status" to "no sentinel anywhere": FORK-DELTA Known costs item 6 and the rollout/CHANGELOG exception disappear; this is an operator-visible improvement and a rollout line |
| `Column.convertEnumValues` | 22 IN-01 (unknown label reported as `NullPointerException`), 22 IN-02 (fixed `No content in enum` text drops the column name) | enum-label domain also moves under 999.5 | ride with the writer phase; extend the 999.5 live matrix with a backslash label |
| `DeclaredNullability` | 23 WR-01 (pin the escaped-quote branch of `normalise`), 23 IN-01 (`apply` Javadoc "all or none"), 23 IN-02 (null-name check inside `try`, logs `null`) | **same file and function 999.5 rewrites** | land with 999.5 (WR-01 pin first, against the current form). 999.4 only adds one public key helper here |
| 999.4 test | 23 WR-02 (`discoverAsDescribeTableV2` copy) | is 999.4 | closed by section 2.5 |
| Tests only | 22 IN-04 (`schemaWithFixedStringMismatchTest` passes if `put` does not throw), 22 IN-05 half (`CountingHelperClient.useClientV2Calls` never asserted), 23 IN-03 (`NullableTupleMatrixLiveTest` premise loops pass vacuously, bare NPE lookups), 24 IN-03 (`JsonAsStringKeyResidualLiveTest` planning ids in Javadoc/`@DisplayName`) | 24 IN-03 shares a file with writer WR-01's flip | `ClickHouseSinkTaskWithSchemaTest` already differs from upstream `v1.7.0`, so adding `fail(...)` after `put` costs no new rebase surface; prefer it over a new test. Put 24 IN-03 in the writer phase (the file is rewritten there anyway); the other three in the hygiene phase |

Cut: **writer-failure phase** = {22 WR-01, IN-01, IN-02, IN-03, IN-05 (src/main half), 24 IN-03}; **names phase** = {999.5, 23 WR-01, IN-01, IN-02}; **discovery phase** = {999.4, 23 WR-02}; **hygiene phase** = {21 WR-01..03, IN-01, IN-02, IN-04, 22 IN-04, 22 IN-05 test half, 23 IN-03, jacoco list}. No advisory is in two phases and no file is edited by two phases except `DeclaredNullability.java` (discovery adds a helper, names rewrites), `FORK-DELTA.md` and `fork-anchors.txt` (each phase edits only its own rows, as in Phases 21-24).

## 5. Release integration

Proven shape (Phase 24, 7 plans, waves 1-7): version bump with anchor row and MBean/client-name probe (tracer); committed live tests and evidence file; shipped-archive diff; measured inputs for the note; `ROLLOUT-*.md` with self-checks per item and rollback commands re-run on a dry-run merge of the tip; CHANGELOG section; FORK-DELTA rows and raw numbers (refreshed in the release phase only); milestone close record; **independent `gsd-code-reviewer` pass on the operator documents before the gate, at most three rounds**; Docker-stopped `testUnit`; two full runs (second with `--rerun`); anchors gate; OPS re-check; rehearsal push of the sync branch as a scratch branch; rollback tag at the pre-merge `custom_build` tip; `--no-ff` merge from a throwaway worktree with the merged tree equal to the rehearsed tip; push `custom_build` only; exactly one green `tests.yaml` run on that SHA; tag pushed by name afterwards; scratch branch deleted. Approval gates run orchestrator-inline with the user's own words. `custom_build` is protected; never checked out for work.

What differs for `v1.7.0_gg4`:

1. **Origin already contains the Dependabot merges**, so the release rehearsal's "re-fetch `origin/custom_build`, merge it if it moved" rule still applies (Dependabot is monthly; the next batch may land mid-milestone, and each batch costs a new exemption row set, see 1.3). After merge 1, `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` has left = 0 until origin moves.
2. **New rollout items** (each needs a self-check):
   - `connect-api` 4.1.1 -> 4.4.0: compile-time only, not bundled; state the archive-diff and class-bytes evidence; no worker requirement change if bytes are identical. If the bytes differ, the item becomes a worker-compatibility statement and needs a measured minimum Connect version.
   - Shadow 9 / Gradle 9.8.1: build-only; the archive diff is the evidence; no operator action.
   - Shipped set changes: guava (pinned/overridden, advisory id to be identified), httpclient5 5.6.5, gson 2.14.0, jackson 2.22.4, lz4Java stays 1.12.0 (upstream's 1.11.4 deliberately not taken).
   - 999.4 and 999.5 behaviour changes (section 2.7, section 3.3), with the rollback caveat that `v1.7.0_gg3` reintroduces the code 32/33 failures and the doubled-name forms; follow the gg3 note's pattern (item 4 in-flight batches, item 7 rollback, `exactlyOnce=false` and `enableDbTopicSplit=true` variants).
   - Writer residual closed (Map key no longer reaches status), if 22 WR-01 ships.
3. **Mini-sync is tagless**: CHANGELOG, FORK-DELTA "For the next syncer" and the rollout note name `a6f1df83`, not a tag; say that the next tagged sync's merge base is expected at or after `a6f1df83` and verify with `git merge-base` when it happens.
4. **Older rollout notes**: `ROLLOUT-v1.7.0_gg1.md` and `ROLLOUT-v1.7.0_gg2.md` get the corrected `enableDbTopicSplit=true` key rule and state-table query. Their text currently says (gg3 note, line ~263) "shorter ... can miss a key, so follow step 2". Put these edits **inside the release phase before the reviewer pass** so they publish with the release; gg3 needed a post-publication docs-only republish (`6a2a87a5`) because review ran after the push. The reviewer prompt must name them.
5. **`VERSION` row**: `VERSION | v1.7.0_gg3 | 1 | BLD-07` moves in the same commit as `VERSION`; a `clean` is needed before the generated `Version.ARTIFACT_VERSION` agrees. FORK-DELTA rows 33 and 63 and the MBean `version=` segment follow.
6. **FORK-DELTA raw numbers** gain a "gg4" column and are refreshed in the release phase only; row 68 (catalog pins) is rewritten, row 34 (`onPartitionsRevoked`) gets the 4.4.0 verification, row 47/51/52/42/64/65, Known costs 6/10/11 and "Adopted wholesale" change per sections 2-4.
7. **Gate form**: the release merge is *not* an upstream-sync merge (as in Phase 24: use two parents, first parent == `origin/custom_build`, merged tree == rehearsed tip). Run the anchors invariant on the branch tip with the cumulative `PRE_TAG=pre-v1.7.0-sync` and the 18 exemptions, or with `pre-v1.7.0-tail` and none (decide once, record it). Each full anchors run takes ~10 minutes at 621+ commits.
8. **CI cost**: one green run is ~6.5 minutes; tag pushes do not trigger CI (branch-only trigger), so the tag push stays last and cheap.

## 6. New vs modified components (explicit)

### New

| Component | Layer | Purpose | Risk |
|-----------|-------|---------|------|
| `sink/db/helper/DescribedTableBuilder` | discovery | per-DESCRIBE accumulator; holds the loop both describe methods share; the real unit the pins drive | medium (V1 not CI-covered) |
| key helper in `sink/db/mapping/DeclaredNullability` (public, e.g. `elementKey`) | discovery | one definition of a Tuple element's name, used by pairing, by the 999.4 rule and by 999.5's unescape | low |
| `TupleSizeNamedElementRoundTripLiveTest`, `DescribedTableBuilderUnitTest`, `TableSizeNamedElementTest` (if `Table` patched) | tests | 999.4 pins | low |
| `ClickHouseFieldDescriptorTest`, `NameEscapeRoundTripLiveTest` | tests | 999.5 pins, both name forms | low |
| `.planning/scripts/conflicted-paths-v1.7.0-tail.txt`, `MERGE-CHECKLIST-v1.7.0-tail.md` | release tooling | mini-sync gate inputs | low |
| `docs/ROLLOUT-v1.7.0_gg4.md`, CHANGELOG `1.7.0_gg4` section | docs | release | review rounds budgeted |
| SHA-pinned rows in `fork-anchor-exemptions.txt` (18) | tooling | Dependabot commits that predate the anchor move | low, mechanical |

### Modified

| Component | Change | Phase |
|-----------|--------|-------|
| `gradle/libs.versions.toml`, `build.gradle.kts`, wrapper files, `.gitignore` | merge 1 and merge 2 | absorption |
| `fork-anchors.txt`, `FORK-DELTA.md` | per-phase rows (kafka 4.4.0, HLP-02 counts, builder rows, FieldDescriptor row, VERSION) | each phase, in the same commit as the code |
| `sink/db/helper/ClickHouseHelperClient` | `describeTableV1/V2` shrink to I/O + `accept`/`build`; `shouldSkipDescribeRow` gains the declared-element rule behind an overload | discovery |
| `sink/db/mapping/Column` (+ optionally `Table`) | `declaredTupleElement` flag; one-line `Table.updateParent` change for `size<digits>` | discovery (decision) |
| `sink/db/helper/ClickHouseFieldDescriptor.fromJsonRow` | natural decode (option A) | names |
| `sink/db/mapping/DeclaredNullability` | `normalise`, key function, Javadoc (IN-01), null-name check (IN-02) | names |
| `sink/db/ClickHouseWriter`, `Column.convertEnumValues`, `WithheldMessageException` comments | 22 WR-01, IN-01..03, IN-05 | writer |
| `TestsWorkflowPinGuardUnitTest`, `JsonAsStringKeyResidualLiveTest`, `ClickHouseHelperClientForkUnitTest`, `DeclaredNullabilityTest`/`Fixtures`, `NullableTupleMatrixLiveTest`, `ClickHouseSinkTaskWithSchemaTest`, `CountingHelperClient` | pin flips and advisory fixes | per component |
| `build.gradle.kts` `jacocoForkReport`, catalog `slf4j-reload4j` | list extension, literal -> catalog | hygiene (last) |
| `VERSION`, `CHANGELOG.md`, `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/ROLLOUT-v1.7.0_gg2.md` | release | release |

### Explicitly untouched (keep it that way)

`Processing.java`, `KeeperStateProvider.java`, `RangeContainer.java`, `ClickHouseSinkConfig.java`, `MetadataFreeJsonWriter.java`, the SMTs, and every exactly-once path. The extraction changes when and how `Table` is *built*, never what is sent per batch, so the `BEFORE`/`AFTER` and byte-identical-retry invariants are unaffected. State this in the plans so reviewers do not need to re-derive it.

## 7. Data flow changes

| Flow | Before | After |
|------|--------|-------|
| DESCRIBE row -> Table | two duplicated loops; structural skip by name suffix | one `DescribedTableBuilder`; skip decision uses the parent's raw type; declared Tuple elements named `size`/`size0` (and `size<digits>` if in scope) become Columns |
| Name domain (999.5, option A) | JSON-escaped text (doubled backslash) | natural name; `DeclaredNullability` key function converts type-text escapes to natural before pairing |
| Writer failure on JSON-as-string route | Jackson `IOException` passes through; Map key in status | `JsonProcessingException` excluded from the pass-through; value-free `DataException` |
| Build/ship | Shadow 8.3.5, Gradle 9.6.1, connect-api 4.1.1 | Shadow 9.6.1, Gradle 9.8.1, connect-api 4.4.0 (compile only); guava pinned |
| Per-record path, exactly-once, offsets | unchanged | unchanged |

## 8. Suggested build order with dependencies

```
P25 Absorption ──► P26 Discovery 999.4 ──► P27 Writer residual+enum ──► P28 Names 999.5 ──► P29 Hygiene (jacoco last) ──► P30 Release
   │                    │                        (independent of 26;            ▲ depends on 26's key helper
   │                    └ extraction first,       gives the user time for        └ needs the user decision on name form
   │                      fix second               the 999.5 decision)
   └ hard dependency: every later gate runs on the post-absorption build
```

| # | Phase | Contents | Depends on | Key gates / research flag |
|---|-------|----------|------------|---------------------------|
| 25 | Dependency absorption and tagless mini-sync | pre-flight (stale refs, fetch, image date, tags); merge 1 with bookkeeping and 18 exemptions; archive diff + class-bytes proof; one full suite; merge 2 (guava) with tail gate; guava evidence; full suite twice = the **baseline for all later phases**; optional PR #10 as a separate commit; `dependencies` label (user) | none | **flag: guava advisory id and whether guava ships today (unmeasured); Shadow 9 archive diff; PR #10 may be red** |
| 26 | Discovery defect 999.4 + WR-02 | live matrix spike (tracer) -> behaviour-preserving extraction -> declared-element rule -> optional `Table` patch -> live round trip -> pin flips -> docs/anchors | 25 | **flag: needs the live DESCRIBE dump; decide `size<digits>`/`Table` scope up front** |
| 27 | Writer failure residual and enum advisories | classification pin green first; 22 WR-01 + sentinel Map-key test; IN-01/02/03/05; 24 IN-03; flip the residual live test | 25 | standard patterns; high-risk file, so no `Processing` change and a measured before/after |
| 28 | Name decode 999.5 + DeclaredNullability advisories | blocking-human decision; live name-matrix spike; WR-01 pin on current code; fix; both-forms live pin; rollout item; IN-01/IN-02 | 25, 26 (key helper) | **flag: deepest research; coupling to pairing and enum labels is inferred, not measured** |
| 29 | Test and build hygiene | pin-guard WR-01..03, IN-01, IN-02; slf4j literal -> catalog; 22 IN-04; `CountingHelperClient`; 23 IN-03; **jacoco list last** | 25..28 (so the list is complete) | standard; test-only except the catalog line |
| 30 | Release and publication | VERSION + anchor; CHANGELOG; FORK-DELTA rows and raw numbers; rollout note incl. gg1/gg2 key-rule corrections; independent reviewer (<=3 rounds) before the gate; two full runs; rehearsal; approval gates inline; `--no-ff`; single green run; tag after; scratch cleanup | 25..29 | follows Phase 24; re-fetch origin before the gate |

Ordering rationale: 25 first is a hard rule (builds, anchors and the suite baseline are what every later gate runs on). 26 before 28 because the key helper and `DescribedTableBuilder` stabilise the code 999.5 rewrites, and because 999.4 is the higher-severity defect (inserts fail). 27 is independent of 26/28 and sits between them so the 999.5 decision has time to be taken. 29 is last among code phases so the jacoco list and FORK-DELTA numbers are edited once. 30 last by user decision.

Parallelism: the repo uses one working tree, phases sequential. Within a phase, tests and docs plans can follow the code plan in waves as in Phases 21-24.

## 9. Anti-patterns to avoid

1. **Gating merge 1 and merge 2 together.** The only conflicts are in merge 2 and the only production-reaching majors are in merge 1; merging both hides which one broke the build.
2. **Using bare `upstream/main` or an unpinned ref in `UPSTREAM_REF`.** It resolves to the v1.6.0 merge today; with no tag, pin the SHA.
3. **Editing `verify-merge-invariants.sh` to tolerate the Dependabot commits.** D-02 forbids it; add exemption rows.
4. **Putting the 999.4 rule in `shouldSkipDescribeRow` alone** while `Table.SIZE_FIELD_MATCHER` still drops `size<digits>`: half the E-15 shapes would still fail, and the test would pass because it only drives the loop.
5. **Reimplementing sub-column naming (Array/Map/Nullable path rules) to build a declared-path set.** Use the per-row rule on the parent's raw type; `Table` already owns routing.
6. **Changing `fromJsonRow` without changing the pairing key in `DeclaredNullability` in the same commit.** Pairing fails open with a WARN, so the suite may stay green while `Nullable(Tuple)` nullability silently reverts to the double-flag bug (code 33). The live `NullableTupleMatrixLiveTest` must stay in the gate for that commit.
7. **Fixing `jacocoForkReport` twice.** One edit, last.
8. **Letting the reviewer pass run after the push.** gg3 needed a docs-only republish; review gg1/gg2 key-rule edits with the rest.
9. **Claiming "guava CVE fixed" without the dependencyInsight and archive evidence.**

## 10. Confidence assessment and gaps

| Area | Confidence | Basis |
|------|------------|-------|
| Conflict surface and merge order | HIGH | measured with `git merge-tree` on dangling merges in both orders; file lists from `git diff --stat` |
| Anchor/exemption arithmetic (18 rows, 17 SHAs) | HIGH | measured by running the unmodified gate on a rehearsal merge (19 violations, one fixed by D-03) |
| Upstream-vs-fork content overlap | HIGH | catalog diffs read directly |
| 999.4 design (per-row rule, `Table` second drop point) | MEDIUM | `Table.java` and the loop read; E-15 shapes and R-01 rows taken from recorded evidence; the `Array(Tuple(size0 ...))` double-row collision is inferred and must be confirmed by the live dump |
| 999.5 coupling (doubling is load-bearing for pairing, enum labels move) | MEDIUM | code and rollout table read, not re-run |
| PR #10 scope (protoc as well as protobuf-java) | HIGH for the catalog/`build.gradle.kts:472` link, LOW for whether it is red |
| connect-api/Shadow having no operator effect | MEDIUM | design argument (not bundled; same `confluentJar` inputs); needs the E-04 style evidence |

**Gaps to close in phase research or discuss:**
- Which advisory the guava pin addresses, and whether the current archive already carries guava (and at what version). Upstream PR #859 names no CVE id.
- Whether to include `size<digits>` (requires the `Table.java` patch) in 999.4.
- Intended name form for 999.5 (user decision).
- Whether PR #10 is merged on origin before merge 1 starts, and whether it is green.
- Whether the user wants the cumulative-with-18-exemptions or the `pre-v1.7.0-tail` form for the release-time anchors gate.
- `describeTableV1` is not covered by CI; one manual V1 run is the only available evidence for the extraction.

## Sources

- Repository state, read-only: `git merge-tree --write-tree` (trial merges in both orders on dangling `commit-tree` objects), `git diff --stat`, `git log`, `git rev-list --count`, `git rev-parse upstream/main`; the unmodified `.planning/scripts/verify-merge-invariants.sh 54e63648 <rehearsal>` run (result line `provenance_paths=21 anchor_commits=574 anchor_rows=69 marker_commits=621 anchor_exempt=1 result=FAIL`). Confidence HIGH.
- Code read: `ClickHouseHelperClient.java` (`shouldSkipDescribeRow`, `buildDescribedTable`, `describeTableV1/V2`), `ClickHouseFieldDescriptor.java`, `DeclaredNullability.java`, `Column.java`, `Table.java`, `ClickHouseWriter.java`, `ClickHouseHelperClientForkUnitTest.java`. HIGH.
- Planning records: `docs/FORK-DELTA.md`, `.planning/PROJECT.md`, `.planning/ROADMAP.md` (Sync Mechanics, Backlog 999.4/999.5), `.planning/MERGE-CHECKLIST-v1.7.0.md`, `.planning/milestones/v1.7.0_gg3-hardening-phases/21-*/21-02-SUMMARY.md`, `24-release-publication/24-MILESTONE-CLOSE-RECORD.md`, `24-COMMAND-EVIDENCE.md` (E-04, E-05, E-15), `v1.7.0_gg3-hardening-ROADMAP.md` Phase 24, `docs/ROLLOUT-v1.7.0_gg3.md` item 3. HIGH.
- Upstream PR #859 page (guava/lz4/httpclient5; no CVE id named). MEDIUM (summary of a web page).
