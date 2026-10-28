# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)
- ✅ **v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene** — Phases 16-20 (shipped 2026-10-14)
- ✅ **v1.7.0_gg3-hardening Record-Data Exposure & Review Debt** — Phases 21-24 (shipped 2026-10-20)
- ✅ **v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt** — Phases 25-29 (shipped 2026-10-23)
- ✅ **v1.7.0_gg5-ci CI & Release Workflow Modernization** — Phases 30-32 (shipped 2026-10-27)

## Phases

<details>
<summary>✅ v1.2.9_gg4-tests Fork Test Coverage (Phases 1-5) — SHIPPED 2026-09-18</summary>

- [x] Phase 1: Test Infrastructure & Green Baseline (3/3 plans) — completed 2026-09-16
- [x] Phase 2: Fork SMT Coverage (5/5 plans) — completed 2026-09-16
- [x] Phase 3: Table Metadata & JSON/Dynamic Types (3/3 plans) — completed 2026-09-17
- [x] Phase 4: JSON Insert Path & Connector Plumbing (3/3 plans) — completed 2026-09-18
- [x] Phase 5: CI & Final Verification (2/2 plans) — completed 2026-09-18

Full phase details: [`milestones/v1.2.9_gg4-tests-ROADMAP.md`](milestones/v1.2.9_gg4-tests-ROADMAP.md)
Requirements as shipped: [`milestones/v1.2.9_gg4-tests-REQUIREMENTS.md`](milestones/v1.2.9_gg4-tests-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.2.9_gg4-tests-phases/`

</details>

<details>
<summary>✅ v1.6.0_gg1-sync Upstream v1.6.0 Sync (Phases 6-11) — SHIPPED 2026-10-07</summary>

- [x] Phase 6: Merge Groundwork & Build Migration (3/3 plans) — completed 2026-09-18
- [x] Phase 7: Type Mapping, Writer, Helper Client & Config (4/4 plans) — completed 2026-09-19
- [x] Phase 8: Orchestration & JMX Re-plumbing (3/3 plans) — completed 2026-09-19
- [x] Phase 9: Metadata-free JSON Path — Keep or Drop (3/3 plans) — completed 2026-09-20
- [x] Phase 10: Test Harness Reconciliation (3/3 plans) — completed 2026-09-20
- [x] Phase 11: Full Suite, CI & Publication (8/8 plans) — completed 2026-10-07

Full phase details: [`milestones/v1.6.0_gg1-sync-ROADMAP.md`](milestones/v1.6.0_gg1-sync-ROADMAP.md)
Requirements as shipped: [`milestones/v1.6.0_gg1-sync-REQUIREMENTS.md`](milestones/v1.6.0_gg1-sync-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.6.0_gg1-sync-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening (Phases 12-15) — SHIPPED 2026-10-10</summary>

- [x] Phase 12: Upstream v1.7.0 Merge & Reconcile (5/5 plans) — completed 2026-10-08
- [x] Phase 13: Fork Hardening on the Merged Tree (8/8 plans) — completed 2026-10-08
- [x] Phase 14: Dependency Bumps & CI (4/4 plans) — completed 2026-10-10
- [x] Phase 15: Suite Gate, Publication & Rollout (8/8 plans) — completed 2026-10-10

Full phase details: [`milestones/v1.7.0_gg1-sync-ROADMAP.md`](milestones/v1.7.0_gg1-sync-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg1-sync-REQUIREMENTS.md`](milestones/v1.7.0_gg1-sync-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.7.0_gg1-sync-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene (Phases 16-20) — SHIPPED 2026-10-14</summary>

- [x] Phase 16: Early Docs Publication (3/3 plans) — completed 2026-10-11
- [x] Phase 17: CI, Build & Test Hygiene (4/4 plans) — completed 2026-10-12
- [x] Phase 18: Writer Correctness (4/4 plans) — completed 2026-10-12
- [x] Phase 19: Nullable Enum & Tuple Mapping (10/10 plans) — completed 2026-10-13
- [x] Phase 20: Release & Publication (8/8 plans) — completed 2026-10-14

Full phase details: [`milestones/v1.7.0_gg2-hardening-ROADMAP.md`](milestones/v1.7.0_gg2-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg2-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg2-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg2-hardening-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg3-hardening Record-Data Exposure & Review Debt (Phases 21-24) — SHIPPED 2026-10-20</summary>

- [x] Phase 21: Dependabot Integration & CI Hygiene (5/5 plans) — completed 2026-10-19
- [x] Phase 22: Writer Record-Data Exposure (5/5 plans) — completed 2026-10-20
- [x] Phase 23: Declared-Nullability Hardening & Review Closure (6/6 plans) — completed 2026-10-20
- [x] Phase 24: Release & Publication (7/7 plans) — completed 2026-10-20
- [x] Quick 261020-vcr: rollout-note Round 3 fixes, review rounds 4-6, republished as `6a2a87a5` — completed 2026-10-20

Full phase details: [`milestones/v1.7.0_gg3-hardening-ROADMAP.md`](milestones/v1.7.0_gg3-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg3-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg3-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg3-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg3-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg3-hardening-phases/` · Quick tasks: `milestones/v1.7.0_gg3-hardening-quick/`

</details>

<details>
<summary>✅ v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt (Phases 25-29) — SHIPPED 2026-10-23</summary>

- [x] Phase 25: Dependency Absorption & Upstream Mini-Sync (8/8 plans) — completed 2026-10-21
- [x] Phase 26: Size-Named Tuple Elements in Discovery (5/5 plans) — completed 2026-10-22
- [x] Phase 27: Writer Failure Path & Test Debt (5/5 plans) — completed 2026-10-23
- [x] Phase 28: Raw Identifier Names in Discovery (6/6 plans) — completed 2026-10-23
- [x] Phase 29: Release & Publication (8/8 plans) — completed 2026-10-24

Full phase details: [`milestones/v1.7.0_gg4-hardening-ROADMAP.md`](milestones/v1.7.0_gg4-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg4-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg4-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg4-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg4-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg4-hardening-phases/` · Research: `milestones/v1.7.0_gg4-hardening-research/`

</details>

<details>
<summary>✅ v1.7.0_gg5-ci CI & Release Workflow Modernization (Phases 30-32) — SHIPPED 2026-10-27</summary>

- [x] Phase 30: Release Workflows & Pin Guard (3/3 plans) — completed 2026-10-26
- [x] Phase 31: Fork-Owned Nightly Integration Tests (6/6 plans) — completed 2026-10-26
- [x] Phase 32: Release & Publication (7/7 plans) — completed 2026-10-27

Full phase details: [`milestones/v1.7.0_gg5-ci-ROADMAP.md`](milestones/v1.7.0_gg5-ci-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg5-ci-REQUIREMENTS.md`](milestones/v1.7.0_gg5-ci-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg5-ci-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg5-ci-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg5-ci-phases/`

</details>

## Sync Mechanics (applies to every upstream sync)

- **Scratch branch, not `custom_build`.** All merge and hardening work happens on a scratch branch, with the pre-sync tip tagged `pre-vX.Y.Z-sync`. `custom_build` is published and protected. It moves forward only by a non-force `--no-ff` merge after human approval, which keeps `git revert -m 1` viable.
- **Upstream ref is the tag.** Never use bare `upstream/main`, because a stale `refs/upstream/main` makes it ambiguous.
- **Commit cadence.** No `.planning/` commit is interleaved with in-progress conflict resolution. No dependency bump rides inside the sync commit.
- **Gradle invocation.** Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. This shell sets the variable to the literal string `"null"`, which breaks Ryuk. Docker is colima (profile `vz`).
- **Gates.** `testUnit` (Docker-free) is the cheap gate. Any two-run gate busts Gradle's cache with `--rerun`. If the full suite goes red on unchanged code, check the `latest` image date first.
- **Pushes.** Pushing any branch triggers `tests.yaml`; a tag push does not (CI-08). Two branches pushed at one SHA still cancel each other.
- **Editing this file by script.** Slice to the target `### Phase N:` section before any find-and-replace. An unanchored replace truncated this roadmap once (`c1480f8f`).

## Backlog

### Phase 999.1: Column.extractColumn drops Nullable for Enum8/Enum16 and Tuple (BACKLOG — scheduled into Phase 19)

**Goal:** Nullable(Enum8/16) values are stored as NULL or shifted (RESEARCH Spike b2, Pitfall 8); multi-field Nullable(Tuple) fails ATTEMPT_TO_READ_AFTER_EOF and single-field Nullable(Tuple) array elements now throw under D-16 (Spike b3); found 2026-10-08, Phase 13 research, deliberately not fixed in Phase 13
**Scheduled:** 2026-10-10 into milestone `v1.7.0_gg2-hardening` as Phase 19 (COLS-08..COLS-11, DEBT-09). The Enum half ships unconditionally. The Tuple half is a go/no-go after a spike. On no-go, COLS-10 updates this entry with the remaining Known cost.
**Requirements:** mapped under Phase 19
**Plans:** planned under Phase 19
**Delivered:** Phase 19 (go), shipped in `v1.7.0_gg2` on 2026-10-14 — `Nullable(Enum8|Enum16)` and `Nullable(Tuple)` round-trip; the discovery WARN is deleted

### Phase 999.2: Test classpath carries org.lz4:lz4-java 1.8.0 beside at.yawk.lz4 (BACKLOG — scheduled into Phase 17)

**Goal:** `testRuntimeClasspath` selects `org.lz4:lz4-java:1.8.0` (via the Confluent 7.9.1 test stack) beside `at.yawk.lz4:lz4-java`, although upstream's capability rule redirects it on `runtimeClasspath` and `integrationTestRuntimeClasspath`; it is test-only and not shipped; it is affected by the 2026-10-07 lz4 advisories, with no fix on that coordinate; fixing it needs a `build.gradle.kts` change outside Phase 14's scope; found 2026-10-09, Phase 14 research
**Scheduled:** 2026-10-10 into milestone `v1.7.0_gg2-hardening` as Phase 17 (DEBT-07)
**Requirements:** mapped under Phase 17
**Plans:** planned under Phase 17
**Delivered:** Phase 17 (DEBT-07), shipped in `v1.7.0_gg2` on 2026-10-14 — excluded from `testRuntimeClasspath` only, pinned by `TestRuntimeClasspathLz4UnitTest`

### Phase 999.3: Rethrown write-failure messages can carry record values (BACKLOG — scheduled into Phase 22)

**Goal:** `ClickHouseWriter.doWriteColValue`'s generic catch rethrows the original exception (kept by Phase 18 D-09 so classification cannot change). For some failures its message embeds the record value (`UUID.fromString`: `Invalid UUID string: <value>`; number parsing: `For input string: ...`), which reaches the Connect task status, the DLQ headers and `doInsertRawBinary`'s two `Error inserting records` ERROR logs. Fixing it means changing the rethrown type or message without changing retriable classification. Found 2026-10-12 in the Phase 18 discussion (D-09)
**Scheduled:** 2026-10-16 into milestone `v1.7.0_gg3-hardening` as Phase 22 (WRT-13, WRT-14, WRT-15). The classification pin (WRT-14) lands and is green on the unchanged writer before the change. WRT-15 marks this entry delivered in the same commit as the writer code.
**Requirements:** mapped under Phase 22
**Plans:** planned under Phase 22
**Delivered:** Phase 22 (WRT-13..WRT-15), shipped in `v1.7.0_gg3` on 2026-10-20: a value-conversion failure names the column, the ClickHouse type and the exception class without the record value; retry classification is pinned by WRT-14

### Phase 999.4: Discovery drops a Tuple element named size or size0 (BACKLOG — scheduled into Phase 26)

**Goal:** `ClickHouseHelperClient.shouldSkipDescribeRow` drops every DESCRIBE row that ends in `.size`, `.size0` or `.null`, so a Tuple element named `size` or `size0` never becomes a Column, and the writer, which iterates `getTupleFields()`, writes a short tuple. Measured on `latest` 26.9.19.9 with client V2 (23-EVIDENCE.md R-01): the insert fails with code 33 `CANNOT_READ_ALL_DATA` and no row is stored, and a silently wrong stored value is reachable when the bytes of the shorter write happen to parse as a longer tuple. Severity: high for an affected table, since the inserts that were run into such tables failed (four shapes, code 32 or 33, `24-COMMAND-EVIDENCE.md` E-15). The server rejects a Tuple element named `null` (code 36), so only `size` and `size0` are affected. Since COLS-14 the unpaired-element WARN names the dropped element at every DESCRIBE. Fix sketch: keep a row that is a declared element of its parent Tuple while still dropping the structural rows `c.name.size`, `d.null`, `d.other.size`, `e.size0` and `e.v.size`. Parent-is-a-Tuple alone is not enough for `Array(Tuple)`, where `e.size0` is the array's offsets row. The fix touches upstream-region `ClickHouseHelperClient`, so it gets its own phase, with a FORK-DELTA row, an anchors row, a live round-trip test and the DESC-05 regression check. It flips the D-04 pin `COLS-14 unpaired-element WARN (D-04, known defect 999.4)`: the element then pairs and no WARN fires. Found 2026-10-20 in the Phase 23 discussion (D-03)
**Scheduled:** 2026-10-21 into milestone `v1.7.0_gg4-hardening` as Phase 26 (DESC-06..DESC-10). The measured defect is wider than this entry's title. `Table.updateParent`'s `SIZE_FIELD_MATCHER` is a second drop site, for `size<digits>` under an Array parent, and the user chose on 2026-10-21 to fix both sites. The live DESCRIBE spike (DESC-06) and the behaviour-preserving extraction of the describe loop (DESC-07) land before the fix. DESC-10 marks this entry delivered in the same commit as the fix.
**Requirements:** mapped under Phase 26
**Plans:** planned under Phase 26
**Delivered:** Phase 26 (DESC-06..DESC-10) on sync/v1.7.0, shipped in v1.7.0_gg4 on 2026-10-23: discovery keeps a declared Tuple element named size, size0 or size<digits> at both drop sites (DescribeTableBuilder and Table.updateParent); rows stored by earlier releases are not repaired

### Phase 999.5: Backslashes in column and element names come out of fromJsonRow doubled (BACKLOG — scheduled into Phase 28)

**Goal:** `ClickHouseFieldDescriptor.fromJsonRow` doubles every backslash before Jackson parses, so a server name with a backslash comes out doubled (`c.a\\nb`), and a Kafka field must use the doubled form for the writer to match it (23-EVIDENCE.md R-02). The D-12 live premise uses the doubled field name. The behaviour is pre-existing, and Phase 23 changes no code or test for it. Fix sketch: decide the intended name form, decode name escapes in upstream-region `fromJsonRow`, add a live round-trip pin for both forms and a rollout note, since operators' field names may depend on the doubled form. Found 2026-10-20 in Phase 23 research (D-20)
**Scheduled:** 2026-10-21 into milestone `v1.7.0_gg4-hardening` as Phase 28 (NAME-01..NAME-07). The canonical form was decided by the user on 2026-10-21: the raw identifier as written in `CREATE TABLE`, with no dual-form alias, and operators rename. The escaped-quote `normalise` pin (NAME-01) lands on the current code first. NAME-07 marks this entry delivered in the same commit as the fix.
**Requirements:** mapped under Phase 28
**Plans:** planned under Phase 28
**Delivered:** Phase 28 (NAME-01..NAME-07) on sync/v1.7.0, shipped in v1.7.0_gg4 on 2026-10-23: names and Enum labels are matched under the raw identifier as written in CREATE TABLE on both clients; a record using the earlier escaped form stores the DEFAULT at the root and fails for a Tuple element; rows stored by earlier releases are not repaired
---
*Roadmap reorganized at the `v1.7.0_gg5-ci` close: 2026-10-28.*
