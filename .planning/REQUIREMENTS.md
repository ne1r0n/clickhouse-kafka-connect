# Requirements: ClickHouse Kafka Connect — `custom_build` Fork

**Defined:** 2026-10-16
**Milestone:** `v1.7.0_gg3-hardening` Record-Data Exposure & Review Debt
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

**Sources:**
- The open items in PROJECT.md → Requirements → Active.
- The re-open triggers in `milestones/v1.7.0_gg2-hardening-phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md`.
- `milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-REVIEW.md` and `…/19-nullable-enum-tuple-mapping/19-REVIEW.md`.
- FORK-DELTA Known costs item 6.

No milestone-level research was run; plan-phase researches each phase.

**Scope decisions made by the user on 2026-10-16:**
- Take every open candidate.
- Exclude the upstream sync, because upstream has nothing past `v1.7.0`.
- The nightly `integrationTests.yaml` is disabled by the user in Actions settings. The file is not edited, deleted or fixed.
- ClickHouse client `0.10.0` stays out, and Dependabot PR #4 is left open.
- Publish as `v1.7.0_gg3`.

## v1 Requirements

### Branch integration

- [ ] **DEPS-01**: The working branch contains `origin/custom_build`'s three Dependabot commits:
  - lombok 1.18.48 (#1), spotless 8.10.4 (#2) and slf4j-reload4j 2.0.20 (#3), tip `6473e24f`;
  - brought in by a merge of `custom_build`, with no rebase and no force;
  - spotless 8.10.4 adds no violations beyond the pre-existing 8.10.1 baseline (the same `spotlessApply` diff, no tree-wide reformat; amended 2026-10-17 per 21-CONTEXT D-14), and `testUnit` stays green.

### Record-data exposure — backlog 999.3

- [ ] **WRT-13**: A value-conversion failure in `ClickHouseWriter.doWriteColValue`'s generic catch reaches three places: the Connect task status, the DLQ exception headers (message and stack trace) and `doInsertRawBinary`'s two `Error inserting records` ERROR logs. Two cases are an invalid UUID string and an unparseable number.
  - In all three, it names the column, the ClickHouse type and the exception class, and never carries the record value.
  - A sentinel test asserts the value is absent from every message in the cause chain and from every captured log line (T-18-07).
- [ ] **WRT-14**: `Utils.handleException`'s retriable vs non-retriable classification is unchanged for every failure kind that reaches that catch.
  - It's pinned both ways by a Docker-free test that passes on the tree before the change and after it.
- [ ] **WRT-15**: These change in the same commit as the code they describe:
  - FORK-DELTA Known costs item 6 and the WRT-12 row of the patch table.
  - The ROADMAP backlog 999.3 entry, which is marked delivered.

### Declared-nullability hardening — 19-REVIEW

- [ ] **COLS-12**: `DeclaredNullability.apply` and `containsTuple` return without throwing for a null root or a Column with a null type, and a Docker-free test pins that (19-REVIEW WR-01).
  - The existing test that provokes a `NullPointerException` through a null-type Column is flipped with a written rationale, not deleted (19-REVIEW IN-02 trigger).
- [ ] **COLS-13**: A literal backslash-n inside a backtick-quoted Tuple element name or an enum label survives `normalise`. Pairing then succeeds, and declared nullability applies without the metadata-only fallback WARN (19-REVIEW IN-03).
  - It's pinned by a Docker-free test whose input is the escaping the server actually returns, captured live.
- [ ] **COLS-14**: It's decided and recorded whether a Tuple Column can legitimately lack a declared element (19-REVIEW IN-04).
  - The pass then either logs and falls back on an unpaired declared element, or keeps today's behavior with a written reason.
  - `missingDeclaredElementDoesNotShiftPairing` stays green either way.
  - A decision that isn't clear-cut goes to the user at a checkpoint.
- [ ] **DEBT-10**: The fallback-WARN content assertions use a column name that cannot occur in the WARN's fixed text and assert it in a delimited form (19-REVIEW WR-02).
  - Each assertion is seen red with the column argument dropped.
- [ ] **DEBT-11**: The backslash-n literal is defined once and shared by every test that needs it. The one-line pass-through helper and the mid-class constant are cleaned up (19-REVIEW IN-01, IN-02).

### CI pin guard — 17-REVIEW

- [ ] **CI-18**: `TestsWorkflowPinGuardUnitTest` fails if `tests.yaml`'s "Test fork count" step stops reading `reportTestForks`, or if that task disappears from `build.gradle.kts` (17-REVIEW IN-01).
- [ ] **CI-19**: The pin guard handles two cases correctly, each seen red and green on a fixture (17-REVIEW IN-02):
  - it accepts `runs-on: ubuntu-24.04 # comment`;
  - it no longer reports a step `name:` or `run:` text containing `: write` as a write scope.
- [ ] **DEBT-12**: Every 17-REVIEW and 19-REVIEW finding ends as either fixed or closed with a written reason. Each archived disposition row points to where it was closed.
  - 17-REVIEW IN-03 is closed as "leave `Lz4RoundTripLiveTest`'s jar-origin check, cross-referenced to `TestRuntimeClasspathLz4UnitTest`, with no new origin assertions". It's not deleted or weakened.

### Test harness

- [ ] **HARN-08**: `ClickHouseWriterNullMessageUnitTest` no longer inline-mocks `ClickHouseHelperClient` with Mockito; it overrides one level up instead, in the `CapturingClickHouseWriter` pattern.
  - Every original assertion of the WRT-11 null-message behavior is kept.
  - The Phase 18 deferred item (OpenJ9 `retransformClasses` crash risk) is closed.

### Operator checks (user, in the GitHub web UI)

- [ ] **OPS-01**: The user disables the nightly `integrationTests.yaml` in the repository's Actions settings.
  - The file stays byte-identical to upstream `v1.7.0`.
  - The REST API shows no `schedule` run of it after the recorded disable date.
- [ ] **OPS-02**: The Dependabot `gradle` entry shows a recent "Last checked" time with no configuration error. This closes the residue of 17-REVIEW WR-01.
- [x] **OPS-03**: The read-only GitHub token is revoked on GitHub, and the Keychain item `gsd17-gh-actions-read` is deleted.

### Release

- [ ] **ROLL-03**: `docs/ROLLOUT-v1.7.0_gg3.md` tells operators the following, and it's reviewed before the publication gate:
  - Writer error text in the task status, the DLQ headers and the ERROR logs no longer carries the record value.
  - What to grep for instead.
  - Anything else operator-visible: the MBean `version=` segment, the client name and the dependency bumps.
- [ ] **PUB-09**: `VERSION` is `v1.7.0_gg3`, `CHANGELOG.md` has a plain-language `1.7.0_gg3` section, and `docs/FORK-DELTA.md` has a row for every new fork-side patch.
- [ ] **SUITE-06**: The full suite is green twice, with Gradle's cache busted by `--rerun`, on ClickHouse `latest` × client V2: 0 failures, 0 errors, every skip named. `testUnit` is green with Docker stopped.
- [ ] **PUB-10**: The release lands on `custom_build` as a non-force `--no-ff` merge, made only after the user's own explicit approval.
  - The published SHA has exactly one green `tests.yaml` run.
  - The `v1.7.0_gg3` tag is pushed only after that run is green.
  - The rollback tag `pre-v1.7.0_gg3` points at the pre-merge `custom_build` tip.

## Future Requirements

- The next upstream sync once upstream tags past `v1.7.0`, in the same shape as `v1.7.0_gg1-sync`.
- ClickHouse client `0.10.0`, offered by Dependabot PR #4. It's a breaking-changes release and needs its own evaluation.
- Making the nightly `integrationTests.yaml` pass on the fork, or moving the other upstream workflows (`release.yaml`, `pre-release.yaml`, `increment-version.yml`) off `@v3`-era actions.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Upstream sync | `upstream/main` = `v1.7.0` (`379b8d01`), 0 commits past the tag. Nothing to merge |
| ClickHouse client `0.10.0` / merging PR #4 | Not approved. The PR stays open, unmerged and not ignored |
| Editing, deleting or fixing `integrationTests.yaml` | The user disables it in settings so the file stays upstream-identical |
| New connector features | The project covers and hardens existing fork behavior |
| Running the upgrade on production connectors | It stays with the operators, as in previous milestones |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| DEPS-01 | Phase 21 | Pending |
| WRT-13 | Phase 22 | Pending |
| WRT-14 | Phase 22 | Pending |
| WRT-15 | Phase 22 | Pending |
| COLS-12 | Phase 23 | Pending |
| COLS-13 | Phase 23 | Pending |
| COLS-14 | Phase 23 | Pending |
| DEBT-10 | Phase 23 | Pending |
| DEBT-11 | Phase 23 | Pending |
| CI-18 | Phase 21 | Pending |
| CI-19 | Phase 21 | Pending |
| DEBT-12 | Phase 23 | Pending |
| HARN-08 | Phase 22 | Pending |
| OPS-01 | Phase 21 | Pending |
| OPS-02 | Phase 21 | Pending |
| OPS-03 | Phase 21 | Complete |
| ROLL-03 | Phase 24 | Pending |
| PUB-09 | Phase 24 | Pending |
| SUITE-06 | Phase 24 | Pending |
| PUB-10 | Phase 24 | Pending |

**Coverage:**
- v1 requirements: 20 total
- Mapped to phases: 20 ✓
- Unmapped: 0

| Phase | Requirements | Count |
|-------|--------------|-------|
| 21. Dependabot Integration & CI Hygiene | DEPS-01, CI-18, CI-19, OPS-01, OPS-02, OPS-03 | 6 |
| 22. Writer Record-Data Exposure | HARN-08, WRT-13, WRT-14, WRT-15 | 4 |
| 23. Declared-Nullability Hardening & Review Closure | COLS-12, COLS-13, COLS-14, DEBT-10, DEBT-11, DEBT-12 | 6 |
| 24. Release & Publication | ROLL-03, PUB-09, SUITE-06, PUB-10 | 4 |

---
*Requirements defined: 2026-10-16*
*Last updated: 2026-10-16 after roadmap creation (Phases 21-24, 20/20 mapped)*
