# Phase 31: Fork-Owned Nightly Integration Tests - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-26
**Phase:** 31-fork-owned-nightly-integration-tests
**Areas discussed:** Schedule gate & NIT-03 dispatch, Test scope, Skip list mechanism, Failure policy & reporting

---

## Schedule gate

| Option | Description | Selected |
|--------|-------------|----------|
| Repo variable gate | `if` on `vars.NIGHTLY_ENABLED`; flipped after the dispatch | ✓ |
| Early CI-only publication | `--no-ff` merge of the workflow mid-milestone | |
| Move NIT-03 to Phase 32 | Dispatch after publication | |

**User's choice:** Repo variable gate, gating only schedule events (manual dispatch always allowed).

## NIT-03 timing

| Option | Description | Selected |
|--------|-------------|----------|
| Move NIT-03 to Phase 32 | Dispatch + schedule enable as operator steps after publication | ✓ |
| Temporary push trigger on scratch branch | Real run pre-merge, trigger removed later | |
| Early publication of workflow | Merge workflow to custom_build now | |

**User's choice:** Move NIT-03 to Phase 32, if research confirms the default-branch dispatch rule.

## Test scope

| Option | Description | Selected |
|--------|-------------|----------|
| Whole integrationTest task | All three classes | ✓ |
| Upstream's standalone filter | ClickHouseSinkConnectorIntegrationTest only | |
| Decide after measurement | Default whole, narrow if needed | |

**User's choice:** Whole task. Timeout set from measurement with headroom (not split per class). Images pulled at run time and recorded.

## Skip list mechanism

| Option | Description | Selected |
|--------|-------------|----------|
| Fork-owned exclude in build | Named list in build.gradle.kts | ✓ |
| Workflow Gradle filters | Exclusions in YAML | |
| JUnit @Disabled | Annotations in test source | |

**User's choice:** Fork-owned build exclude; entries carry name + cause category + reason (+ re-open trigger).

## Failure policy & reporting

**User's choice:** Connector-defect failures stop for a user decision. Failed nightly uploads reports and writes failing names to the step summary; no issue automation.

## Claude's Discretion

Cron time, concurrency group, artifact naming, byte-guard shape, measurement record layout.

## Deferred Ideas

- Issue automation on failure
- ClickHouse image digest pinning
