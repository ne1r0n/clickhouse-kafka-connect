# Phase 20: Command evidence (D-03, D-04)

- **Base commit:** `P20_BASE` = `d73848807c58c4ba535da05e7515ecbbf5025bdf` (the tip when Phase 20 planning started; every "Phase 20 changed nothing here" check diffs non-`.planning` paths against it).
- **Shell:** macOS zsh, BSD `grep`. Gradle is always run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` because this shell sets that variable to the literal `null`. Log lines carry the local time zone (UTC+3); the date below is UTC.
- **Date (UTC):** 2026-10-14.
- **Rule:** a `Matches text: no` verdict means the doc text gets fixed, never the evidence. Entries that back no doc text carry `Matches text: n/a (gate check)`. Entries that back text not yet written carry the verdict `pending (20-03)` until plan 20-03 writes that text.

### E-01: server version and deduplication windows

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the dedup item, the window sentence (written in plan 20-03). The class reads `SELECT version()` and the three window settings from `system.merge_tree_settings` and asserts only that `replicated_deduplication_window` is above zero.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests '*DedupTokenRetryLiveTest' --rerun
python3 -I -c 'import sys,xml.etree.ElementTree as E;r=E.parse(sys.argv[1]).getroot();o="".join(x.text or "" for x in r.iter("system-out"));print("\n".join(l for l in o.split("\n") if "DEDUP_TOKEN_RETRY server version=" in l or "DEDUP_TOKEN_RETRY settings" in l))' build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest.xml
```

```text
[2026-10-14 15:16:25,731] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY server version=26.9.18.11
[2026-10-14 15:16:25,739] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY settings replicated_deduplication_window=10000 replicated_deduplication_window_seconds=3600 non_replicated_deduplication_window=0
```

The research phase measured `26.9.17.8` with the same three values; `latest` moved to `26.9.18.11` between the research and this run, and the windows did not change.

Matches text: pending (20-03)

### E-02: re-delivered range on ReplicatedMergeTree, exactlyOnce=false (tracer)

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the dedup item, the `exactlyOnce=false` wording (written in plan 20-03). Three fresh tasks (an empty in-memory state each) put the same offsets 0 to 2 of one partition into a `ReplicatedMergeTree` table through the real connector: step 1 the original rows, step 2 the same offsets with every `name` changed, step 3 the original rows again. After every step the table is read back by typed SQL (`SELECT id, name, count() ... GROUP BY id, name`) and compared with the rows step 1 left.

```sh
python3 -I -c 'import sys,xml.etree.ElementTree as E;r=E.parse(sys.argv[1]).getroot();o="".join(x.text or "" for x in r.iter("system-out"));print("\n".join(l for l in o.split("\n") if "DEDUP_TOKEN_RETRY outcome case=eo-false-replicated" in l))' build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest.xml
```

```text
[2026-10-14 15:16:26,248] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=1 rows=3
[2026-10-14 15:16:26,358] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=2 rows=3
[2026-10-14 15:16:26,455] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=3 rows=3
```

Assertion summary: after step 2 the table still reads back as step 1's three rows (the changed-bytes retry was dropped, not stored), and after step 3 it is unchanged. The research premise (same token, changed bytes, dropped) holds on today's `latest`, so the plan continued.

Matches text: pending (20-03)
