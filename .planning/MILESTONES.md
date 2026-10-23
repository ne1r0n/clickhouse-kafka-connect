# Milestones

## v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt (Shipped: 2026-10-23)

**Phases completed:** 5 phases (25–29), 32 plans, 81 tasks

**Delivered:** The fork absorbed nine Dependabot merges and upstream's untagged tail on top of `v1.7.0`, and the shipped Confluent archive was repaired and then guarded: under Shadow 9 it had nested 25 jars into an unusable archive while the build stayed green. Two discovery defects that `v1.7.0_gg3` disclosed are fixed. A Tuple element named `size`, `size0` or `size<digits>` is no longer dropped (backlog 999.4), and column, element and Enum-label names are matched under the raw identifier written in `CREATE TABLE` on both clients (backlog 999.5). The writer failure path no longer leaks a Jackson-chain Map key, an unknown Enum label fails with a column-named message instead of a `NullPointerException`, and the carried 21–24 review debt is cleared. It shipped as `v1.7.0_gg4` with a reviewed rollout note.

**Stats:**

- **Git range:** `73cb565d` (milestone start, 2026-10-21) → `2325b3ef` (Phase 29 UAT), 298 commits (266 first-parent) on `sync/v1.7.0`.
- **Timeline:** 2026-10-21 → 2026-10-24 (3 days). Published 2026-10-23, closed 2026-10-24.
- **Files changed:** 320 in total (+38,756 / −674), mostly `.planning/`. Of those, 174 are in `src/`, the build, `gradle/` and `.github/` (+7,270 / −483). `src/main` changed in 12 files (+792 / −256), including the upstream tail taken in Phase 25. The new classes are `DescribeTableBuilder` and `ParentShape`.
- **Codebase:** 11,059 LOC of main Java and 38,839 LOC of test Java.
- **Fork delta over upstream** (`docs/FORK-DELTA.md` raw numbers), wider scope then `src/main`:
  - files: 130 → 277 and 33 → 37;
  - insertions: 22,783 → 29,679 and 2,245 → 2,877;
  - deletions: 534 → 643 and 145 → 241.
- **Suite:** `testUnit` runs 1,278 tests with Docker stopped (2 skipped). Two full runs on ClickHouse `latest` (26.9.19.9) × client V2 ran 1,865 tests each, with 0 failures and 18 named skips.
- **Requirements:** 36/36 complete (the audit text says 37; `REQUIREMENTS.md` and its traceability table hold 36 IDs). The milestone audit is `tech_debt` with no gaps: phases 5/5, integration 5/5, flows 5/5.
- **Published:** `v1.7.0_gg4` is the `--no-ff` merge `21c51598` (parents `69cd680b` and `3d847009`) on `custom_build`. Its tags `v1.7.0_gg4` and `pre-v1.7.0_gg4` (rollback → `69cd680b`) are on `origin`. The merge has one green `tests.yaml` run, [`40017036598`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/40017036598).

**Closeout type:** `override_closeout`

**Known verification overrides:** 0 newly acknowledged, 0 carried forward from a prior close. The open-artifact audit was clear. There are two overrides, both accepted by the user:

- At close, `init.manager` read Phases 25–28 as `phase_complete: false, verification: stale`. Each `VERIFICATION.md` has `status: passed`. For Phases 25, 27 and 28 the covered-file digests drifted because later work edited covered files: `docs/FORK-DELTA.md`, `CHANGELOG.md` and `.planning/scripts/fork-anchors.txt`, plus `build.gradle.kts` (Phase 28's coverage list) and `Column.java` and its tests (Phase 28's Enum-label keys) for Phases 25 and 27. That is sequential drift, not a failed check. For Phase 26 no covered file was edited after its verification date, so its stale reading is unexplained.
- Phase 27 success criterion 2 was narrowed by the user's decision (`overrides_applied: 1` in `27-VERIFICATION.md`).

**Known gaps:** none against requirements. The follow-ups below are carried into `PROJECT.md` → Requirements → Active. The 16 unfixed review findings (25-REVIEW WR-01..WR-03 and IN-01..IN-04, plus the 26–28 findings) each have a reason, an owner and a re-open trigger in `29-MILESTONE-CLOSE-RECORD.md`.

- **PUB-12 / SC3:** the Phase 29 review ran 4 rounds (the roadmap allowed at most 3) and round 4 found IN-09, so there was no clean round. The user authorised round 4 and accepted the deviation in `29-UAT.md`. The IN-09 fix (CHANGELOG wording only) was never re-reviewed.
- **Archive compare:** `confluent-archive-allowlist-29.txt` has no CI consumer. The entry-by-entry archive compare against gg3 is a manual release-time step; CI runs only the `verifyConfluentArchive` finalizer.
- **Test gap:** no single test drives a size-named Tuple element whose name also contains an escape character.
- **`FORK-DELTA.md:224`** still reads, as history, that gg3 shipped items 10 and 11 as open. Line 226 states they are closed.
- **Branch state:** `sync/v1.7.0` is 6 docs and test commits ahead of `custom_build`. The tag `v1.7.0_gg4` points at the `custom_build` merge `21c51598`.
- **Not assessed:** Nyquist validation is disabled in config, and the `connect-api` 4.4.0 / Shadow 9 CHANGELOG body was not spot-checked.

**Key accomplishments:**

1. Nine Dependabot merges and upstream's untagged tail `a6f1df83` were absorbed through two mechanical merges (M1 `50f56289`, M2 `38265d26`). The unedited anchors gate passes, and the pin guard now uses a one-pass Kotlin scanner (DEBT-13).
2. The Confluent archive is repaired under Shadow 9 (it nested 25 jars). It is now finalized by `verifyConfluentArchive`, and 176 Kafka references are proven present in the production worker's `connect-api` and `kafka-clients` 7.9.7 jars.
3. Discovery keeps a declared Tuple element named `size`, `size0` or `size<digits>` through one fork-owned `DescribeTableBuilder`. The fix changes the Column tree of exactly 25 of 47 catalog tables, and a 46-case live round-trip fails on exactly those shapes on the pre-fix tree.
4. Column, element and Enum-label names are matched under the raw `CREATE TABLE` identifier on both clients. This changes 16 of 24 name tables and 9 of 12 Enum entries, and a doubled-form Tuple element now fails the put loudly.
5. A Jackson failure on the JSON-as-string route no longer names a Map key, an unknown Enum label throws a value-free `IllegalArgumentException` naming the column, and the writer's failure texts are built from one prefix constant.
6. `v1.7.0_gg4` is published with `docs/ROLLOUT-v1.7.0_gg4.md`, where every command equals an evidence block and the rollback is proven by script, one green CI run on the published SHA, and an origin-freeze check.

---

## v1.7.0_gg3-hardening Record-Data Exposure & Review Debt (Shipped: 2026-10-20)

**Phases completed:** 4 phases (21–24), 23 plans, 60 tasks, plus quick task `261020-vcr`

**Delivered:** The last known record-data exposure is closed (backlog 999.3). A writer value-conversion failure now names the column, the Kafka and ClickHouse types and the exception class, and the record value no longer reaches the task status, the DLQ headers or the `Error inserting records` logs. Retry classification is pinned unchanged. Every advisory carried from 17-REVIEW and 19-REVIEW is closed: `DeclaredNullability` is null-safe and quote-aware, and it warns on an unpaired element. The pin guard and its tests are fixed as well. Further work in this milestone:
- The three Dependabot bumps on `origin/custom_build` were absorbed without a rebase.
- The writer tests no longer use Mockito inline mocking.
- The user disabled the inherited-red nightly workflow and revoked the read-only token.

The result shipped as `v1.7.0_gg3`. Three more review rounds then corrected its rollout note, and it was republished as `6a2a87a5` the same day.

**Stats:**

- **Git range:** `c4521f81` (milestone start) → `8fdfcbfd` (quick 261020-vcr bookkeeping), 177 first-parent commits on `sync/v1.7.0`. There was no upstream sync: `upstream/main` is still `v1.7.0`.
- **Timeline:** 2026-10-16 → 2026-10-20 (5 days). Published 2026-10-20, closed 2026-10-21.
- **Files changed:** 135 in total (+27,419 / −324), mostly `.planning/`. Of those, 26 are in `src/`, the build, `gradle/` and `.github/` (+4,183 / −207). `src/main` changed in 4 files (+227 / −26); the one new file is `WithheldMessageException`.
- **Codebase:** 10,523 LOC of main Java and 38,242 LOC of test Java.
- **Fork delta over upstream** (`docs/FORK-DELTA.md` raw numbers), wider scope then `src/main`:
  - files: 119 → 130 and 32 → 33;
  - insertions: 18,802 → 22,783 and 2,042 → 2,245;
  - deletions: 529 → 534 and 145.
- **Suite:** 1281 tests on ClickHouse `latest` (26.9.19.9), with 0 failures, 0 errors and 18 named skips. It passed twice, the second time with `--rerun`. `testUnit` runs 838 tests with Docker stopped.
- **Requirements:** 20/20 complete. The milestone audit first came back `tech_debt`, because seven review findings on the published rollout note had no owner or trigger. After quick task 261020-vcr it passed on re-audit: requirements 20/20, phases 4/4, integration 12/12, flows 5/5.
- **Published:**
  - `v1.7.0_gg3` is the `--no-ff` merge `3376a18f` (parents `6473e24f` and X = `8b1c318d`). Its tags `v1.7.0_gg3` and `pre-v1.7.0_gg3` (rollback → `6473e24f`) are on `origin`.
  - On top of it, `custom_build` is `6a2a87a5`, a docs-only `--no-ff` merge of the corrected note. No tag was added or moved.
- **CI:**
  - Run [`39472696952`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/39472696952) on `3376a18f` is green, and the rehearsal SHA had its own green run.
  - Run [`39496482074`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/39496482074) on `6a2a87a5` is green.
  - Each is the only run on its SHA.

**Closeout type:** `override_closeout`

**Known verification overrides:** 0 newly acknowledged, 0 carried forward from a prior close. The open-artifact audit was clear. The override has one reason, which the user accepted at close.

All four phase verifications read stale. Each `VERIFICATION.md` has `status: passed`, but its covered-file digest no longer matches. The cause is that later work edited files these phases covered:
- for Phases 21–23, `docs/FORK-DELTA.md` and `.planning/scripts/fork-anchors.txt`, edited in Phases 22–24;
- for Phase 24, the rollout note, `CHANGELOG.md` and `docs/FORK-DELTA.md`, edited in quick 261020-vcr.

That is sequential drift, not a failed check. No covered `src` file changed after its phase's verification. Every UAT passed (7/7), and all four `SECURITY.md` files read `threats_open: 0`.

The final tree is covered by:
- the suite gate;
- the anchors gate on the published tip (`result=PASS`, 548 commits);
- the three review rounds of the quick task;
- the two CI runs.

**Known gaps:** none against requirements. These follow-ups are carried into `PROJECT.md` → Requirements → Active. Each review advisory has its owner and re-open trigger in `24-MILESTONE-CLOSE-RECORD.md`:

- **Backlog 999.4:** discovery drops a Tuple element named `size` or `size0`. The inserts that were run failed with code 32 or 33, and a wrong stored value is possible but was not measured. A WARN names the element, and FORK-DELTA Known costs item 10 and rollout item 2 disclose it.
- **Backlog 999.5:** `fromJsonRow` doubles escaped name characters. FORK-DELTA Known costs item 11 records it.
- **22-REVIEW WR-01:** an accepted, measured residual. On the opt-in JSON-as-string route a Jackson reference chain can name a Map key.
- **Deferred advisories:**
  - 21-REVIEW: WR-01..WR-03, IN-01, IN-02, IN-04;
  - 22-REVIEW: IN-01..IN-05;
  - 23-REVIEW: WR-01, WR-02, IN-01..IN-03;
  - 24-REVIEW: IN-03;
  - `jacocoForkReport` omits two fork classes.
- **Older rollout notes:** the gg1 and gg2 notes, both published and protected, keep a shorter `enableDbTopicSplit=true` key rule. The gg3 note tells the reader to follow its own item 4 step 2.
- **User reminders:** create the `dependencies` label, and decide Dependabot PRs #4 and #9 (fork-only pins) and #6–#8.

**Key accomplishments:**

- **Closed backlog 999.3.** `doWriteColValue`'s generic catch throws a metadata-only `DataException` (``Failed to write column `<name>` ...``) with a `WithheldMessageException` cause, which keeps the frames and withholds every message.
  - IO chains, `ConnectException` and exact `RuntimeException` pass through.
  - Sentinel tests were seen red before the twelve-file fix `c713a837`, and they pass live and Docker-free.
  - A 184-case `WriterFailureClassificationUnitTest`, green before and after, pins the retry classification as byte-identical.
- **Hardened `DeclaredNullability`.**
  - `apply` and `containsTuple` are total for a null root or a null type.
  - `normalise` is one quote-aware pass, so a backslash-n inside a quoted element name or an enum label pairs.
  - An unpaired declared element logs one metadata-only WARN and still applies to the elements that paired. This exposed backlog 999.4.
  - The fallback-WARN assertions now use a sentinel column, and they were seen red under two mutations.
- **Integrated Dependabot and closed CI debt.**
  - Lombok 1.18.48, Spotless 8.10.4 and slf4j-reload4j 2.0.20 came in through merge `82524e12`. The one anchor drift is forgiven by a SHA- and row-pinned exemption list, proven by twelve fail-first self-tests.
  - The pin guard now fails if the fork-count step or `reportTestForks` disappears. It accepts a commented `runs-on`, and it no longer treats step text as a write scope.
  - The user disabled the nightly workflow, confirmed the Dependabot entry and revoked the token.
- **Removed the last inline Mockito mock** of a connector class from the writer tests. A hand-written `CountingHelperClient` and a `ScriptedWriteFailureWriter` replace it, which closes the OpenJ9 `retransformClasses` crash risk. Every 17-REVIEW and 19-REVIEW finding is fixed or closed with a commit pointer.
- **Measured the release before writing about it.**
  - Running the real gg2 and gg3 class trees over 44 fixture tables showed 14 `Nullable(Tuple)` shapes that encode differently.
  - A gg2 retry of such a range fails with code 33, so a rollback needs a clean stop for that shape.
  - The Jackson Map-key residual is pinned by `JsonAsStringKeyResidualLiveTest`.
  - `docs/ROLLOUT-v1.7.0_gg3.md` ships seven three-part items, every query proven against an oracle and a look-alike database, and the item 7 rollback proven on a dry-run merge.
- **Published `v1.7.0_gg3`, then corrected its note.**
  - The frozen tree was gated twice (1281 tests), rehearsed on CI, and published as `3376a18f` after the user's direct approvals. The rollback tag went out first, and the release tag only after green.
  - Review rounds 3–6 found 14 more findings in the operator documents. One of them would have given a false "safe to roll back" for connectors with three or more topics or with `enableDbTopicSplit=true`.
  - Quick task 261020-vcr fixed all of them. It republished the note as `6a2a87a5` after a second direct `publish`, with one green run and no tag moved.

---

## v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene (Shipped: 2026-10-14)

**Phases completed:** 5 phases (16–20), 29 plans, 59 tasks

**Delivered:** All 11 follow-ups that `v1.7.0_gg1-sync` left open are closed. They shipped as `v1.7.0_gg2`. The corrected gg1 rollout doc went out first: the published copy no longer offers `TRUNCATE` on the shared KeeperMap state table, and its rollback step works on the published tip. Backlog 999.1 is fixed: `Nullable(Enum8|Enum16)` and `Nullable(Tuple)` values are stored as written. Empty Debezium records are skipped on every write route. Writer errors name the column without carrying record data, and DateTime parse logs no longer print the raw value. CI runs on a pinned `ubuntu-24.04` runner with SHA-pinned actions and a read-only token. Operators get `docs/ROLLOUT-v1.7.0_gg2.md`, whose every quoted query was run live.

**Stats:**

- Git range: `9ff1affc` (milestone start) → `bf8f56c6` (milestone audit). That is 217 commits on `sync/v1.7.0`. There was no upstream sync this milestone.
- Timeline: 2026-10-10 → 2026-10-15 (6 days). Published 2026-10-14, closed 2026-10-16.
- Files changed: 167 total (+31,700 / −413), mostly `.planning/`. Of those, 37 are in `src/`, build, `gradle/` and `.github/` (+5,433 / −263).
- Codebase: 10,322 LOC main Java; 34,467 LOC test Java (`src/test` 28,360, `src/testFixtures` 4,329, `src/integrationTest` 1,778)
- Fork delta over upstream: files 95→119 (wide) and 31→32 (`src/main`). Insertions 13,587→18,802 (wide) and 1,826→2,042 (`src/main`). Tests are 16,288 of the 18,802 wide insertions; `src/main` gained one file, `DeclaredNullability`.
- Suite: 988 tests, 0 failures, 0 errors, 18 named skips on ClickHouse `latest` (26.9.18.11), passed twice (cache-busted `test --rerun`). `testUnit` runs 556 tests with Docker stopped.
- Requirements: 26/26 complete. Milestone audit passed: requirements 26/26, integration 4/4, flows 3/3.
- Published: `custom_build` = `--no-ff` merge `a246ca01` (parents `be6b8861` and the rehearsed tip `3879b883`), with tags `v1.7.0_gg2` and `pre-v1.7.0_gg2` (rollback → `be6b8861`) on `origin`. Before it, Phase 16 published the docs-only merges `537beb1f` and `be6b8861`.
- CI: run [`38587062864`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38587062864) on `a246ca01` is green in 408 s. It is the only run on that SHA. The rehearsal SHA had its own green run, `38578966373`.

**Closeout type:** `override_closeout`

**Known verification overrides:** 1 newly acknowledged, 0 carried forward from a prior close (see STATE.md Deferred Items). Unlike the two previous closes, the milestone audit was run, and it passed. The override is for two reasons, both accepted by the user at close:

1. **Phases 16–19 verification reads stale.** All four `VERIFICATION.md` files have `status: passed`. Their covered-file digests no longer match because Phase 20 later edited files they covered, such as `CHANGELOG.md`, `docs/FORK-DELTA.md` and `.planning/scripts/fork-anchors.txt`. That is ordinary sequential drift, not a failed or reversed check. Phase 20 verification is current and `passed` (8/8), and Phase 20 UAT was 35/35. The audit, the two suite runs and the CI run on the published SHA cover the final tree.
2. **One deferred item acknowledged.** `ClickHouseWriterNullMessageUnitTest` (Phase 18) inline-mocks `ClickHouseHelperClient` with Mockito. That carries an intermittent risk of an OpenJ9 `retransformClasses` crash on this machine. It has passed in every observed run.

**Known gaps:** none against requirements. These follow-ups are carried into `PROJECT.md` → Requirements → Active. Each has its owner and re-open trigger in `20-MILESTONE-CLOSE-RECORD.md`:

- Backlog 999.3: a rethrown write-failure message can carry a record value into the task status, the DLQ headers and two `Error inserting records` ERROR logs.
- 19-REVIEW WR-01, WR-02 and IN-01..IN-03 are deferred advisories on the declared-nullability pass and its tests. IN-04 is owned by the next milestone.
- 17-REVIEW WR-01 residue: confirm that the Dependabot `gradle` entry shows a recent "Last checked". IN-01..IN-03 are still open.
- Housekeeping for the user: revoke the read-only GitHub token and delete the Keychain item `gsd17-gh-actions-read`.

**Key accomplishments:**

- Published the corrected gg1 rollout doc before any code work, as docs-only `--no-ff` merges `537beb1f` and `be6b8861`, each with one green run. The `TRUNCATE` advice on the shared KeeperMap state table is gone. Item 8's rollback is now `git read-tree -u --reset pre-v1.7.0-sync`, because `git revert -m 1` hits 97 conflicts once docs merges sit on top of the release.
- Hardened CI. `tests.yaml` now has:
  - an `ubuntu-24.04` runner and all four actions pinned to commit SHAs on Node 24 majors
  - `permissions: contents: read` and `persist-credentials: false`
  - a fork count read from a new `reportTestForks` Gradle task
  - a grouped `github-actions` Dependabot entry

  `test-head.yaml` is deleted. A Docker-free guard pins all of this. One scratch run, `38172811430`, proved it. The duplicate test-classpath lz4 (999.2), the unasserted LZ4 response leg, the Lombok Jackson warning and the NanoTimestamp edge row were closed alongside.
- Made the writer correct on four fronts:
  - The empty-CDC skip covers all four write routes. It is pinned live on clients V1 and V2 and by 24 Docker-free byte, token and state pins, with a mutation check.
  - A missing non-nullable value throws a column-naming, data-free `DataException`.
  - A message-less failure is rethrown as itself instead of becoming a `NullPointerException`.
  - DateTime parse-failure logs no longer print the raw value.
- Closed backlog 999.1:
  - `Nullable(Enum8|Enum16)` needed a one-line `.isNullable(isNull)` in `Column.extractColumn`.
  - `Nullable(Tuple)` takes element nullability from the declared type string through `DeclaredNullability`, after a live twin-table spike answered `go`.
  - Gap closure added the `SimpleAggregateFunction` unwrap and made the pass total, with a fallback WARN that names the database and table.
  - The discovery WARN is deleted.
- Pinned the deduplication-token retry consequence live (`DedupTokenRetryLiveTest`, 8 cases) before writing the rollout note. This corrected the gg1 note: a same-token retry is dropped even when its bytes changed. Shipped `docs/ROLLOUT-v1.7.0_gg2.md` with six delta items and detection queries, a `1.7.0_gg2` CHANGELOG section and a refreshed `FORK-DELTA.md`.
- Gated the frozen tree twice (988 tests), rehearsed it on CI and published it as `v1.7.0_gg2`. The rollback tag was pushed first and the release tag only after the merge's own run was green. After publication the user enabled Dependabot version updates and switched the fork's default branch to `custom_build`.

---

## v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening (Shipped: 2026-10-10)

**Phases completed:** 4 phases (12–15), 25 plans, 55 tasks

**Delivered:** Upstream `v1.7.0` was merged into `custom_build` and published as `v1.7.0_gg1`, with all 17 follow-ups carried from earlier milestones closed on the merged tree. The biggest change is that R-01's silent data loss is fixed: `Struct` and CDC records under `insertFormat=json` now land through a schema-aware JSON writer, and a live twin-table matrix proves parity with RowBinary. Nested nulls in RowBinary either round-trip or fail loudly. D-17 is retired onto upstream's `setCompressionMethod`, and five dependencies were bumped one commit at a time. CI now runs exactly once per published SHA. Operators get a rollout checklist (`docs/ROLLOUT-v1.7.0_gg1.md`) covering every behaviour change.

**Stats:**

- Git range: `94d98615` (milestone start) → `cb6918f9`. That is 153 first-parent commits, 175 including the merged upstream history.
- Timeline: 2026-10-07 → 2026-10-10 (4 days)
- Files changed (incl. the upstream merge): 161 total (+26,882 / −369). Of those, 49 are in `src/`, build, `gradle/` and `.github/` (+4,927 / −221).
- Codebase: 10,119 LOC main Java; 29,523 LOC test Java (`src/test` 23,416, `src/testFixtures` 4,329, `src/integrationTest` 1,778)
- Fork delta over upstream: files 84→95 (wide) and 30→31 (`src/main`). Insertions 9,476→13,587 (wide) and 1,368→1,826 (`src/main`). The growth is hardening and tests on top of the merge, not unretired debt.
- Suite: 830 tests, 0 failures, 0 errors, 18 named skips on ClickHouse `latest` (26.9.14.10), passed twice (`clean test`, then `test --rerun`). `testUnit` runs 454 tests with Docker stopped.
- Requirements: 39/39 complete
- Published: `custom_build` = `--no-ff` merge `f328fd21` (of the rehearsed tree `ae8520ed`), with tags `v1.7.0_gg1` and `pre-v1.7.0-sync` (rollback → `c61d90ce`) on `origin`
- CI: run [`38050860929`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38050860929) on `f328fd21` is green in 389 s. It is the only run on that SHA, with no cancelled sibling, which proves CI-08 end to end.

**Closeout type:** `override_closeout`

**Known verification overrides:** 0 newly acknowledged, 0 carried forward from a prior close. The pre-close artifact audit was clear in every category. The override is for two other reasons, both accepted by the user at close:

1. **Phases 12–13 verification reads stale.** Both `VERIFICATION.md` files have `status: passed`. Their covered-file digests no longer match because Phases 13–15 later edited the files they covered (`ClickHouseWriter`, `docs/FORK-DELTA.md` and others). That is ordinary sequential drift, not a failed or reversed check. Phases 14 and 15 verification are current and `passed`, and Phase 15 UAT was 1/1. Phase 15's two full-suite runs and the CI run on the published SHA cover the final tree.
2. **No milestone audit was run.** There is no `v1.7.0_gg1-sync-MILESTONE-AUDIT.md`. Accepted because 39/39 requirements are checked and mapped, and Phase 15 verified the end-to-end result directly.

**Known gaps:** none against requirements. TYPE-08 (`Int256`/`UInt256` only) and JSONP-08 (evaluation order only) were narrowed on the record. Eleven follow-ups are open under `PROJECT.md` → Requirements → Active:

- **Most urgent:** the corrected rollout doc `505e4981` is not yet on `origin`. The published copy still offers `TRUNCATE` on the KeeperMap state table that exactly-once connectors share.
- Backlog 999.1: `Nullable(Enum8|Enum16|Tuple)` is mis-mapped by `Column.extractColumn`. It is warned about at discovery.
- Backlog 999.2: the test classpath carries a second lz4.

**Key accomplishments:**

- Merged upstream `v1.7.0` as one mechanical commit (`54e63648`, zero authored lines) behind a parameterised invariant script, every check of which was proven able to fail. A 33-verdict per-hunk checklist covers the auto-merged hunks in fork-touched files.
- Retired D-17 onto upstream's `setCompressionMethod` for every client the connector builds, pinned Docker-free and proven live with the flag on and off. Made upstream #820's null-Map bytes reachable by scoping the fork's MAP guard to `Nullable`. Proved `KeeperStateProvider`'s new streamed-row read with a restart test that was seen to fail against the old code.
- Fixed R-01 by routing. `Struct` and Debezium CDC records in metadata-free JSON mode go through a new `MetadataFreeJsonWriter`, which writes `Timestamp`, `Date`, `Time`, `Decimal`, `bytes` and Debezium `Timestamp`/`NanoTimestamp` as RowBinary would. A live twin-table matrix proves it.
- Nested nulls in RowBinary now round-trip in `Nullable` slots, coerce to empty for `Array`/`Map`, or fail the batch with a data-free `DataException` naming the column. Metadata, config and test-debt findings were closed: DESCRIBE skip at any depth, `Int256` fail-fast, `extractColumn` argument order, the `USERNAME` default, typed read-back and attributable teardown.
- Bumped lz4-java (1.10.4 → 1.12.0, advisory-free), okhttp, mockito, Jackson (two CVEs closed) and org.json, one catalog commit each, each gated by its own tests. `tests.yaml` now triggers on branch pushes only, compiles integration sources and reports its fork count.
- Gated the exact tree twice (830 tests), rehearsed it on CI, and published it as `v1.7.0_gg1` after a recorded human approval. It shipped with a CHANGELOG section, a rewritten `FORK-DELTA.md` and an eight-item operator rollout checklist.

---

## v1.6.0_gg1-sync Upstream v1.6.0 Sync (Shipped: 2026-10-07)

**Phases completed:** 6 phases, 24 plans, 63 tasks

**Delivered:** Upstream `v1.6.0` (581 commits past `v1.2.9`) merged into `custom_build` and published as `v1.6.0_gg1`. Every fork feature was either re-applied onto upstream's rewritten code or consciously retired in favour of upstream. The fork's suite plus upstream's 56 test files run green on ClickHouse `latest` × client V2, and CI is green on the published SHA. `docs/FORK-DELTA.md` records what the fork still patches and what it gave up. Two behaviours that vanished silently during the merge were caught by the gate before publication: the `latest` test-image default, and client `0.9.6` failing against ClickHouse 26.9's ZSTD default.

**Stats:**

- Git range: `214571b2` (milestone start) → `01b7880d` — 170 first-parent commits (751 including the merged upstream history)
- Timeline: 2026-09-18 → 2026-10-07 (20 days)
- Files changed (incl. the upstream merge): 360 total (+48,383 / −5,486); 216 in `src/`, build, `gradle/` and `.github/` (+19,778 / −4,153)
- Codebase: 9,622 LOC main Java; 25,473 LOC test Java (`src/test` 19,571, `src/testFixtures` 4,215, `src/integrationTest` 1,687)
- Fork delta over upstream: files 93→84 (wide) / 32→30 (`src/main`); insertions 8,401→9,476 (wide) / 1,308→1,368 (`src/main`)
- Suite: 663 tests, 0 failures, 0 errors, 18 named skips on ClickHouse `latest` (26.9.12.8); `testUnit` 331 tests with Docker stopped
- Requirements: 53/53 complete
- Published: `custom_build` = merge `c61d90ce`, tags `v1.6.0_gg1` and `pre-v1.6.0-sync` (rollback) on `origin`
- CI: run [`37619434848`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/37619434848) on `c61d90ce` — green, 386 s

**Closeout type:** `override_closeout`

**Known verification overrides:** 0 newly acknowledged, 0 carried forward from a prior close. The pre-close artifact audit was clear in every category. The override is for two other reasons, both accepted by the user at close:

1. **Phases 6–10 verification reads stale.** Every phase `VERIFICATION.md` has `status: passed`. Phases 6–10 fall outside their freshness digest because later phases edited files those verifications covered: Phase 11 touched `ClickHouseHelperClient`, `ClickHouseTestHelpers` and `tests.yaml`. That is ordinary sequential drift, not a failed or reversed check. The milestone claim rests on Phase 11, whose verification is current and `passed`, with UAT 5/5. It re-ran the full suite locally and verified CI on the published SHA.
2. **No milestone audit was run.** There is no `v1.6.0_gg1-sync-MILESTONE-AUDIT.md`. Accepted because 53/53 requirements are checked and mapped, and Phase 11 verified the end-to-end result directly.

**Known gaps:** none against requirements. Phase 11 UAT resolved its five human-verification items, including CI-06's narrowed "exactly one run" locator and SUITE-03's mixed raw delta numbers. `PROJECT.md` → Requirements → Active carries 17 follow-ups. R-01's silent data loss is still pinned, not fixed. The `lz4` pin is a client-`0.9.6` workaround with a recorded removal condition.

**Key accomplishments:**
- Landed the 581-commit upstream v1.6.0 merge onto scratch branch `sync/v1.6.0` as one 100%-mechanical commit (`673d29c3`) with every one of the 40 conflicted paths verified byte-identical to upstream, backed by a committed, self-testing invariant script.
- Re-applied all six fork `build.gradle.kts` additions (JaCoCo report-only, `testUnit`, JDK-17 test-JVM pin, LZ4 capability resolution, ben-manes `dependencyUpdates`, Spotless config) onto upstream v1.6.0's version-catalog build file, landed `VERSION=v1.6.0_gg1` plus the `.gitignore`/`tests.yaml` settlement, and proved the four D-12 resolved-version facts (`com.clickhouse@0.9.6`, `connect-api@4.1.1`, `slf4j-reload4j@2.0.17`, `at.yawk.lz4:lz4-java:1.10.2`) against the real dependency graph.
- Generated `.planning/MERGE-CHECKLIST.md` — a 40-row, generated (not transcribed) per-path resolution gate for Phases 7-11, an 8-row "read anyway" list correcting a research-prose error about `Processing.java`, and a `## Recorded Facts` section closing SYNC-04/SYNC-05 — then re-proved both merge invariants across the full 588-commit phase range.
- Unblocked `compileJava` with a one-line `@Override` removal, then re-applied `Type.DYNAMIC` dispatch and a shared RowBinary fail-fast (`DataException`) for `DYNAMIC`/`UNKNOWN` columns onto upstream `v1.6.0`'s rewritten mapping and writer classes — three isolated commits, each independently bisectable.
- Re-threaded server-side `targetTableFilter` table discovery filtering and the DESCRIBE sub-column skip onto upstream `v1.6.0`'s rewritten `showTables`/describe-loop shape, re-expressing the fork's `shouldSkipDescribeRow` over a loop-local column map since upstream's `Table` is now immutable — two isolated commits, each independently bisectable against `v1.6.0`.
- Landed the remaining fork config settings (`jsonInsertColumns` key + explicit-format flag, `async_insert` un-forcing) and the planned four-item Phase 10 test carve-out, then discovered and mechanically fixed a much larger compile gap — twelve additional fork test files depending on pre-merge `ClickHouseBase`/`ClickHouseTestHelpers` methods the mechanical merge silently dropped — while deliberately leaving two files unfixed because completing them would require writing Phase 8's or Phase 10's own work inside this plan.
- Pinned all thirteen new upstream config options at their defaults with a new Docker-free test, fixed a test-construction NPE the acceptance-gate run surfaced, landed a user-approved build.gradle.kts test-compile carve-out for two future-phase files, and ran `testUnit` green with Docker genuinely stopped - then flipped Phase 7's twelve merge-checklist rows and recorded the two free wins.
- Restored the JMX `connector=<name>` ObjectName segment (deleted by the v1.6.0 merge) by threading `connectorName` from `ClickHouseSinkTask.start()` through `ProxySinkTask` into a new `SinkTaskStatistics(connectorName, taskId)` constructor, with the per-topic name composed from the task-level name rather than a second format string, and put the `MBeanNameUnitTest` canary back into the compiled test suite.
- Closed Phase 8 on live evidence: testUnit green (317 tests, 0 failures), the full live suite green modulo exactly the one pre-known carry-in (636 tests, 1 failure, 0 errors, 35 explained skips), and `TableTest.extractNullables()` traced to `ClickHouseHelperClient.shouldSkipDescribeRow` unconditionally stripping Nullable `.null` subcolumns — a fork behavior already pinned by `DESC-01`, not a Table.java defect.
- Discharged ORCH-01, ORCH-04 and FREE-01 by grep/diff evidence and a written record (no `src/` line touched), flipped Phase 8's eight `MERGE-CHECKLIST.md` rows to done, annotated the two Phase-10-owned rows Phase 8 landed early work against, and appended three Recorded Facts — while catching and reporting that the plan's own "`ProcessingTest.java` is byte-identical" premise had gone stale due to plan 08-01's own necessary compile fix.
- Re-applied the fork's four metadata-free `insertFormat=json` guard sites behind one shared predicate, rebuilt the synthetic `Table` against upstream's immutable constructor, re-attached `jsonInsertColumns` to the surviving `client.insert` overload, and un-parked the Docker-free canary test that now pins all of it inside `testUnit`.
- Re-enabled all 13 parked live JSON-path test methods (unchanged), adopted the TableTest.extractNullables() carry-in fix, and ran the full suite against real ClickHouse latex x client V2 clean: 643 tests, 0 failures, 0 errors, 18 skips (down from Phase 8's 35).
- Read the resolved `ClickHouseWriter.java` diff against `v1.6.0` to discharge JSONP-04 (no code, no test), then put Phase 9's decisions on the permanent record: JSONP-03's corrected rationale, all four JSONP requirements closed, `MERGE-CHECKLIST.md` row 26 annotated, Recorded Fact 13 resolved, and a new operator-confirmed Recorded Fact 14 stating the KEEP disposition with its accepted cost.
- Re-derived `ClickHouseBase`'s shared-container-per-fork contract over upstream's per-class rewrite, restored the `@AfterEach` MBean sweep with a proven per-topic extension, and fixed a silent `latest`→pinned-LTS version regression that had every local gate since the v1.6.0 merge testing the wrong ClickHouse server.
- Proved HARN-01's Docker-free claim with the daemon genuinely stopped, ran two back-to-back full suites on ClickHouse `latest` with byte-identical executed-class sets and zero failures, and settled D-06 with nothing to attribute — the milestone's first full-suite evidence on the server it actually claims to test.
- Closed Phase 10's books by re-running every HARN-04/05/06/07 verifying command live against the current tree, flipping all 17 Phase-10-owned MERGE-CHECKLIST.md rows to done with two new Recorded Facts, and closing HARN-01 through HARN-07 in REQUIREMENTS.md with a correction blockquote naming which four requirements closed on evidence rather than new code.
- The merge-invariant script now fails closed when `git grep` itself errors (proved by a rejected pathspec that the old script passed), the last six MERGE-CHECKLIST rows are closed on diffs measured now, and Phases 07 and 09 have 0-open security audits, with no production code touched.
- CI no longer pins the ClickHouse server or client version in its job environment, so it exercises the same code defaults as a local run, with the Docker-free `ClickHouseVersionDefaultUnitTest` as the detector, and the Javadoc that asserted the pin is corrected in the same commit.
- docs/FORK-DELTA.md: a grep-confirmed ledger of what the fork still patches and retired in favour of upstream v1.6.0, all twelve raw numbers (insertions up, stated plainly), R-01 silent data loss published first, and the 18-skip inventory, linked from README and AGENTS
- One `--rerun` full suite on ClickHouse `latest` (26.9.12.8) x client V2 passes at 663 tests, 0 failed, 0 errors, 18 skipped with the skip set identical to Phase 10's named 18, all five SMT classes clean and unskipped, and `testUnit` green at 331 tests with the Docker daemon confirmed down; the run was a 6-fork run, not the single-fork shape the plan assumed.
- `sync/v1.6.0` was pushed to origin and its single `tests.yaml` run (id 37617508949) finished `success` in 350 s (5.8 min) against a 22.5-minute threshold, with a 816160-byte `jacoco-coverage-reports` artifact; the six-item D-03 bundle is re-derived below and surfaces three findings for plan 11-06's gate, none of them a blocker.
- SUITE-01..03 and CI-05..07 are closed in `REQUIREMENTS.md` on commands re-run now (a fresh `--rerun` of 663 tests with 0 failures and a skip set identical to Phase 10's, plus the REST API, `git ls-remote` and four shortstat commands), with a blockquote that says plainly that this phase's real risk was CI and the one-way push, not the suite; nothing was pushed.
- Connector now sends `network_compression_method=lz4` on every V1 and V2 client (V2 `serverSetting` before operator options, V1 `CUSTOM_SETTINGS` in the shared default options), so client 0.9.6 decodes ClickHouse 26.9's responses again; pinned Docker-free and live.

---

## v1.2.9_gg4-tests Fork Test Coverage (Shipped: 2026-09-18)

**Phases completed:** 5 phases, 16 plans, 45 tasks

**Delivered:** Every behavior the `custom_build` fork adds on top of upstream ClickHouse Kafka Connect is now pinned by a named automated test running against ClickHouse `latest` × client V2 — the four fork SMTs, `targetTableFilter`, `describeTable` sub-column skipping, `JSON`/`JSON(...)`/`Dynamic` column mapping and round-trips, the metadata-free JSON insert path, `jsonInsertColumns`, JMX MBean naming, and `async_insert` pass-through. Seven production bugs surfaced by those tests were fixed in the phase that found them; an eighth (R-01 silent data loss) was pinned rather than fixed by explicit user decision. CI runs the suite on every push and publishes coverage.

**Stats:**

- Git range: `f8e4271` (roadmap created) → `a351121` — 110 commits
- Timeline: 2026-09-15 → 2026-09-18 (4 days)
- Files changed: 115 total (+26,583 / −249); 38 files in `src/`, `.github/`, and build config (+5,742 / −117)
- Codebase: 15,179 LOC test Java, 6,850 LOC main Java
- Suite: 371 tests, 0 failures, 0 errors, 7 pre-existing skips
- Requirements: 75/75 v1 requirements complete, 0 unmapped
- CI: run [`35317272594`](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594) on `a295af8` — green

**Closeout type:** `override_closeout`

**Known verification overrides:** 0 newly acknowledged, 0 carried forward from a prior close. The pre-close artifact audit was clean across every category — no open debug sessions, threads, seeds, quick tasks, UAT gaps, verification gaps, context questions, or deferred items. The override is recorded for two other reasons, both accepted by the user at close:

1. **Phases 1–4 verification reads stale.** All five phase `VERIFICATION.md` files carry `status: passed`, but phases 1–4 fall outside their freshness window because later phases legitimately edited files those earlier verifications covered — e.g. Phase 1 verified `ClickHouseBase.java`, which Phase 3 then modified. This is ordinary sequential-phase drift, not a failed or reversed check. The milestone-level claim rests on Phase 5, which is freshly `passed` and independently re-confirmed the full suite green both locally and in CI.
2. **No milestone audit was run.** `/gsd-audit-milestone` was never invoked, so no `v1.2.9_gg4-tests-MILESTONE-AUDIT.md` exists in the archive. Accepted on the basis that all 75/75 requirements are checked off and mapped, and Phase 5 verified the end-to-end result directly.

**Known gaps:** none against v1 requirements. Nine review-surfaced follow-ups are carried into `PROJECT.md` → Requirements → Active; two affect production behavior (R-01 fail-fast, client-V2 error classification is dead code).

**Key accomplishments:**

- The untouched `custom_build` suite was already green against ClickHouse `latest` x client V2 (0 failures) once a sandbox environment artifact was neutralized; the default test image now points at `latest`, and a new server-version smoke test pins the container-image and version-string contract for future phases.
- One shared ClickHouse `latest` container per test JVM fork (down from one per class), a fresh isolated database per test class, isolated 23.8 proxy containers, an `@AfterEach` MBean leak sweep proven by a deliberate mid-test failure, and UTF-8 decoding in test helpers proven RED-then-GREEN — full suite green at 194 tests, 0 failed, 7 skipped.
- JaCoCo 0.8.15 wired to `test` only with a full report and a 15-class fork-changed report (Lombok-generated methods excluded via `lombok.config`), plus a Docker-free `testUnit` task that stays out of `check` — full suite still green at 194 tests, 0 failed, 7 skipped with the agent attached.
- All five TableOperationFilter unit-level requirements (TOF-01..05) pinned by 17 new named tests, plus the shared `TransformTestHelpers` builder class and a two-line `configure()` reconfigure fix (D-03).
- All six HeaderToValue unit-level requirements (HDR-01..06) pinned by 22 new named tests, plus a D-02 stale-schema-cache guard that closes a real mid-stream Debezium data-loss path.
- All eight HeadersToValue unit-level requirements (HDRS-01..08) pinned by 23 new named tests (25 total), plus the D-02 stale-schema-cache guard and a corrected HDRS-04 requirement wording.
- Fixed the D-01 nested-Struct-in-collection bug in `ConvertToMapOrJson.convertFieldValue()` and pinned all seven unit-level requirements (CONV-01..CONV-07) with 24 new named tests, taking the file from 13 to 37 tests.
- A new `SmtSinkLandingTest` class proves all four fork SMTs against ClickHouse `latest`/client V2 through the real insert path, and fixes a production RowBinary null-marker bug in `ClickHouseWriter`'s MAP branch that the HDRS-09 test tripped.
- Extracted `shouldSkipDescribeRow`/`buildTableFilterClause` from `ClickHouseHelperClient`, pinned FILT-01..04 and DESC-01..04 with 25 new Docker-free/ClickHouse-backed tests, and fixed the deferred Phase 1 non-ASCII DESCRIBE-reader bug.
- Pinned TYPE-01/02 type-string dispatch and TYPE-03/04/05/07 JSON+Dynamic round-trips across both fork insert routes (schemaless JSONEachRow, bypassRowBinary Struct records) with typed SQL read-back instead of row counts, plus non-ASCII values under the windows-1252 test JVM.
- RowBinary path now throws an actionable DataException (naming the column, its type, and the insertFormat=json/bypassRowBinary=true escape hatches) instead of silently corrupting the stream, pinned Docker-free at both internal gates and live through a full ClickHouseSinkTask.put() failure that leaves zero rows.
- Pinned the metadata-free `insertFormat=json` insert path (gate, table resolution, field cleanup, tombstone handling) with two new fork-only test classes, upgraded both pre-existing explicit-JSON tests to typed assertions, and resolved the R-01 silent-data-loss finding via a blocking-human checkpoint answered `pin-current-behavior`.
- Pinned every insert-time consequence of `jsonInsertColumns` (default list, explicit subset against non-trivial DEFAULTs, extra payload keys, a nonexistent column's non-retriable classification, and the non-explicit path's interaction with `cleanupExtraFields`) plus its Docker-free parsing contract, in two new fork-only test classes.
- Extracted `ProxySinkTask.buildMBeanName` byte-for-byte (the phase's one pre-approved main-code change) and pinned every JMX MBean naming/registration branch (JMX-01..04) plus the fork's `async_insert` pass-through (ASYNC-01..03) in four new fork-only test classes, closing out Phase 4's third and final plan.
- Rewrote the fork's push-triggered `tests.yaml` into a single ClickHouse `latest` x client V2 job on current GitHub Actions tags, backed by a new stdlib-only `jacoco_summary.py` that renders JaCoCo coverage into both the job summary and stdout with no coverage threshold.
- Pushed `custom_build` to the public fork after human approval and machine-verified a fully green `tests.yaml` run (run 35317272594) on the pushed SHA via the unauthenticated GitHub REST API — exactly one job, both key steps and both artifacts confirmed.

---
