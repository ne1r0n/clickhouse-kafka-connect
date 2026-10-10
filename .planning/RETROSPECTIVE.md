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

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.2.9_gg4-tests | 5 | 16 | First GSD milestone on this repo. Established parallel independent test tracks, blocking-human checkpoints for out-of-scope production changes, and real test/build commands in `config.json`. |
| v1.6.0_gg1-sync | 6 | 24 | First upstream sync. Mechanical-merge-then-intent with a byte-identity invariant, a generated per-path merge checklist, scratch-branch CI rehearsal, and human-gated one-way publication. |

### Cumulative Quality

| Milestone | Tests | Critical Review Findings | Production Bugs Fixed | Tracked Follow-ups |
|-----------|-------|--------------------------|----------------------|--------------------|
| v1.2.9_gg4-tests | 187 → 371 | 0 | 7 (+1 pinned by decision) | 9 |
| v1.6.0_gg1-sync | 371 → 663 (incl. upstream's) | 4 (all fixed) | 2 silent merge reversions + D-17 + CR-01 auto.evolve (R-01 still pinned) | 17 |

### Top Lessons (Verified Across Milestones)

1. **Verification goes stale in sequential phases.** Both milestones closed as `override_closeout` because later phases edited files that earlier verifications covered. Re-verify at close, or plan for the override.
2. **Blocking-human checkpoints on irreversible or production-affecting calls pay off.** They fired 3 times in each milestone and were right each time. Examples: R-01 pinned rather than fixed, KEEP over DROP, and both one-way publications.
3. **Check the environment and the gate before trusting a result.** In v1.2.9_gg4-tests a red baseline turned out to be an environment artifact (`TESTCONTAINERS_HOST_OVERRIDE`). In v1.6.0_gg1-sync a green run turned out to be against the wrong server (`CLICKHOUSE_VERSION_DEFAULT`).
