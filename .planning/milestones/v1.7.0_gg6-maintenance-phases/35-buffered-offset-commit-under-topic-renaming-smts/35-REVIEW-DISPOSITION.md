---
phase: 35
review: 35-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "No test pins `originalKafkaPartition()` at the two strategy sites, so a partition-only regression passes"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "`ChunkFlusher.flush` calls the new accessors only after the insert, and the rollout wording misdescribes the failure on a runtime that lacks them"
  - id: IN-01
    severity: info
    disposition: open
    title: "The `acceptedByConnect` assertions can never fail once the key assertion has passed"
  - id: IN-02
    severity: info
    disposition: open
    title: "`RecordingDBWriter.Inserted` describes a consumer position but its `offset` is the transformed offset"
  - id: IN-03
    severity: info
    disposition: open
    title: "The live `scenario()` leaks the ClickHouse client when table creation fails"
  - id: IN-04
    severity: info
    disposition: open
    title: "`ClickHouseSinkTask.close` now passes original keys to `onPartitionRemoved`, which holds transformed keys, and this is not recorded under OI-35-1"
  - id: IN-05
    severity: info
    disposition: open
    title: "The state-key tripwire is described as the exactly-once key but exercises only `InMemoryState`"
open: 5
total: 7
recorded: 2026-10-29T10:24:31.763Z
---

# Phase 35: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 35-REVIEW-FIX.md |
| WR-02 | warning | fixed | 35-REVIEW-FIX.md |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |
| IN-05 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
