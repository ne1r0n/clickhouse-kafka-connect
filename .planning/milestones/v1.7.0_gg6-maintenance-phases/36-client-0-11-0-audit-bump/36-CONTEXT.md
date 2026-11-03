# Phase 36: Client 0.11.0 Audit & Bump - Context

**Gathered:** 2026-10-29
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

The connector runs on ClickHouse Java client 0.11.0, and every behaviour change is recorded. Classification, endpoint, config-key and compression behaviours that operators rely on are pinned first on 0.9.6, then on 0.11.0. The two required `createClientV2` fixes land. An exactly-once retry across the upgrade either sends identical bytes or needs a documented clean stop. The archive's change is recorded. The bump is one catalog commit, so if the audit fails gg6 ships on 0.9.6 by reverting it (CLT-09). Builds on the post-tail, post-BUF tree (Phases 34-35).

Requirements: CLT-01..CLT-09. Research flags: 503/`ConnectException` fault injection feasibility, zstd-jni native loading in a Connect plugin classloader and under noexec `/tmp`, V1 client path, socket buffers (record only), what the CLT-09 revert keeps (does 0.9.6 accept `ignore_unknown_config_key`). Order: 0.9.6 pins first, then the bump with the two fixes, then the pin class, byte comparison, doc rewording, archive listings, the two full runs last. Make the CLT-09 revert really one commit; the 10 three-argument `ServerException` sites (UtilsTest 8, AdditionalRetriableErrorCodesTest 1, AdditionalRetriableErrorCodesPinUnitTest 1) must compile on both clients or ride in the catalog commit. Record `SELECT version()` in every evidence block.

CLT-07's manual nightly dispatch needs the scratch branch pushed to origin: that is the user's decision and must NOT be done by an agent. Pushes nothing; prepare everything and leave CLT-07 as a user-gated item (stop and ask the user at that point). Dependency bump authorization: the milestone ROADMAP plus the user's /gsd-autonomous request; disclose in plan text. Out of scope: `ServerTimeoutExceeded` in the client retry list, `socket_rcvbuf`/`socket_sndbuf`, removing D-17.

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
