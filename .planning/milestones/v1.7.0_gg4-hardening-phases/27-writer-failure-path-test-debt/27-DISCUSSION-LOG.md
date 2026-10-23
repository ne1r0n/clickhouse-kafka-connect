# Phase 27: Writer Failure Path & Test Debt - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-22
**Phase:** 27-writer-failure-path-test-debt
**Areas discussed:** Jackson exclusion rule, Enum lookup-miss shape, Failure metadata dedup, Planning-ID scrub scope

---

## Jackson exclusion rule

| Option | Description | Selected |
|--------|-------------|----------|
| JacksonException | Base class since Jackson 2.12; covers every Jackson-originated message | ✓ |
| JsonProcessingException | Narrower class from the review sketch | |
| You decide | Claude picks during planning | |

**User's choice:** JacksonException

| Option | Description | Selected |
|--------|-------------|----------|
| Pass through unchanged | Retry classification sees the original chain | ✓ |
| Rethrow retriable with sanitized cause | Wrap with a WithheldMessageException copy | |
| You decide | Decide once the WRT-16 rows exist | |

**User's choice:** Pass through unchanged (Jackson over a timeout)

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, flip it | Sign-off to flip JsonAsStringKeyResidualLiveTest, seen red first | ✓ |
| Add a new test, keep the old | Old test would have to be disabled | |

**User's choice:** Yes, flip it

---

## Enum lookup-miss shape

| Option | Description | Selected |
|--------|-------------|----------|
| In Column.convertEnumValues | Covers every call site | ✓ |
| At the writer call sites | Duplicates the null check | |

**User's choice:** In Column.convertEnumValues

| Option | Description | Selected |
|--------|-------------|----------|
| IllegalArgumentException | Generic catch wraps it, prefix byte-identical | ✓ |
| Exact RuntimeException | Passes through, loses the shared prefix | |
| A new typed fork exception | Extra class for a one-line failure | |

**User's choice:** IllegalArgumentException

| Option | Description | Selected |
|--------|-------------|----------|
| Append the column | `No content in enum column <name>` | ✓ |
| Leave as is | Would not close IN-02 | |

**User's choice:** Append the column

---

## Failure metadata dedup

| Option | Description | Selected |
|--------|-------------|----------|
| Two small helpers | sourceTypeName(Data), valueClassName(Data) | ✓ |
| One metadata record/holder | Value object computed once | |

**User's choice:** Two small helpers

| Option | Description | Selected |
|--------|-------------|----------|
| Prefix constant only | ClassCastException text otherwise byte-identical | ✓ |
| Merge into one format | Changes wording, needs ledger row | |
| Helpers only, leave text | Drift stays possible | |

**User's choice:** Prefix constant only

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, import it | Same method WRT-17 edits | ✓ |
| Leave it | Smaller diff | |

**User's choice:** Yes, import ConnectException

---

## Planning-ID scrub scope

| Option | Description | Selected |
|--------|-------------|----------|
| All of src/main | ~24 hits in 9 files, plus a standing guard | ✓ |
| Only the files IN-05 named | ClickHouseWriter, WithheldMessageException | |
| Files this phase edits | Least churn, leaves others dirty | |

**User's choice:** All of src/main

| Option | Description | Selected |
|--------|-------------|----------|
| Only that one test class | JsonAsStringKeyResidualLiveTest, matches the roadmap | ✓ |
| All tests | ~70 files, scope creep | |

**User's choice:** Only that one test class

| Option | Description | Selected |
|--------|-------------|----------|
| Remove it | Delete useClientV2Calls | ✓ |
| Assert it in the pin | Pins a helper-client detail | |

**User's choice:** Remove it

---

## Claude's Discretion

- Form and patterns of the standing "no planning IDs in src/main" guard.
- Exact wording of the new messages, within the value-free rules.
- Plan split, commit order inside the fixed four-step order, evidence file layout.
- Shape of the red-first proof for the premise-loop changes.

## Deferred Ideas

- Scrubbing planning IDs from the other ~70 test files.
- Merging the two failure-message builders.
- Wrapping a retriable Jackson-over-timeout exception with a sanitised cause.
