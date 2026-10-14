# Requirements: ClickHouse Kafka Connect — `custom_build` Fork

**Defined:** 2026-10-10
**Milestone:** `v1.7.0_gg2-hardening` Follow-up Hardening & CI Hygiene
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

**Sources:** the 11 open follow-ups in PROJECT.md → Active; research in `.planning/research/` (SUMMARY, STACK, FEATURES, ARCHITECTURE, PITFALLS). Scope decisions made by the user on 2026-10-10: early docs-only publication first; release as `v1.7.0_gg2`; CI changes approved (Node 24 action majors, `ubuntu-24.04` pin, SHA pinning + `permissions:`, Lombok warning); upstream-identical code fixed in the fork with each patch recorded in FORK-DELTA; Tuple handled by spike then go/no-go; empty-CDC skip on all routes; IN-07..IN-10 folded into the early publication; Dependabot for actions and the `test-head.yaml` fix in scope.

## v1 Requirements

### Publication — early docs fix

- [x] **PUB-05**: 15-REVIEW docs findings IN-07..IN-10 are fixed in `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/FORK-DELTA.md` and `CHANGELOG.md` — rollout item 2 recovery steps qualified by `exactlyOnce`, item 8 states that going forward again after `git revert -m 1` requires reverting the revert, FORK-DELTA grep commands print totals, CHANGELOG/ROLLOUT entries are plain language (no bare planning IDs) and mention the MBean `version=`/client-name change and the lz4 advisory reason — and the changed docs pass a code review **before** the merge gate
- [x] **PUB-06**: An operator reading `origin/custom_build` sees the corrected rollout doc: a docs-only non-force `--no-ff` merge of `sync/v1.7.0` into `custom_build`, made only after the user's own explicit approval, whose diff against `origin/custom_build` is limited to `.planning/` and `docs/` (plus `CHANGELOG.md` if PUB-05 touches it); one branch pushed, no tag, exactly one green `tests.yaml` run on that SHA

### Column mapping — 999.1

- [x] **COLS-08**: `Nullable(Enum8)` and `Nullable(Enum16)` columns round-trip through RowBinary for NULL and non-NULL values, top-level and nested (`Array(Nullable(Enum))`, `Map(K, Nullable(Enum))`), checked by typed read-back; the D-16/L-13 pin tests that asserted the old behavior are flipped with a written rationale, not deleted
- [x] **COLS-09**: A Tuple spike runs a live twin-table matrix on ClickHouse `latest` — `Nullable(Tuple(...))` with nullable and non-nullable elements, and `Array(Nullable(Tuple(...)))` — and records a go/no-go decision (with the type-string parsing approach) before any Tuple production code lands
- [x] **COLS-10**: On go, `Nullable(Tuple(...))` round-trips with element nullability taken from the declared type string (not DESCRIBE's all-`Nullable` sub-columns, not `Table.java`), and `Array(Nullable(Tuple(...)))` still round-trips; on no-go, the discovery WARN narrows to Tuple and FORK-DELTA records a Known cost with the 999.1 backlog entry updated
- [x] **COLS-11**: The discovery WARN, the `fork-anchors.txt` row for `hasNullableEnumOrTuple` and `docs/FORK-DELTA.md` change in the same commit as the code they describe, and the merge-invariant script stays green

### Writer correctness

- [x] **JSONP-09**: An empty Debezium record (`op=t`, or null `before` and `after`) inserts no row on all three routes — explicit `insertFormat=json`, upstream `bypassRowBinary=true` JSON, and RowBinary — while offsets and exactly-once state still advance; the skip is decided inside the writer so the batch sent on retry stays byte-identical; one live test per route
- [x] **WRT-10**: A missing value for a non-nullable column, including inside `Array(Tuple(...))`, raises a `DataException` naming the column and its ClickHouse type, so Connect status and the DLQ show which column failed (13-REVIEW WR-03)
- [x] **WRT-11**: `ClickHouseWriter`'s error path handles an exception whose message is null without a `NullPointerException`, and retriable vs non-retriable classification is unchanged; pinned by a Docker-free test (13-REVIEW WR-04)
- [x] **WRT-12**: A DateTime parse failure logs the column name, ClickHouse and Kafka types, exception class and value length — never the raw value, directly or via the parse exception's message; pinned by a log-capture test; the patch to upstream-identical lines is recorded in FORK-DELTA (T-13-19)

### Test & build debt

- [x] **DEBT-06**: `Lz4RoundTripLiveTest` asserts LZ4 on the response leg with a standing assertion (`ProfileEvents['NetworkSendBytes']` vs `result_bytes` for the read's query id), seen red with server-response compression disabled (14-REVIEW WR-01)
- [x] **DEBT-07**: `testRuntimeClasspath` resolves only `at.yawk.lz4:lz4-java` — the `org.lz4:lz4-java:1.8.0` duplicate from the Confluent 7.9.1 stack is gone — and the `runtimeClasspath`/`integrationTestRuntimeClasspath` resolution is unchanged (backlog 999.2)
- [x] **DEBT-08**: A pin row covers the mantissa-with-fraction scientific form for Debezium `NanoTimestamp` in `MetadataFreeJsonWriterTest` (15-REVIEW IN-06)
- [x] **DEBT-09**: 15-REVIEW IN-01..IN-05 are each fixed or closed with a written reason in the disposition file, consistent with the COLS-10 outcome
- [x] **BLD-14**: The Lombok "Ambiguous: Jackson2 and Jackson3 exist" warning no longer appears in the build, and a `javap` diff shows `ClickHouseFieldDescriptor`'s generated code is unchanged

### CI hygiene

- [x] **CI-11**: `tests.yaml` uses Node 24 action majors (`actions/checkout` v7.x, `actions/upload-artifact` v7.x) and every action is pinned by full commit SHA (the peeled commit for annotated tags) with a `# vX.Y.Z` comment (T-14-16)
- [x] **CI-12**: `tests.yaml` runs on `ubuntu-24.04` instead of `ubuntu-latest`
- [x] **CI-13**: `tests.yaml` declares a least-privilege `permissions: contents: read` block and checkout uses `persist-credentials: false` (T-14-16)
- [x] **CI-14**: The "Test fork count" step reads the effective `maxParallelForks` from a Gradle task instead of recomputing the formula in shell (14-REVIEW WR-02)
- [x] **CI-15**: `.github/dependabot.yml` includes the `github-actions` ecosystem so the SHA-pinned actions still get update PRs
- [x] **CI-16**: `test-head.yaml` is fixed or disabled in the fork so no scheduled run fails because `tests.yaml` is not a reusable workflow
- [x] **CI-17**: One green CI run on a pushed SHA proves CI-11..CI-16 together — job summary, fork-count notice and both artifact uploads present

### Release

- [x] **ROLL-02**: `docs/ROLLOUT-v1.7.0_gg2.md` gives operators the 999.1 value change (rows already stored wrongly are not repaired), the dedup-token retry consequence verified on `latest`, the empty-CDC skip change, and the MBean `version=` change — reviewed before the publication gate
- [x] **PUB-07**: `VERSION` is `v1.7.0_gg2`, `CHANGELOG.md` has a plain-language `1.7.0_gg2` section, and `docs/FORK-DELTA.md` has a row for every new fork-side patch
- [ ] **SUITE-05**: The full suite is green twice on ClickHouse `latest` × client V2 (0 failures, 0 errors, every skip named) and `testUnit` is green with Docker stopped
- [ ] **PUB-08**: The release lands on `custom_build` as a non-force `--no-ff` merge after the user's own explicit approval, the published SHA has exactly one green CI run, the `v1.7.0_gg2` tag is pushed only after that run is green, and the rollback tag points at a commit that already carries the branch-only trigger

## Future Requirements

- Next upstream sync once upstream tags past `v1.7.0` — same shape as `v1.7.0_gg1-sync`
- Upgrading the other upstream workflows (`integrationTests.yaml`, `release.yaml`, `pre-release.yaml`, `increment-version.yml`) off `@v3`-era actions
- Gradle 9.8.0 and Lombok 1.18.48 — noted by research, not approved

## Out of Scope

| Feature | Reason |
|---------|--------|
| Upstream sync | `upstream/main` = `v1.7.0`; nothing to merge |
| ClickHouse client `0.10.0` | Not approved; same exclusion as the last milestone |
| `serializeJsonRow` refactor (13 IN-04) / shared nested-null helper (13 IN-01) | Rebase churn in a high-risk file for no behavior change |
| Moving to Ubuntu 26.04 runners | Docker 29 / Java 25 image; pinning 24.04 is the approved choice |
| Repairing rows already stored wrongly by 999.1 | Operator data; documented in the rollout note instead |
| Running the upgrade on production connectors | Stays with operators, as in previous milestones |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PUB-05 | Phase 16 | Complete |
| PUB-06 | Phase 16 | Complete |
| COLS-08 | Phase 19 | Complete |
| COLS-09 | Phase 19 | Complete |
| COLS-10 | Phase 19 | Complete |
| COLS-11 | Phase 19 | Complete |
| JSONP-09 | Phase 18 | Complete |
| WRT-10 | Phase 18 | Complete |
| WRT-11 | Phase 18 | Complete |
| WRT-12 | Phase 18 | Complete |
| DEBT-06 | Phase 17 | Complete |
| DEBT-07 | Phase 17 | Complete |
| DEBT-08 | Phase 17 | Complete |
| DEBT-09 | Phase 19 | Complete |
| BLD-14 | Phase 17 | Complete |
| CI-11 | Phase 17 | Complete |
| CI-12 | Phase 17 | Complete |
| CI-13 | Phase 17 | Complete |
| CI-14 | Phase 17 | Complete |
| CI-15 | Phase 17 | Complete |
| CI-16 | Phase 17 | Complete |
| CI-17 | Phase 17 | Complete |
| ROLL-02 | Phase 20 | Complete |
| PUB-07 | Phase 20 | Complete |
| SUITE-05 | Phase 20 | Pending |
| PUB-08 | Phase 20 | Pending |

**Coverage:**
- v1 requirements: 26 total
- Mapped to phases: 26 ✓
- Unmapped: 0

| Phase | Requirements | Count |
|-------|--------------|-------|
| 16. Early Docs Publication | PUB-05, PUB-06 | 2 |
| 17. CI, Build & Test Hygiene | CI-11, CI-12, CI-13, CI-14, CI-15, CI-16, CI-17, BLD-14, DEBT-06, DEBT-07, DEBT-08 | 11 |
| 18. Writer Correctness | JSONP-09, WRT-10, WRT-11, WRT-12 | 4 |
| 19. Nullable Enum & Tuple Mapping | COLS-08, COLS-09, COLS-10, COLS-11, DEBT-09 | 5 |
| 20. Release & Publication | ROLL-02, PUB-07, SUITE-05, PUB-08 | 4 |

---
*Requirements defined: 2026-10-10*
*Last updated: 2026-10-10 after roadmap creation (Phases 16-20, 26/26 mapped)*
