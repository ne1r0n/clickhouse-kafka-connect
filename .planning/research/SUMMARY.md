# Project Research Summary

**Project:** clickhouse-kafka-connect fork (`ne1r0n/clickhouse-kafka-connect`, branch `custom_build`, `v1.7.0_gg3` shipped, target `v1.7.0_gg4`)
**Milestone:** `v1.7.0_gg4-hardening` (subsequent milestone, brownfield)
**Domain:** Production Kafka Connect sink for ClickHouse (Java 17, Gradle, version catalog), exactly-once via Keeper state machine
**Researched:** 2026-10-21
**Confidence:** MEDIUM-HIGH overall. HIGH for dependency facts, merge mechanics and the Shadow 9 defect. MEDIUM for the 999.4 and 999.5 designs (not yet run against a live DESCRIBE). LOW for the guava advisory identity and the Kafka 4.4.0 release content.

## Executive Summary

This milestone is a hardening pass on a production fork, not a feature build. Its work falls into four groups: absorb the Dependabot batch already on origin plus a small upstream tail (guava, httpclient5, lz4); fix two discovery defects in table DESCRIBE handling (backlog 999.4: declared Tuple elements named `size`/`size0`/`sizeN` are silently dropped and their inserts fail with code 32/33; backlog 999.5: backslash, line feed, tab, slash and quote characters in names are mis-decoded); close 17 carried review advisories; and ship `v1.7.0_gg4` with an operator rollout note. The connector's data path (Kafka to ClickHouse via `Processing`, `KeeperStateProvider`, `ClickHouseWriter`) does not change shape. The work lives in the build layer, the discovery layer (`ClickHouseHelperClient`, `ClickHouseFieldDescriptor`, `Column`, `Table`, `DeclaredNullability`), the writer failure path and the release documents.

The recommended approach is strict sequencing: absorb dependencies first, because every later gate must run on the post-absorption build. Within absorption, merge `origin/custom_build` (`69cd680b`) first, because it is conflict-free, then merge the upstream tail `a6f1df83`, whose conflicts are isolated to one catalog block and `.gitignore`. Then fix 999.4 (highest severity: every insert into an affected table fails) before 999.5 (its decision checkpoint needs time, and its name-matching rewrite depends on the key helper 999.4 introduces). Release last.

The key risks are concentrated in four places. First, Shadow 9 changed `from(...)`, so the hand-registered `confluentJar` very likely now ships nested `.jar` files rather than classes. CI never builds that jar, so the connector would fail at plugin load in a worker. Second, the 999.4 fix is wider than one function: `Table.SIZE_FIELD_MATCHER` is a second drop site, so a helper-only fix leaves `Array(Tuple(size1 ...))` broken. Third, both discovery fixes change the bytes of a retried batch, which touches the exactly-once invariant, so rollback needs clean-stop guidance. Fourth, the release process has repeatedly failed on unsupported quantifiers and inline-only review; the note needs an independent, fresh-context reviewer before the gate.

## Key Findings

### Baseline and merge mechanics

- **Baseline is `origin/custom_build` at `69cd680b`, not `d20bae63`.** Two more Dependabot merges landed on 2026-10-21: #14 jackson `2.22.3` to `2.22.4`, and #15 slf4j `2.0.18` to `2.0.20`. Nine Dependabot PRs are on origin, including #8 (Gradle wrapper 9.6.1 to 9.8.1), #9 (`connect-api` 4.1.1 to 4.4.0), #11 (Shadow 8.3.5 to 9.6.1), #12 (httpclient5 5.6.5), #13 (gson 2.14.0). Open and unmerged: #10 (protobuf) and #4 (client 0.10.0, out of scope). Re-run `git ls-remote origin custom_build` before every merge and gate.
- **Merge order: `origin/custom_build` first, then the upstream tail.** `git merge-tree` confirms the first merge is conflict-free and the second conflicts in exactly `.gitignore` and `gradle/libs.versions.toml`. The catalog conflict is one hunk covering `[versions]` from `kafka` through `lz4Java`, plus `guava`. The reverse order conflicts twice in the catalog.
- **Resolve the catalog hunk to the fork side for shared keys.** Keep `kafka = "4.4.0"` (upstream's side reverts Kafka to 2.7.0), keep `lz4Java = "1.12.0"` (upstream's 1.11.4 is older and carries advisories 1.12.0 does not), keep gson 2.14.0, jackson 2.22.4, lombok 1.18.48. **Add** `guava = "33.7.2-jre"` (dropping it fails the build with an unresolved `libs.guava`). **Delete** `httpclientTest` (orphan after upstream removed the alias).
- **Delete stale local refs before any merge.** `refs/upstream/main` and `refs/upstream/v1.6.0` both point at `789a57c2`, so bare `upstream/main` resolves to the v1.6.0 merge. Use `refs/remotes/upstream/main` or the full SHA `a6f1df839c079180bc7c343b5823f205e204dafc` as `UPSTREAM_REF`. The mini-sync is tagless; the next tagged sync's merge base is expected at or after `a6f1df83`.
- **The anchors gate needs exemption rows for published Dependabot commits.** The unmodified `verify-merge-invariants.sh` on a rehearsal merge gave 19 violations: the `kafka = "4.1.1"` anchor (BLD-08) is violated by every origin commit after #9, and the `slf4j-reload4j:2.0.17` anchor by four Dependabot branches. The result is 18 SHA-pinned rows covering 17 distinct SHAs, generated mechanically from the gate's stderr. Do not edit the script (D-02). The merge commit must carry the anchor edits (`kafka = "4.4.0"`, FORK-DELTA rows) in the same `git commit`. Budget a new exemption set for every Dependabot batch that lands mid-milestone.

### Dependencies

- **Shadow 9.6.1 breaks `confluentJar` silently (blocking).** `from(clickhouseDependencies, ...)` no longer unzips jars. Only the task's `configurations` property is exploded, and a hand-registered task leaves it empty. Each ClickHouse client jar would land as a nested `*.jar` entry, and the connector fails with `NoClassDefFoundError` at worker plugin load. Baseline: 7,375 entries (gg2), 7,376 at gg3. Recommended repair: `configurations.add(clickhouseDependencies)` then `from(sourceSets.main.get().output)`, with `addMultiReleaseAttribute.set(false)` to keep the manifest unchanged. Fallback if the entry diff shows an unwanted difference: `from(provider { clickhouseDependencies.files.map { zipTree(it) } }, ...)`. Prove the repair with an entry-by-entry SHA-256 compare against gg3, in throwaway worktrees, using `python3 -I`. Pass criteria: zero `.jar` entries; only allow-listed changes (guava and its five small dependencies, version bumps); `MANIFEST.MF` unchanged apart from versions.
- **The pin-guard reader must be fixed before the first `build.gradle.kts` edit.** `TestsWorkflowPinGuardUnitTest.ktsCode` strips block comments with `(?s)/\*.*?\*/` without string-literal awareness (21-REVIEW WR-02). The Shadow repair and the jacoco list both edit that file, and a `/*` inside a string (for example `exclude("META-INF/*.SF")`) would swallow the `reportTestForks` registration and turn `testUnit` red for an unrelated reason. Fix WR-02 as the first commit of the absorption phase, with fixtures for `/*` in a string and in a `//` comment. This overrides the usual "dependency first" order for this one item.
- **`connect-api` 4.4.0 is compile-time only and not bundled.** No `src/main` signature change is needed: the only new class is `ConnectPlugin`, and every class the fork imports keeps its public signatures. `connect-api` is not in `clickhouseDependencies`, so it is not in the shipped jar. Prove "no operator effect" by showing the compiled connector references no Connect member added after 4.1.1 (grep `javap -c -p` for `ConnectPlugin` and `InterfaceAudience`; diff constant-pool references against a 4.1.1 build). Update the anchors row, the `ClickHouseSinkTask` comment, the three test emulation Javadocs, and FORK-DELTA's sentence that "production runs the shaded jar built here, so 4.1.1 is the production Connect API version". That sentence is wrong: the worker supplies the API at runtime.
- **PR #10 (protobuf-java 3.25.9 to 4.36.2) stays open. This contradicts the earlier user choice to merge it and needs the user's explicit decision.** Its own CI run is red (step "Run tests" failed; cause not read). No security driver exists for 3.25.9. The catalog key `libprotobuf` feeds both `protobuf-java` (testFixtures) and `protoc`, so generated test classes change too. Confluent 7.9.1 is built on protobuf 3.25.5, and the fork does not declare `protobuf-java-util`, so the pair would be mixed-major. Recommendation: leave it open this milestone; if the user insists, first download the failing run's test report and name the failing tests, then try adding `protobuf-java-util` at the same version. Blast radius is test-only; the shipped jar does not include protobuf.
- **Guava CVE-2026-102554 (fixed in 33.7.2) and httpclient5 CVE-2026-64607 (fixed in 5.6.3; gg3 shipped 5.6.2) both go in the rollout note.** The guava advisory identity comes from secondary sources (Guava release notes, HeroDevs, Snyk); OSV had not indexed it. Label it as such, and write "per Guava release notes" rather than "closes CVE-X" without evidence; upstream's commit names no CVE. Exposure is very low: the vulnerable path needs `ObjectInputStream` on attacker bytes, which the connector does not do. Guava reaches the shipped jar transitively today (client-v2 0.9.6 declares 33.4.6-jre), so the change is a version bump plus five small dependencies that Guava 33.7.2 adds (failureaccess, listenablefuture, jspecify, error_prone_annotations, j2objc-annotations). List them as expected deltas. Keep httpclient5's explicit pin.
- **Gradle 9.8.1 is safe and closes three Gradle advisories.** No build-script change. Record a `--warning-mode all` run for the deprecated Kotlin DSL delegates as future cleanup.
- **Shadow 9 side effects.** `assemble` now builds a `*-all.jar` that includes connect-api. Optional: `addShadowJarToAssembleLifecycle.set(false)`. Record the decision.

### Discovery defects (999.4 and 999.5)

- **999.4 has two drop sites, not one.** `ClickHouseHelperClient.shouldSkipDescribeRow` drops `.null`, `.size0` and `.size` rows. `Table.updateParent`'s ARRAY branch (`SIZE_FIELD_MATCHER = .+\.size[0-9]+$`) drops `size<digits>` children of an Array parent, and `Table.java` is adopted wholesale from upstream. A helper-only fix leaves `Array(Tuple(size1 ...))` and `Array(Tuple(v String, size1 ...))` failing. The backlog text names only `size` and `size0`; the measured defect is wider. Recommendation: fix both sites, with the `Table` change narrowed to "child is not a declared element", and record the `Table.java` fork delta in FORK-DELTA.
- **DESCRIBE has no field separating structural rows from declared elements.** `describe_include_subcolumns=1` gives `is_subcolumn=1` for both. Name alone cannot decide, and for bare `size` even the row's own type cannot (`Array(Tuple(size UInt64))` yields a row byte-identical to the String-length row of `Array(String)`). The classifier that held in all 75 measured rows across 21 tables uses three inputs: the parent's innermost non-wrapper type (Tuple means declared element for bare `size`), the parent's array depth, and the row's own `type` (a structural `sizeN` has type `Array^N(UInt64)`; a declared element of depth D has type `Array^D(T)`, so they cannot coincide when N < D). Classify on the parent's raw type; never on occurrence order, never on a closed list of names, and never "keep every row whose parent is a Tuple" (wrong for `Array(Tuple)`).
- **A structural twin must not poison the skip set.** `Array(Tuple(size0 Tuple(a UInt8)))` emits `e.size0` as a structural row, then the declared element, then `e.size0.a`. If the structural name enters `skippedCols`, the child is dropped too. Do not remember structural names by name.
- **Spike first, then extract, then fix.** Dump live DESCRIBE rows on ClickHouse `latest` for the 13 affected fixtures, the 19 look-alikes and `s4_o01` in `24-COMMAND-EVIDENCE.md` E-15, plus new shapes (`Array(Array(Tuple(size1, size0)))`, `Array(Tuple(size0 Tuple(a ...)))`, `Array(Tuple(size0 String, size String))`, `Array(Map(String, Tuple(size, size0)))`, `Tuple(keys, values)`). Extract the duplicated V1/V2 DESCRIBE loop into one fork-owned `DescribedTableBuilder` so the test pins the production loop (23-REVIEW WR-02). Extraction must be behaviour-preserving and land green first.
- **999.5 canonical form: the raw identifier as written in `CREATE TABLE`.** Reasons: it is what operators and Debezium sources know; upstream reached the same conclusion (issue #838, PR #839, which fix the same `fromJsonRow` doubling); the metadata-free JSON route already uses raw names; it repairs silent slash loss, unmapped tables with a double quote in a name, and `Nullable(Tuple)` pairing failures. Recommended firmly, but it is the user's decision.
- **Decode names, not types; re-key pairing; rewrite `normalise`.** The brief's directive is names only, and that is the recommendation. Feature research measured that decoding the name alone breaks pairing for backslash names (`bs\b` vs `bs\\b`) and brings back code 33. Pitfall research resolves this by changing the Column name and making the pairing element key apply the SQL-identifier unescape, so both sides compare like with like. Architecture research also considered decoding the type text; the measured arithmetic shows that changes every multi-line Tuple type, so do not do it. `DeclaredNullability.normalise` must be rewritten, not extended: after the decode the type text holds one escape layer fewer, so the "half the run" arithmetic and the lone-backslash-n rule change. The 23-REVIEW WR-01 pin lands first, against current code. The COLS-13 live twins (`NullableTupleMatrixLiveTest`, line-feed case) are the oracle; a naive decode turns them red for line feed, tab and carriage return.
- **Enum label keys change too.** `Column.extractEnumValues` uses type-text labels as lookup keys, and labels with backslash, quote, line feed or slash change form. Keep this a separate commit with its own rollout line, or defer with a note. Recommendation: document now; fix only if the phase has room.
- **Operator rename is required for 999.5, and the failure modes are asymmetric.** A root column whose producer still sends the doubled name is silently ignored and its `DEFAULT` is stored; a Tuple element fails loudly. Slash names need no rename. Include the gg3 item 3 detection query unchanged, a per-character table of old and new names, and no permanent dual-form lookup (it adds a per-record branch and hides the problem).
- **999.4 before 999.5.** Both edit the describe loop and `fromJsonRow` tests. 999.4's new pins should use backslash-free names so they survive 999.5; the key helper 999.4 introduces is what 999.5 reuses.

### Writer and advisories

- **22-REVIEW WR-01: exclude `JacksonException`, per link.** `passesThroughUnchanged` passes any chain containing an `IOException` straight through, and every Jackson checked exception is an `IOException`, so a Map key can reach task status and the DLQ header on the opt-in JSON-as-string route. Use `!(t instanceof com.fasterxml.jackson.core.JacksonException)` inside the chain walk, testing each link, so a genuine socket timeout beneath a Jackson exception still passes through and stays retriable. Do not exclude the whole chain by its head. Add rows to `WriterFailureClassificationUnitTest` (JPE alone; JPE caused by `SocketTimeoutException`; JPE caused by a timeout-text IOException; plain IOException) and show existing rows unchanged. Then flip `JsonAsStringKeyResidualLiveTest` deliberately; FORK-DELTA Known costs item 6 and the rollout exception sentence then go.
- **Advisory closure spans four phases.** The 17 carried review findings plus two carried items cut into writer-failure (22 WR-01, IN-01, IN-02, IN-03, IN-05 src half; 24 IN-03), names (999.5 with 23 WR-01, IN-01, IN-02), discovery (999.4 with 23 WR-02), and hygiene (21 WR-01..03, IN-01, IN-02, IN-04; 22 IN-04; 22 IN-05 test half; 23 IN-03). No advisory is in two phases. Under the Phase 22 L-47 and Phase 23 D-19 precedents, existing tests keep every name and assertion; add rows, never delete or weaken. Any flip of an existing assertion needs the user's sign-off and seen-red mutation evidence.
- **Keep greppable text byte-identical.** Operators grep `Failed to write column ` and `have no matching DESCRIBE sub-column`. Pin current text before any refactor (WRT-14 pattern); any intended change is a ledger row and a rollout line.
- **Jacoco list: edit once, last.** It omits `DeclaredNullability` and `WithheldMessageException` and must gain the new fork classes. Do it in the hygiene phase, with the FORK-DELTA row in the same commit.

### Rollout and rollback

- **Both discovery fixes change the bytes of a retried batch.** RowBinary inserts carry no column list, so the DESCRIBE Column list is the wire format; this is the exactly-once invariant in AGENTS.md. Upgrade for 999.4 needs no clean stop: failing inserts left no rows, so an in-flight range holds nothing and the retry lands once. Rollback to gg3 needs a clean stop first: gg3 rebuilds the short bytes, the server rejects them, and a range already stored by gg4 fails again on the same deduplication token. Tables where the short write happened to parse are a data-tail problem: wrong rows are not repaired, and the grep cannot find them. For 999.5, the in-flight range was written with the other form, so a retry with different bytes under the same token can be dropped; recommend a clean stop for both directions, with the field rename as a precondition.
- **Note content.** 999.4: what changes, suspects query before, WARN grep after, clean-stop rule, data-tail caveat, Known-cost item 10 retired. 999.5: canonical form, character table with a natural-name column, detection query unchanged, "root column silent `DEFAULT`, element loud failure", enum-label sentence, rename instruction, rollback paragraph mirroring gg3 item 4. WR-01: the gg3 Map-key sentence is removed; no operator action. connect-api 4.4.0: semver-minor, not bundled, worker version governs, bytecode evidence. Shadow 9 and Gradle 9.8.1: no operator action, archive diff as evidence. Guava and httpclient5 advisories. Shipped-set changes. VERSION and MBean `version=` segment change again (gg3 item 5 pattern). Correct the gg1 and gg2 notes' `enableDbTopicSplit=true` key rule through a docs-only `--no-ff` merge, and name them in the reviewer prompt.
- **Every quantifier must match a measurement.** Each claim is "measured", "read from connector code", or "inferred, to be pinned live". Run every command in the note live.
- **Independent reviewer before the gate.** An orchestrator-spawned, fresh-context `gsd-code-reviewer` on every operator document, repeated (at most three rounds) until a round finds nothing, with never-reused IDs and disposition counts. Inline review by the executor failed in gg1, gg3 and Phases 15/24. Reviewer-proposed fix text is itself reviewed: gg3's WR-10 fix would have reintroduced a false "safe" statement.

## Implications for Roadmap

Suggested phases (sequential, one working tree). Phases 25 to 30 follow the architecture research order, with two adjustments: the pin-guard fix moves to the front of 25, and the jacoco edit stays last in 29.

### Phase 25: Dependency absorption and tagless mini-sync
**Rationale:** Every later gate runs on the post-absorption build, and this is the only phase that can reach production through the shipped Confluent jar. Shadow is the one blocking defect.
**Delivers:** Pin-guard WR-02 fix as the first commit (own gate); pre-flight (delete stale refs, `ls-remote`, record `SELECT version()` of `latest`, local rollback tag at the pre-merge tip); merge 1 (`origin/custom_build`) with anchor edits and 18 SHA-pinned exemptions in the same commit; Shadow repair plus the archive entry diff against 7,375 and connect-api bytecode-reference evidence; one full suite run; merge 2 (`a6f1df83`) with the catalog resolution table, `CONFLICTED_PATHS_FILE`, `PRE_TAG=pre-v1.7.0-tail`, and a `MERGE-CHECKLIST-v1.7.0-tail.md`; guava `dependencyInsight` evidence; two full suite runs as the baseline for later phases; PR #10 decision (user).
**Addresses:** Shadow 9 nested-jar defect (blocking), connect-api 4.4.0 no-bundle proof, guava and httpclient5 advisories (as evidence), Gradle 9.8.1, mini-sync.
**Avoids:** Pitfalls 1 (nested jar), 2 (anchor violations), 3 (wrong-side catalog resolution), 4 (unsupported CVE claim), 9 (connect-api skew), 11 (pin-guard misread), 16 (`-all.jar`), 17 (wrapper checksum).
**Research flag:** NEEDS measurement. Shadow archive diff and guava presence in the shipped jar are unmeasured; PR #10 red cause unknown.

### Phase 26: Discovery defect 999.4 (+ WR-02 production-loop pin)
**Rationale:** Highest-severity defect (affected inserts fail on every row). Precedes 999.5 because it introduces the element key helper and the DESCRIBE builder 999.5 reuses.
**Delivers:** Live DESCRIBE spike for E-15 and new shapes on `latest`; behaviour-preserving extraction of `DescribedTableBuilder`, green first; declared-element classifier at both drop sites with the cascade guard; unit table of captured real DESCRIBE rows; live round-trip pin (`TupleSizeNamedElementRoundTripLiveTest`) with DESC-05 regression rows; D-04 pin flipped; FORK-DELTA, `fork-anchors.txt`, CHANGELOG line; one manual V1 run recorded.
**Uses:** Parent raw type, array depth, row `type`.
**Avoids:** Pitfalls 5 (rule wider than one function), 6 (Column list is the wire format), 19 (preserve `.size1` and `.keys`/`.values` behaviour).
**Research flag:** NEEDS a live spike before planning the classifier; confirm the `Array(Tuple(size0 ...))` double-row collision.

### Phase 27: Writer failure residual and enum advisories
**Rationale:** Independent of discovery and names; sits between them so the 999.5 decision is not on the critical path. High-risk file: no change to `Processing` or `KeeperStateProvider`.
**Delivers:** Classification pins green on the unchanged writer first; WR-01 per-link `JacksonException` exclusion with sentinel Map-key test; flipped `JsonAsStringKeyResidualLiveTest`; `Column.convertEnumValues` fixes (22 IN-01, IN-02); 22 IN-03, IN-05 src half; 24 IN-03.
**Avoids:** Pitfalls 12 (weakening tests), 13 (retry classification change), 20 (greppable text changes).
**Research flag:** Standard patterns (WRT-14 and L-47 precedents).

### Phase 28: Name decode 999.5 (+ DeclaredNullability advisories)
**Rationale:** Depends on the Phase 26 key helper. Carries the one unresolved product decision.
**Delivers:** User checkpoint on canonical form (recommendation: raw identifier) and enum labels (recommendation: document, fix if room); live name-matrix spike; WR-01 pin on current `normalise` first; names-only `fromJsonRow` decode (the #839 hunks, with V1 raw-line read in UTF-8); element key helper unescapes SQL identifiers; `normalise` rewrite; both-form live pins (`NameEscapeRoundTripLiveTest`, `ClickHouseFieldDescriptorTest`) for backslash, backslash-n, LF, tab, slash, double quote, backtick, Unicode; 23 IN-01, IN-02; rollout item; optional metadata-only WARN for changed-form names (P2).
**Avoids:** Pitfalls 7 (load-bearing doubling), 8 (silent direction flip); never change `fromJsonRow` without the pairing key in the same commit.
**Research flag:** NEEDS the user's name-form decision and a live matrix spike. Coupling to pairing and enum labels is inferred, not measured.

### Phase 29: Test and build hygiene
**Rationale:** Last code phase, so the jacoco class list and FORK-DELTA numbers are edited once.
**Delivers:** Remaining pin-guard items (WR-01, WR-03, IN-01, IN-02); `slf4j-reload4j` literal moved to catalog with anchor move (21 IN-04); 22 IN-04 (`fail()` after `put`, preferred to a new test); 22 IN-05 test half; 23 IN-03; `CountingHelperClient` assertion; jacoco list extended once.
**Research flag:** Standard; test-only except the catalog line.

### Phase 30: Release and publication (`v1.7.0_gg4`)
**Rationale:** Follows Phase 24's proven shape; the rollout note is the largest new document and needs the independent reviewer.
**Delivers:** `VERSION` bump with anchor row in the same commit and a `clean` build; CHANGELOG `1.7.0_gg4`; FORK-DELTA rows and raw numbers (refreshed here only); `docs/ROLLOUT-v1.7.0_gg4.md` with clean-stop guidance; gg1/gg2 key-rule corrections via docs-only merge; independent reviewer (at most three rounds); two full `--rerun` runs on `latest` x V2; anchors gate on the tip (decide once: cumulative with `pre-v1.7.0-sync`, or `pre-v1.7.0-tail` with none); rehearsal; `--no-ff` merge with merged tree equal to the rehearsed tip; push; exactly one green `tests.yaml` run on that SHA; tag after; scratch branch deleted.
**Avoids:** Pitfalls 14 (published SHA differs from tested SHA; re-fetch and re-gate if origin moved), 15 (unsupported quantifiers, inline review, VERSION traps).
**Research flag:** Follows Phase 24. Re-fetch origin before the gate.

### Phase Ordering Rationale

- Absorption first is a hard rule: builds, anchors and the suite baseline are what every later gate runs on. The Shadow repair is its first measured gate because it is the only production-reaching defect.
- The WR-02 pin-guard fix precedes the Shadow repair because the repair adds a `/*` string the old reader mis-parses.
- 999.4 before 999.5: higher severity, and it introduces the key helper and extracted builder that 999.5 rewrites.
- Writer work (Phase 27) is independent and sits between discovery and names, keeping the 999.5 decision off the critical path.
- Jacoco is edited once, in hygiene, so it lists every new class.

### Research Flags Summary

Needs measurement or a user decision during planning:
- **Phase 25:** Shadow archive entry diff; guava presence in the shipped jar; PR #10 failing test names; guava advisory identity (secondary sources only).
- **Phase 26:** live DESCRIBE spike before the classifier is planned; confirm the `Array(Tuple(size0 ...))` double-row collision.
- **Phase 28:** user decision on name form and enum labels; live name-matrix spike; pairing-key design validated by the COLS-13 twins.

Standard patterns (skip research-phase): Phase 27 (WRT-14 and L-47 precedents), Phase 29 (test hygiene), the DEPS-01 merge recipe (gg3 Phase 21), and the release gating recipe (gg3 Phase 24).

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack and dependency absorption | HIGH | Read from jars, poms, Shadow 9.6.1 source and `git merge-tree`. Open: whether guava ships today (STACK says yes transitively; ARCHITECTURE could not see it in cached poms). Resolve by the Phase 25 archive diff. |
| Shadow 9 defect | HIGH from changelog and source; not yet built | Baseline 7,375 recorded; the repair must be proven by the compare. |
| Features (999.4, 999.5, WR-01, rollout) | HIGH for measured DESCRIBE and escaping behaviour; MEDIUM for the 999.5 coupling | 75 rows classified; pairing and enum-label consequences measured on 26.9.19.9. |
| Architecture and build order | HIGH for conflict surface and merge order; MEDIUM for the 999.4 design | Anchor arithmetic measured by running the unmodified gate. |
| Pitfalls | MEDIUM-HIGH | Fork-history and code findings HIGH; protobuf 4.x behaviour and Kafka 4.4.0 content MEDIUM or LOW. |

**Overall confidence:** MEDIUM-HIGH. Dependency and merge work is well understood; the two discovery designs need a live spike before the plan is trusted.

### Gaps to Address

- **Shipped archive contents today:** whether guava is in the gg3 jar and at what version. Resolve in Phase 25 with `dependencyInsight` and the archive compare, before the note says "guava is added".
- **Guava advisory identity:** CVE-2026-102554 from secondary sources only. Re-query before the note is written.
- **PR #10:** cause of the red CI run unknown. User decides: leave open (recommended), or fund the investigation.
- **999.4 `size<digits>` scope and `Table.java` patch:** recommend fixing; needs confirmation at plan time because it changes an adopted-wholesale file.
- **999.5 canonical form and enum labels:** raw identifier recommended; user decides. Decide also whether to ship a transitional alias (recommendation: no).
- **Production Connect worker version:** unknown. The note must state the tested minimum and the KIP-1161 LIST-config behaviour for operators on 4.2+ workers. Ask the user before writing it.
- **V1 client path:** not covered by CI. One manual `CLIENT_VERSION=V1` run of the helper live tests is the only evidence for the extracted builder and the `fromJsonRow` change.
- **Dependabot freeze:** decide with the user whether origin stays frozen from absorption to publication; if not, the release gate re-fetches and re-gates.
- **Kafka 4.4.0 upgrade notes:** not published at research time (HTTP 404 on 2026-10-21). Release-note titles only; re-check before the note states any Connect behaviour.
- **`dependencies` label:** the user creates it; the "labels could not be found" comment stops only on the next monthly Dependabot PR, so it cannot be verified within the milestone.

## Sources

### Primary (HIGH)
- Repository, read-only: `git merge-tree --write-tree` trial merges (both orders), `git diff --stat`, `origin/custom_build` at `69cd680b`; unmodified `verify-merge-invariants.sh` on a rehearsal merge (`anchor_exempt=1`, 19 violations); `fork-anchors.txt`, `fork-anchor-exemptions.txt`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg3.md`, `build.gradle.kts`, `gradle/libs.versions.toml`, `ClickHouseHelperClient.java`, `ClickHouseFieldDescriptor.java`, `DeclaredNullability.java`, `Column.java`, `Table.java`, `ClickHouseWriter.java`
- Phase evidence: `23-EVIDENCE.md` (R-01, R-02), `24-COMMAND-EVIDENCE.md` (E-04, E-15, E-16), `21-REVIEW.md`, `22-REVIEW.md`, `23-REVIEW.md`, `24-MILESTONE-CLOSE-RECORD.md`, `RETROSPECTIVE.md`, `STATE.md`
- Live measurement on `clickhouse/clickhouse-server:latest` (version `26.9.19.9`, image `bdb5c911a23d`) via `clickhouse local`; client parsing via `clickhouse-data-0.9.6` and Jackson 2.22.3 jars
- Shadow 9.6.1 source (`ShadowJar.kt`, `ShadowJavaPlugin.kt`, `ShadowCopyAction.kt`) and changelog (9.0.0 breaking `from` change)
- Maven Central: `connect-api` 4.1.1 and 4.4.0; `kafka-clients` 4.1.1 and 4.4.0; `guava` 33.7.2-jre metadata; `client-v2` 0.9.6 pom; `httpclient5` 5.6.5; `protobuf-java`, `protoc`, `protobuf-java-util`; `at.yawk.lz4`
- Confluent `common-parent` 7.9.1 pom (`kafka.version` 7.9.1-ccs, `protobuf.version` 3.25.5, `guava.version` 32.0.1-jre)
- OSV API batch queries, 2026-10-21 (lags on very new advisories)
- GitHub REST (unauthenticated): `actions/runs?branch=custom_build`; PR #10 run 39501799049 (red); PR #859; PR #839; issues #838, #518; PR #529
- Upstream commits `30521f74`, `fc82b263`, `bc896506`, `a6f1df83`
- ClickHouse documentation: DESCRIBE TABLE (`describe_include_subcolumns`, `is_subcolumn`), Array, Nullable, Map, Tuple, String
- Gradle 9.8.1 release notes and 9.x upgrade guide

### Secondary (MEDIUM)
- Guava GitHub releases, HeroDevs `cve-2026-102554`, Snyk (identity and severity disputed)
- Kafka 4.1 to 4.3 upgrade notes (KIP-1161, KIP-1205, KIP-1188); Kafka 4.4.0 release notes (titles only; KAFKA-19414, KAFKA-20430, KAFKA-19404, KAFKA-20881)
- protobuf.dev cross-version runtime guarantee; 2023-12-05 planned 26.x changes
- Kafka `DeadLetterQueueReporter` at `4.1.1` and `4.4.0-rc4` (raw source diff)

### Tertiary (LOW, needs validation)
- `kafka-protobuf-serializer` 7.9.x with protobuf-java 4.x: no public report either way
- Kafka 4.4.0 Connect behaviour beyond release-note titles: unverified

---
*Research completed: 2026-10-21*
*Ready for roadmap: yes, after the user decisions in Gaps (PR #10, 999.5 name form, 999.4 `size<digits>` scope, Dependabot freeze, production worker version)*
