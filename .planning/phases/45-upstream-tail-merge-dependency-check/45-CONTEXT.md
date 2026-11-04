# Phase 45: Upstream Tail Merge & Dependency Check - Context

**Gathered:** 2026-11-04
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

The code-gap phases start from a tree that already holds upstream's tail, so a later red points at the fork's own change and not at the merge. The tail is merged in the fork's sync shape: scratch branch, rollback tag, one mechanical commit, a per-path checklist, the invariant script and two full runs. Upstream #857's record context is kept only where it carries no record value. An operator still sees the column, the types and the exception class, never the value, and the retry classification does not move. Slash column names (#839) round-trip, and `fromJsonRow` agrees with the fork's raw-identifier names. The dependencies that ship have been checked for open CVEs on the merged catalog, and every bump is the user's decision.

</domain>

<decisions>
## Implementation Decisions

### Locked user decisions (2026-11-04, from RESEARCH.md U-1..U-5)
- U-1 = A: serialization failures log topic, partition, offset and column only; the fork's exception text and retry classification stay untouched (WRT-10 and the 216-case matrix unchanged). Add a Docker-free pin through `writeRecordsRowBinary`.
- U-2 = edit to pass: minimal compile fix to `ClickHouseWriterRecordContextTest`; edit the one red `FieldDescriptorEscapeTest` method to assert the fork's literal-JSON `comment`/`default_expression`; name it in FORK-DELTA. No `@Disabled`.
- U-3 = upstream's `1.7.1, Unreleased` CHANGELOG bullets are written in Phase 49, not here.
- U-4/U-5 = record-only dependency check (shipped `clickhouseDependencies` tree first, then test/plugin/workflow actions via OSV.dev + GitHub advisories); no bump unless a CVE is found, and any bump is then asked separately.
- Research facts to honor: tag HEAD for `pre-v1.7.0_gg8`; drop #839's dead `BufferedReader reader` hunk in `describeTableV1` (fork side); baseline totals on HEAD are measured as task 0. Research: 45-RESEARCH.md.

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
