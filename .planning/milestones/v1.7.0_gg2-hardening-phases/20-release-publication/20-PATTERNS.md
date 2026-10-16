# Phase 20: Release & Publication - Pattern Map

**Mapped:** 2026-10-14
**Files analyzed:** 11 new/modified
**Analogs found:** 11 / 11 (all analogs verified git-tracked via `git ls-files`: `docs/`, `src/test/...`, `.planning/phases/16-*`, `.planning/scripts/*`)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/test/java/com/clickhouse/kafka/connect/sink/DedupTokenRetryLiveTest.java` (name is discretion) | test (live) | request-response (insert + read back) | `src/test/java/com/clickhouse/kafka/connect/sink/EmptyCdcSkipRouteLiveTest.java` + `.../sink/processing/EmptyCdcExactlyOnceStateTest.java` (token/body capture) | role-match |
| `docs/ROLLOUT-v1.7.0_gg2.md` | docs (operator note) | n/a | `docs/ROLLOUT-v1.7.0_gg1.md` | exact |
| `CHANGELOG.md` (new `1.7.0_gg2` section) | docs | n/a | `CHANGELOG.md` lines 1-50 (`1.7.0_gg1`) | exact |
| `docs/FORK-DELTA.md` (stale text, WR-03 wording, raw numbers) | docs | n/a | itself, "The raw numbers" (~lines 107-123) | exact |
| `VERSION` | config | n/a | itself (single line `v1.7.0_gg1`) | exact |
| `.planning/scripts/fork-anchors.txt` (VERSION row) | config | n/a | line 41: `VERSION<TAB>v1.7.0_gg1<TAB>1<TAB>BLD-07` | exact |
| `.planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md` | docs (evidence) | n/a | `.planning/phases/16-early-docs-publication/16-COMMAND-EVIDENCE.md` | exact |
| `.planning/phases/20-release-publication/20-REVIEW-DISPOSITION.md` | docs (frontmatter) | n/a | `.planning/phases/16-early-docs-publication/16-REVIEW-DISPOSITION.md` | exact |
| `.planning/phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md` (flip rows) | docs | n/a | same file / 16 disposition | exact |
| Merge/publication gate script (inline) | utility (shell) | batch | `16-03-PLAN.md` line 266 (`MERGE-SHAPE-OK`) and line 307 (`CI-STILL-ONE-RUN`) | exact |
| Suite gate command | utility (shell) | batch | `19-10-PLAN.md` Task 1 `<automated>` | exact |

## Pattern Assignments

### Dedup live test (test, request-response)

**Analog:** `EmptyCdcSkipRouteLiveTest.java` (live, extends `ClickHouseBase`); token/body facts from `EmptyCdcExactlyOnceStateTest.java` with `CapturingClickHouseWriter` (`Capture(route, body, deduplicationToken, supportDefaults)`, record at line 36).

**Live-test skeleton** (EmptyCdcSkipRouteLiveTest lines 26, 75-95):
```java
public class EmptyCdcSkipRouteLiveTest extends ClickHouseBase {
    Map<String, String> props = new HashMap<>(getBaseProps());
    props.put("name", uniqueConnectorName(testInfo));
    String topic = createTopicName(topicPrefix);
    ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
    ClickHouseTestHelpers.dropTable(chc, topic);
    ClickHouseTestHelpers.executeQueryIgnoreResult(chc, String.format(ddl, topic));
    ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
    try { sinkTask.start(props); ... } finally { ... }
```
Imports to copy: `ClickHouseHelperClient`, `ClickHouseTestHelpers`, `@DisplayName`, `@Test`, `@TestInfo`, static `assertEquals`.

**Token pin** (EmptyCdcExactlyOnceStateTest): asserts `TOPIC + "-0-10-" + lastOffset`; token format from `QueryIdentifier.getDeduplicationToken()` is `topic-partition-min-max`, null when partition == -1.

**Rules for this test:**
- Read back with typed SQL and `assertEquals` (never `countRows`); unique db/table per test; create tables before `start()`.
- Replicated table: `ENGINE = ReplicatedMergeTree('/clickhouse/tables/<unique>', '<r>') ORDER BY id`. `ClickHouseBase` mounts `clickhouse_keeper_map.xml` (lines 120-124), which supplies the embedded keeper.
- Assert `replicated_deduplication_window` is non-zero (read it from `system.merge_tree_settings`) and log `SELECT version()` (the `NULLABLE_TUPLE_MATRIX server version=` log-line style from `NullableTupleMatrixLiveTest`).
- Per D-13/D-14, pin three measured outcomes: same token with identical bytes is dropped; same token with changed bytes is dropped; a different token with identical data lands. Add a case with `exactlyOnce=false` through the real task. Do NOT assert "changed-bytes duplicates".
- Seen-red check: window 0 or a changed token must fail the test.

### `docs/ROLLOUT-v1.7.0_gg2.md` (docs)

**Analog:** `docs/ROLLOUT-v1.7.0_gg1.md`

**Header/intro format** (lines 1-13): H1 "Rollout checklist: `vA` to `vB`"; a placeholder-only paragraph (`<db>`, `<zkDatabase>`, `<topic>`); the three-part item shape; ledger tags as trailing "(ledger L-xx)"; a numbered TOC with anchors.
```
Each item has three parts:
- **What changed**: ...
- **Am I affected?**: a self-check ... Every query says what an empty result means.
- **What to do**: ...
```
**Item body shape:** `## Item N: <title>` then `### What changed`, `### Am I affected?`, `### What to do`.

**KeeperMap self-check to reuse** (gg1 lines 54-60; research proved it live on 26.9.17.8 with `clickhouse_keeper_map.xml`):
```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>', '<topic>')
ORDER BY `key`;
```
Keep the "Empty result / Non-empty result" sentences. Drop the gg1 caveat "was not executed against a live KeeperMap" (line 66) because D-03 requires live runs. Point at gg1 item 1 for the shared-table explanation rather than duplicating it (D-01).

**Required content:** see CONTEXT `<domain>` and the research Ledger-to-Document Map (L-25, L-26, L-27..L-29 log-alert notes, L-30, L-31..L-33, L-35..L-40, MBean `version=`, rollback item). The dedup item must carry the D-13 correction of gg1 line 45 ("hashes the parsed data"). Rollback item: use the read-tree form with `pre-v1.7.0_gg2`; do not use `git revert -m 1`.

### `CHANGELOG.md` (docs)

**Analog:** `CHANGELOG.md` lines 1-50. Insert above `# 1.7.0_gg1, 2026-10-10`.
```markdown
# 1.7.0_gg2, <publication date>

<intro paragraph linking [docs/FORK-DELTA.md](docs/FORK-DELTA.md) and [docs/ROLLOUT-v1.7.0_gg2.md](docs/ROLLOUT-v1.7.0_gg2.md)>

## Important Changes
* ...plain language, no planning IDs, no ledger tags...
## Bug Fixes
* ...
## Build and CI   (new per D-10)
## Dependencies   (only if `git diff origin/custom_build HEAD -- gradle/libs.versions.toml` is non-empty)
```
Do not edit the gg1 section.

### `docs/FORK-DELTA.md` (docs)

Edit in place. Stale gg1 text sits at the intro (lines 5, 7), line 33, line 50 (the WR-03 per-twin wording; soften to "a read-past-end code"), line 61 (`VERSION` row), line 192, and the raw numbers.

**Raw-numbers pattern** (lines 107-123): keep the table of 4 rows and add gg2 "after" rows. Reuse the four commands:
```
git diff --shortstat v1.7.0 v1.7.0_gg1 -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 v1.7.0_gg1 -- src/main
```
with `v1.7.0_gg2` substituted. Measure at the final X, after the last `src`/`build`/`.github` change. Record the outputs in the evidence file as in E-03.

### `VERSION` and `fork-anchors.txt` (config)

Both change in ONE commit. `.planning/scripts/fork-anchors.txt` line 41 is tab-separated:
```
VERSION	v1.7.0_gg1	1	BLD-07
```
Becomes `VERSION	v1.7.0_gg2	1	BLD-07`. `VERSION` holds the single line `v1.7.0_gg2`. After the bump run `./gradlew clean compileJava`, then check `build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java`.

### `20-COMMAND-EVIDENCE.md` (docs)

**Analog:** `16-COMMAND-EVIDENCE.md` lines 1-56. Header with base commit, shell, date and rule ("a `Matches text: no` verdict means the doc text gets fixed, never the evidence"). Then per entry:
```
### E-NN: <title>
Backs: `<doc>`, "<section>", <quoted text>.
```sh
<command>
```
```text
<output>
```
Matches text: yes
```
Entries that back no doc text carry `Matches text: n/a (gate check)`. First entry: `SELECT version()`. Then the detection queries run on a live gg1 fixture (a `be6b8861` worktree) and on gg2-written rows (D-16), the KeeperMap self-check, the rollback proof on a throwaway branch (E-16 `demo2.sh` pattern, lines ~314-403 of the Phase 16 file), and the raw-numbers commands.

### `20-REVIEW-DISPOSITION.md` / `19-REVIEW-DISPOSITION.md` (docs)

**Analog:** `16-REVIEW-DISPOSITION.md` lines 1-40: YAML frontmatter, then a table.
```yaml
---
phase: 20
review: 20-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "..."
open: 0
total: N
recorded: <ISO timestamp>
---
| Finding | Severity | Disposition | Source |
| IN-01 | info | fixed | <sha> (docs only, Phase 20) |
```
Reviewer findings must use `### <CR|BL|WR|IN>-NN: <title>` headings, or the parser drops them. In the 19 file, flip WR-03 to `fixed` with the Phase 20 commit SHA as the source. WR-01, WR-02 and IN-01..IN-03 get `deferred` with a written reason each (IN-04 is already deferred).

## Shared Patterns

### Merge-shape gate (publication, orchestrator-inline)
**Source:** `16-03-PLAN.md` line 266, adapted. For Phase 20, `B=be6b8861c25f3d7611194e7150b18362c5c75e32`:
```sh
M=$(git rev-parse custom_build) && P=$(git rev-list --parents -n 1 "$M") \
&& test "$(printf '%s\n' "$P" | awk '{print NF}')" -eq 3 \
&& test "$(git rev-parse "$M^1")" = "$B" && X=$(git rev-parse "$M^2") \
&& git diff --quiet "$M" "$X" && git merge-base --is-ancestor "$X" sync/v1.7.0 \
&& T=$(git ls-tree -r --name-only "$M") \
&& test -z "$(printf '%s\n' "$T" | grep -x '\.planning/\.gsd-ws-arg')" && echo MERGE-SHAPE-OK
```
Drop the Phase 16 allow-list (`.planning/`, `docs/`, `CHANGELOG.md` only), because Phase 20 ships `src` changes. Replace it with the X0-to-X `.planning`-only check, where X0 is the rehearsed tip. Create the merge from a throwaway worktree: `git worktree add /tmp/gsd20/cb custom_build`, then `git -C /tmp/gsd20/cb merge --no-ff -F <msg> <X-full-sha>`. Push with `git push origin refs/heads/custom_build:refs/heads/custom_build` (no force, no tag).

### CI single-run check
**Source:** `16-03-PLAN.md` line 307:
```sh
M=$(git rev-parse custom_build) && J=$(curl -sS --max-time 30 "https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=$M") \
&& printf '%s' "$J" | jq -e --arg p .github/workflows/tests.yaml '(.workflow_runs | type == "array") and (.workflow_runs | length == 1) and (.workflow_runs[0].path == $p) and (.workflow_runs[0].head_branch == "custom_build") and (.workflow_runs[0].conclusion == "success")' && echo CI-STILL-ONE-RUN
```
Re-check at least 120 s later. Push the tag only after this passes. The unauthenticated limit is 60 requests an hour.

### Suite gate
**Source:** `19-10-PLAN.md` Task 1 `<automated>`. Prefix every Gradle call with `env -u TESTCONTAINERS_HOST_OVERRIDE`. Run `clean` first, then `testUnit --rerun` with `colima stop --profile vz` (check `docker info` fails), then `colima start --profile vz`, then `test --rerun` twice. Copy run 1's XML aside. Update the expected totals (Phase 19 had 84 classes, 980 tests and 18 named skips; expect 85 or more classes). Compare the skip set, not the count.

### Rollback tag and check
Create `pre-v1.7.0_gg2` on `be6b8861`. Verify the trigger with `git show pre-v1.7.0_gg2:.github/workflows/tests.yaml | grep -n "branches: \['\*\*'\]"`. Push it by explicit refspec before the release merge. Rerun the rollback commands (`git read-tree -u --reset <tag>` plus commit) on a dry-run merge, per the E-16 pattern in `16-COMMAND-EVIDENCE.md`.

### Writing discipline
Plain language, no bare planning IDs (ledger tags trail only in the rollout note), placeholders only, every quoted command run live and logged in the evidence file. Never write a `TRUNCATE` instruction for the shared state table; rerun the merged-tree no-TRUNCATE check on M.

## No Analog Found

| File | Role | Reason |
|---|---|---|
| Dedup test with `ReplicatedMergeTree` and an explicit insert token | test | No existing test posts a custom `insert_deduplication_token`. The only mention is a comment at `ClickHouseSinkTaskBufferTest.java:246`. Use the research probe SQL (RESEARCH "Probe" block) and the `ClickHouseBase` keeper mount. |
| Detection queries for the Nullable(Enum) and Nullable(Tuple) shapes | docs/SQL | These are new. Take the symptom definitions from `19-SPIKE.md` and ledger L-13, and prove them against fixtures written by the `be6b8861` code (D-16). |

## Metadata

**Analog search scope:** `src/test/java/.../sink`, `.../sink/processing`, `docs/`, `CHANGELOG.md`, `.planning/phases/16-*`, `.planning/scripts/`
**Files scanned:** about 14 read, plus grep and ls over the test tree
**Pattern extraction date:** 2026-10-14
**Not read:** lines 384-475 of RESEARCH.md (Open Questions and later); the planner should read them.
