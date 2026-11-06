---
phase: 46-insert-timeout-limits
plan: "05"
subsystem: config
tags: [help-text, config-pin, phase-gate, clickhouse-latest, client-v2, fork-delta, anchors, evidence]
status: complete

requires:
  - phase: 46-insert-timeout-limits
    provides: plans 46-01..46-04 (raw-socket stub, V2 write-phase deadline, V1 bounded wait, live measurements, FORK-DELTA item 14); B0 3d4ce2e8, testUnit 2152, anchors rows=212
provides:
  - the measured help text of clickhouseClientInsertTimeoutMs (text only, in ClickHouseSinkConfig)
  - InsertTimeoutHelpTextPinUnitTest (18 Docker-free cases)
  - two TMO-01 anchor rows (mirror rows=214)
  - FORK-DELTA item 14 and the per-insert socket_timeout row describing the new text
  - evidence E-13 (help text) and E-14 (phase gate); the E-01..E-14 index complete
affects: [49]

actuals:
  tokens: 9618
  tasks: 2
  commits: 4
plan_head_before: b5d24a0520398c7c36bc1252cfd69da1b567d0b5
plan_head_after: d124641c96c80aa7bff2617b038f4e14cfc33c0c

tech-stack:
  added: []
  patterns: [one constant phrase list per help text, each phrase tied to the evidence entry that measured it, text pinned from the registered ConfigKey]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md

key-decisions:
  - "The V1 clause is the plan's `not applicable` variant (E-09), so the text claims the response-wait bound with a measured scope (batches up to 256 MiB) and claims no bound for a stall inside the client's pipe write, which no run reached"
  - "The help-text wording follows the plan's list exactly (12 phrases); the two ambiguities below are reported, not patched on the fly, because the plan's verify fixes the phrase count and the text"

requirements-completed: [TMO-01, TMO-02, TMO-03]

coverage:
  - id: D1
    description: "The help text of clickhouseClientInsertTimeoutMs states what the setting bounds (client V2 read inactivity and the sending of the batch, client V1 response wait) and what it does not (a V2 insert whose batch was sent while progress streams, non-insert requests, nothing when the operator names socket_timeout), the rollout consequence, and no longer claims a single-response wait; the pin was red on the unchanged text (13 of 18) and is green (18 of 18)"
    requirement: TMO-01
    verification:
      - kind: other
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java (E-13 help_red_failed_count 13, help_green_failed_count 0)"
        status: pass
      - kind: other
        ref: "P46-05-T1-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "The ClickHouseSinkConfig diff since B0 is 23 added and 7 removed string-literal lines inside the one define call; define count 48 unchanged, async_insert lines 2 unchanged; the pin asserts type, default, validator, importance, display name and width unchanged; two anchor rows, mirror rows=214 bad=0"
    requirement: TMO-01
    verification:
      - kind: other
        ref: "P46-05-T1-OK (config diff: text only; anchors rows=214 bad=0)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Phase gate: cache-busted testUnit with Docker unreachable 2170 tests, 2 skipped; two --rerun full suites on ClickHouse 26.10.3.7 x client V2 at 2773 tests each with the 18 named skips and no failing testcase; delta over E-01 equals the phase's 84 Docker-free cases; gg7 pins byte-unchanged with 15/23/38; src/main changed only in the three expected files; tail gate PASS with anchor_rows=214; nothing pushed"
    requirement: TMO-03
    verification:
      - kind: other
        ref: "P46-05-T2-OK (E-14, re-checked against xml-gate-testunit, xml-gate-suite-1, xml-gate-suite-2 and a live re-run of the tail gate)"
        status: pass
    human_judgment: false
  - id: D4
    description: "That the help-text wording reads truthfully to an operator, in particular the client V1 clause, 'the timeout setting' in the ping clause, and the 30 s figure for client V1 non-insert requests (see Findings)"
    requirement: TMO-01
    verification: []
    human_judgment: true
    rationale: "No test asserts how prose reads; the plan routes FORK-DELTA to the orchestrator's independent gsd-code-reviewer pass, and the help text should be read in the same pass"
---

# Phase 46 Plan 05: Help Text, Pin and Phase Gate Summary

**The help text of `clickhouseClientInsertTimeoutMs` now states, phrase by phrase from the phase's measurements, what the setting bounds on client V2 and V1 and what it does not, a Docker-free 18-case pin holds it (red on the unchanged text, green after), and the phase gate passes: testUnit 2170 with Docker unreachable, two full suites at 2773 on ClickHouse 26.10.3.7 x V2, tail gate PASS with 214 anchor rows, nothing pushed.**

## Performance

- **Duration:** about 27 min by the session clock (11:20 to 11:47 UTC), most of it two full suites and two tail-gate runs
- **Tasks:** 2
- **Files:** 5 (1 created, 4 modified); `src/main` changed only in the help-text literals of `ClickHouseSinkConfig.java`

## Names later plans use

- Commits: `63aade66` (text, pin, anchors; E-13 `help_commit`), `1aefb2be` (FORK-DELTA; `help_docs_commit`), `3a399c38` (E-13), `d124641c` (E-14).
- Gate head: `3a399c38e0e19639b8939238cc667e96f183eabf` (E-14 `gate_head`); the E-14 commit follows it and changes only the evidence file.
- ClickHouse `latest`: 26.10.3.7, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (unchanged since 46-01).
- Counts: testUnit **2170**, full suite **2773** (both runs), anchors **214** rows, tail gate `anchor_commits=75 marker_commits=93`.

## Accomplishments

- **Help text (TMO-01, criterion 4).** Written after criteria 1-3 settled, in the plan's wording: it opens with "the connector gives one ClickHouse insert before it fails the batch with a RetriableException", then "What it bounds" (client V2: the read inactivity limit that restarts on every byte, and the sending of the batch, serialization included, even while the server already sends progress headers; client V1: the whole wait for the insert response, scoped to batches up to 256 MiB), "What it does not bound" (a V2 insert whose batch has been sent while progress headers stream; requests that are not inserts, with the Keeper state read on client V1; nothing at all when the operator names `socket_timeout`), the consequence (a legitimately longer insert fails and is retried, a timed-out insert may still be committed so `exactlyOnce=false` can duplicate), and the existing `max.poll.interval.ms` advice. The old claim of bounding "a single ClickHouse insert response" is gone.
- **Pin.** `InsertTimeoutHelpTextPinUnitTest`, 18 cases, no mocking library, no tag: `REQUIRED_PHRASES` (12, each with the evidence entry that measured it), `KEPT_PHRASES` (4), the retired claim, and the key's type, default, validator, importance, display name and width. Red on the unchanged text: 13 of 18 failed (the 12 new phrases and the retired claim); the four kept-phrase cases and the attribute pin were green. Green after: 18 of 18. Neighbours (`AdditionalRetriableErrorCodesPinUnitTest`, `NewUpstreamConfigDefaultsUnitTest`, `ClickHouseSinkConfigTest`, `SourcePlanningIdGuardUnitTest`): 49 tests, no failure.
- **Scope of the config diff.** Every changed line is a string literal inside the one define call (checked by script against B0); the define count stays 48; `async_insert` lines stay 2; two anchor rows (`What it does not bound` 1, the retired claim 0), mirror `rows=214 bad=0`.
- **FORK-DELTA.** Item 14's help-text sentence now describes the new text and its pin (and no longer says the text "is left as it is because"); the per-insert `socket_timeout` row says the only `ClickHouseSinkConfig` change is that help text.
- **Phase gate (E-14).** Cache-busted `testUnit` with `DOCKER_HOST=unix:///nonexistent.sock`: 2170 tests (E-09's 2152 + 18), 0 failures, 2 skipped. Two `--rerun` full suites on `latest` x V2, 4 min 9 s and 4 min 10 s, 2773 tests each (E-01's 2689 + 84 = 6 raw-stub + 39 V2 + 21 V1 + 18 help), 0 failures, 0 errors, the 18 named skips (`named=7 state_mismatch_cases=11 unexpected=0`), `SELECT version()` 26.10.3.7 and the same digest after each, all re-checked against the XML snapshots. gg7 pins byte-unchanged with 15, 23 and 38 tests; the five classes this phase added are green. `src/main` differs from B0 only in `ClickHouseSinkConfig.java`, `ClickHouseWriter.java` and `ClickHouseHelperClient.java`, no file added; `Utils`, `Processing`, `ProxySinkTask` and `KeeperStateProvider` untouched; client-level `socket_timeout` rows 0; build files, workflows, CHANGELOG and VERSION unchanged; gate scripts and the helper unedited; no merge since B0; tail gate over `M..3a399c38` printed `result=PASS` with `anchor_rows=214`, run once by hand and once more by the verify.
- **Not pushed.** `pre-v1.7.0_gg8` and `sync/v1.7.0` are absent on origin; `upstream/main` is still `83808df9` (no move during this plan).

## Findings the reviewer should read (nothing was changed on the fly)

1. **Client V1 body-write clause.** E-09 selected the plan's `not applicable` variant, whose text reads "the whole wait for the insert response, including a stalled body write (measured with batches up to 256 MiB)". That is what was measured: with a server that stops reading, 64 and 256 MiB V1 inserts ended at the bound (1.2 s and 1530 ms) because the task thread was in the response wait. A stall inside the client's pipe write was **not reached** up to 256 MiB (E-07, E-09), so it is recorded as "not reached", never "bounded". The help text states the measured scope and nothing about the pipe-write stall; FORK-DELTA item 14 now says this explicitly. If the reviewer wants the text to say so too, it needs a plan change (the plan's verify rejects the words "in-memory pipe" for this variant).
2. **Retry count behind the V1 "30 s".** The V1 non-insert clause (phrase H8) says those requests "end at the client library's default 30 s inactivity limit". E-11 measured with `setRetry(1)`; the connector default is 3 and `pingV1`/`queryV1` loop over it (read from the code, not measured), so 30 s is the per-attempt figure. Not added to the help text because the plan fixes the 12-phrase list and its verify counts it; FORK-DELTA item 14 now says so. A one-sentence help-text addition (a 13th pinned phrase) would close it.
3. **"The timeout setting".** In the same clause "ping at the timeout setting" means the connector's `timeoutSeconds` (E-11's ping timeout), not `clickhouseClientInsertTimeoutMs`. The planned wording is ambiguous inside this key's own help text. Same remedy as item 2.
4. **H13 rests on a stub pin.** "even if the server already sends progress headers while the batch is still being sent" is pinned by `progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound`; the live 64 MiB body (E-10) left the client in 2305 ms, inside the bound, so no live run exercised that shape. E-13 says so.

## Task Commits

1. **Task 1 (tracer): help text, pin, anchors** - `63aade66`; **FORK-DELTA** - `1aefb2be`; **E-13** - `3a399c38`
2. **Task 2: phase gate** - `d124641c` (E-14)

**Plan metadata:** this SUMMARY is committed alone; STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/config.json` and `.planning/state.json` are left to the orchestrator, per the dispatch instructions. `actuals.commits: 4` is the measured `git rev-list --count plan_head_before..plan_head_after`; `actuals.tokens` is chars/4 over the added lines of the five files.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` - the documentation string of `clickhouseClientInsertTimeoutMs` only
- `src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java` - the pin (new)
- `.planning/scripts/fork-anchors.txt` - one comment line and two TMO-01 rows
- `docs/FORK-DELTA.md` - item 14's help-text sentence, the per-insert `socket_timeout` row's config clause
- `.planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md` - E-13, E-14, index complete

## Decisions Made

See `key-decisions`. Nothing belonging to the user came up beyond the findings above; U-1 to U-7 were applied as recorded in CONTEXT.md.

## Deviations from Plan

None - plan executed exactly as written.

### Observations (not rule triggers)

- A first idea to add a retry-count sentence and `(timeoutSeconds)` to the help text was dropped: it would have made 13 required phrases, against the plan's fixed 12 and red count of 13, so the verify would have failed. The two points are reported as findings 2 and 3 instead.
- The tail gate run by hand took 7 min 20 s against 3 to 4 min recorded in earlier plans (the verify's own re-run was not timed); the cause was not investigated.
- The probe and verify scripts live in `/tmp/gsd46/exec`; no probe was committed and no worktree is registered.

## Issues Encountered

None. No JVM crash occurred in either full suite, no crash file was left in the repository root, and `-PjavaVersion=21` was not needed.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None. The plan changed a help-text string, a test, an anchor file, documentation and evidence; no network endpoint, auth path, file access pattern or schema was added.

## Next Phase Readiness

- Phase 46 plans are all executed. The orchestrator's independent `gsd-code-reviewer` pass on FORK-DELTA (and, per Finding 1-3, the help text) is next, then verification.
- Phase 49 carries the rollout paragraph from the 46-04 summary into the gg8 operator note; the help text now says the same consequence in the operator-facing place.
- Upstream moved: no (`83808df9`). Origin moved: no (`sync/v1.7.0` and `pre-v1.7.0_gg8` absent).

## Self-Check: PASSED

- Files: the pin, `ClickHouseSinkConfig.java`, `fork-anchors.txt`, `FORK-DELTA.md` and `46-COMMAND-EVIDENCE.md` present; this SUMMARY present.
- Commits `63aade66`, `1aefb2be`, `3a399c38` and `d124641c` are ancestors of HEAD.
- Verify blocks P46-05-T1-OK and P46-05-T2-OK printed on the committed tip; anchors mirror `rows=214 bad=0`; no worktree is registered; nothing pushed.
