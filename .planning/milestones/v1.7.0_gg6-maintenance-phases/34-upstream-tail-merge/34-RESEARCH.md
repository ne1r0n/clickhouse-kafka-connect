# Phase 34: Upstream Tail Merge - Research

**Researched:** 2026-10-28
**Domain:** Git upstream-sync recipe for a forked Kafka Connect sink (mechanical merge + re-expression commits + pin tests + anchor rows)
**Confidence:** HIGH (the whole merge was rehearsed end to end in a throwaway clone and the real gates were run on it)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md is "Auto-generated (discuss skipped via workflow.skip_discuss)". Its `## Decisions` section contains only the `### Claude's Discretion` block below.

### Claude's Discretion
All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

### Deferred Ideas (OUT OF SCOPE)
None — discuss phase skipped.

Phase boundary restated by CONTEXT.md: upstream's untagged tail `9bed6f79` (or whatever `upstream/main` reads immediately before the merge) is merged in the fork's sync shape while the client is still 0.9.6. This phase pushes nothing. Research flag: if `upstream/main` moved past `9bed6f79`, re-measure the conflict set; if upstream tagged a release past `v1.7.0`, STOP (tagged sync FUT-01, user decides scope).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| TAL-01 | Upstream `main` merged as one mechanical commit passing `verify-merge-invariants.sh`, with `MERGE-CHECKLIST-v1.7.0-tail-2.md` | Conflict set re-measured (5 paths, matches roadmap); per-hunk resolution rules; rehearsed merge passed the unedited gate (`provenance_paths=27 ... result=PASS`); recipe from tail-1 checklist (Section "Architecture Patterns") |
| TAL-02 | `ProxySinkTask` keeps fork `connectorName` ctor and upstream `DBWriter` seam ctor via one 4-arg package-private ctor | Exact shape rehearsed and compiled; why the mechanical merge leaves it fork-only and where the ambiguity would come from (Pitfall 1) |
| TAL-03 | Every fork caller/test of `Utils.handleException` uses the 5-arg form; compile gates pass after the `CreateTableStatement` refactor | Measured compile breaks: exactly 2 files / 5 javac errors (and none from `CreateTableStatement`); one hidden `testUnit` break (BLD-16) that no compile gate catches |
| TAL-04 | `additionalRetriableErrorCodes` pinned by tests (parse matrix on validator+constructor, 15-code default set incl. 159, default-empty no-op, DataException never retriable) | Upstream-vs-needed test matrix; the DataException semantics were probed and one case needs a decision (Open Question 3) |
| TAL-05 | New anchor rows for `ProxySinkTask` statistics/`connectorName` plumbing (ORCH-02) and the `DEFAULT_RETRIABLE_ERROR_CODES` lookup; anchors gate passes | Measured candidate rows with counts on the rehearsed final tree; authoring rules (same commit as the code, never inside M) |
</phase_requirements>

## STOP-condition check (done first, as instructed)

| Check | Result | Evidence |
|---|---|---|
| `git ls-remote upstream main` | `9bed6f7970670a55c66be8efda246889e1f95bd0 refs/heads/main` — NOT moved past `9bed6f79` | [VERIFIED: `git ls-remote upstream main`, run 2026-10-28; `git fetch upstream --tags` then `git rev-parse upstream/main` = `9bed6f7970670a55c66be8efda246889e1f95bd0`] |
| Upstream release tag past `v1.7.0` | NONE. Newest upstream tag is `refs/tags/v1.7.0` = `379b8d018c090ba5279c2a9f86dbfe4934a19329` | [VERIFIED: `git ls-remote --tags upstream | tail`] |
| `origin/custom_build` | `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, an ancestor of HEAD, `rev-list --left-right --count origin/custom_build...HEAD` = `0 59`; origin `main` = `bec167d8` | [VERIFIED: `git ls-remote origin refs/heads/custom_build refs/heads/main`] |

**No STOP condition. Planning proceeds.** The planner must still re-run both `ls-remote` commands immediately before the merge (success criterion 1) and compare to the SHA above; if upstream moved, re-measure with the merge-tree command below before continuing.

## Summary

The phase is a fork-recipe repeat. The merge is mechanical (5 conflicted paths, all resolved by "take a side / union" with zero authored lines), and everything the tail breaks is repaired in separate, small commits afterwards. I rehearsed the whole thing in a throwaway clone (`/tmp/gsd34probe/repo`, outside the repo; the real repo is untouched, `git status` shows only the two pre-existing dirty `.planning` files): mechanical merge, tail gate, compile gates, `testUnit`, and one full `--rerun` suite on `latest` × V2. The numbers in this document were measured on that rehearsal.

Three findings change the plan relative to the roadmap text: (1) `compileJava` passes on the bare mechanical merge and `compileTestJava` fails with exactly **5 javac errors in 2 files** (`AdditionalRetriableErrorCodesTest` ×3 for the `ProxySinkTask` constructor, `UtilsNullMessageClassificationUnitTest` ×2 on one line for `handleException`). (2) The roadmap's "about 20 test files must follow the `CreateTableStatement` refactor" needs **no fork-side edit**: upstream kept the old constructors as `@Deprecated`, and the tree contains zero `new CreateTableStatement(` outside the class itself, so those test files simply arrive already refactored. (3) A break that **no compile gate and no conflict marker catches**: `ConnectApiReferencesUnitTest` (BLD-16) fails in `testUnit` because the tail's `new ConfigException(name, o, message)` is a Kafka API member missing from the blessed list `src/test/resources/connect-api-references.txt`. It is safe to bless (the 3-arg constructor exists in the production worker's jars, 4.1.1 and 4.4.0, all verified) and is one added line.

After those repairs the rehearsal tree was green: `testUnit` 1418 tests / 0 failures / 0 errors / 2 skipped (6 s), full suite `test --rerun` on `latest` × V2: 110 XML files, 2005 tests, 0 failures, 0 errors, 18 skipped, BUILD SUCCESSFUL in 2m 42s, no JVM crash. Baseline at the sweep tip was 1385 and 1972, so the tail adds 33 tests.

**Primary recommendation:** Run the Phase 25 "rehearse-then-reproduce" recipe: tag `pre-v1.7.0-tail-2` on the pre-merge tip, one mechanical merge commit M3 (resolutions in the table below), a record commit (conflicted-paths file + `MERGE-CHECKLIST-v1.7.0-tail-2.md`), then separate commits for (a) the `ProxySinkTask` dual-constructor shape plus its anchor rows, (b) the 5-arg `handleException` callers plus the `DEFAULT_RETRIABLE_ERROR_CODES` anchor row, (c) the BLD-16 re-bless, (d) the TAL-04 pin tests, (e) the gate evidence. Never put an authored line or an anchor edit inside M3.

## Architectural Responsibility Map

This phase is a source-merge and test phase inside one JVM sink connector; there is no browser/SSR/CDN tier. Tiers here are connector layers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Retriable-vs-fatal classification of a failed insert | `util.Utils.handleException` (pure static) | `ProxySinkTask.put` and `ChunkFlusher` (callers that supply the config's extra codes) | One decision function; both delivery paths must pass the same 5 arguments |
| `additionalRetriableErrorCodes` parse and validation | `ClickHouseSinkConfig` (`parseErrorCodes`, `ErrorCodeListValidator`, `CONFIG` ConfigDef) | Kafka Connect `ConfigDef.validate` (runs the validator on the parsed list) | Two entry paths (constructor string, ConfigDef list) must agree |
| JMX `connector=` plumbing | `ClickHouseSinkTask.start` reads `props.get("name")` | `ProxySinkTask` ctor, `SinkTaskStatistics(connectorName, id)` | Fork patch ORCH-02; the DBWriter seam must not drop it |
| `DBWriter` test seam | `ProxySinkTask` package-private ctor | `InMemoryDBWriter` and upstream's `FailOnceDBWriter` | Upstream test-only seam; the fork adds a second ctor beside it |
| Offset/exactly-once state machine (`Processing`, `KeeperStateProvider`) | untouched | — | The tail does not touch these high-risk files |
| Merge provenance / anchor invariants | `.planning/scripts/verify-merge-invariants.sh` | `fork-anchors.txt`, `conflicted-paths-*.txt` | Git-level gate; no runtime tier |

## Standard Stack

No new library, version bump or build-file change comes with this phase. The merge brings no `build.gradle.kts` or `gradle/libs.versions.toml` change (`git diff --name-status a6f1df83 9bed6f79` lists none). [VERIFIED: that command, run this session]

### Core (unchanged)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| ClickHouse Java client | 0.9.6 (stays until Phase 36) | Driver | Phase goal: every compile break must be attributable to the tail, so the client must not move [CITED: ROADMAP Phase 34 goal] |
| Kafka Connect API | compiled against 4.4.0, worker supplies it at runtime | `ConfigException`, `RetriableException`, `DataException` | Existing fork override (`kafka = "4.4.0"` anchor) |
| JUnit 5 + Mockito | existing | Tests | Fork convention (CLAUDE.md) |

### Tooling used (no install)
| Tool | Purpose |
|------|---------|
| `git merge-tree --write-tree --name-only` | Conflict-set measurement and gate cross-check |
| `.planning/scripts/verify-merge-invariants.sh` (+ `-selftest.sh`) | Provenance, marker, anchor invariants |
| `.planning/scripts/connect-api-refs.py` | BLD-16 re-bless (stdlib + JDK `javap`) |

**Installation:** none. **Package Legitimacy Audit:** not applicable — this phase installs no external package. Packages removed due to SLOP verdict: none. Packages flagged SUS: none.

## Architecture Patterns

### System Architecture Diagram

```
 upstream/main 9bed6f79 ──► (1) pre-flight record ──► ls-remote x2, fetch, merge-tree (exit 1, 5 paths)
                                     │
 sweep tip (HEAD) ──tag──► pre-v1.7.0-tail-2 (local, never pushed; = M3 first parent)
                                     │
                       (2) git merge --no-ff --no-commit 9bed6f79...
                                     │  5 conflicted paths, resolved by side/union only
                                     ▼
                       M3  (one mechanical commit; ProxySinkTask = fork side; no .planning file inside)
                                     │  tail gate: PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=<full sha>
                                     ▼
              (3) record commit: conflicted-paths-v1.7.0-tail-2.txt + MERGE-CHECKLIST-v1.7.0-tail-2.md
                                     ▼
   state after M3: compileJava OK | compileTestJava 5 errors | testUnit 1 red (BLD-16)
                                     ▼
 (4a) ProxySinkTask 3 ctors + anchors ─► (4b) 5-arg handleException callers + anchor ─► (4c) BLD-16 re-bless
                                     ▼
 (4d) TAL-04 pin tests (Docker-free) ─► (5) gate: cache-busted testUnit, 2x test --rerun, anchors, tail gate at tip
                                     ▼
                      tip = baseline for Phases 35-37 (BUF edits ChunkFlusher on this shape)
```

### Measured conflict set (re-run this session)

`git merge-tree --write-tree --name-only HEAD 9bed6f7970670a55c66be8efda246889e1f95bd0` exits 1 and lists exactly [VERIFIED: command run 2026-10-28; result tree `9bc56d2e17429b501284e63d291326a1017b20ce`]:

```
CHANGELOG.md
src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
src/main/java/com/clickhouse/kafka/connect/util/Utils.java
src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java
src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
```

The same five paths result against `origin/custom_build` (`39c25317a8c0...`). The merge base is `a6f1df83` (the previous tail). Upstream changed 27 paths since then [VERIFIED: `git diff --name-status a6f1df83 9bed6f79`]: `CHANGELOG.md`, `ChunkFlusher.java`, `ClickHouseSinkConfig.java`, `ProxySinkTask.java`, `Utils.java`, 3 integration tests, 2 new tests (`AdditionalRetriableErrorCodesTest`, `ClickHouseSinkConfigTest`), 14 other test files, 1 new fixture (`ClickHouseTableEngine`) and `CreateTableStatement`. `ClickHouseSinkConfig.java` (a high-risk file) and `ChunkFlusher.java` auto-merge with no marker.

### Resolution rules per conflicted path (all line-provenance safe)

Invariant 1 requires every line of a changed path to exist in P1 or P2 (set membership). Every rule below satisfies it. The probe merge used exactly these rules and the unedited gate passed.

| Path | Hunks | Rule | Detail |
|---|---|---|---|
| `CHANGELOG.md` | 1 | union, fork lines first | HEAD side = the fork's whole section stack (`# 1.7.0_gg5` ... through the last gg bullets, lines 2-345); upstream side = the new `# Unreleased` block (10 lines). Keep both. Plain union glues `# Unreleased` directly under the last fork bullet; a blank line before it is allowed (blank lines exist in both parents) and is recommended. RLS-01 (Phase 38) absorbs the `Unreleased` text later, so do not rewrite it now. |
| `src/main/.../sink/ProxySinkTask.java` | 2 | fork (ours) side in both hunks | Roadmap note: "The mechanical merge takes the fork side of ProxySinkTask". Hunk 1 is the constructor signature, hunk 2 the `statistics/dbWriter/processing` lines. Upstream's non-conflicting hunk (the 5-arg `Utils.handleException(... getAdditionalRetriableErrorCodes(), records)` call) auto-merges in. Result: main compiles; the dual-ctor shape is NOT present yet. |
| `src/main/.../util/Utils.java` | 1 (imports only) | union | The fork's import block was sorted by Spotless (java.* first, org.* after); upstream added `import java.util.Set;` in its old position. Keep the fork block and the upstream `import java.util.Set;` line (it lands after the SLF4J imports; legal Java, cosmetic only). The rest of upstream's `Utils` change (the `DEFAULT_RETRIABLE_ERROR_CODES` set, the 5-arg signature, the `contains` lookup) auto-merges and sits next to the fork's null-message guard without conflict. |
| `src/test/.../ClickHouseSinkTaskMappingTest.java` | 1 (imports) | HEAD import lines + upstream's `import java.util.function.Supplier;` | Drop upstream's duplicate blank line and its duplicate static `assertEquals` import. The fork-added `tableFilterTest` uses raw SQL, not `CreateTableStatement`, so it is unaffected by the `Supplier` template change. |
| `src/test/.../db/ClickHouseWriterTest.java` | 1 (imports) | HEAD side (empty) + upstream's `import java.util.function.Supplier;` | The fork has already moved all other imports to the top block. |

If any listed line is spelled differently on the day of the merge, the planner should regenerate the hunk list with `git merge-tree` rather than trust this table.

### Recommended commit sequence (all on `sync/v1.7.0`, nothing pushed)

| # | Commit | Contents | Gate at that commit |
|---|---|---|---|
| 0 | (no commit) | Record `git ls-remote upstream main`, `git ls-remote --tags upstream`, `git fetch upstream --tags`, the `merge-tree` output; `git tag pre-v1.7.0-tail-2 HEAD` (local only) | tag must equal the commit that becomes M3's first parent |
| 1 | M3 `Merge upstream tail 9bed6f79 (...)` | `git merge --no-ff --no-commit 9bed6f7970670a55c66be8efda246889e1f95bd0`, resolve 5 paths per the table, `git commit` (message pattern of `38265d26`, below) | tail gate PASS; expect `provenance_paths=27` |
| 2 | record | `.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt` (5 sorted lines) + `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md`; Recorded Facts continue at **44** | tail gate re-run with `CONFLICTED_PATHS_FILE=...-tail-2.txt` |
| 3 | `fix(34)` TAL-02 | `ProxySinkTask` three constructors + new `ProxySinkTask` anchor rows (same commit) | `compileJava`; anchors |
| 4 | `fix(34)` TAL-03 | `UtilsNullMessageClassificationUnitTest` to the 5-arg form + `DEFAULT_RETRIABLE_ERROR_CODES` anchor row (same commit) | `compileJava compileTestJava compileIntegrationTestJava`; anchors |
| 5 | `test(34)` BLD-16 | re-bless `src/test/resources/connect-api-references.txt` | `testUnit` green |
| 6 | `test(34)` TAL-04 | the Docker-free pin tests | cache-busted `testUnit` |
| 7 | docs | evidence file, ROADMAP/STATE | phase gate |

M3 pattern to copy (`git log -1 38265d26`): subject `Merge upstream tail a6f1df83 (tagless mini-sync after v1.7.0) (SYNC-11)`, body lists the conflicts exactly and states "The conflicted-paths record and the resolution checklist follow in the next commit". [VERIFIED: `git log -1 --format='%H%n%s%n%b' 38265d26`] Trailer for this session's commits: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` (system attribution reminder).

**Why the record is a separate commit (tail-1 research correction X-3):** a new `.planning` file inside the merge commit fails the provenance invariant. Also no `.planning` commit may sit between `git merge --no-commit` and M3 (tail checklist rule 2). The dirty `.planning/config.json` and `.planning/state.json` are untouched by upstream and do not block `git merge`; Phase 33 ran with them dirty throughout and never committed them. [CITED: `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` rules 1-3; 33-COMMAND-EVIDENCE E-14]

### Gate invocation (unedited script)

The tail-1 standing command, with this phase's values [CITED: `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` "Gate commands"; script defaults read at `verify-merge-invariants.sh:1-34`]:

```bash
PRE_TAG=pre-v1.7.0-tail-2 \
UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 \
CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt \
.planning/scripts/verify-merge-invariants.sh <M3-full-sha> HEAD
```

- `UPSTREAM_REF` is the full SHA, never `upstream/main` (invariant 0 compares the second parent to it).
- `PRE_TAG` must be an ancestor of M3's first parent (invariant 0). Tag the exact pre-merge tip.
- Invariant 1 reads `CONFLICTED_PATHS_FILE` from the **range-end** tree, so it only passes after the record commit exists. Run it at M3 with `HEAD` = the record commit.
- Invariant 3 evaluates every commit in `PRE_TAG..HEAD` against that commit's own copy of `fork-anchors.txt` (`verify-merge-invariants.sh` anchors loop). Consequence: an anchor row may only be added in the same commit as, or after, the code it pins; a row added one commit early fails on that commit.
- Rehearsal result with these rules (probe clone, gate run on the merge commit): `invariants: provenance_paths=27 anchor_commits=1 anchor_rows=131 marker_commits=8 anchor_exempt=0 result=PASS`. [VERIFIED: run this session] In the real repo `anchor_commits` and `marker_commits` will be larger (they count commits in range); `provenance_paths=27` and `anchor_rows=131` (before the new rows) and `anchor_exempt=0` are the stable parts.
- Anchor exemptions (`fork-anchor-exemptions.txt`) are SHA-pinned rows for already-published commits only (format: `40-hex SHA TAB path TAB fixed-string TAB reason`). Nothing in this phase needs one: `origin/custom_build` is an ancestor of HEAD and no Dependabot commit is unmerged. Only add a row if `git fetch origin` shows a new published bump of an anchored line (memory note `dependabot-merges-on-custom-build`).
- The cumulative default-environment gate `verify-merge-invariants.sh 54e63648 HEAD` took most of Phase 33's 52 minutes (999 anchor commits) and is run in the background there. Self-test `verify-merge-invariants-selftest.sh` takes about 13 minutes (Phase 25 SUMMARY). Run the cumulative gate once at the phase tip in the background; for the self-test, prove the two scripts are unedited with `git diff --quiet pre-v1.7.0-tail-2 HEAD -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh` instead of re-running it (planner's call; Phase 33 re-ran it).

### Pattern 1: `ProxySinkTask` — two public-facing constructors, one 4-arg package-private core (TAL-02)

**What:** The fork's constructor is `(ClickHouseSinkConfig, ErrorReporter, String connectorName)`; upstream's test seam is `(ClickHouseSinkConfig, ErrorReporter, DBWriter)` (package-private) with a public 2-arg `(config, reporter)` that calls `this(config, reporter, null)`. Both 3-arg forms differ only in the third parameter's type, so any `null` third argument is ambiguous, and upstream's own `this(cfg, rep, null)` would stop compiling if both were present.

**Rehearsed shape (compiled and fully tested in the probe):**

```java
// Source: rehearsal in /tmp/gsd34probe/repo (compiled; testUnit 1418/0/0/2 and full suite 2005/0/0/18 green)
public ProxySinkTask(final ClickHouseSinkConfig clickHouseSinkConfig, final ErrorReporter errorReporter, final String connectorName) {
    this(clickHouseSinkConfig, errorReporter, connectorName, null);
}

ProxySinkTask(final ClickHouseSinkConfig clickHouseSinkConfig, final ErrorReporter errorReporter, final DBWriter dbWriter) {
    this(clickHouseSinkConfig, errorReporter, null, dbWriter);
}

ProxySinkTask(final ClickHouseSinkConfig clickHouseSinkConfig, final ErrorReporter errorReporter, final String connectorName, final DBWriter dbWriter) {
    this.clickHouseSinkConfig = clickHouseSinkConfig;
    this.errorReporter = errorReporter;
    /* ... existing exactlyOnce / stateProvider block unchanged ... */
    this.statistics = new SinkTaskStatistics(connectorName, id);
    this.dbWriter = dbWriter != null ? dbWriter : new ClickHouseWriter(this.statistics);
    processing = new Processing(stateProvider, this.dbWriter, errorReporter, clickHouseSinkConfig, statistics);
}
```

Why it is unambiguous: the 4-arg constructor is the only 4-arg one, so `this(..., connectorName, null)` and `this(..., null, dbWriter)` each have one candidate; both 3-arg constructors are only ever called with a typed expression (`String`, or `FailOnceDBWriter`/`DBWriter`). Upstream's 2-arg public constructor is dropped: nothing calls it (the fork's `ClickHouseSinkTask.start` calls the 3-arg String form; upstream's only test callers are `new ProxySinkTask(config, null, writer)` with a typed writer). [VERIFIED: `grep -rn "new ProxySinkTask" src` in the tree and `git grep` at `9bed6f79`]

**Hard constraints from the repo:**
- Keep the private field names `processing` and `dbWriter` and their types: `ScriptedWriteFailureWriter.installInto` reflects on `ProxySinkTask.processing` and `Processing.dbWriter` and throws `IllegalStateException` if they change. [VERIFIED: `src/test/java/.../db/ScriptedWriteFailureWriter.java:40-45,108-120`]
- Keep `new SinkTaskStatistics(connectorName, id)` (the only `SinkTaskStatistics` constructor is `(String connectorName, int taskId)`; there is no `(int)` form, so upstream's `new SinkTaskStatistics(id)` cannot compile and must not come back). [VERIFIED: `grep -n "public SinkTaskStatistics" SinkTaskStatistics.java` → line 34 only]
- A null connector name (the seam constructor) renders `connector=null` in the MBean name; this is pinned today (`MBeanNameUnitTest.nullConnectorNameTopicNameInheritsNullHandling`: `assertTrue(statistics.getMBeanName().contains("connector=null"))`). Acceptable for a test seam.
- Style: this file's imports were Spotless-sorted by the fork; keep the Google Java Style fix per-file (`spotlessApply` must leave the touched file byte-identical; the repo baseline is 46 pre-existing violating files, so the check is per-file, not `spotlessCheck` clean).

**Tests that pin TAL-02 (Docker-free, new fork file, extends nothing so it stays in `testUnit`):**
1. Reflection: `ProxySinkTask.class.getDeclaredConstructors()` has exactly 3 entries, parameter types exactly `(ClickHouseSinkConfig, ErrorReporter, String)` [public], `(ClickHouseSinkConfig, ErrorReporter, DBWriter)` [package-private], `(ClickHouseSinkConfig, ErrorReporter, String, DBWriter)` [package-private]; no 2-arg constructor.
2. Behaviour, ORCH-02 plumbing through the real constructor: `new ProxySinkTask(cfg, null, "my-conn", new InMemoryDBWriter())`, then `start()` registers an MBean whose `ObjectName` contains `connector=my-conn`, and `stop()` unregisters it (`InMemoryDBWriter.start` returns true). The existing `MBeanRegistrationLiveTest` extends `ClickHouseBase` (Docker); this new test makes the same plumbing Docker-free through the new seam. Do not edit or delete the existing live test.
3. Seam behaviour: the DBWriter constructor uses the supplied writer (insert reaches `InMemoryDBWriter.recordsInserted()`), the String constructor builds a `ClickHouseWriter`.

### Pattern 2: handleException 5-arg form (TAL-03)

Signature after the merge: `handleException(Exception e, boolean errorsTolerance, boolean retryOnSocketException, Set<Integer> additionalRetriableErrorCodes, Collection<SinkRecord> records)`. [VERIFIED: merge-tree result tree `9bc56d2e...:Utils.java` line 91]

All callers (grep over `src` this session): `ProxySinkTask.java:100` and `ChunkFlusher.java:72` (both already 5-arg after the auto-merge), the `UtilsTest` calls (upstream changes them to 5-arg), and the **one stale caller**: `src/test/java/.../util/UtilsNullMessageClassificationUnitTest.java:32` (`Utils.handleException(e, errorsTolerance, NO_SOCKET_RETRY, new ArrayList<>())`). That file auto-merges untouched (upstream never edited it), which is why only compilation catches it. Fix: insert `Collections.emptySet()` (or `Set.of()`) before the record list. Do not pass `null`: the new code dereferences the set without a guard (see Pitfall 4).

### Pattern 3: BLD-16 re-bless (hidden `testUnit` break)

`ClickHouseSinkConfig.ErrorCodeListValidator.ensureValid` calls `new ConfigException(name, o, e.getMessage())`. That is `ConfigException.<init>:(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)V`, absent from the blessed list. [VERIFIED: probe `testUnit` run: `ConnectApiReferencesUnitTest > BLD-16 ... FAILED ... M org/apache/kafka/common/config/ConfigException.<init>:(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)V`]

The constructor exists in all three reference jar sets, so blessing is correct and safe (verified by `javap` this session): worker `kafka-clients-7.9.7-ccs.jar` (sha256 `28423086...f962`, extracted from the local image `confluentinc/cp-kafka-connect-base:7.9.7` = `93b9afd3e7f8`), `kafka-clients-4.1.1.jar` and `kafka-clients-4.4.0.jar`; each printed `public org.apache.kafka.common.config.ConfigException(java.lang.String, java.lang.Object, java.lang.String);`. The jar hashes match the header already in the blessed file.

Re-bless command (rehearsed, exit 0, `REFERENCE worker/4.1.1/4.4.0 classes-missing 0 members-missing 0`, `derived classes 39 members 138 total 177`). Run after `compileJava`; extract the worker jars from the image first into a **new empty directory** (`docker create` + `docker cp` of `/usr/share/java/kafka/connect-api-7.9.7-ccs.jar` and `kafka-clients-7.9.7-ccs.jar`; Docker is colima profile `vz`, `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock`). The earlier `/tmp/gsd25/img/native` directory is empty and cannot be reused.

```bash
python3 -I .planning/scripts/connect-api-refs.py --classes build/classes/java/main \
  --compile-classpath <4.4.0 connect-api jar>:<4.4.0 kafka-clients jar> \
  --reference worker=<img>/connect-api-7.9.7-ccs.jar:<img>/kafka-clients-7.9.7-ccs.jar \
  --reference 4.1.1=<4.1.1 connect-api>:<4.1.1 kafka-clients> \
  --reference 4.4.0=<4.4.0 connect-api>:<4.4.0 kafka-clients> \
  --image-note "image: confluentinc/cp-kafka-connect-base@sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055 (tag 7.9.7; linux/amd64 and linux/arm64 jars byte-identical; Temurin 17.0.19)" \
  --write src/test/resources/connect-api-references.txt
```

Gotcha: without `--image-note` the script drops the `# image:` header line, and the script writes `# <text>` so the note text must itself begin with `image: ` to reproduce the original line. Expected diff after doing it right: +1 `M ...ConfigException.<init>:(String,Object,String)V` line and a new `# date:` line (the `# date:` change is expected). Jar paths in the Gradle cache: `~/.gradle/caches/modules-2/files-2.1/org.apache.kafka/{connect-api,kafka-clients}/{4.1.1,4.4.0}/<hash>/...`.

### Anti-Patterns to Avoid
- **Editing anchors or authoring any line inside M3.** Gate invariant 1 and the roadmap note forbid it; the record file goes in the next commit.
- **Resolving `ProxySinkTask` by taking upstream's side.** That silently drops `connectorName` and the ORCH-02 plumbing (and `SinkTaskStatistics(int)` does not exist). Take the fork side, then re-express in a separate commit.
- **Calling `new ProxySinkTask(cfg, rep, null)` anywhere.** Ambiguous between `String` and `DBWriter`; always pass a typed expression.
- **Weakening or deleting existing tests.** AGENTS.md: only add. Upstream-owned test files stay byte-identical to upstream (`UtilsNullMessageClassificationUnitTest` documents this: `UtilsTest` "should not churn on a sync"); put new pins in fork-owned files.
- **Migrating fork tests off deprecated `CreateTableStatement` constructors.** There are none left to migrate (0 matches), and it would only add sync-conflict surface.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Conflict-set / provenance checking | A new script | `verify-merge-invariants.sh` (unedited) + `git merge-tree --write-tree --name-only` | Phase 25 research: the script's own derivation is the cross-check; editing it voids the "unedited gate" criterion |
| Re-blessing the Kafka API list | Hand-editing `connect-api-references.txt` | `.planning/scripts/connect-api-refs.py --write` | The script only writes when every reference reports 0 missing, and the sorted/unique/header rules are tested by `ConnectApiReferencesUnitTest` |
| Test suite result tally | Ad hoc greps | Sum the JUnit XML under `build/test-results/test` (and `testUnit`), the method of 33-COMMAND-EVIDENCE E-02/E-14 | Gradle's console summary omits parameterized-case skips |
| Anchor-count verification between gate runs | Re-deriving counts by eye | `grep -cF -e "<pattern>" <path>` (what the gate runs) and the `anchors.sh` mirror loop (inline in "Code Examples") | Same semantics as the gate: counts matching LINES |
| Config parsing of the code list | New parser | Upstream's `ClickHouseSinkConfig.parseErrorCodes` + `ErrorCodeListValidator` as merged | Upstream owns it; the fork only pins it |

**Key insight:** every artifact this phase needs already exists as a script or an upstream implementation; the work is sequencing and pinning, not building.

## Common Pitfalls

### Pitfall 1: Ambiguous `null` / missing constructor after a "fork-side" `ProxySinkTask` resolution
**What goes wrong:** M3 compiles main (fork constructor only), but upstream's `AdditionalRetriableErrorCodesTest` calls `new ProxySinkTask(config, null, writer)` with a `DBWriter`; javac reports "incompatible types: FailOnceDBWriter cannot be converted to String" ×3. Naively adding upstream's constructor beside the fork's reintroduces `this(cfg, rep, null)` ambiguity.
**Why:** same arity, different third type; upstream never knew about `connectorName`.
**How to avoid:** the 3-constructor shape in Pattern 1; pin it with the reflection test.
**Warning signs:** `reference to ProxySinkTask is ambiguous`; any `, null)` third argument.

### Pitfall 2: Breaks that compile gates do not see
**What goes wrong:** `UtilsNullMessageClassificationUnitTest` merges without a marker with a stale 4-arg call (compile error only); BLD-16 fails at runtime of `testUnit`, not compile. [VERIFIED: probe]
**How to avoid:** the phase gate must run `compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava` **and** a cache-busted `testUnit`, not just conflict-marker greps. After M3 the expected red set is exactly: 5 javac errors (2 files) and, once those are fixed, BLD-16 (1 test). Anything else red is a new finding.

### Pitfall 3: The roadmap's "~20 test files must follow `CreateTableStatement`" is not fork work
**What goes wrong:** a plan spends tasks migrating tests that already arrive migrated.
**Facts:** upstream's refactor adds `CreateTableStatement.create()`, a default `MergeTree` engine in the constructor, an `engine(ClickHouseTableEngine)` overload and the `ClickHouseTableEngine` enum, and marks `CreateTableStatement()` and the copy-constructor `@Deprecated` — it does not remove them. [VERIFIED: `git diff a6f1df83 9bed6f79 -- src/testFixtures`] Upstream touched 22 test-tree files (20 modified + 2 new tests) of which only `ClickHouseSinkTaskMappingTest` and `ClickHouseWriterTest` conflicted, both on import lines. `grep -rl "new CreateTableStatement(" src` after the merge matches only `CreateTableStatement.java` itself. `compileTestJava` and `compileIntegrationTestJava` pass once the 5 javac errors are fixed. [VERIFIED: probe, `BUILD SUCCESSFUL`]
**How to avoid:** TAL-03's second half is a verification (compile gates), not an edit task.

### Pitfall 4: `handleException` does not null-guard `additionalRetriableErrorCodes`
**What goes wrong:** passing `null` (or a Mockito mock config whose getter returns `null`) throws `NullPointerException`, but only on the server-error path.
**Evidence:** probe: `Utils.handleException(new RuntimeException(new ServerException(60, "m", 500)), false, false, null, list)` → `NullPointerException`. `ChunkFlusher` guards a null *config*; `ProxySinkTask` relies on `parseErrorCodes` always returning a (possibly empty) set. [VERIFIED: probe run + source]
**How to avoid:** tests pass `Collections.emptySet()`; do not "fix" this in Phase 34 (it is upstream code; changing it is outside TAL scope and adds sync surface). Mention it as a documented sharp edge only.

### Pitfall 5: DataException wrapping a retriable server error is retriable (pre-existing)
**What goes wrong:** a test asserting "a DataException is never retriable" for `new DataException("x", new ServerException(216,...))` with `Set.of(216)` fails, because the server-error check precedes the DataException check.
**Evidence (probe):**
```
plain DataException, tolerance=false, add{216}        -> DataException (rethrown)
plain DataException, tolerance=true,  add{216}        -> returns normally
DataException(cause=ServerException 216), add{216}    -> RetriableException
DataException(cause=ServerException 252), add{}       -> RetriableException   (default code, pre-merge behaviour)
```
[VERIFIED: throwaway probe test run in the clone, deleted after]
**How to avoid:** pin the plain cases (the intent of TAL-04) and do NOT pin the wrapped case as "non-retriable". See Open Question 3.

### Pitfall 6: Tail-2 gate needs the record commit; the tail checklist order matters
Running the gate with `CONFLICTED_PATHS_FILE` before the record commit exists prints `CONFLICTED-PATHS MISMATCH ... is missing at HEAD`. Run it after commit 2. Also `PRE_TAG` created after M3 is not an ancestor of M3's first parent and fails invariant 0.

### Pitfall 7: Operational traps from memory notes (this repo)
- `gsd-dispatch-isolation-sentinel-clobber`, `gsd-phase25-orchestration-gotchas`: GSD verbs use the space form; do not query `dispatch-isolation --json` after `--force-isolation none`.
- `zsh` expands `$T:src/...` as a history modifier: write `${T}:path`. (Hit twice this session.)
- `openj9-mockito-test-jvm-crash`: `./gradlew test` exit 255 is an intermittent OpenJ9 GPF in Mockito inline mocking. Delete the `core.*.dmp` (~8 GB), `javacore.*`, `Snap.*.trc`, `jitdump.*` one by one (never `git clean`), re-run once, record both runs; escape hatch `-PjavaVersion=21`. No crash occurred in the probe's full run.
- `colima-vz-profile`: Docker is profile `vz` (verified Running, 8 CPU / 16 GiB); never bare `colima start`.
- `gsd-phase11-orchestration-gotchas`: ClickHouse `latest` drifts; a red full suite on unchanged code means check the image date first. `latest` is currently 26.10.3.7, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (same as Phase 33's gate, the local image is unchanged) [VERIFIED: `docker images` + 33-COMMAND-EVIDENCE E-14].
- `dependabot-merges-on-custom-build`: `git fetch origin` and compare before the gate; none pending now.
- Do not push anything, do not push `pre-v1.7.0-tail-2`.

## Test strategy for TAL-04: upstream vs needed

Upstream's tail already ships tests; the fork must keep them byte-identical and add only what is missing. [VERIFIED: read `AdditionalRetriableErrorCodesTest`, `ClickHouseSinkConfigTest`, `UtilsTest` diff at `9bed6f79`]

| TAL-04 element | Upstream coverage | Gap the fork must add (new fork-owned, Docker-free file, extends nothing) |
|---|---|---|
| trim | `ClickHouseSinkConfigTest.additionalRetriableErrorCodesParsed`: `" 216 , 216 , "` → `{216}` (constructor path only) | validator path with whitespace/empty items (`Arrays.asList("216"," ","999")`); the real `ClickHouseSinkConfig.CONFIG.parse(props)` / `.validate(props)` wiring (`CONFIG` is `public static final ConfigDef`, line 402) with `"216, 999"` and `"216,,999"` |
| de-duplicate | same test, `{216}` from `"216 , 216"` | `Set` result equality plus unmodifiability of `getAdditionalRetriableErrorCodes()` (`parseErrorCodes` returns `Collections.unmodifiableSet(TreeSet)`) |
| reject non-integers and values <= 0 | `additionalRetriableErrorCodesInvalid` over `abc, 21.6, -1, 0, 99999999999`, asserting `IllegalArgumentException` from the constructor and `ConfigException` from `ErrorCodeListValidator.ensureValid` | `null`/`"null"` input to the validator; the ConfigDef-level rejection through `CONFIG.parse` (confirms the validator is actually registered on the key with default `""`); error message names the key. Note the two paths throw different exception types by design (IAE vs `ConfigException`); pin both, do not unify |
| default set is exactly 15 codes incl. 159 | `UtilsTest.TestDefaultCodesStillRetried` is parameterized **over the set itself**, so it cannot detect an added/removed code | literal oracle: `assertEquals(Set.of(3,107,159,164,202,203,209,210,241,242,252,285,319,425,999), Utils.DEFAULT_RETRIABLE_ERROR_CODES)`, `assertEquals(15, size())`, `contains(159)`, and `add(...)` throws `UnsupportedOperationException` (it is `Set.of`) |
| key unset → classification unchanged | `UtilsTest` default-code and unconfigured-code cases; `AdditionalRetriableErrorCodesTest.unconfiguredCodeFailsTask` (216 stays fatal) | sweep: for every code 0..1100, both `ServerException(code, ...)` (V2) and `ClickHouseException(code, ...)` (V1), `handleException(..., Set.of(), ...)` is `RetriableException` iff code is in the 15-code literal (the pre-merge `switch`); and `new ClickHouseSinkConfig(Map.of())` yields an empty set |
| DataException never retriable | none | plain `DataException` under `Set.of(...)` including a default code and 216: rethrown as `DataException` with tolerance `false`, returns with tolerance `true`, never `RetriableException`; plus the wrapped-case decision (Open Question 3). A DBWriter-seam end-to-end variant through `ProxySinkTask.put` with a writer that throws `DataException` is possible and also exercises TAL-02 |

Fork regression harness that must stay green and unedited: `WriterFailureClassificationUnitTest` (WRT-14, outcome matrix through `ProxySinkTask.put` and `ChunkFlusher.putDirect`, no ClickHouse server errors in its matrix) and `UtilsNullMessageClassificationUnitTest` (only its one call changes).

**Counts to carry into evidence:** `AdditionalRetriableErrorCodesTest` 3 tests, `ClickHouseSinkConfigTest` 8, `UtilsTest` +20 (5 plain + 15 parameterized); sweep tip 1385 → rehearsal 1418 (`testUnit`) and 1972 → 2005 (full). New TAL pin tests add on top; recompute, do not reuse these totals.

## Code Examples

### Re-measure the conflict set (record this output)
```bash
git ls-remote upstream main
git ls-remote --tags upstream | tail -3
git fetch upstream --tags
git merge-tree --write-tree --name-only HEAD 9bed6f7970670a55c66be8efda246889e1f95bd0 > /tmp/mt34.txt 2>&1; echo "exit=$?"   # expect exit=1
awk 'NR==1{next} /^$/{exit} {print}' /tmp/mt34.txt | LC_ALL=C sort -u                                                      # the 5 paths
```
Source: this session; same derivation the gate uses (`verify-merge-invariants.sh` lines "Line 1 is the result tree OID").

### Anchors mirror (same semantics as gate invariant 3, working-tree version)
```bash
#!/bin/bash
bad=0; rows=0
while IFS=$'\t' read -r p pat cnt id; do
  case "$p" in ''|'#'*) continue;; esac
  rows=$((rows+1)); got=$(grep -cF -e "$pat" "$p" 2>/dev/null)
  [ "${got:-0}" = "$cnt" ] || { echo "ANCHOR MOVED $p [$pat] expected $cnt got ${got:-0}"; bad=$((bad+1)); }
done < .planning/scripts/fork-anchors.txt
echo "rows=$rows bad=$bad"
```
Source: `/tmp/gsd33/exec/anchors.sh` (Phase 33 gate). It is a throwaway script that may not survive; re-create it outside the repo.

### Candidate anchor rows (TAL-05), measured on the rehearsed final tree
Format: `path TAB fixed-string TAB expected-count TAB backs-ID`, appended under a new comment line `# Phase 34 (v1.7.0_gg6): ...`. Counts below are `grep -cF -e` line counts on the rehearsed tree (probe `ProxySinkTask`/`Utils`/`ChunkFlusher`/`ClickHouseSinkConfig`). Re-measure on the real tip before committing.

| Path | Pattern | Count | backs-ID | Pins |
|---|---|---|---|---|
| `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` | `new SinkTaskStatistics(connectorName, id)` | 1 | ORCH-02 | the statistics plumbing |
| same | `new SinkTaskStatistics(id)` | 0 | ORCH-02 | upstream's connector-less call stays gone |
| same | `final String connectorName` | 2 | ORCH-02 | public 3-arg and package-private 4-arg constructors both carry it |
| same | `this(clickHouseSinkConfig, errorReporter, connectorName, null);` | 1 | TAL-02 | String ctor delegates to the core |
| same | `this(clickHouseSinkConfig, errorReporter, null, dbWriter);` | 1 | TAL-02 | seam ctor delegates to the core |
| same | `dbWriter != null ? dbWriter : new ClickHouseWriter(this.statistics)` | 1 | TAL-02 | the seam |
| same | `getAdditionalRetriableErrorCodes()` | 1 | TAL-04 | `put` passes the config's extra codes |
| `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` | `DEFAULT_RETRIABLE_ERROR_CODES.contains(errorCode)` | 1 | TAL-05 | the lookup |
| same | `Set<Integer> additionalRetriableErrorCodes, Collection<SinkRecord> records` | 1 | TAL-03 | the 5-arg signature |
| same | `case 159` | 0 | TAL-05 | the pre-tail `switch` stays gone (optional, 0 on the tree) |
| `src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java` | `Utils.handleException(e, errorTolerance, retryOnSocketException, additionalRetriableErrorCodes, records)` | 1 | TAL-03 | buffered path passes the set (optional) |

Notes: `DEFAULT_RETRIABLE_ERROR_CODES` alone counts 2 (declaration + lookup), so use the `.contains(errorCode)` form. Avoid patterns that appear on a line the fork will edit again in Phase 35 (`ChunkFlusher` changes). The row for `ChunkFlusher` is therefore optional and the planner may skip it to leave Phase 35 room. Existing anchor `Utils.java | if (msg != null && (msg.indexOf( | 1 | WRT-11` and `SinkTaskStatistics.java | connector=%s | 1 | ORCH-02` still hold (gate PASS on the rehearsal, `anchor_rows=131`).

### Rehearse in a throwaway clone (what produced this research)
```bash
mkdir -p /tmp/gsd34probe && cd /tmp/gsd34probe && git clone -q --local <repo> repo && cd repo
git checkout -q <HEAD> && git fetch -q <repo> 9bed6f7970670a55c66be8efda246889e1f95bd0
git merge --no-commit --no-ff 9bed6f7970670a55c66be8efda246889e1f95bd0     # 5 conflicts
# resolve per the table, then: git add -A && git commit
```
The real merge reproduces the rehearsal's blobs (Phase 25 "rehearse-then-reproduce"), so the real tree is byte-equal to the rehearsed one.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Retriable codes as a hard-coded `switch` in `Utils.handleException` | `Utils.DEFAULT_RETRIABLE_ERROR_CODES` (`Set.of`, 15 codes) plus operator-extensible `additionalRetriableErrorCodes` | upstream PR #848, tail `9bed6f79` | Same default behaviour; a code listed by an operator throws `RetriableException`, so Connect redelivers with no retry budget and bypasses `errors.tolerance` and the DLQ |
| `new CreateTableStatement().engine("MergeTree")` templates | `CreateTableStatement.create()` + `Supplier<CreateTableStatement>` templates, `ClickHouseTableEngine` enum, default `MergeTree` | upstream PRs in the tail | Old constructors `@Deprecated`, not removed; no fork edit required |
| `ProxySinkTask(config, errorReporter)` only | adds a package-private `DBWriter` seam constructor | upstream tail | Fork must reconcile with its `connectorName` constructor (TAL-02) |

**Deprecated/outdated:** none in scope. Record for Phase 38 (FUT-05): a code in `additionalRetriableErrorCodes` raises `RetriableException`, which Connect redelivers without a budget and without DLQ/tolerance; the upstream CHANGELOG `Unreleased` entry says so too. Record for Phase 36: the tail adds `ServerException` 3-argument call sites that compile on 0.9.6 and must be revisited for 0.11.0. Measured count after merge: 8 in `UtilsTest` (lines 84, 93, 102, 112, 130, 139, 147, 162) **plus 1 in `AdditionalRetriableErrorCodesTest` (line 42)** = 9 sites in 2 files. The roadmap Note says "all eight"; the ninth is in the new `AdditionalRetriableErrorCodesTest` FailOnceDBWriter. [VERIFIED: `grep -n "new ServerException(" -r src` in the rehearsal tree]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The `anchor_commits` and `marker_commits` numbers in the real tail-gate run will exceed the probe's (1 and 8) simply because the real range holds the real history; only `provenance_paths=27`, `anchor_rows` (131 before new rows) and `anchor_exempt=0` are predicted exactly | Gate invocation | Low: planner should assert `result=PASS` and `provenance_paths=27`, not the other counts |
| A2 | Whether to skip the ~13-minute gate self-test in favour of an "unedited script" diff is the planner's call | Gate invocation | Low; Phase 33 re-ran it, Phase 25 proved "unchanged" |
| A3 | The optional `ChunkFlusher` anchor row and the `case 159` negative row are recommendations, not requirements | Candidate anchor rows | None for TAL-05 (the two required rows are the `ProxySinkTask` statistics/`connectorName` rows and the `DEFAULT_RETRIABLE_ERROR_CODES.contains` row) |

All other claims in this document were verified by command or file read in this session (tagged inline).

## Open Questions (RESOLVED)

1. **Has upstream moved past `9bed6f79` or tagged past `v1.7.0`?**
   - RESOLVED: No on both. `ls-remote upstream main` = `9bed6f79...`, newest upstream tag = `v1.7.0`. No STOP. The planner re-runs both commands immediately before the merge as a recorded step.

2. **Does the conflict set match the roadmap's expectation?**
   - RESOLVED: Exactly the five expected paths, against both `HEAD` and `origin/custom_build`.

3. **What does "a `DataException` is never reclassified as retriable" mean for a `DataException` that wraps a ClickHouse server error?**
   - What we know: probed behaviour — a plain `DataException` (no ClickHouse cause) is rethrown (tolerance `none`) or tolerated (`all`) under any additional-code set; a `DataException` whose cause is a `ServerException` with a retriable code (default or additional) is `RetriableException`, because `handleException` checks the root-cause server error before the `DataException` branch. This is pre-existing for the default codes, not introduced by the tail.
   - Resolution (planner default, within Claude's discretion since discuss was skipped): pin the plain `DataException` and a `DataException` with a non-ClickHouse cause; do not assert the wrapped-server-error case as non-retriable, and add an explicit comment in the test that the wrapped case is retriable by design of the existing root-cause-first rule. If the user wants the wrapped case to be non-retriable, that is a behaviour change to upstream code and belongs in a separate decision, not Phase 34. Marked RESOLVED as a planning default; **flag to the user in the plan summary** because it is a semantic interpretation of TAL-04.

4. **Who owns `docs/FORK-DELTA.md` and the `CHANGELOG.md` `Unreleased` text for this merge?**
   - RESOLVED: Phase 38. RLS-01 reads "with CHANGELOG (absorbing upstream `Unreleased`) and `docs/FORK-DELTA.md` updated", and BUF-07 owns the buffered-fix row. Phase 34 only unions the CHANGELOG hunk. The FORK-DELTA rows that name `Utils.java` as differing from upstream "in import order and in the null-message guard" stay accurate in substance (the new `Set` import is one more import-order difference); leave them for RLS-01. Adding `MERGE-CHECKLIST-v1.7.0-tail-2.md` to FORK-DELTA's reading-order sentence (around its line 239) is likewise a Phase 38 item.

5. **Does `CreateTableStatement` require fork-side migration?**
   - RESOLVED: No. See Pitfall 3; compile gates confirm.

6. **Is the BLD-16 re-bless safe, and what does it need?**
   - RESOLVED: Safe (constructor present in worker 7.9.7, 4.1.1, 4.4.0). It needs Docker only to copy two jars out of the already-local `cp-kafka-connect-base:7.9.7` image (no pull). One added `M` line plus a new `# date:` header; keep the `# image:` line via `--image-note "image: ..."`.

7. **Should the `ConfigException`-using validator be a concern for the oldest supported worker?**
   - RESOLVED: No. `ConfigException(String,Object,String)` exists in `kafka-clients-7.9.7-ccs.jar` (Kafka 3.9 line, the oldest verified worker).

8. **Does any user decision remain open that blocks planning?**
   - None block planning. Item 3 is a recommended-default interpretation that the plan summary should surface for the user.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| git, `merge-tree --write-tree` | merge, gate | ✓ | system git (merge-tree write-tree verified this session) | — |
| Network to `upstream` (github.com/ClickHouse) | pre-flight record | ✓ | `ls-remote` and `fetch` succeeded | — |
| Docker (colima profile `vz`) | full suite ×2, Confluent worker jars for BLD-16 | ✓ | `colima list`: `vz` Running, aarch64, 8 CPU, 16 GiB; `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock` | Docker-free work (merge, compile, `testUnit`, pins) proceeds without it |
| Docker CLI | image extraction | ✓ at `/opt/homebrew/Cellar/docker/29.8.2/bin/docker` (not linked in PATH) | 29.8.2 | use full path |
| `clickhouse/clickhouse-server:latest` | full suite | ✓ local | 26.10.3.7, `sha256:739e099abd11...` | — |
| `confluentinc/cp-kafka-connect-base:7.9.7` | BLD-16 worker jars | ✓ local | image id `93b9afd3e7f8` | — |
| JDK | Gradle toolchain 17 | ✓ | default `java` is Homebrew OpenJDK 21.0.12.1; toolchain resolved 17 for the test runs (Phase 26 notes OpenJ9 Semeru 17) | `-PjavaVersion=21` |
| Gradle wrapper | build | ✓ | 9.8.2 (Phase 33 bump; builds in the probe ran on it) | — |
| Python 3, `javap` | `connect-api-refs.py` | ✓ | system | — |

**Missing dependencies with no fallback:** none. **Missing with fallback:** none. Disk: 325 GiB free in `/tmp` volume (relevant for the ~8 GB OpenJ9 core-dump risk).

## Validation Architecture

> `workflow.nyquist_validation` is not set to false in `.planning/config.json` as far as the init context shows (key absent = enabled).

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) + Mockito; Testcontainers for `@Tag("clickhouse")` classes |
| Config file | `build.gradle.kts` (`testUnit` task lines ~277-286: `excludeTags("clickhouse")`, JaCoCo off) |
| Quick run command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*<Class>'` |
| Full suite command | `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun` |

`build.gradle.kts` is an input of every `Test` task (`inputs.file(layout.projectDirectory.file("build.gradle.kts"))`), so "cache-busted" means `rm -rf build/test-results/testUnit` plus `--rerun`, exactly as Phase 33 E-14.

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| TAL-01 | one mechanical merge, 5 conflicts, no authored line | git gate | tail gate command above (expect `provenance_paths=27 ... result=PASS`) | ✅ script; ❌ Wave 0: `conflicted-paths-v1.7.0-tail-2.txt`, `MERGE-CHECKLIST-v1.7.0-tail-2.md` (created in the record commit) |
| TAL-02 | 3 ctors, no ambiguity, ORCH-02 plumbing | unit (Docker-free) | `testUnit --tests '*ProxySinkTask*'` (new fork class, e.g. `ProxySinkTaskConstructorUnitTest`) | ❌ Wave 0 |
| TAL-03 | 5-arg callers; compile gates | compile | `./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava` | ✅ |
| TAL-03 | BLD-16 blessed list | unit | `testUnit --tests '*ConnectApiReferencesUnitTest'` | ✅ (data file changes) |
| TAL-04 | parse/validate matrix, 15-code set, no-op, DataException | unit (Docker-free) | `testUnit --tests '*RetriableErrorCodes*'` (new fork class) | ❌ Wave 0 |
| TAL-04 | upstream's own tests | unit | `testUnit --tests '*AdditionalRetriableErrorCodesTest' --tests '*ClickHouseSinkConfigTest' --tests '*UtilsTest'` | ✅ (arrive with merge) |
| TAL-05 | anchor rows + gate | git gate | tail gate at tip + anchors mirror (`rows=<131+n> bad=0`) | ❌ rows added in commits 3 and 4 |
| Phase gate | green twice, same skips | integration | two `test --rerun` runs | ✅ |

### Sampling Rate
- **Per task commit:** the narrowest relevant `testUnit --tests` plus the compile gates for source commits; the tail gate after any commit touching `fork-anchors.txt`.
- **Per wave merge:** cache-busted full `testUnit` (≈6 s in the probe, 66 XML roots).
- **Phase gate:** cache-busted `testUnit`; two `--rerun` full suites on `latest` × V2 (rehearsal: 2m 42s each, 2005 tests / 0 / 0 / 18 skipped before the new TAL tests; the 18 skips are the 7 `@Disabled` methods plus 11 `exactlyOnceStateMismatchTest` cases, unchanged by the tail — upstream's diff touches no `@Disabled`, `Assumptions` or `@Tag`); anchors mirror `bad=0`; tail gate at the tip; cumulative gate in the background; `git diff --quiet` on the two gate scripts; `git ls-remote origin refs/heads/custom_build` still `3a3a15a9` (nothing pushed).

### Wave 0 Gaps
- [ ] `.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt` — covers TAL-01 (5 sorted `LC_ALL=C` lines)
- [ ] `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md` — covers TAL-01 (Section 1 conflicted, Section 2 every upstream-changed path from `git diff --name-only a6f1df83 9bed6f79`, gate commands, Recorded Facts from 44)
- [ ] fork-owned `ProxySinkTask` constructor test — covers TAL-02
- [ ] fork-owned retriable-codes pin test — covers TAL-04
- [ ] Framework install: none

## Security Domain

`security_enforcement` is not disabled in the context I was given; included.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes | `ErrorCodeListValidator` + `parseErrorCodes` (positive-integer-only, `NumberFormatException` and overflow rejected, entries trimmed, empties skipped); pinned by tests |
| V6 Cryptography | no | — |
| V7 Error handling and logging | yes | Do not log record data or secrets; `handleException` logs only the exception's localized message and the numeric code (existing behaviour, untouched) |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Operator misconfiguration of `additionalRetriableErrorCodes` makes a non-transient code retry forever (no retry budget, bypasses DLQ and `errors.tolerance`) | Denial of service (poison batch) | Validator rejects malformed input; ConfigDef description warns "only list codes that are transient"; documented for Phase 38 / FUT-05; default empty |
| Provenance: unreviewed lines slipping into a "mechanical" merge | Tampering | Gate invariant 1 (line set membership), no authored line in M3 |
| Re-blessing the Kafka API list to hide a worker-incompatible reference | Tampering | `connect-api-refs.py --write` refuses unless worker, 4.1.1 and 4.4.0 report 0 missing |
| Secrets in evidence files | Information disclosure | Evidence holds no tokens; nothing is pushed |

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` -> `AGENTS.md` and `.claude/CLAUDE.md`; treat as locked:
- Do NOT commit to the branch without the user asking explicitly, and do not change a PR title/description. (This phase's plans commit on `sync/v1.7.0` as part of the GSD workflow the user invoked; the researcher committed nothing.)
- Start repo edits through a GSD command (this is `/gsd-plan-phase` → execute).
- Behaviour preservation: code changes limited to what the phase needs; the fork runs in production. Keep changes additive to minimise conflicts with future upstream syncs (new test files/cases, small targeted fixes).
- `ClickHouseSinkConfig`, `Processing`, `KeeperStateProvider`, `ClickHouseWriter`, `RangeContainer` are high-risk: this phase must not hand-edit them (the tail changes `ClickHouseSinkConfig` and it auto-merges; do not edit it).
- Run `./gradlew spotlessApply` on files you author; baseline is 46 pre-existing violating files, so verify per-file (the touched file is unchanged by a second `spotlessApply`), not `spotlessCheck` clean.
- Only add tests; never delete or weaken existing ones. Test names descriptive. Use `InMemoryDBWriter`/`InMemoryState`, not mocks of `ClickHouseWriter`, in state-machine tests.
- Java 17, Lombok, SLF4J levels, JUnit 5 + Mockito; do not log secrets or record data.
- `VERSION` bump and `CHANGELOG.md` are release work (Phase 38), not this phase.
- Tests target ClickHouse `latest` + client V2 only; Docker is colima `vz`.
- Do not upgrade dependencies without asking (none are touched here).

## Sources

### Primary (HIGH confidence — executed or read this session)
- `git ls-remote upstream main` / `--tags`, `git fetch upstream --tags`, `git merge-tree --write-tree --name-only` against `HEAD` and `origin/custom_build`; `git diff --name-status a6f1df83 9bed6f79`; `git diff a6f1df83 9bed6f79 -- src/main src/testFixtures CHANGELOG.md`
- Rehearsal clone `/tmp/gsd34probe/repo` (outside the repo): mechanical merge with the resolution table, tail gate run, `compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava` (first: 5 errors; after fix: BUILD SUCCESSFUL), `testUnit --rerun` (first: BLD-16 red; after re-bless: 1418/0/0/2), full `test --rerun` on `latest` × V2 (2005/0/0/18, 2m42s), throwaway DataException probe
- `.planning/scripts/verify-merge-invariants.sh` (read in full), `fork-anchors.txt` (read in full), `fork-anchor-exemptions.txt`, `connect-api-refs.py` header, `src/test/resources/connect-api-references.txt` header
- `.planning/MERGE-CHECKLIST-v1.7.0-tail.md`, `MERGE-CHECKLIST-v1.7.0.md`, `25-07-SUMMARY.md`, `25-COMMAND-EVIDENCE.md` E-12/E-13 (re-bless), `33-COMMAND-EVIDENCE.md` E-02/E-14 (gate commands and baselines), `33-03-SUMMARY.md`
- Upstream test sources at `9bed6f79`: `AdditionalRetriableErrorCodesTest`, `ClickHouseSinkConfigTest`, `UtilsTest` diff
- `javap` of `ConfigException` in worker `kafka-clients-7.9.7-ccs.jar` (extracted from the local image), 4.1.1 and 4.4.0

### Secondary (MEDIUM confidence)
- Memory notes under `~/.claude/projects/.../memory/` (phase 11/16 gotchas, openj9 crash, colima vz, dependabot merges, plan-phase orchestration), treated as hints and cross-checked against the commands above where used

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — nothing changes; confirmed by the file-level diff.
- Architecture (merge recipe, commit order, gate behaviour): HIGH — executed end to end on a clone, including the unedited gate.
- Pitfalls: HIGH — each one reproduced (javac errors, BLD-16 failure, DataException/null probes), except the OpenJ9 crash which did not recur this session and is carried from memory.
- Test gap analysis: HIGH for what upstream covers (files read); the exact design of the new fork tests is the planner's.

**Research date:** 2026-10-28
**Valid until:** until `git ls-remote upstream main` changes or upstream tags a release (re-check immediately before the merge regardless); otherwise 30 days.
