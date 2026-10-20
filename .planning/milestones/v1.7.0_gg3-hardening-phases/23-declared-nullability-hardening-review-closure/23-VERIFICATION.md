---
phase: 23-declared-nullability-hardening-review-closure
verified: 2026-10-20T12:40:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-01-PLAN.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-01-SUMMARY.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-02-PLAN.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-02-SUMMARY.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-03-PLAN.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-03-SUMMARY.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-04-PLAN.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-04-SUMMARY.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-05-PLAN.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-05-SUMMARY.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-06-PLAN.md"
  - ".planning/phases/23-declared-nullability-hardening-review-closure/23-06-SUMMARY.md"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java"
covered_digest: "v3:sha256:26d991c9324fb436900f06cdd8fbc7f951a2d9757cd9ab1f9055e3fc91e681c6"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 23: Declared-Nullability Hardening & Review Closure Verification Report

**Phase Goal:** The declared-nullability pass is total for any input, keeps a literal backslash-n inside element names and enum labels, and has a recorded rule for an unpaired declared element, and its tests can actually fail. Every 17-REVIEW and 19-REVIEW finding ends fixed or closed with a written reason.
**Verified:** 2026-10-20T12:40:00Z
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| #   | Truth | Status | Evidence |
| --- | ----- | ------ | -------- |
| 1 | `apply` and `containsTuple` return without throwing for a null root and a null-type Column; Docker-free pin; the NPE-provoking test is flipped with a rationale, not deleted | VERIFIED | `DeclaredNullability.java` lines 119-122 (`containsTuple` null guard), 144-146 (null root returns false, no log), 192-194 and 220 (`collect`/`collectTuple` null guards), 236 (null field or name). Tests `nullRootReturnsFalseAndLogsNothing`, `containsTupleIsFalseForANullTypeAtAnyDepth`, and 8 more null-shape pins. `unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning` still exists (same method name, same null-type input), now asserts `(structure mismatch)`, with a written D-08 rationale comment. Own mutation (removed the `containsTuple` null guard): 2 COLS-12 / COLS-10 null pins went red. |
| 2 | A literal backslash-n inside a backtick-quoted element name or an enum label survives `normalise`; pairing succeeds with no fallback WARN; the Docker-free pin input is the live-captured server escaping | VERIFIED | Quote-aware linear `normalise` (lines 77-116). Pins use `DeclaredNullabilityFixtures` constants captured live (23-EVIDENCE R-02), and `assertCapturedShapePairs` asserts `apply` returns true with an empty WARN list. Own mutation (reverted `normalise` to plain `replace`): 8 COLS-13 pins plus the edges test went red. Live: `NullableTupleMatrixLiveTest` 33 tests, 0 failures, freshly run on ClickHouse latest x client V2 (includes `describeReturnsTheCapturedBackslashNEscapingAndPairs` and the round-trip premise on both twins). I also checked by hand that production keeps the quote open for an escaped backtick or single quote (run of 2) and keeps a lone line break outside quotes as a space. |
| 3 | Whether a Tuple Column can lack a declared element is decided and recorded (user at checkpoint); on an unpaired element the pass logs one metadata-only WARN naming it, still applies to the paired elements, no DESCRIBE fallback; `missingDeclaredElementDoesNotShiftPairing` stays green with its body unchanged | VERIFIED | Rule is recorded in the class Javadoc (lines 32-37), 23-CONTEXT D-01, FORK-DELTA, and the `UNPAIRED_ELEMENT_WARNING` constant. `apply` lines 156-166: `collect` succeeds, assignments applied, then one WARN listing `unpaired`, returns true. `missingDeclaredElementDoesNotShiftPairing` body is byte-identical before (`4478016a^`) and after. Own mutation (turn the unpaired case into a fallback): 17 tests went red, including the original COLS-10 test. Backlog 999.4 and 999.5 exist in ROADMAP. |
| 4 | Fallback-WARN content assertions use a name that cannot occur in the WARN's fixed text, in delimited form, each seen red with the column argument dropped; backslash-n literal defined once and shared; pass-through helper and mid-class constant cleaned up | VERIFIED | `ROOT_SENTINEL = "nullability_root"` with `assertWarnNamesColumn` (`column %s of table`) and a premise test (`sentinelRootOccursInNoFixedWarnText`). Own mutation (column argument replaced with `database` in all 3 `FALLBACK_WARNING` calls): 14 tests red, and every failure carries the `WR-02 column segment:` message. `grep -rnF '= "\\n";' src` prints exactly one line, `DeclaredNullability.ESCAPED_NEWLINE`. `normaliseDeclaredType` has 0 references. `ClickHouseHelperClientForkUnitTest` has `DATABASE`, `TABLE`, `DISCOVERED_TABLE` in a constants block at lines 38-40. |
| 5 | Every 17-REVIEW and 19-REVIEW finding is fixed or closed with a reason; each archived disposition row points to where it was closed; 17-REVIEW IN-03 closed as "leave" with no new origin assertions and the live test neither deleted nor weakened | VERIFIED | Archived `17-REVIEW-DISPOSITION.md`: 4 rows, `open: 0` (3 fixed, IN-03 skipped with the written reason). Archived `19-REVIEW-DISPOSITION.md`: 8 rows, `open: 0`, all fixed, each Source cell cites a commit. All 10 cited SHAs resolve in git with matching subjects. `Lz4RoundTripLiveTest.java` has no diff in this phase (last touched by `4bbe32f9`), and its Javadoc line 39 names `TestRuntimeClasspathLz4UnitTest`. |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

The behavior-dependent truths (1, 2, 3, 4) all have a passing behavioral test that I re-ran and I mutation-checked, so none is left on symbol presence alone.

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `src/main/.../DeclaredNullability.java` | null-total pass, quote-aware `normalise`, unpaired WARN, public `ESCAPED_NEWLINE` | VERIFIED | Read in full; substantive, called from `ClickHouseHelperClient.buildDescribedTable` |
| `src/test/.../DeclaredNullabilityTest.java` | Docker-free pins (62 testcases) | VERIFIED | 62 run, 0 failures, rerun by me |
| `src/test/.../DeclaredNullabilityFixtures.java` | sentinel, delimited assertion, live-captured literals | VERIFIED | Holds no copy of `ESCAPED_NEWLINE` |
| `src/test/.../ClickHouseHelperClientForkUnitTest.java` | discovery-level WARN pins and constants block | VERIFIED | 51 run, 0 failures |
| `NullableTupleMatrixLiveTest`, `SimpleAggregateTupleRoundTripLiveTest`, `NullableRoundTrip` | shared literal, no pass-through, D-12 live premise | VERIFIED | Matrix live test 33/33 green, freshly run |
| `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt` | COLS-12/13/14 rows | VERIFIED | Anchor rows at lines 84-87; FORK-DELTA row 51 covers the declared-nullability pass |
| Archived 17/19 dispositions | closure pointers | VERIFIED | See truth 5 |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `ClickHouseHelperClient.buildDescribedTable` | `DeclaredNullability.apply` / `containsTuple` | direct call, once per root per DESCRIBE | WIRED | Exercised by the discovery-level pins in `ClickHouseHelperClientForkUnitTest` and by the live tests |
| Tests | `DeclaredNullability.ESCAPED_NEWLINE` / `normalise` | direct reference | WIRED | No other definition of the literal under `src` |
| Dispositions | closing commits | SHA in the Source cell | WIRED | All SHAs verified present |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Unit pins green | `gradlew testUnit --tests '*DeclaredNullabilityTest' --tests '*ClickHouseHelperClientForkUnitTest' --rerun-tasks` | 62 + 51 tests, 0 failures | PASS |
| Live premises green | `gradlew test --tests '*NullableTupleMatrixLiveTest'` (CLICKHOUSE_VERSION=latest, CLIENT_VERSION=V2) | 33 tests, 0 failures, fresh timestamp | PASS |
| Tests can fail: `normalise` plain replace plus no `containsTuple` guard | temporary source mutation, reverted | 9 red | PASS |
| Tests can fail: unpaired element turned into fallback | temporary source mutation, reverted | 17 red | PASS |
| Tests can fail: column argument dropped from the fallback WARN | temporary source mutation, reverted | 14 red, each with `WR-02 column segment:` | PASS |
| Escaped-quote handling in `normalise` | ad hoc `java` run on the built classes | quote stays open, behavior correct | PASS |

Production source was restored from a byte copy after each mutation; `git status --short src` is empty.

### Probe Execution

SKIPPED: the phase declares no probe scripts.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| COLS-12 | 23-02 | `apply` / `containsTuple` total for null root and null-type Column | SATISFIED | Truth 1 |
| COLS-13 | 23-04 | backslash-n inside quoted name or enum label survives `normalise` | SATISFIED | Truth 2 |
| COLS-14 | 23-05 | unpaired declared element rule decided and recorded | SATISFIED | Truth 3 |
| DEBT-10 | 23-03 (also 23-02 fixture) | sentinel column, delimited assertion | SATISFIED | Truth 4 |
| DEBT-11 | 23-01 | one shared literal, helper and constant cleanup | SATISFIED | Truth 4 |
| DEBT-12 | 23-06 | every 17/19-REVIEW finding closed with a pointer | SATISFIED | Truth 5 |

All six IDs are marked complete in REQUIREMENTS.md (lines 44-62, traceability 116-123). No orphaned Phase 23 requirement.

### Anti-Patterns Found

No TBD/FIXME/XXX markers introduced in the phase's source files. No stubs.

### Advisory: 23-REVIEW findings (open, none blocking)

I weighed whether any open finding undermines a must-have. None does.

| Finding | Effect on must-haves |
| ------- | -------------------- |
| WR-01 (escaped-quote branch of `normalise` not pinned) | Real test gap on code beyond the captured shapes the roadmap requires. Production behavior is correct (hand-checked). Truth 2 is pinned by the captured shapes (mutation-proven). It does not affect "its tests can actually fail" for the WARN assertions, which is truth 4. Recommend a pin in a follow-up. |
| WR-02 (`discoverAsDescribeTableV2` copies the production loop) | Test-quality risk for the 999.4 known-defect pin only. The apply-level pins are independent of it. |
| IN-01 to IN-04 | Javadoc wording, cosmetic WARN slot, vacuous-premise loop in a live test, WARN consequence hint. No effect on behavior. |

These are recorded `open` in `23-REVIEW-DISPOSITION.md` and are outside DEBT-12's scope (17 and 19 findings only). They are not gaps against this phase's goal.

### Human Verification Required

None. The SC3 user decision is recorded (23-CONTEXT D-01, 23-DISCUSSION-LOG checkpoint).

### Gaps Summary

No gaps. All five success criteria hold in the code, were confirmed by re-running the unit and live tests, and the tests were shown to fail under targeted mutations. Working tree: only the pre-existing `.planning/state.json` change and the untracked `.planning/milestone.lock`, both left alone.

---

_Verified: 2026-10-20T12:40:00Z_
_Verifier: Claude (gsd-verifier)_
