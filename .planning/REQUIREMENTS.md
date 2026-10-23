# Requirements: ClickHouse Kafka Connect — `custom_build` Fork

**Defined:** 2026-10-21
**Milestone:** `v1.7.0_gg4-hardening` Dependency Absorption, Discovery Defects & Review Debt
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

**Sources:**
- The open items in PROJECT.md → Requirements → Active, and the milestone-start items.
- `milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md` (the 17 carried review advisories and two carried items, with re-open triggers).
- ROADMAP backlog entries 999.4 and 999.5.
- Milestone research in `.planning/research/` (STACK, FEATURES, ARCHITECTURE, PITFALLS, SUMMARY), run 2026-10-21 with live measurements on ClickHouse `26.9.19.9`.

**Scope decisions made by the user on 2026-10-21:**
- Take every open candidate except ClickHouse client `0.10.0`.
- Merge the Dependabot batch already on `origin/custom_build`; upstream's untagged tail is taken as a tagless mini-sync in the usual shape.
- PR #10 (protobuf-java 4.36.2) stays open: its own CI run is red and the catalog key also drives `protoc`. (Reversed from the pre-research "merge it" answer after the Stack research.)
- 999.5: the raw identifier as written in `CREATE TABLE` is the canonical name form. No dual-form alias; operators rename.
- 999.4: fix both drop sites, including `size<digits>` under an Array parent in `Table.java`.
- The user creates the `dependencies` label in the GitHub UI.
- Dependabot freeze on `origin/custom_build` from the absorption merge until publication.
- Production worker: `confluentinc/cp-kafka-connect-base:7.9.7` (Kafka 3.9), so the connector must stay loadable on a 3.9 worker although it compiles against `connect-api` 4.4.0.
- Publish as `v1.7.0_gg4`.

## v1 Requirements

### Dependency absorption and tagless mini-sync

- [x] **DEPS-02**: The working branch contains `origin/custom_build`'s nine Dependabot merges as one merge commit:
  - Gradle wrapper 9.8.1 (#8), `connect-api` 4.4.0 (#9), commons-lang3 3.21.0 (#6), ben-manes versions 0.66.0 (#7), gson 2.14.0 (#13), httpclient5 5.6.5 (#12), Shadow 9.6.1 (#11), jackson 2.22.4 (#14), slf4j 2.0.20 (#15), tip `69cd680b` (re-read with `git ls-remote origin custom_build` immediately before the merge);
  - the full suite (`./gradlew test`, ClickHouse `latest` × client V2) is green on the merged tree, with the same 18 named skips;
  - spotless is baseline-equal, and a `--warning-mode all` run is recorded for the Gradle 9.8.1 Kotlin DSL deprecations with no build-script change.
- [x] **DEPS-03**: The anchors invariant holds on the merged tree without editing `verify-merge-invariants.sh`:
  - `fork-anchors.txt` is updated in the merge commit itself (`kafka = "4.4.0"`, the slf4j literal);
  - every published Dependabot commit that violates a pre-merge anchor has a SHA-pinned row in `fork-anchor-exemptions.txt`, generated from the gate's own output;
  - the gate passes on the tip with `PRE_TAG` at the pre-merge rollback tag.
- [x] **BLD-15**: The shipped Confluent archive is intact under Shadow 9.6.1:
  - `createConfluentArchive` built on the tip contains zero nested `*.jar` entries;
  - an entry-by-entry SHA-256 compare against the `v1.7.0_gg3` archive (7,376 entries, 24-COMMAND-EVIDENCE E-04) differs only by an allow-listed set (version bumps, guava and its five small dependencies);
  - `MANIFEST.MF` is unchanged apart from versions (`addMultiReleaseAttribute` decision recorded);
  - the repair and the compare are recorded as evidence.
- [x] **BLD-16**: `connect-api` 4.4.0 is proven compile-time only:
  - the archive contains no Connect API classes;
  - the compiled connector references no Connect API member that is absent from the production worker's API, Apache Kafka 3.9 (`cp-kafka-connect-base:7.9.7`), nor any member added after 4.1.1 (bytecode evidence against both jars);
  - FORK-DELTA's "production Connect API version" sentence is corrected to say the worker supplies the API at runtime, and the `ClickHouseSinkTask` comment and the three DLQ-emulation Javadocs are updated.
- [x] **CI-20**: `tests.yaml` builds `createConfluentArchive` and fails on any nested `*.jar` entry, so the next Shadow bump cannot empty the jar unnoticed; `TestsWorkflowPinGuardUnitTest` pins the step.
- [x] **SYNC-11**: Upstream's untagged tail is merged as a tagless mini-sync:
  - the stale local refs `refs/upstream/main` and `refs/upstream/v1.6.0` are deleted first, and `UPSTREAM_REF` is the full SHA `a6f1df839c079180bc7c343b5823f205e204dafc`;
  - one mechanical merge commit behind `verify-merge-invariants.sh` with a rollback tag at the pre-merge tip, conflicts only in `.gitignore` and `gradle/libs.versions.toml`;
  - the catalog hunk is resolved to the fork's values for shared keys (`kafka` 4.4.0, `lz4Java` 1.12.0, jackson 2.22.4, gson 2.14.0, lombok 1.18.48), `guava = "33.7.2-jre"` is added, and the orphan `httpclientTest` key is deleted;
  - `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` records the resolution table and the conflicted paths.
- [x] **SYNC-12**: The post-sync tree is the baseline for every later phase: two full `--rerun` suite runs are green on `latest` × V2, `dependencyInsight` shows guava 33.7.2 on the runtime classpath, and FORK-DELTA records the guava addition and the httpclient5 pin with their advisories labelled by source.

### Discovery defect 999.4 (Tuple element named `size`, `size0`, `size<digits>`)

- [x] **DESC-06**: A live DESCRIBE spike on ClickHouse `latest` records the rows for the E-15 matrix (13 affected tables, 19 look-alikes, `s4_o01`) and the new shapes (`Array(Array(Tuple(size1, size0)))`, `Array(Tuple(size0 Tuple(a ...)))`, `Array(Tuple(size0 String, size String))`, `Array(Map(String, Tuple(size, size0)))`, `Tuple(keys, values)`), confirms or denies the structural-then-declared double row, and states the classifier rule before any code changes.
- [x] **DESC-07**: The duplicated V1/V2 DESCRIBE loop is extracted into one fork-owned builder, behaviour-preserving and landing green first, so the 999.4 pin exercises the production loop instead of a copy (closes 23-REVIEW WR-02); one manual `CLIENT_VERSION=V1` run of the helper live tests is recorded.
- [x] **DESC-08**: A declared Tuple element named `size`, `size0` or `size<digits>` becomes a `Column` at both drop sites (`shouldSkipDescribeRow` and `Table.updateParent`'s `SIZE_FIELD_MATCHER`), classified on the parent's raw type, array depth and the row's own type; structural rows are still dropped (Array offsets, Nullable null map, Map `keys`/`values`, nested `.size1`, the DESC-05 ancestor walk under JSON/Dynamic), and a structural twin never enters the skip set; a Docker-free unit table of captured real DESCRIBE rows pins the rule.
- [x] **DESC-09**: Live round-trip: for every E-15 affected shape, records insert and the typed read-back equals the original records; the DESC-05 regression rows stay green; the D-04 pin flips (no unpaired-element WARN for these tables).
- [x] **DESC-10**: FORK-DELTA rows for `ClickHouseHelperClient` and `Table.java`, `fork-anchors.txt` rows, a CHANGELOG line, Known costs item 10 retired, and ROADMAP backlog 999.4 marked delivered in the same commit as the fix.

### Discovery defect 999.5 (raw identifier as the canonical name form)

- [x] **NAME-01**: The escaped-quote branch of `DeclaredNullability.normalise` is pinned on the current code, seen red under a mutation that always closes the quote (closes 23-REVIEW WR-01), before any decode change.
- [x] **NAME-02**: A live name-matrix spike on `latest` records, for root columns, Tuple elements and enum labels, the current and the decoded form of names containing a backslash, a literal backslash-n, LF, tab, CR, slash, double quote, backtick and non-ASCII characters.
- [x] **NAME-03**: `ClickHouseFieldDescriptor.fromJsonRow` yields the raw identifier for column and element names (names only; type text is not decoded); the V1 describe path reads raw lines in UTF-8; `TableTest.extractCommentV1` stays green.
- [x] **NAME-04**: `DeclaredNullability` pairs on the raw-name key (the element key unescapes SQL identifiers; `normalise` is rewritten for the one-layer-fewer escaping); the COLS-13 live twins in `NullableTupleMatrixLiveTest` stay green; 23-REVIEW IN-01 (Javadoc "all or none") and IN-02 (null-name check inside the `try`) are closed.
- [x] **NAME-05**: Live both-form pins: a Kafka record using the raw name lands with typed read-back equal to the record for every spiked character; a record still using the doubled form behaves as the rollout note states (root column ignored and `DEFAULT` stored; Tuple element fails loudly), pinned, not changed.
- [x] **NAME-06**: Enum labels containing an escaped character are measured; they either round-trip under the raw form with a live pin, or the remaining behaviour is stated in FORK-DELTA Known costs and the rollout note.
- [x] **NAME-07**: FORK-DELTA row, CHANGELOG line, Known costs item 11 retired, and ROADMAP backlog 999.5 marked delivered in the same commit as the fix.

### Writer failure path and enum advisories

- [x] **WRT-16**: `WriterFailureClassificationUnitTest` gains rows for a `JsonProcessingException` alone, over a `SocketTimeoutException`, over a timeout-text `IOException`, and a plain `IOException`, green on the unchanged writer first; the existing 184 cases are byte-identical.
- [x] **WRT-17**: `JacksonException` is excluded per link of the cause chain from the `IOException` pass-through in `ClickHouseWriter`, so on the opt-in JSON-as-string route a sentinel Map key reaches none of the task status, the DLQ stack-trace header and the `Error inserting records` throwable, while a timeout beneath a Jackson exception stays retriable; `JsonAsStringKeyResidualLiveTest` is flipped deliberately; the exception sentence in FORK-DELTA Known costs item 6 is removed (closes 22-REVIEW WR-01).
- [x] **WRT-18**: An unknown Enum label is reported with the column name and a lookup-miss cause instead of `NullPointerException` (22-REVIEW IN-01), and the `No content in enum` text names the column (IN-02); both stay value-free and the `Failed to write column ` prefix is byte-identical.
- [x] **WRT-19**: The metadata-derivation duplication in `ClickHouseWriter` is removed (22-REVIEW IN-03), planning-only identifiers leave `src/main` comments (IN-05, src half), and `JsonAsStringKeyResidualLiveTest`'s Javadoc and display names carry no planning IDs (24-REVIEW IN-03).

### Test and build hygiene

- [x] **DEBT-13**: `TestsWorkflowPinGuardUnitTest.ktsCode` strips comments string- and line-comment-aware, with fixtures for `/*` inside a string and inside a `//` comment (21-REVIEW WR-02); this lands before the first `build.gradle.kts` edit of the milestone.
- [x] **DEBT-14**: The pin guard's `runs-on` rule ignores a trailing comment (21-REVIEW WR-01), the fork-count rules reject commented-out or guarded text (WR-03), the `dependabot.yml` reader tolerates valid YAML layouts (IN-01), and the fixture list is derived from the production constant (IN-02); each change is seen red on a fixture.
- [x] **DEBT-15**: The `slf4j-reload4j` literal in `build.gradle.kts` moves into the version catalog with its anchor row moved (21-REVIEW IN-04).
- [x] **DEBT-16**: `schemaWithFixedStringMismatchTest` fails when `put` does not throw (22-REVIEW IN-04); `CountingHelperClient.useClientV2Calls` is asserted or removed (IN-05, test half); `NullableTupleMatrixLiveTest`'s premise loops fail with a message instead of passing vacuously or throwing a bare NPE (23-REVIEW IN-03); no existing assertion is deleted or weakened.
- [x] **DEBT-17**: `jacocoForkReport` lists `DeclaredNullability`, `WithheldMessageException` and every fork class this milestone adds, edited once in the last code phase, with the FORK-DELTA row in the same commit.

### Operator items

- [x] **OPS-04**: The user creates the `dependencies` label; the milestone records the creation, and the first Dependabot PR opened after it (if any within the milestone) arrives without the "labels could not be found" comment, otherwise the check is recorded as pending the next monthly run.
- [ ] **OPS-05**: No merge lands on `origin/custom_build` between the DEPS-02 merge and the publication merge; the release gate re-fetches and verifies that the origin tip equals the merge base, or re-absorbs with a fresh exemption set before gating.
- [x] **OPS-06**: Production runs `confluentinc/cp-kafka-connect-base:7.9.7` (Confluent Platform 7.9, Apache Kafka 3.9; stated by the user 2026-10-21). The rollout note names that worker, states the oldest worker the connector is verified against, and says that the KIP-1161 LIST-config strictness applies only from Kafka 4.2 workers.

### Release and publication

- [x] **PUB-11**: `VERSION=v1.7.0_gg4` with its anchor row in the same commit and a `clean` build; `CHANGELOG.md` has a `1.7.0_gg4` section; FORK-DELTA rows and raw numbers are refreshed once, in this phase.
- [x] **ROLL-04**: `docs/ROLLOUT-v1.7.0_gg4.md` covers, with every quantifier matched to a measurement and every command run live: 999.4 (suspects query before, WARN grep after, no clean stop on upgrade, clean stop before rollback, the unrepaired data tail); 999.5 (canonical form, per-character old/new name table, rename instruction, unchanged detection query, root-silent versus element-loud, clean stop in both directions); the removed Map-key exception; `connect-api` 4.4.0 (not bundled, worker version governs); Shadow 9 and Gradle 9.8.1 (no operator action, archive compare as evidence); the guava and httpclient5 advisories labelled by source; the MBean `version=` segment.
- [x] **ROLL-05**: `docs/ROLLOUT-v1.7.0_gg1.md` and `docs/ROLLOUT-v1.7.0_gg2.md` carry the corrected `enableDbTopicSplit=true` key rule and state-table query from the gg3 note.
- [x] **PUB-12**: An orchestrator-spawned, fresh-context `gsd-code-reviewer` reviews every operator document before the publication gate, repeated until a round finds nothing (at most three rounds), with dispositions recorded and no reused finding IDs.
- [ ] **PUB-13**: Two full `--rerun` suite runs are green on `latest` × V2 on the release tip; the anchors gate passes on the tip; the rehearsed merge tree equals the published merge tree; the `--no-ff` merge into `custom_build` lands after human approval; the published SHA has exactly one `tests.yaml` run and it is green; tags `v1.7.0_gg4` and `pre-v1.7.0_gg4` are pushed after that run; the scratch branch is deleted from `origin`.

## v2 Requirements

Deferred to a later milestone. Tracked but not in the current roadmap.

### Dependencies

- **DEPS-F1**: ClickHouse client `0.10.0` (Dependabot PR #4) — the user's standing exclusion; the PR stays open and unignored.
- **DEPS-F2**: protobuf-java 4.x (Dependabot PR #10) — revisit when its CI is green; needs the failing test names and probably `protobuf-java-util` at the same major.
- **DEPS-F3**: Kotlin DSL deprecations surfaced by Gradle 9.8.1 `--warning-mode all`.
- **DEPS-F4**: The upstream workflows still on `@v3`-era actions (`integrationTests.yaml`, `release.yaml`, `pre-release.yaml`, `increment-version.yml`) — upstream-identical by policy.

### Coverage

- **COV-F1**: Client V1 coverage in CI for the describe path — today one manual run per change is the only evidence.
- **NAME-F1**: A one-release metadata-only WARN for a Kafka field that matches only the doubled form — recommended against by research; reconsider if operators report missed renames.

## Out of Scope

| Feature | Reason |
|---------|--------|
| ClickHouse client `0.10.0` | User exclusion (2026-10-21); PR #4 left open, unmerged, not ignored |
| protobuf-java 4.36.2 (PR #10) | Its own CI run is red; `libprotobuf` also drives `protoc`; Confluent 7.9.1 is built on 3.25.5; no advisory on 3.25.9 (user decision 2026-10-21) |
| A tagged upstream sync | No upstream tag past `v1.7.0`; the tail is taken as a tagless mini-sync (SYNC-11) |
| Decoding the DESCRIBE type text in `fromJsonRow` | Measured to change every multi-line Tuple type; names only (NAME-03) |
| A permanent dual-form name lookup | Adds a per-record branch and hides unrenamed producers; operators rename (ROLL-04) |
| Editing `verify-merge-invariants.sh` | Standing decision D-02; exemptions and anchors carry the change |
| Editing, deleting or fixing the nightly `integrationTests.yaml` | Disabled in settings since 2026-10-16; file stays upstream-identical |
| New connector features; refactoring `Processing`, `KeeperStateProvider`, `ClickHouseSinkConfig` | Standing exclusions; this milestone changes discovery, the writer failure path, the build and the docs |
| Running the upgrade on production connectors | Operators' call; the rollout note is the deliverable |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DEPS-02 | Phase 25 | Complete |
| DEPS-03 | Phase 25 | Complete |
| BLD-15 | Phase 25 | Complete |
| BLD-16 | Phase 25 | Complete |
| CI-20 | Phase 25 | Complete |
| SYNC-11 | Phase 25 | Complete |
| SYNC-12 | Phase 25 | Complete |
| DESC-06 | Phase 26 | Complete |
| DESC-07 | Phase 26 | Complete |
| DESC-08 | Phase 26 | Complete |
| DESC-09 | Phase 26 | Complete |
| DESC-10 | Phase 26 | Complete |
| NAME-01 | Phase 28 | Complete |
| NAME-02 | Phase 28 | Complete |
| NAME-03 | Phase 28 | Complete |
| NAME-04 | Phase 28 | Complete |
| NAME-05 | Phase 28 | Complete |
| NAME-06 | Phase 28 | Complete |
| NAME-07 | Phase 28 | Complete |
| WRT-16 | Phase 27 | Complete |
| WRT-17 | Phase 27 | Complete |
| WRT-18 | Phase 27 | Complete |
| WRT-19 | Phase 27 | Complete |
| DEBT-13 | Phase 25 | Complete |
| DEBT-14 | Phase 25 | Complete |
| DEBT-15 | Phase 25 | Complete |
| DEBT-16 | Phase 27 | Complete |
| DEBT-17 | Phase 28 | Complete |
| OPS-04 | Phase 25 | Complete |
| OPS-05 | Phase 29 | Pending |
| OPS-06 | Phase 29 | Complete |
| PUB-11 | Phase 29 | Complete |
| ROLL-04 | Phase 29 | Complete |
| ROLL-05 | Phase 29 | Complete |
| PUB-12 | Phase 29 | Complete |
| PUB-13 | Phase 29 | Pending |

**Coverage:**
- v1 requirements: 36 total
- Mapped to phases: 36 (Phase 25: 11, Phase 26: 5, Phase 27: 5, Phase 28: 8, Phase 29: 7)
- Unmapped: 0 ✓

---
*Requirements defined: 2026-10-21*
*Last updated: 2026-10-21 after roadmap creation (Phases 25-29, 36/36 mapped)*
