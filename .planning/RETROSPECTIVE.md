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

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.2.9_gg4-tests | 5 | 16 | First GSD milestone on this repo. Established parallel independent test tracks, blocking-human checkpoints for out-of-scope production changes, and real test/build commands in `config.json`. |

### Cumulative Quality

| Milestone | Tests | Critical Review Findings | Production Bugs Fixed | Tracked Follow-ups |
|-----------|-------|--------------------------|----------------------|--------------------|
| v1.2.9_gg4-tests | 187 → 371 | 0 | 7 (+1 pinned by decision) | 9 |

### Top Lessons (Verified Across Milestones)

*Needs a second milestone before any lesson here is cross-validated. The five above are single-milestone observations.*
