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

- Plan-check answers (2026-11-04, user via AskUserQuestion): (1) Fix the Jackson-over-socket-timeout residual with a value-free wrapper that keeps the retriable classification — a main-code change to the fork's exception text for that shape, approved; re-check gg3 pins and the matrix. (2) Edit the three coordinate assertions of upstream's `serializationFailureNamesTopicPartitionOffsetAndColumn` to read the captured log line, named in FORK-DELTA. (3) The log line carries topic, partition, offset, column and exception class name (amends U-1); never message or value. (4) The four upstream coordinates (slf4j-simple 2.0.20, spotless 8.10.4, commons-lang3 3.21.0, connect-json 4.4.0 test-only) are approved to land in the merge commit. ROADMAP criterion 2 '184-case' is stale: the class is 216.

- User approved (2026-11-04) editing the two existing pins `jacksonOverSocketTimeoutPassesThrough` and `jacksonOverPlainIoExceptionPassesThroughAsAcceptedResidual` in `WriterValueFreeFailureUnitTest` (rename + sentinel + wrapper assertion), scoped to those two methods and imports.

- User answer (2026-11-04, plan-check iteration 2): extend the value-free wrapper to ALL value-carrying Jackson chains — (a) Jackson over Retriable/ConnectException over a plain IO error, (b) a plain IOException wrapping a Jackson root, (c) Jackson over ConnectException over an IO link — keeping every link and the root that retry classification reads, so criterion 2 holds without exception. Add them to the no-sentinel pins; the existing pin `plainIoExceptionOverJacksonPassesThroughAsAcceptedResidual` may be edited in place (rename + sentinel + wrapper assertion), named in FORK-DELTA. Also: bound 45-03's edit of upstream's ClickHouseWriterRecordContextTest mechanically against 83808df9 (two constructor lines + three approved assertions only), as 45-02 does for the escape test; note in E-11 that three of four approved coordinates already sit at HEAD (only connect-json changes); add pointers on UPS-02/criterion 2 to the amendments.

- User approved (2026-11-04): (1) `Utils.java` makes its two private client-timeout prefix constants public, nothing else in Utils changes; (2) server-error links above a Jackson link (client V1 and V2) are rebuilt value-free keeping their error code.

- User answer (2026-11-04, plan-check iteration 3): cover the JSON insert route too — `doInsertJson`/`writeRecordsJson` (incl. `OBJECT_MAPPER.writeValueAsBytes` and `MetadataFreeJsonWriter`) get a measured shape set, a value-free guard that keeps what retry classification reads, and pins; criterion 2 stays unqualified. Main-code change on a hot path: keep the success path allocation-free (guard only on the failure path), no change to bytes sent. User also approved one more planner revision folding in warnings 2-5 and infos 6-7 (per-path checklist greps, malformed-row clause in FORK-DELTA, M5 retargeted to RETRIABLE_OVER_JACKSON_ROOT, between-link stand-in chains IOException(RuntimeException(J)) / IOException(IOException(J)) / IOException(ConnectException(J)), explicit return/throw type for withheldJacksonChain), then execution without a fourth checker pass.

- User decisions on the DEP-01 findings (2026-11-04, plan 45-06; relayed by the coordinator as the user's AskUserQuestion answers, recorded in spirit, not verbatim): (1) the 16 open advisories on test-scope coordinates (P1-P16 in 45-DEPENDENCY-CHECK.md section 7) are record only: all declined, nothing bumped, kept as backlog, none ships. (2) The 12 Confluent `7.9.1-ccs` Kafka artifacts: note the unmapped Apache version as an unknown in the record and do not chase it.

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
