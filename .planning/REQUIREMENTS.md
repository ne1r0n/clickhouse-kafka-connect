# Requirements: ClickHouse Kafka Connect — `custom_build` Fork

**Defined:** 2026-10-28
**Milestone:** v1.7.0_gg6-maintenance — Dependency Absorption, Upstream Tail & Advisory Debt
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

## v1 Requirements

### Dependency Sweep

- [x] **SWP-01**: The lz4-java 1.12.1 bump (PR #19) is absorbed as its own commit on the scratch branch, with the suite green.
- [x] **SWP-02**: The okhttp 5.5.1 (PR #18), Gradle wrapper 9.8.2 (PR #20, only `gradle-wrapper.properties` changes, `gradlew.bat` line endings intact) and `actions/setup-java` 6.1.0 (PR #16, SHA-pinned, pin-guard tests pass) bumps are each absorbed as their own commit.
- [x] **SWP-03**: PR #10 (protobuf-java 4.36.2) is closed with a Dependabot `ignore` for major versions >= 4 in the `gradle` block only, and the decision (the `VerifyError` against the Confluent 7.9.1 test stack) is recorded in `docs/FORK-DELTA.md`.
- [x] **SWP-04**: Dependabot PR heads are merged on the scratch branch, not on `origin`, and a fresh `pre-v1.7.0_gg6` rollback tag exists before the first change.

### Upstream Tail

- [ ] **TAL-01**: Upstream `main` is merged (tail past `v1.7.0`, re-checked with `git ls-remote upstream main` and `git merge-tree` immediately before) as a mechanical merge commit that passes `verify-merge-invariants.sh`, with a `MERGE-CHECKLIST-v1.7.0-tail-2.md`.
- [ ] **TAL-02**: `ProxySinkTask` keeps the fork's `connectorName` constructor and upstream's `DBWriter` test-seam constructor without an ambiguous `null`, through a 4-argument package-private constructor both delegate to.
- [ ] **TAL-03**: Every fork caller and test of `Utils.handleException` uses the 5-argument form, and `compileTestJava` and `compileIntegrationTestJava` pass after the upstream `CreateTableStatement` refactor.
- [ ] **TAL-04**: `additionalRetriableErrorCodes` is a supported setting, pinned by tests: the parse/validate matrix (trim, de-duplicate, reject non-integers and values <= 0, on both the `ConfigDef` validator and the constructor path), the 15-code default set including 159, a default-empty no-op, and that a `DataException` is never reclassified.
- [ ] **TAL-05**: New anchor rows protect the `ProxySinkTask` statistics/`connectorName` plumbing (ORCH-02) and the `DEFAULT_RETRIABLE_ERROR_CODES` lookup, and `fork-anchors.txt` passes after the merge.

### Client 0.11.0

- [ ] **CLT-01**: Before the bump, pins on client 0.9.6 record the current behavior: error 159 stays a `RetriableException`, a 502/503 and unexpected-status `ClientException` classification, and timeout-message prefixes.
- [ ] **CLT-02**: `clickHouseDriver` moves to 0.11.0 as a single catalog change marked `# FORK OVERRIDE (upstream 0.9.6)`, with an anchor row and a Docker-free catalog pin test.
- [ ] **CLT-03**: `ClickHouseHelperClient.createClientV2` passes only `protocol://host:port` to `addEndpoint` and sets `ignore_unknown_config_key=true` before the `jdbcConnectionProperties` loop, so `?ssl=true&sslmode=strict` still works; both pinned live.
- [ ] **CLT-04**: A `Client011BehaviourPinLiveTest` pins 159 as `RetriableException`, the 502/503 shapes with and without `retryOnSocketException`, `ClientException` as non-retriable, and ZSTD decoding on V2 with `setCompressionMethod=false` and `compress=true`.
- [ ] **CLT-05**: The D-17 `network_compression_method=lz4` default stays; its rationale in the Javadoc, `docs/FORK-DELTA.md` (Recorded Fact 26, CMP rows) and the gg1 rollout warning is reworded to what was measured on 0.11.0.
- [ ] **CLT-06**: Exactly-once safety is checked: a RowBinary byte comparison of 0.9.6 versus 0.11.0 on the type matrices (nested `Tuple`/`Map` nullable markers), and a `BEFORE_PROCESSING` batch replayed across the upgrade has either identical bytes or a documented clean-stop requirement.
- [ ] **CLT-07**: The date/datetime live pins also pass on a non-UTC JVM, and the Connect-worker `integrationTest` plus one manual nightly dispatch pass on 0.11.0.
- [ ] **CLT-08**: Before/after Confluent archive listings are captured, and the `zstd-jni` addition and the loss of the guava CVE override are recorded.
- [ ] **CLT-09**: If the audit fails, gg6 ships on 0.9.6 by reverting the one catalog commit, with the two connector fixes and pins kept where safe.

### CI Hardening

- [ ] **CIH-01**: `increment-version.yml` never writes `vnull`: input and output are validated against `^v\d+\.\d+\.\d+(_gg[1-9]\d*)?$`, a new `gg` step bumps the counter (a suffix-less version becomes `_gg1`), and patch/minor/major reset the suffix to `_gg1`.
- [ ] **CIH-02**: The version logic lives in a script with a table-driven Docker-free test, the npm `semver` install is gone, SHA pins and permissions are unchanged, and the "known and left unfixed" note is removed.
- [ ] **CIH-03**: A version-bump PR also edits the `VERSION` row of `fork-anchors.txt`, or the script fails when it does not.
- [ ] **CIH-04**: `release.yaml` and `pre-release.yaml` run the archive compare against an allow-list keyed by `VERSION`, with the baseline rebuilt from the previous tag in a separate worktree, the report published as an artifact, and `REL-xx` anchor counts and `TestsWorkflowPinGuardUnitTest` updated.
- [ ] **CIH-05**: The compare script lives under `.github/scripts/` and has a synthetic-archive self-test covering the UNALLOWED and STALE-ROW failures and the exit codes 0, 1 and 2.

### Advisory Debt

- [ ] **ADV-01**: Every Phase 30 review row (WR-01..WR-03, IN-01..IN-04) and Phase 31 row (WR-01..WR-03, IN-01..IN-03) is fixed or dispositioned with an owner and a re-open trigger.
- [ ] **ADV-02**: The 16 unfixed review findings in `29-MILESTONE-CLOSE-RECORD.md` are fixed or dispositioned the same way.
- [ ] **ADV-03**: The real release of `v1.7.0_gg5` (GitHub release of 2026-10-28) is recorded as evidence, closing the "no run exercised the publish path" caveat.

### Buffered Offset Commit (upstream #777)

- [ ] **BUF-01**: With `bufferCount > 0` and a topic-renaming SMT (e.g. `RegexRouter`), `ChunkFlusher.flush` keys flushed offsets by `originalTopic()`, `originalKafkaPartition()` and `originalKafkaOffset() + 1`, so `preCommit` returns offsets for the assigned partitions and Connect no longer logs "Ignoring invalid task provided offset".
- [ ] **BUF-02**: `AtLeastOnceBufferStrategy.close` removes buffered records of revoked partitions by their original topic and partition, and `ExactlyOnceBufferStrategy.put` keys `perPartitionBuffer` by the original topic and partition.
- [ ] **BUF-03**: `Processing.doLogic` is checked for the same defect: the exactly-once state record is keyed by `record.getSinkRecord().topic()` (the transformed topic); the result (fix with pins, or a measured and documented non-issue) is recorded, since the operator runs with `exactlyOnce=false`.
- [ ] **BUF-04**: Docker-free unit tests build a `SinkRecord` through `newRecord` with a different topic and pin that `preCommit` returns offsets under the original topic and partition, that `close()` drops buffered records by the original partition, and that the exactly-once buffer groups by it; the non-renamed case is unchanged.
- [ ] **BUF-05**: A live pin on ClickHouse `latest` × client V2 (`bufferCount > 0` plus `RegexRouter`) shows committed offsets equal the end offsets; the non-buffered `DirectDeliveryStrategy` path and the `bufferCount`/`bufferFlushTime` contract are unchanged.
- [ ] **BUF-06**: The stand at `~/dev/clickhouse_mvp/local_kc_test` is rerun on the fixed jar (300000 records, 3 partitions, `buf-20k` and `buf-100k`): `scripts/offsets.sh` shows committed = end rather than none, and after `docker kill kc_connect_new` duplicates stay within the at-least-once window (reporter's measurement: 392k and 400k duplicates before, 41.5k and 0 after).
- [ ] **BUF-07**: FORK-DELTA gets a row and `fork-anchors.txt` an anchor for the fix, and the rollout note states the behaviour change (offsets now committed, so the group no longer rereads from `auto.offset.reset` after a restart or rebalance). Upstream PR #786 was closed unmerged, so the fix is fork-only until upstream resolves #777.

### Release

- [ ] **RLS-01**: `VERSION=v1.7.0_gg6` changes in the same commit as its anchor row, with CHANGELOG (absorbing upstream `Unreleased`) and `docs/FORK-DELTA.md` updated.
- [ ] **RLS-02**: `docs/ROLLOUT-v1.7.0_gg6.md` states each operator-visible effect with a "measured" or "read from the code" label: `ssl`/`sslmode` never honoured on V2, 159 retry cadence, 502/503 shape, `User-Agent` tail, socket-buffer opt-back keys, zstd native platforms, `additionalRetriableErrorCodes`, and the clean-stop requirement.
- [ ] **RLS-03**: The gg6 allow-list is generated last from the final tree and the compare passes against it.
- [ ] **RLS-04**: A freshly spawned reviewer checks the operator documents before the push gate.
- [ ] **RLS-05**: The release dry run passes, `git log sync/v1.7.0..origin/custom_build` is checked for divergence, a `--no-ff` merge is made after human approval, and one `tests.yaml` run on the published SHA is green before the tag is pushed.
- [ ] **RLS-06**: The suite passes twice on one pinned ClickHouse `latest` build (`SELECT version()` recorded) with client V2, and `testUnit` passes with Docker stopped.

## v2 Requirements

- **FUT-01**: A tagged upstream sync once upstream tags past `v1.7.0`.
- **FUT-02**: Upstream workflows still on `@v3`-era actions.
- **FUT-03**: Protobuf 4 in the test stack, which needs a Confluent 8.x test-stack move.
- **FUT-04**: Retiring the V1 client path.
- **FUT-05**: A retry budget or a "remove from default list" setting for retriable codes.

## Out of Scope

| Feature | Reason |
|---------|--------|
| protobuf-java 4.x (PR #10) | Breaks `ProtobufIngestTest` with `VerifyError`; needs a Confluent 8.x test stack |
| Adding `ServerTimeoutExceeded` to the client retry list | A client-side re-send of an insert can double-write; Connect redelivery is the safe layer |
| Setting `socket_rcvbuf`/`socket_sndbuf` | Client 0.11.0 deliberately leaves them to OS auto-tuning; throughput effect unmeasured |
| Removing the D-17 `lz4` default | V1 still needs it, it is upstream's flag in a high-risk file, and it avoids loading `zstd-jni` natives |
| New connector features beyond the upstream tail | Maintenance milestone |
| Running the upgrade on production connectors | Operator action, covered by the rollout note |
| Switching to `-ggN` version suffixes | semver `patch` would drop the suffix |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| SWP-01 | Phase 33 | Complete |
| SWP-02 | Phase 33 | Complete |
| SWP-03 | Phase 33 | Complete |
| SWP-04 | Phase 33 | Complete |
| TAL-01 | Phase 34 | Pending |
| TAL-02 | Phase 34 | Pending |
| TAL-03 | Phase 34 | Pending |
| TAL-04 | Phase 34 | Pending |
| TAL-05 | Phase 34 | Pending |
| BUF-01 | Phase 35 | Pending |
| BUF-02 | Phase 35 | Pending |
| BUF-03 | Phase 35 | Pending |
| BUF-04 | Phase 35 | Pending |
| BUF-05 | Phase 35 | Pending |
| BUF-06 | Phase 35 | Pending |
| BUF-07 | Phase 35 | Pending |
| CLT-01 | Phase 36 | Pending |
| CLT-02 | Phase 36 | Pending |
| CLT-03 | Phase 36 | Pending |
| CLT-04 | Phase 36 | Pending |
| CLT-05 | Phase 36 | Pending |
| CLT-06 | Phase 36 | Pending |
| CLT-07 | Phase 36 | Pending |
| CLT-08 | Phase 36 | Pending |
| CLT-09 | Phase 36 | Pending |
| CIH-01 | Phase 37 | Pending |
| CIH-02 | Phase 37 | Pending |
| CIH-03 | Phase 37 | Pending |
| CIH-04 | Phase 37 | Pending |
| CIH-05 | Phase 37 | Pending |
| ADV-01 | Phase 37 | Pending |
| ADV-02 | Phase 37 | Pending |
| ADV-03 | Phase 37 | Pending |
| RLS-01 | Phase 38 | Pending |
| RLS-02 | Phase 38 | Pending |
| RLS-03 | Phase 38 | Pending |
| RLS-04 | Phase 38 | Pending |
| RLS-05 | Phase 38 | Pending |
| RLS-06 | Phase 38 | Pending |

**Coverage:**
- v1 requirements: 39 total
- Mapped to phases: 39 (Phase 33: 4, Phase 34: 5, Phase 35: 7, Phase 36: 9, Phase 37: 8, Phase 38: 6)
- Unmapped: 0 ✓

Two mappings have a hand-off to Phase 38:
- **BUF-07** is owned by Phase 35. Its FORK-DELTA row and anchor land with the fix, and its rollout item is recorded there with evidence. Phase 38 places that item in `docs/ROLLOUT-v1.7.0_gg6.md`, and RLS-02's success criterion checks it.
- **CLT-09** is conditional. It is satisfied either by a recorded "go" on 0.11.0 or by the one-commit revert to 0.9.6. If it fires, RLS-02's note keeps only the items that hold on 0.9.6.

---
*Requirements defined: 2026-10-28*
*Last updated: 2026-10-28 after roadmap creation (Phases 33-38, 39/39 mapped)*
