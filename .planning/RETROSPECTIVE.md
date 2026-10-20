# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.2.9_gg4-tests — Fork Test Coverage

**Shipped:** 2026-09-18
**Phases:** 5 | **Plans:** 16 | **Tasks:** 45 | **Commits:** 110 | **Timeline:** 4 days (2026-09-15 → 2026-09-18)

### What Was Built

- A test harness that made the suite usable: one shared ClickHouse `latest` container per test JVM fork (down from one per class), a fresh database per test class, an `@AfterEach` MBean leak sweep, UTF-8 test helpers, and a Docker-free `testUnit` task kept out of `check`.
- Full unit coverage for all four fork SMTs — `TableOperationFilter` 9→26 tests, `ConvertToMapOrJson` 13→37, `HeadersToValue` 2→25, `HeaderToValue` 2→24 — plus one sink-level landing test per SMT proving records reach ClickHouse through the real insert path.
- Coverage from zero for `targetTableFilter`, `describeTable` sub-column skipping, `JSON`/`JSON(...)`/`Dynamic` type mapping and round-trips, `jsonInsertColumns`, JMX MBean naming, and `async_insert` pass-through.
- Seven production bug fixes, each in the phase whose tests exposed it; suite grew 187 → 371 tests, all green.
- CI rewritten to a single `latest` × V2 job on current action tags, publishing JaCoCo coverage to the job summary via a checked-in stdlib-only renderer, verified green on the pushed SHA.

### What Worked

- **Writing the test before deciding whether there was a bug.** The "fixed if broken" convention in REQUIREMENTS.md meant every fix started from a RED test proving the defect, not from a code reading. All seven fixes landed with a failing-then-passing test attached. The D-11 non-ASCII describe-reader bug, deferred from Phase 1, was reproduced by a Phase 3 test before the fix was written.
- **Bug fixes living in the phase that found them.** No separate bug-fix phase meant no queue of findings going stale. Phase 2 found four SMT bugs and fixed four SMT bugs.
- **Blocking-human checkpoints on out-of-scope production changes.** Fired three times and was right all three: the `ClickHouseWriter` MAP-branch fix (approved), the R-01 silent data loss (deliberately pinned, not fixed), and the one-way public push (approved after a credential scan of the actual delta). An autonomous agent would have "fixed" R-01 and broken production pipelines.
- **Parallel independent test tracks.** Phases 2, 3, and 4 depended only on Phase 1 and not on each other, so file ownership could be assigned per-plan and waves ran genuinely in parallel (Phase 4 ran all three plans at once; only 04-03 touched `src/main`).
- **Typed read-back over row counts (D-14).** Directly caught what a `countRows` assertion would have missed — a `JSON` column holding the wrong nested value, a `Dynamic` column holding `42` as a `String`.

### What Was Inefficient

- **The `TESTCONTAINERS_HOST_OVERRIDE="null"` environment bug cost a whole phase's worth of confusion.** Phase 1's "baseline" was not actually red — the shell was breaking Ryuk. It was only diagnosed as an environment artifact after real investigation. Setting `build_command`/`test_command` in `config.json` fixed it permanently, but that happened in Phase 3, two phases late.
- **`workflow.test_command` and `test_gate_timeout` unset until Phase 3.** Until then the post-merge and regression gates ran `true` — they were passing vacuously. Every phase gate before Phase 3 was weaker than it appeared.
- **Escape-literal handling in the editing toolchain.** Phase 2 lost time to `\uXXXX` escape text being silently decoded into real non-ASCII bytes on write. The workaround (build the string from `int[]` code points at runtime) is now the established pattern, but it was discovered by debugging a mysterious test failure.
- **A stale per-plan commit ledger from a superseded milestone** (`.git/gsd-plan-head-before-02-03`) would have attributed 48 unrelated commits to one plan. Caught by hand, not by tooling.
- **Verification staleness across sequential phases.** Phases 1–4 all ended `passed`, then all read stale at milestone close because later phases legitimately edited files those verifications covered. Benign, but it forced an `override_closeout` on a milestone that was in substance fully verified.

### Patterns Established

- **`ClickHouseBase` contract:** shared container per fork, per-class database, inherited `@Tag("clickhouse")`, MBean sweep, `uniqueConnectorName(TestInfo)`. New sink-task tests must create their tables before `start()` — `ClickHouseWriter.start()` fails against an empty database.
- **The stale-schema-cache guard shape.** `HeaderToValue` and `HeadersToValue` had the identical lazily-cached `valueSchema` bug. The fix (a `sourceSchema` field plus reference comparison) was written once and then applied verbatim to the second class. Treat it as a template if a third SMT caches a derived schema.
- **Snapshot client-v2 `GenericRecord` rows during iteration.** It is a mutable, reused row view; collecting references into a list yields N copies of the last row. Documented as a Javadoc on the `allRows` helper after a full false-positive pass/fail cycle.
- **A checked-in stdlib-only script beats a marketplace action** for anything a repo can compute itself. `jacoco_summary.py` imports `os`, `sys`, and `xml.etree.ElementTree` — no third-party supply-chain surface for markdown formatting, and it writes `GITHUB_STEP_SUMMARY` directly rather than through a `tee` pipeline that would hide failures under `bash -e` without `pipefail`.
- **Attribute a CI run by the pushed commit's own `head_sha` + workflow `path`, asserting exactly one match.** "Most recent run on the branch" can credit an unrelated push's green result to your work.

### Key Lessons

1. **Verify the environment before believing a red baseline.** A broken `TESTCONTAINERS_HOST_OVERRIDE` looked exactly like a broken test suite. Check the harness before concluding anything about the code.
2. **Set the real `test_command` in `config.json` on day one.** A gate running `true` is worse than no gate — it reports success. Two phases shipped behind vacuous gates before this was noticed.
3. **Pinning a bug is a legitimate outcome, and sometimes the right one.** R-01 is real silent data loss, and fixing it would break every pipeline running explicit JSON behind a Struct converter. The labelled test makes the loss suite-detectable so a rebase cannot change it unnoticed — that was worth more than a fix nobody could deploy.
4. **Coverage work finds bugs in proportion to how specifically you assert.** The seven fixes came from typed read-back, non-ASCII fixtures, and repeated-`configure()` cases — never from a row count or a smoke test.
5. **"Stale" verification is structural in sequential phases, not a quality signal.** Any phase whose files a later phase edits will read stale at close. Either re-verify at close or expect an `override_closeout`; don't read it as a failure.

### Quality Observations

- Code review ran on all five phases: **0 critical findings across the milestone**, 13 warnings, 8 info. Every warning was triaged; nine became tracked follow-ups, none blocked a phase.
- Final suite: 371 tests, 0 failures, 0 errors, 7 pre-existing skips — green locally and in CI (run `35317272594`).
- `src/main` changes across the whole milestone: seven targeted bug fixes plus one behavior-preserving `buildMBeanName` extraction. The rebasability constraint held.
- One caveat on that constraint: `.planning/` is tracked in this repo (`commit_docs: true`), so an upstream sync will carry 80+ planning files through the merge.

---

## Milestone: v1.6.0_gg1-sync — Upstream v1.6.0 Sync

**Shipped:** 2026-10-07
**Phases:** 6 | **Plans:** 24 | **Tasks:** 63 | **Commits:** 170 first-parent (751 with the merged upstream history) | **Timeline:** 20 days (2026-09-18 → 2026-10-07)

### What Was Built

- A 581-commit upstream merge (`v1.2.9` → `v1.6.0`) landed as one purely mechanical commit. All 40 conflicted paths were byte-identical to upstream, checked by a committed, self-testing invariant script, with a generated per-path `MERGE-CHECKLIST.md` as the gate for every later phase.
- Upstream's version-catalog build (Gradle 9.6.1, Testcontainers 2.0.5, `testFixtures`) adopted wholesale, with the fork's six build additions re-applied on top.
- Every fork behaviour re-applied by intent onto upstream's rewritten code: `Type.DYNAMIC` and its fail-fast, `targetTableFilter`, the describe sub-column skip, JMX `connector=<name>` (re-plumbed into upstream's new naming owner), the metadata-free JSON path (KEEP), `jsonInsertColumns`, and the shared-container test harness.
- Suite at 663 tests on ClickHouse `latest` (26.9.12.8) × V2, 0 failures, 18 named skips. CI green on the published SHA. `custom_build` published as a non-force `--no-ff` merge tagged `v1.6.0_gg1`, with a rollback tag.
- `docs/FORK-DELTA.md`: a grep-confirmed ledger of what the fork still patches and what it retired.

### What Worked

- **The previous milestone's suite paid for itself.** It was written to make a rebase safe, and it was the acceptance gate here. It caught fork behaviours that disappeared with no conflict marker and no compile error — the risk the research had flagged as six silent vanishings, which turned out to be seven.
- **Mechanical merge first, intent second.** A byte-identity invariant on commit 1 turned each re-applied fork behaviour into its own small, reviewable diff against upstream. Without it, those judgement calls would have been buried in a 500-file merge commit.
- **Re-plumbing onto upstream's shape instead of restoring the fork's.** JMX naming went into `SinkTaskStatistics`, the harness contract was re-derived over `@TestInstance(PER_CLASS)`, and `targetTableFilter` went onto `showTables*`. Each choice shrinks the conflict surface of the next sync.
- **Blocking-human checkpoints on irreversible or contested calls.** Three fired: the JSON path KEEP/DROP decision (made on evidence from the merged code), the one-way publication, and the D-17 fix-before-publish. The executor also refused, correctly, to act on an approval relayed by another agent.
- **Rehearsing CI on a scratch branch before publishing.** Run `37617508949` proved the workflow end-to-end before anything touched `custom_build`.

### What Was Inefficient

- **A silent default reversion went unseen for four phases.** The merge reverted `CLICKHOUSE_VERSION_DEFAULT` to `25.8.18.1`, and CI pinned the version in its job environment, so local and CI both looked green. Phases 8 and 9 closed against a server the milestone does not claim to test. Phase 10 found it, and Phase 11 removed the CI pin that masked it.
- **The mechanical merge dropped more than the plan expected.** Twelve fork test files lost their `ClickHouseBase`/`ClickHouseTestHelpers` dependencies, which surfaced as a compile gap mid-Phase 7. The planned carve-out was four items.
- **Server drift arrived during the milestone.** ClickHouse `latest` moved to 26.9 with a ZSTD default that client `0.9.6` cannot decode. That added a gap-closure plan (11-08) to the publishing phase. Testing against a floating tag means accepting this.
- **The publication gate needed manual orchestration.** The tag push cancelled the branch run on the same SHA, CI-06's locator had to be narrowed after the fact, and the approval had to be re-confirmed at UAT.
- **Verification staleness again.** Phases 6–10 all read stale at close, for the same structural reason as last milestone, so this was a second `override_closeout`.

### Patterns Established

- **Upstream sync recipe:** permanent `upstream` remote → rollback tag → scratch branch → one mechanical merge commit with a byte-identity invariant → generated per-path checklist → re-apply by intent, one bisectable commit per behaviour → full suite on `latest` → scratch CI rehearsal → human-approved non-force `--no-ff` merge.
- **Pin defaults Docker-free.** `ClickHouseVersionDefaultUnitTest` and `NewUpstreamConfigDefaultsUnitTest` catch silent default changes in `testUnit`, without a server.
- **No version pins in CI's job environment.** CI should run the code defaults, so the unit test pinning them is the single source of truth.
- **Two-run acceptance gates must bust Gradle's cache.** Use `--rerun`, or the second run just replays the first one's results.
- **Recorded Facts beside the checklist.** Free wins, triage outcomes and accepted costs live as numbered facts in `MERGE-CHECKLIST.md`, citing commands re-run at execution time.

### Key Lessons

1. **A merge's most dangerous changes are the ones that compile.** Seven fork behaviours vanished with no marker and no error. Only a suite asserting on them, run against the right server, caught them.
2. **Check what the gate is actually testing against.** A green run on the wrong ClickHouse version is worse than a red one. Pin the default with a test, and keep CI from overriding it.
3. **Prefer upstream's shape even when restoring the fork's is easier.** Every re-plumb onto upstream's owner is one less conflict next time. Where the fork had to diverge, writing it down in `FORK-DELTA.md` is what makes the next sync tractable.
4. **A floating `latest` tag means server drift can land mid-milestone.** Budget a gap-closure slot in the final phase.
5. **One-way publication needs its own CI hygiene.** Branch and tag pushes at one SHA must not share a cancel-in-progress concurrency group, or "exactly one green run" stops being checkable.

### Quality Observations

- Code review ran on all six phases: **4 critical findings, all fixed**. They were the fail-open invariant script (Phase 6), two in Phase 7, and the `auto.evolve` guard gap (Phase 9). Warnings and info findings were triaged, and 9 new follow-ups were carried into PROJECT.md → Active.
- Security audits completed for all six phases, with 0 open threats at close.
- Final suite: 663 tests, 0 failures, 0 errors, 18 named skips, green locally and in CI (run `37619434848`).
- `src/main` changes beyond re-applying fork behaviour: one, the D-17 `lz4` default. The fork delta shrank by files (93→84 wide, 32→30 `src/main`) while insertions grew (8,401→9,476), mostly tests.

---

## Milestone: v1.7.0_gg1-sync — Upstream v1.7.0 Sync & Follow-up Hardening

**Shipped:** 2026-10-10
**Phases:** 4 | **Plans:** 25 | **Tasks:** 55 | **Commits:** 153 first-parent (175 with the merged upstream history) | **Timeline:** 4 days (2026-10-07 → 2026-10-10)

### What Was Built

- Upstream `v1.7.0` was merged as one mechanical commit (`54e63648`, zero authored lines), behind an invariant script now parameterised by `PRE_TAG`/`UPSTREAM_REF` and a 39-row fork-anchor grep. A per-hunk checklist covers the auto-merged hunks in fork-touched files.
- Three silent merge hazards were resolved by intent:
  - D-17 is retired onto upstream's `setCompressionMethod`.
  - Upstream #820's null-Map bytes are reachable, because the fork's MAP guard is now scoped to `Nullable`.
  - The build has a single LZ4 rule.

  `KeeperStateProvider`'s new state read is proven by a restart test.
- R-01's silent data loss is fixed by routing through a new `MetadataFreeJsonWriter`, which a live twin-table matrix proves at parity with RowBinary. Debezium `Timestamp`/`NanoTimestamp` are fixed on that route too.
- RowBinary nested-null rules: values round-trip, coerce to empty, or fail loudly with a data-free `DataException`. Plus 13 further metadata, config and test-debt fixes.
- Five dependency bumps, each in its own commit. lz4-java went to `1.12.0`, past the advisory floor. CI runs once per SHA, compiles integration sources and reports its fork count.
- `v1.7.0_gg1` was published after two full runs (830 tests) and a CI rehearsal. It shipped with a CHANGELOG section, a rewritten `FORK-DELTA.md` and an eight-item operator rollout checklist.

### What Worked

- **The sync recipe repeated at a fraction of the cost.** It took 4 days against 20 for v1.6.0. The mechanical-merge invariant, rollback tag, scratch-branch rehearsal and human-gated `--no-ff` publication all carried over unchanged. Parameterising the invariant script (`PRE_TAG`, `UPSTREAM_REF`) is what made it reusable.
- **Merge-line decisions were placed in the merge phase.** CMP, WRT-06/09, BLD-09/13 and EOS-01 lived in Phase 12 rather than in hardening. The hazards no conflict marker reveals were fixed before anything was built on top of them: the shadowed MAP guard, the duplicate LZ4 rule and the D-17 pin beside upstream's flag.
- **Mutation checks made pins trustworthy.** The EOS-01 restart test was seen red against the old read, the WRT-07 pin was seen red under a mutation, and the parity matrix was seen red under a Timestamp-encoding mutation. A pin that has never failed proves little.
- **Parity tests instead of hand-written expectations.** The twin-table matrix compares the JSON route to RowBinary on a live server. That removed a whole class of "what should it store?" debates and found the Debezium `Timestamp` gap.
- **Behaviour ledger as the source for release docs.** Phase 13 recorded every operator-visible change as it landed (L-01..L-22). Phase 15 wrote the CHANGELOG, FORK-DELTA and rollout prose from the ledger, without reconstructing anything.
- **CI hygiene fixed before it was needed.** CI-08 (branch-only trigger) landed in Phase 14. Phase 15's publication then produced exactly one run on the published SHA without any locator narrowing.

### What Was Inefficient

- **Review found a safety defect in the release docs after publication.** 15-REVIEW WR-01 showed that the rollout doc's `TRUNCATE` example erases other connectors' exactly-once state. The fix (`505e4981`) landed after `f328fd21` and was left unpublished. Release docs were reviewed after the publish gate rather than before it.
- **Verification staleness, third time.** Phases 12–13 read stale at close for the same structural reason as before. This was a third `override_closeout`.
- **A known defect was surfaced but not fixed.** The `Column` nullability defect (999.1) was found in Phase 13 research. It makes the new nested-null guards throw on legal `Nullable(Enum)`/`Nullable(Tuple)` slots. It shipped as a documented cost with a discovery WARN rather than a fix.
- **Requirements were narrowed mid-flight.** TYPE-08 (Int256 only) and JSONP-08 (evaluation order only) needed dated corrections in REQUIREMENTS.md. The requirement wording ran ahead of what was safe to change.

### Patterns Established

- **Parameterised invariant script.** `verify-merge-invariants.sh` takes `PRE_TAG`/`UPSTREAM_REF` and self-tests every check, so the next sync reuses it unchanged.
- **Behaviour ledger.** Every operator-visible change is numbered as it lands. Release docs are written from the ledger, not from memory.
- **Twin-table parity tests** for any alternative write route, checked against the reference route on a live server.
- **Seen-red mutation check** for every pin that guards a fix.
- **One attributable commit per dependency bump**, each with its own gate. Live-path bumps (Jackson, org.json) each get a full live run.
- **Publish on one branch, then push tags.** Push tags only after the published SHA's own run is green, then re-check that SHA for extra runs.

### Key Lessons

1. **Review release docs before the publish gate, not after.** Operator instructions can destroy state just as code can. Give them the same pre-publication review as code.
2. **The second sync is cheap only if the first one left tools behind.** The invariant script, merge checklist, FORK-DELTA and sync mechanics made a 4-day sync possible. Keep them current at every close.
3. **Write requirements as outcomes, with explicit scope boundaries.** Two narrowings would have been avoided if TYPE-08 and JSONP-08 had named their safe scope up front.
4. **A fix that routes around a defect should name what it doesn't cover.** The empty-CDC skip and the Nullable Enum/Tuple gap were recorded as knowingly open, which kept the release honest. Keep doing that.

### Quality Observations

- Code review ran on all four phases with **0 critical findings**: 13 warnings and 23 info in total. One warning was fixed in the milestone (13-REVIEW WR-02, Debezium Timestamp). The 15-REVIEW warnings were fixed locally in `505e4981` but are unpublished. The rest are deferred with reasons in each `REVIEW-DISPOSITION.md`.
- Security audits passed for all four phases with 0 open threats at or above the block threshold. T-13-19 and T-14-16 stay open below it.
- Final suite: 830 tests, 0 failures, 0 errors, 18 named skips. It was green twice locally and once in CI on the published SHA (run `38050860929`).
- `src/main` changes were all within decided scope: the R-01 routing and new writer, the nested-null rules, Int256 fail-fast, the DESCRIBE depth fix, `extractColumn` argument order, the `USERNAME` default, the operand swap, the Debezium Timestamp fix and the Nullable Enum/Tuple WARN. The fork delta grew (84→95 files wide) because of these changes.

---

## Milestone: v1.7.0_gg2-hardening — Follow-up Hardening & CI Hygiene

**Shipped:** 2026-10-14 (closed 2026-10-16)
**Phases:** 5 | **Plans:** 29 | **Tasks:** 59 | **Commits:** 217 (no upstream sync) | **Timeline:** 6 days (2026-10-10 → 2026-10-15)

### What Was Built

- The corrected gg1 rollout doc was published first, as docs-only `--no-ff` merges `537beb1f` and `be6b8861`. The `TRUNCATE` advice on the shared KeeperMap state table is gone. The item 8 rollback was rewritten so that it works on the published tip.
- CI hardening. `tests.yaml` runs on `ubuntu-24.04` with SHA-pinned Node 24 actions, a read-only token, a Gradle-reported fork count and a Dependabot `github-actions` entry, and a Docker-free guard pins all of it. Phase 17 also excluded the duplicate test-classpath lz4, asserted the LZ4 response leg, silenced the Lombok warning and pinned the NanoTimestamp row.
- Writer correctness:
  - the empty-CDC skip covers all four routes
  - a missing non-nullable value throws a column-naming, data-free `DataException`
  - message-less failures no longer become NPEs
  - DateTime parse logs no longer carry the raw value
- Backlog 999.1 closed. `Nullable(Enum8|Enum16)` round-trips through a one-line fix. `Nullable(Tuple)` round-trips through `DeclaredNullability`, which reads nullability from the declared type string. A live spike decided it, and review gap closure covered `SimpleAggregateFunction`.
- `v1.7.0_gg2` was published with `docs/ROLLOUT-v1.7.0_gg2.md`, whose every quoted query was run live. A live `DedupTokenRetryLiveTest` corrected the gg1 note's deduplication claim. The tree was gated by two 988-test runs and a CI rehearsal.

### What Worked

- **Last milestone's lesson was applied.** "Review release docs before the publish gate" shaped Phase 16, and Phase 20 reviewed everything it committed before the release gate (20-05). Both rollout docs were reviewed before they reached `origin`. 16-REVIEW WR-01 found a rollback step that hit 97 conflicts on the published tip. It was fixed and republished at Phase 16 UAT.
- **Spike-first go/no-go that could not hold the release.** The Enum half shipped either way. The Tuple spike ran a live twin-table matrix before any production code, and its `go` was decided at a gate on measured evidence.
- **Measure before you write prose.** `DedupTokenRetryLiveTest` showed that the gg1 note's "rows written twice" was wrong; the real risk is that old rows persist. Every query in the gg2 note was run against rows written by both gg1 and gg2 code.
- **Rehearse-then-publish, again with zero surprises.** A scratch push for one green run on the exact tip came first. The rollback tag went up before the merge, the release tag only after the merge's own run was green, and the scratch branch was deleted last. Exactly one run landed on the published SHA.
- **The milestone audit ran this time.** It passed: 26/26, integration 4/4, flows 3/3. That makes this the first close where the override rests only on stale digests, not on a missing audit.

### What Was Inefficient

- **Review gap closure in Phase 19.** CR-01 (`SimpleAggregateFunction(f, Nullable(T))` flipped to non-nullable) was found after the main fix landed. It took four more plans (19-07..19-10), so Phase 19 ran 10 plans. A wrapper-type matrix in the spike would have found it earlier.
- **Verification staleness, fourth time.** Phases 16–19 read stale at close because Phase 20 edited the docs and anchors they covered. It was again an `override_closeout`.
- **Review IDs were reused across re-runs.** A second Phase 19 review reused IDs of the first, so "WR-02" meant two different things, L-38 and vacuous assertions. Phase 20 had to write a disambiguation section into the close record.
- **A repository setting changed runtime behaviour after the release.** Switching the default branch to `custom_build` (the right call for Dependabot) also activated upstream's nightly `integrationTests.yaml`, which is red by inheritance. The consequence was not foreseen at the decision.

### Patterns Established

- **Docs-only early publication.** A path allow-list diff plus a tree-equality check replaces the merge-invariant script when no code moves.
- **Fail-open, total metadata passes.** `DeclaredNullability` wraps parse, pairing and assignment in one `try` and applies only once a whole root pairs. On failure it falls back with a metadata-only WARN that names the database and table, so a surprise can never corrupt a batch.
- **A close record as the hand-off artifact.** `20-MILESTONE-CLOSE-RECORD.md` holds every carried advisory with its reason, re-open trigger and owner. It also holds the post-publication manual-check outcomes in the user's words.
- **One branch per SHA, always.** Every push in this milestone (two docs merges, the CI-17 proof, the rehearsal and the release) produced exactly one run.

### Key Lessons

1. **Put wrapper and modifier types in the spike matrix.** `SimpleAggregateFunction`, `LowCardinality` and `Nullable` nest around the types a fix targets. The Tuple spike covered the targets but not their wrappers, and review found the gap.
2. **Never reuse a review ID for a different finding.** When a review is re-run, number new findings past the old ones, or prefix them by report. A reused ID makes "deferred" and "fulfilled" collide.
3. **Before changing a repository setting, list what it activates.** The default branch decides which `schedule:` workflows run and which Dependabot config applies. Check `.github/workflows/*` for `schedule:` triggers first.
4. **Verify claims in operator docs with a live test, not with reasoning.** Two consecutive milestones shipped a rollout claim that a live test later overturned.

### Quality Observations

- Code review ran on all five phases. The only critical was 19-REVIEW CR-01, found and fixed by gap closure in Phase 19. Final reports: 10 warnings and 15 info. Phase 16 is clean after its fixes. The rest are fixed or deferred with re-open triggers in each `REVIEW-DISPOSITION.md` and the close record.
- Security audits for Phases 16, 18, 19 and 20 verified with 0 open threats at or above the block threshold. In Phase 19, T-19-12 and T-19-26 stay open at medium because 19-REVIEW WR-01 and IN-04 limit their claims.
- Final suite: 988 tests, 0 failures, 0 errors, 18 named skips on `latest` 26.9.18.11. It was green twice locally, once on the rehearsal SHA and once on the published SHA (run `38587062864`).
- `src/main` changes were within decided scope:
  - the empty-CDC skip, the column-naming `DataException`, the null-message guards and the log masking (Phase 18)
  - the Enum one-liner and `DeclaredNullability` (Phase 19)

  The fork delta grew 95→119 files wide and 31→32 in `src/main`, mostly tests.

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.2.9_gg4-tests | 5 | 16 | First GSD milestone on this repo. Established parallel independent test tracks, blocking-human checkpoints for out-of-scope production changes, and real test/build commands in `config.json`. |
| v1.6.0_gg1-sync | 6 | 24 | First upstream sync. Mechanical-merge-then-intent with a byte-identity invariant, a generated per-path merge checklist, scratch-branch CI rehearsal, and human-gated one-way publication. |
| v1.7.0_gg1-sync | 4 | 25 | Second sync, 4 days against 20, because the recipe was reused with a parameterised invariant script. Hardening went on top of the merged tree. Behaviour ledger, twin-table parity tests and seen-red mutation checks; one commit per dependency bump. |
| v1.7.0_gg2-hardening | 5 | 29 | First milestone without a sync. Docs-only early publication with release docs reviewed before the gate, a spike-first go/no-go that could not hold the release, operator claims pinned by live tests, and the first close with a passed milestone audit. |

### Cumulative Quality

| Milestone | Tests | Critical Review Findings | Production Bugs Fixed | Tracked Follow-ups |
|-----------|-------|--------------------------|----------------------|--------------------|
| v1.2.9_gg4-tests | 187 → 371 | 0 | 7 (+1 pinned by decision) | 9 |
| v1.6.0_gg1-sync | 371 → 663 (incl. upstream's) | 4 (all fixed) | 2 silent merge reversions + D-17 + CR-01 auto.evolve (R-01 still pinned) | 17 |
| v1.7.0_gg1-sync | 663 → 830 | 0 | R-01 data loss, Debezium Timestamp, nested-null desync, #820 shadowing, DESCRIBE depth, Int256, 3 config/metadata | 11 (all 17 carried closed) |
| v1.7.0_gg2-hardening | 830 → 988 | 1 (CR-01, fixed) | 999.1 Nullable Enum/Tuple mis-storage, empty-CDC rows on 3 routes, message-less NPE, nameless missing-value error, raw DateTime in logs | 10 (all 11 carried closed) |

### Top Lessons (Verified Across Milestones)

1. **Verification goes stale in sequential phases.** All four milestones closed as `override_closeout` because later phases edited files that earlier verifications covered. That held even when the milestone audit passed. Re-verify at close, or plan for the override.
2. **Blocking-human checkpoints on irreversible or production-affecting calls pay off.** They fired at least twice in every milestone and were right each time. Examples: R-01 pinned rather than fixed, KEEP over DROP, and every one-way publication.
3. **Check the environment and the gate before trusting a result.** In v1.2.9_gg4-tests a red baseline turned out to be an environment artifact (`TESTCONTAINERS_HOST_OVERRIDE`). In v1.6.0_gg1-sync a green run turned out to be against the wrong server (`CLICKHOUSE_VERSION_DEFAULT`).
4. **Reuse compounds.** Each milestone's tools made the next one cheaper: the suite (v1.2.9) gated the first sync, and the first sync's invariant script and checklist made the second take 4 days. Keep those tools current at every close.
5. **Operator-facing claims need live evidence.** In v1.7.0_gg1-sync review caught a destructive `TRUNCATE` instruction, and in v1.7.0_gg2-hardening a live test overturned the gg1 note's deduplication claim. Review rollout docs before the publish gate, and back every behavioural claim with a test.
