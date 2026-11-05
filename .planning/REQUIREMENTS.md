# Requirements: ClickHouse Kafka Connect `custom_build` Fork

**Defined:** 2026-11-04
**Milestone:** v1.7.0_gg8 closeout — Gap Closure, Upstream Tail & Release
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

## v1 Requirements

Requirements for this milestone. Each maps to a roadmap phase.

### Upstream Tail (UPS)

- [x] **UPS-01**: A rollback tag `pre-v1.7.0_gg8` and a scratch branch exist, and upstream `83808df9` is merged in one mechanical commit with a generated per-path merge checklist.
- [x] **UPS-02**: Upstream #857 is merged value-free. What it puts in the record context is measured first. No error message, DLQ header or log line carries a record value, and the gg3 column/type/class-only pins still pass unchanged. Amendments (2026-11-04): the plan-check answers in `.planning/phases/45-upstream-tail-merge-dependency-check/45-CONTEXT.md` (log line content, every Jackson chain withheld, the named pin edits), recorded in `docs/FORK-DELTA.md` by plan 45-04.
- [x] **UPS-03**: Slash column names in DESCRIBE (#839) round-trip on ClickHouse `latest` × client V2, checked against the fork's raw-identifier matching (gg4).
- [x] **UPS-04**: Upstream's `fromJsonRow` fix is reconciled with the fork's Phase 28 raw-identifier handling (backlog 999.5, delivered in gg4). The gg4 NAME-01..NAME-07 pins stay green unchanged, and any behavioural difference from upstream is pinned.
- [x] **UPS-05**: Upstream's slf4j-simple 2.0.20, spotless 8.10.4 and commons-lang3 3.21.0 bumps are taken, with `verify-merge-invariants.sh` at `PASS` and its anchor count updated.

### Dependencies (DEP)

- [x] **DEP-01**: A manual dependency and CVE check is recorded, because Dependabot alerts are disabled and no PRs are open. Any dependency with an open CVE is listed. Nothing is upgraded without the user's approval.
- [x] **DEP-02**: Every approved bump lands in its own commit, with the suite green on ClickHouse `latest` × client V2 and `testUnit` green with Docker down.

### Task Lifecycle (LIF)

- [ ] **LIF-01**: `ClickHouseSinkTask.stop()` stops every delivery strategy and still reaches the later cleanup when one throws (`try`/`finally`), pinned red-first and Docker-free.
- [ ] **LIF-02**: The CHANGELOG `stop()` bullet and its Known Issues bullet match the fixed behaviour.

### Insert Timeout and Limits (TMO)

- [x] **TMO-01**: The `clickhouseClientInsertTimeoutMs` help text says what the setting really bounds, and a Docker-free test pins the text against the measured behaviour.
- [x] **TMO-02**: An insert that streams progress headers is bounded, or its escape is re-measured against a real server and kept as a recorded limit with the number.
- [x] **TMO-03**: Write-side stalls, client V1 `future.get()` inserts and non-insert requests are each bounded with a red-first pin, or recorded with a measurement showing why they cannot be.

### Retry Classification (RTY)

- [ ] **RTY-01**: A retriable `DataException` case has a retry budget, or the missing budget is pinned and documented as a limit through `ProxySinkTask.put` under both `errors.tolerance` values.
- [ ] **RTY-02**: FUT-05 is closed. The start-up WARN is kept, and a validator is added only if the user approves it. The decision is recorded with its reason.

### Phase 37 Dispositions (P37)

- [ ] **P37-01**: The four `src/main` advisory rows (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01) and the one null assertion are each fixed in their own commit with a test, or closed with a written reason.
- [ ] **P37-02**: The 28 IN-05(b) items each have a fix or a named disposition in the ledger, and none stays "owner: user".
- [ ] **P37-03**: The broad allow-list glob rows (UF-37-3) are narrowed or justified, and the allow-list dry run reads `0 unallowed, 0 stale rows`.
- [ ] **P37-04**: The review Infos each have a disposition.

### Documents (DOC)

- [ ] **DOC-01**: IN-03 is fixed: the planning identifier in the published gg6 rollout note is replaced with a public reference.
- [ ] **DOC-02**: FORK-DELTA evidence paths point at their `.planning/milestones/` locations and the test path missing its package is corrected. Item 20 names its ledger by a path that survives archiving.

### Release (RLS)

- [ ] **RLS-01**: `VERSION=v1.7.0_gg8` is set through `bump_version.py`. CHANGELOG, FORK-DELTA and `fork-anchors.txt` are updated in the same commits as the counts they change.
- [ ] **RLS-02**: `confluent-archive-allowlist-v1.7.0_gg8.txt` is generated from the final tree and the dry run reads `0 unallowed, 0 stale rows`.
- [ ] **RLS-03**: `docs/ROLLOUT-v1.7.0_gg8.md` has every effect labelled with its evidence and has been through fresh-context review rounds.
- [ ] **RLS-04**: Two full suites on ClickHouse `latest` × client V2 are equal and green. `testUnit` is green with Docker unreachable.
- [ ] **RLS-05**: The `--no-ff` merge is published after the user's explicit approval. One `tests.yaml` run on the published SHA is green, and the tag and release are created by the publish run.
- [ ] **RLS-06**: `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` are deleted from `origin` after a separate confirmation before the push.

## Future Requirements

- Upstream PRs #861, #855, #854 and #788, once they merge upstream.
- Many-to-one rename and partition-changing SMTs for exactly-once (design limit).

## Out of Scope

| Feature | Reason |
|---------|--------|
| New connector features | This milestone only closes gaps and syncs upstream |
| Running the upgrade on production connectors | Operator action, outside the repository |
| `NIGHTLY_ENABLED`, Dependabot settings | They stay with the user |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| UPS-01 | Phase 45 | Complete |
| UPS-02 | Phase 45 | Complete |
| UPS-03 | Phase 45 | Complete |
| UPS-04 | Phase 45 | Complete |
| UPS-05 | Phase 45 | Complete |
| DEP-01 | Phase 45 | Complete |
| DEP-02 | Phase 45 | Complete |
| LIF-01 | Phase 47 | Pending |
| LIF-02 | Phase 47 | Pending |
| TMO-01 | Phase 46 | Complete |
| TMO-02 | Phase 46 | Complete |
| TMO-03 | Phase 46 | Complete |
| RTY-01 | Phase 47 | Pending |
| RTY-02 | Phase 47 | Pending |
| P37-01 | Phase 48 | Pending |
| P37-02 | Phase 48 | Pending |
| P37-03 | Phase 48 | Pending |
| P37-04 | Phase 48 | Pending |
| DOC-01 | Phase 48 | Pending |
| DOC-02 | Phase 48 | Pending |
| RLS-01 | Phase 49 | Pending |
| RLS-02 | Phase 49 | Pending |
| RLS-03 | Phase 49 | Pending |
| RLS-04 | Phase 49 | Pending |
| RLS-05 | Phase 49 | Pending |
| RLS-06 | Phase 49 | Pending |

**Coverage:**
- v1 requirements: 26 total
- Mapped to phases: 26
- Unmapped: 0 ✓

---
*Requirements defined: 2026-11-04*
*Last updated: 2026-11-04 after roadmap creation (Phases 45-49)*
