# Phase 42: Isolated High-Risk Edits - Context

**Gathered:** 2026-11-02
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

**Goal**: Three small edits in high-risk files land one commit each, tests first and each behind its own gate, so any one can be reverted alone (the user's decision of 2026-11-01):
- Exactly-once processing reads the original topic where OI-35-1 found the transformed one, and the state key does not move.
- `Utils.handleException` keeps an exception that is already a `RetriableException` retriable, and the set of classification rows it changes is written down in advance.
- A connector that lists a non-transient code in `additionalRetriableErrorCodes` gets a WARN at start-up.

The OI-35 cases that the edit does not fix are written down as limits.

**Depends on**: Phase 41. The PIN-02 and TMO-02 classification pins are the baseline that RSK-02's row list is measured against.

**Requirements**: RSK-01, RSK-02, RSK-03, RSK-04

**Success Criteria** (what must be TRUE):

  1. *[Docker-free, record]* In the RSK-01 commit, `Processing.doLogic` line 137 reads `originalTopic()`. The `KeeperStateProvider` diff is empty, and the state key and the KeeperMap row format are unchanged. All 14 `ProcessingTest` cases are unchanged and pass. The remaining `onPartitionRemoved` keying mismatch is recorded in FORK-DELTA (RSK-01).
  2. *[Docker-free, record]* Before the RSK-02 change, the expected changed rows are written down: rows of the 184-case `WriterFailureClassificationUnitTest` matrix, and of the PIN-02 and TMO-02 pins. After the change, the actual diff of outcomes equals that list exactly, and a `RetriableException` passed to `handleException` comes out retriable (RSK-02).
  3. *[Docker-free]* In the RSK-03 commit, a task started with a non-transient code in `additionalRetriableErrorCodes` logs one WARN that names the code and carries no record data, and a transient code logs none. Start-up still succeeds. Nothing validates the codes or budgets retries, the `ClickHouseSinkConfig` diff is empty, and the existing `additionalRetriableErrorCodes` pins are unchanged (RSK-03).
  4. *[record]* FORK-DELTA states OI-35-2 and OI-35-3 as limits: exactly-once under a topic-renaming SMT needs an injective rename and a partition-preserving SMT. The state key does not change (RSK-04). Each of the three code commits touches only its production file, its tests and its record rows. Each is followed by cache-busted `testUnit` and a full suite.

**Plans**: TBD

**Research flag**: Settle these before planning:
- RSK-01: trace where the line 137 value flows before editing. If it reaches the state key, a KeeperMap row or table routing in a way that changes stored state, stop for the user. A state-key change is out of scope.
- RSK-02: write the expected row list from the matrix before the code change. A mismatch is a stop, not an expectation update.
- RSK-03: settle what counts as a transient code (where the list comes from, and where the WARN fires) without adding a config key.

**Note**: These are the user-approved high-risk edits (`Processing.java`, `Utils.java` and the start-up path). Any diff beyond RSK-01's one line, RSK-02's pass-through or RSK-03's WARN stops for a user decision. Preserve the `BEFORE`/`AFTER` flag sequence (AGENTS.md).

Phase gate: the per-commit gates above, then the anchors invariant.

</domain>

<decisions>
## Implementation Decisions

### Claude's Discretion
All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

</decisions>

<code_context>
## Existing Code Insights

Codebase context will be gathered during plan-phase research.

</code_context>

<specifics>
## Specific Ideas

No specific requirements — discuss phase skipped. Refer to ROADMAP phase description and success criteria.

</specifics>

<deferred>
## Deferred Ideas

None — discuss phase skipped.

</deferred>
