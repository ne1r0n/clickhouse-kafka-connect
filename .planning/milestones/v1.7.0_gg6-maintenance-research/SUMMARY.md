# Project Research Summary

**Project:** ClickHouse Kafka Connect `custom_build` fork, milestone `v1.7.0_gg6-maintenance` (Dependency Absorption, Upstream Tail & Advisory Debt)
**Domain:** Maintenance milestone on a patched fork of a Kafka Connect sink (client bump, upstream sync, CI hardening)
**Researched:** 2026-10-28
**Confidence:** HIGH (client-bump findings were measured on a scratch build against ClickHouse 26.10.3.7; merge conflicts measured with `git merge-tree`)

## Executive Summary

This is a maintenance milestone, not a feature build. It needs no new libraries. The work is: absorb six open Dependabot PRs (five safe, one to reject), merge a 7-commit upstream tail (`9bed6f79`) that adds `additionalRetriableErrorCodes`, bump the ClickHouse Java client `0.9.6` -> `0.11.0`, fix the `increment-version.yml` `vnull` defect, wire the Confluent archive compare into CI, and ship `v1.7.0_gg6`. The fork's established discipline applies: mechanical merge commit under `verify-merge-invariants.sh`, then separate re-expression commits, pins written before behaviour changes, two full suite runs, operator docs reviewed by an independent reviewer.

The one finding that changes the plan is that **the client bump is not a drop-in**. STACK.md measured it: PR #17 alone fails `compileTestJava` (`ServerException` constructor now takes 4 args), and even after fixing that, 23 of 30 sampled live tests fail at runtime. Two production fixes in `ClickHouseHelperClient.createClientV2` are required: (1) pass only `protocol://host:port` to `addEndpoint` (0.9.7 now keeps the URL base path, so every request goes to `POST /<database>` and ClickHouse 26.10 rejects it), and (2) set `ignore_unknown_config_key=true` before the `jdbcConnectionProperties` loop (0.9.7 throws on unknown keys such as the documented `ssl`/`sslmode`). With both fixes the full suite is 1,961 tests, 0 failures, 18 skipped, identical to the gg5 baseline. ARCHITECTURE.md and PITFALLS.md predate this measurement and treat the bump as a pins-first, possibly no-source-change affair; STACK.md supersedes them on this point.

Main risks: (1) merging PR #10 (protobuf-java 4.x) breaks `ProtobufIngestTest` with `VerifyError` because the Confluent test stack is 7.9.1 (protobuf 3.x); reject it and add a Dependabot ignore. (2) The tail merge is textually clean in several places but semantically broken (`ProxySinkTask` constructor conflict, 4-arg `handleException` callers in fork tests), so the compile and anchors gates matter more than conflict markers. (3) Operator-visible behaviour shifts under 0.11.0 (159 no longer client-retried, 503 shape, user-agent, no socket-buffer defaults, zstd-jni in the archive, stale D-17 claims) must be recorded with evidence labels, since operator-doc quantifier errors have been the dominant review defect for five milestones.

## Key Findings

### Recommended Stack

No additions. Version changes only, all through `gradle/libs.versions.toml` and the wrapper properties. Keep all four ClickHouse artifacts on the single `clickHouseDriver` ref.

**Changes:**
- `clickHouseDriver` 0.9.6 -> 0.11.0 (PR #17): merge only with the two production fixes and the test-source fix; add a `# FORK OVERRIDE (upstream 0.9.6)` marker, anchor row and catalog pin test.
- `lz4Java` 1.12.0 -> 1.12.1 (PR #19): merge first; security release (GHSA-48mj-8f7c-7qfm) in a shipped jar.
- `okhttp` 5.5.0 -> 5.5.1 (PR #18): integration-test source set only; merge.
- Gradle wrapper 9.8.1 -> 9.8.2 (PR #20): merge; verify only `gradle-wrapper.properties` changes and `gradlew.bat` CRLF is intact.
- `actions/setup-java` 6.0.1 -> 6.1.0 (PR #16): Dependabot already wrote the full SHA into all four workflows; merge as generated, run the pin-guard tests.
- protobuf-java 3.25.9 -> 4.36.2 (PR #10): **do not merge.** Close, add a Dependabot `ignore` for major >= 4 in the `gradle` block only. Revisit only with a Confluent 8.x test-stack move (separate, larger change).

**Explicitly not to add:** `opentelemetry-api`, `micrometer-core`, `socket_rcvbuf`/`socket_sndbuf` settings, `ServerTimeoutExceeded` in the client retry list, any JDBC-v2 artifact, a direct zstd-jni or lz4 dependency, `distributionSha256Sum`, a hand-rolled client config-key allowlist.

**D-17 (`network_compression_method=lz4`):** measured; client V2 0.11.0 now decodes ZSTD, so the old "must set one of two controls" constraint is lifted. Keep the code, default and anchors (V1 stack still needs it, high-risk config file, avoids loading zstd-jni natives). Reword the stale claims in Javadoc, FORK-DELTA (Recorded Fact 26, CMP rows) and the gg1 rollout warning. Add one new live pin: V2, `setCompressionMethod=false`, `compress=true`, ZSTD server round-trips.

### Expected Features

**Must have (table stakes):**
- Tail merge of `additionalRetriableErrorCodes` with the 4-arg -> 5-arg `handleException` migration of every fork caller and test in one go; default empty and unchanged behaviour when unset; parse/validate matrix pinned; default 15-code set pinned (including 159).
- Client 0.11.0 audit with live pins: 159 stays `RetriableException`, 503 `ConnectException` shapes (with and without `retryOnSocketException`), `ClientException` non-retriable, unknown JDBC key tolerated, endpoint has no path, timeout-message prefixes still produced, V2 decodes ZSTD with both controls off.
- `increment-version.yml`: validate input and output against `^v\d+\.\d+\.\d+(_gg[1-9]\d*)?$`, never write `vnull`, handle `_ggN` (new `gg` choice; patch/minor/major reset suffix to `_gg1`), extract the logic into a script with a table-driven Docker-free test, keep SHA pins and permissions, delete the "known and left unfixed" note. Keep the `_ggN` scheme.
- Archive-compare CI consumer: allow-list keyed by `VERSION`, baseline built from the previous tag in a separate worktree, script moved or copied under `.github/scripts/`, synthetic-archive self-test, pin-guard update, summary output.
- Rollout note `ROLLOUT-v1.7.0_gg6.md` covering each operator-visible effect, each claim labelled "measured" or "read from the code".

**Should have (differentiators):** a single `Client011BehaviourPinLiveTest` class holding the client pins; live fault-injection pin for the 216 case; non-UTC JVM run of the date/datetime live pins; compare report as artifact; replacing the npm `semver` install with shell parsing.

**Defer:** draft-mode allow-list generation; compare gate inside release workflows (after push-CI version proves stable); a retry budget or "remove from default list" setting for retriable codes; client observability SPI adoption; Confluent 8.x test-stack migration; `distributionSha256Sum`; V1 client retirement (track as debt).

### Architecture Approach

The tail changes no build file or workflow, so it does not overlap the Dependabot or client bumps textually; build order is decided by risk isolation. Five paths conflict (measured against both `sync/v1.7.0` and `origin/custom_build`): `CHANGELOG.md`, `Utils.java` (imports only), `ClickHouseSinkTaskMappingTest.java` and `ClickHouseWriterTest.java` (imports only), and `ProxySinkTask.java` (the only semantic conflict: fork `connectorName` ctor vs upstream `DBWriter` test-seam ctor, `null` becomes ambiguous). Resolution: M takes the fork side mechanically, then a re-expression commit introduces a dual-constructor shape (3-arg `String` public, 3-arg `DBWriter` package-private, 4-arg delegate). Auto-merged but broken: `UtilsNullMessageClassificationUnitTest` (stale 4-arg call) and the upstream `CreateTableStatement` refactor across ~20 test files, caught only by `compileTestJava` and `compileIntegrationTestJava`.

**Major components touched:**
1. `Utils.handleException` + `ClickHouseSinkConfig` — new `DEFAULT_RETRIABLE_ERROR_CODES` set and `additionalRetriableErrorCodes` key; redelivery bypasses `errors.tolerance`/DLQ with no retry budget.
2. `ProxySinkTask` / `ChunkFlusher` — pass the extra set; dual-ctor re-expression preserving ORCH-02 `connectorName`.
3. `ClickHouseHelperClient` — the only production-code change from the client bump (endpoint + `ignore_unknown_config_key`).
4. `.planning/scripts` (anchors, gate, allow-list) and `.github/workflows` — new anchor rows (`clickHouseDriver`, `DEFAULT_RETRIABLE_ERROR_CODES.contains`, `ProxySinkTask` statistics ctor), archive-compare consumer, `increment-version.yml` fix.

### Critical Pitfalls

1. **Protobuf 4 "testFixtures-only" bump (PR #10)** — measured red (27 `ProtobufIngestTest` `VerifyError`s). Close with a Dependabot ignore; record the decision in FORK-DELTA.
2. **Trusting "PR CI green/compiles" for the client bump** — PR #17 never reached the `Test` task, and the Docker-free `testUnit` hides both runtime breaks. Gate on the full suite twice on one pinned `latest` server version, evidence block with `SELECT version()`.
3. **Tail merge that is textually clean but broken** — mechanical M under `verify-merge-invariants.sh` with full upstream SHA (re-check `git ls-remote upstream main` just before), authored fixes only in later commits, never hand-edit anchors in M. Merge the tail on 0.9.6 first so compile breaks (`handleException`, `ServerException`) are attributable.
4. **Stale or fictional compression/retry claims in operator docs** — D-17 rationale, 159/503/UA/socket-buffer effects. Re-measure, change text not behaviour, label evidence, use an orchestrator-spawned fresh-context reviewer before the push gate.
5. **Dependabot divergence and archive blind spots** — user merging PRs on `origin` diverges `sync/v1.7.0` again (absorb PR heads on the scratch branch, ask the user not to merge on origin); the new zstd-jni native jar passes `verifyConfluentArchive`, so only the entry-by-entry compare with a gg6 allow-list generated last can see it. Also: exactly-once hazard if a `BEFORE` batch is retried across a client change with different bytes, so the rollout note requires a clean stop before upgrade, backed by a RowBinary byte-diff on the type matrices.

## Implications for Roadmap

### Phase 1: Safe Dependency Sweep and Protobuf Decision
**Rationale:** Independent, each already green; doing them first and one commit each keeps later reds attributable. Resolves the only known-red PR by decision.
**Delivers:** lz4 1.12.1, okhttp 5.5.1, Gradle 9.8.2, setup-java 6.1.0 absorbed by merging PR heads on the scratch branch; #10 closed with a Dependabot `ignore`; FORK-DELTA decision row; fresh `pre-v1.7.0_gg6` rollback tag.
**Addresses:** Dependabot absorption.
**Avoids:** Pitfalls 1, 8, 9, 10, 18.

### Phase 2: Upstream Tail Merge (`9bed6f79`)
**Rationale:** Isolates merge-resolution bugs from client effects; client pins should be written against the final 5-arg `handleException` and `ProxySinkTask` shape.
**Delivers:** mechanical merge M plus re-expression commits (dual `ProxySinkTask` ctor, `UtilsNullMessageClassificationUnitTest` 5-arg call, new anchor rows, default-empty and 15-code pins, `MERGE-CHECKLIST-v1.7.0-tail-2.md`); `additionalRetriableErrorCodes` present with default empty.
**Uses:** gate script with `PRE_TAG=pre-v1.7.0-tail-2`.
**Avoids:** Pitfall 5, Anti-Pattern 1 and 2.

### Phase 3: Client 0.11.0 Audit and Bump
**Rationale:** The only optional, behaviour-bearing change; placed last among code changes so descoping is a one-commit revert (gg6 can ship on 0.9.6, same as upstream).
**Delivers:** pins written on 0.9.6 first, then the one-line catalog bump with the two `createClientV2` fixes, 8 `UtilsTest` `ServerException` sites fixed in one commit, `Client011BehaviourPinLiveTest`, reworded D-17 docs, `# FORK OVERRIDE` marker and anchor, catalog pin test, RowBinary byte-diff and dedup/restart tests, pre/post archive listing diff, two full green runs.
**Avoids:** Pitfalls 2, 3, 4, 6, 7, 11, 12, 13, 14.

### Phase 4: CI Hardening (version workflow and archive-compare consumer)
**Rationale:** Textually independent of 1-3, but the compare consumer should be written against the real post-bump archive and its gg6 allow-list generated last.
**Delivers:** `increment-version.yml` fix with extracted script and table-driven test; archive-compare step with `VERSION`-keyed allow-list naming, tag-baseline worktree, script self-test, pin-guard and REL anchor-row updates; carried advisory items (Phase 30/31 WR/IN rows, gg4 close-record findings).
**Avoids:** Pitfalls 7, 15, 16.

### Phase 5: Release v1.7.0_gg6
**Rationale:** Final integration and evidence.
**Delivers:** `VERSION=v1.7.0_gg6` with anchor edit in the same commit; CHANGELOG (absorbs upstream `Unreleased`); FORK-DELTA; `ROLLOUT-v1.7.0_gg6.md`; gg6 allow-list generated from the final tree and compared; independent operator-doc review; release dry-run and one manual nightly dispatch from `custom_build`; pre-publication `git log sync/v1.7.0..origin/custom_build` check; one green `tests.yaml` run on the published SHA before tagging.
**Avoids:** Pitfalls 10, 14, 16, 17, 20.

### Phase Ordering Rationale

- Trivial and mechanical first, behaviour-bearing last, so any red is attributable and the client can be descoped cheaply.
- Tail before client: pins target the final classification code; the operator remedy (`additionalRetriableErrorCodes`) exists before client behaviour is documented; the tail adds 5 more 3-arg `ServerException` sites to `UtilsTest`, so fix all 8 once.
- Allow-list and `VERSION` last: STALE-ROW semantics make the allow-list valid for exactly one tree.
- Phase 4 can run in parallel with 1-3 for `increment-version.yml`, but shares pin-guard test files with the archive step.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3:** 503/`ConnectException` classification (source-derived, not exercised against a real proxy), zstd-jni native loading in a plugin classloader and noexec `/tmp`, socket-buffer throughput effect, V1 client path (compile-gated only), RowBinary byte equivalence across clients.
- **Phase 4:** archive-compare placement decision (see Gaps) and allow-list naming; moderate, mostly design decisions already framed in FEATURES.md.

Standard patterns (skip research-phase):
- **Phase 1:** each bump is a single-line change with green PR CI; only the protobuf decision is already made.
- **Phase 2:** the fork has a rehearsed recipe (gg1-gg4 merges, gate script, checklist); conflict set already measured. Re-run `git merge-tree` immediately before merging.
- **Phase 5:** repeats the gg5 release shape.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Client bump measured on a scratch copy incl. full suite, archive build and D-17 matrix; only PR CI facts and advisory text for lz4 are secondary |
| Features | HIGH / MEDIUM | Upstream tail, version workflow and compare script read from code; client effects partly from release notes |
| Architecture | HIGH / MEDIUM | Conflict surface measured with `git merge-tree`; 0.11.0 behavioural effects were release-note derived (superseded by STACK where measured) |
| Pitfalls | HIGH / MEDIUM | PR #17/#10 failures read from CI logs; client semantics and some process items from release notes and history |

**Overall confidence:** HIGH

### Gaps to Address

- **Research files disagree, resolve in requirements:**
  - Client bump scope: ARCHITECTURE/PITFALLS imply possible no-source-change; STACK measured two required production fixes. Adopt STACK.
  - PR #10: ARCHITECTURE step 1 lists it for merging; STACK/PITFALLS say reject. Adopt reject-with-ignore.
  - Archive-compare placement: FEATURES says push CI (`tests.yaml`, run only when `VERSION` differs from latest tag or an allow-list exists); ARCHITECTURE says release/pre-release workflows only (count-based REL anchors, per-push would fail on STALE-ROW). Ask the user; recommended default: release/pre-release workflows first.
  - 503 outcome: STACK says net classification unchanged for the default config; ARCHITECTURE flags a possible regression for LB 503s. Settle with a pin and rollout wording, labelled source-derived until a proxy test exists.
  - Baseline for the compare: FEATURES says build from the tag in a worktree; ARCHITECTURE says `gh release download` of the gg5 asset. Prefer tag rebuild (reproducible); confirm the Shadow 9 rebuild caveat.
- **Open questions for the user:** is `additionalRetriableErrorCodes` adopted or merely present; what `gg` does on suffix-less `vX.Y.Z` (fail vs `_gg1`); should the bump PR edit `fork-anchors.txt`; allow-list naming (per `VERSION` vs single replaced file); ask the user not to merge Dependabot PRs on `origin` during the milestone.
- **Unmeasured:** integration-test (Connect worker) harness and nightly on 0.11.0 (one manual nightly dispatch is a release gate); real 502/503 and 159 cadence; socket-buffer throughput; zstd-jni natives on Alpine/musl or noexec `/tmp`; V1 client path beyond existing live tests; severity of the lz4 advisory (text not retrievable).
- **`latest` drift:** record `SELECT version()` per evidence block; reserve a gap-closure slot.
- **Whether upstream moves again** before the merge: re-check `git ls-remote upstream main` against `9bed6f79` and re-run `git merge-tree`.

## Sources

### Primary (HIGH confidence)
- Scratch build of `HEAD` with client 0.11.0 against ClickHouse 26.10.3.7 (full `./gradlew test`, `createConfluentArchive`, D-17 matrix, archive compare vs 0.9.6)
- `git merge-tree`, `git show c7303abc`, `git diff` against `upstream/main` (`9bed6f79`); `verify-merge-invariants.sh`, `fork-anchors.txt`, `compare-confluent-archives.py`, allow-lists 25/29/32
- Dependabot PR #17 run 40416377063 and PR #10 run 39501799049 failed logs; PRs #16-#20 check runs
- clickhouse-java v0.9.7, 0.9.9, 0.10.0, 0.11.0 release notes and v0.11.0 source
- Maven Central POMs for client-v2 0.9.6/0.11.0; `docs/FORK-DELTA.md`; `.github/workflows/*`; `increment-version.yml`

### Secondary (MEDIUM confidence)
- Release-note-derived client effects not yet exercised (503 mapping, socket buffers, UA consumers); Confluent parent POMs for the protobuf 4.x line (8.0.0 read); upstream PRs via `gh pr list`

### Tertiary (LOW confidence)
- Confluent Platform 8.x as the path to protobuf 4 in the test stack (unverified for this fork); zstd-jni behaviour on exotic worker platforms

---
*Research completed: 2026-10-28*
*Ready for roadmap: yes*
