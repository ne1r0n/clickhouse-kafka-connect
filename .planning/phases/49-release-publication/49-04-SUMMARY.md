---
phase: 49-release-publication
plan: "04"
subsystem: release
tags: [release, allow-list, archive-compare, tail-gate, testUnit, full-suite, evidence]
status: complete

requires:
  - phase: 49-release-publication
    provides: plans 49-01 to 49-03 (VERSION v1.7.0_gg8, the frozen release documents, E-01 to E-10)
provides:
  - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt, 17 exact rows generated last from the final tree
  - evidence entries E-11 (allow-list), E-12 (Docker-free gates), E-13 (two full suites) and the go decision
affects: [49-05]

actuals:
  tokens: 6300
  tasks: 3
  commits: 5
plan_head_before: b1fc06443f5b0f3e9607b7e5a32af59a29c87d5e
plan_head_after: 5ee96ac95b0f726dc8a397e6fc492c5f6222e1bc

key-decisions:
  - "The allow-list was derived by owner from the compare of two clean rebuilds, never copied: 17 exact rows, no wildcard"
  - "The first E-12 attempt was not written: the default hygiene scan read 10 hits of one plain word in the review record; the orchestrator reworded them (89f3e5ce) and the scan was re-run clean, so E-12 records go"
---

# Plan 49-04 summary: the gg8 allow-list and the release gates

The allow-list was generated last from the final tree and the tip was gated: the compare of the rebuilt `v1.7.0_gg7` archive and the tip archive ends `RESULT: PASS` with 0 unallowed and 0 stale rows, the tail-form anchors gate passes on an explicit SHA, testUnit passes with Docker unreachable, and two full suites on one pinned ClickHouse `latest` with client V2 are equal test by test. Nothing was pushed.

## Facts

- **Allow-list commit:** `dcf13360892b496e589129bbf5bd3bf63b181c0d`, one file, 17 rows (equal to E-05's measured difference count), no wildcard row. It is the last non-evidence commit and `generated_last: yes` (no archive input, CHANGELOG or doc path changed between the frozen documents `c96816f4` and it).
- **compare_summary:** `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg7, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt`. The empty-list compare reproduced E-05: 17 UNALLOWED, totals `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`, 7932 and 7935 inner entries. Mutations: stale row exit 1, deleted `Version.class` row exit 1, leading wildcard exit 2. The three guard classes (58, 38, 14 tests) are green with Docker unreachable.
- **projection_equal:** yes (the 14 Phase 48 connector paths plus `Version`, `SinkTaskStatistics` and `META-INF/MANIFEST.MF`).
- **Gated head:** `d9b10b9bf670b8236119c725e3639c53ad3e570b`. `last_non_planning_commit` is `315f5de1c85fb61bd2a1e887d2be085bc62571ce`; `last_scripts_commit` is `dcf13360892b496e589129bbf5bd3bf63b181c0d`. One later commit, `89f3e5ce`, is the orchestrator's reword of the review record (a `.planning/phases` file only).
- **Tail gate** (once, alone, on the gated head, `PRE_TAG=pre-v1.7.0_gg8`, exit 0): `invariants: provenance_paths=11 anchor_commits=270 anchor_rows=218 marker_commits=288 anchor_exempt=0 result=PASS`.
- **testUnit** (Docker unreachable): 2325 tests, 0 failures, 0 errors, 2 skipped; per-testcase delta against E-04 `match=yes`.
- **Full suite**, runs 1 and 2: each 2928 tests, 0 failures, 0 errors, 18 skipped (7 named, 11 state-mismatch cases), about 3 min 15 s, equal test by test.
- **Image:** `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, `SELECT version()` 26.10.3.7, the same before run 1, after run 1 and after run 2, and equal to E-04.
- **JVM line:** default toolchain (Gradle 9.8.2, launcher JVM 21.0.12.1) for the archive builds, testUnit and both suites; `-PjavaVersion=21` was not needed and no crash file appeared.
- **Anchors mirror:** `rows=218 bad=0`. Scope outside `.planning`: `CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg8.md`; `.planning/scripts`: the allow-list and `fork-anchors.txt`. FORK-DELTA numbers equal a fresh run and its path resolver reads `unresolved=0`.
- **Origin and upstream recheck 2:** origin `custom_build` still `94834763`, left count 0, 288 unpublished commits, no sync ref, no gg8 tag, no open PR. Upstream main `83808df9`, newest tag `v1.7.0`, PRs 861, 855 and 854 open, 788 closed: nothing moved since E-01.

## Gate decision

**go.** E-12 `docker_free_decision: go` (`gate_failures: none`) and E-13 `gate_decision: go` (`gate_failures: none`). `integrationTest` was not re-run (PL-11).

## Deviations from Plan

None to code, tests, documents or workflows. One process event: the first default-mode hygiene scan over the files this phase added read 10 hits of one plain verb in `49-REVIEW.md` (a false positive of the helper's fifth default pattern, as in the earlier release phase). I stopped before writing E-12 and reported; the orchestrator reworded the ten lines in commit `89f3e5ce`, and the scan was re-run clean before E-12 was written. The `commits: 5` measured from the ledger counts that orchestrator commit besides my four (allow-list, E-11, E-12, E-13). Tool notes: the plan's verify blocks end with a dirty-path test on the evidence file, which cannot pass before its own commit, so each was run once before the commit without that clause and once after it exactly as written; all three print their OK lines.

## Handoffs

- **Plan 49-05 pre-checks:** the gated head is `d9b10b9bf670b8236119c725e3639c53ad3e570b` and the gated SHA (last non-`.planning` commit) is `315f5de1c85fb61bd2a1e887d2be085bc62571ce`; the pushed SHA may differ from the gated head only under `.planning` outside `.planning/scripts`, plus the U-3 date line. `unpublished_at_gate` is 288.
- **For the bundle:** the JVM record above, the image digest and `SELECT version()`, the upstream recheck 2 values, and that `integrationTest` was not re-run (the user may overrule it).
- **Scratch:** archives, logs, snapshots and dumps remain under `/tmp/gsd49` and are not committed; the archive build directories under `/tmp/gsd49/w/final-*` can be removed.

## Self-Check: PASSED

The allow-list exists; commits `dcf13360`, `d9b10b9b`, `eb0d68dd` and `5ee96ac9` exist on the branch; the three task verify blocks print `P49-04-T1-OK`, `P49-04-T2-OK` and `P49-04-T3-OK`.
