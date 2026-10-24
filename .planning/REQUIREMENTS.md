# Requirements: ClickHouse Kafka Connect — `custom_build` Fork

**Defined:** 2026-10-24
**Milestone:** v1.7.0_gg5-ci — CI & Release Workflow Modernization
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

## v1 Requirements

### Release workflows

- [ ] **REL-01**: `pre-release.yaml` builds with the project Gradle wrapper (via setup-gradle), with no pinned Gradle 7.4.2 (DEPS-F4)
- [ ] **REL-02**: `release.yaml`, `pre-release.yaml` and `increment-version.yml` pin every action by full SHA with a version comment
- [ ] **REL-03**: `gradle-build-action` is replaced by `setup-gradle` in release and pre-release
- [ ] **REL-04**: `create-release` / `upload-release-asset` are replaced by `gh release create` with the Confluent archive attached
- [ ] **REL-05**: A dry run proves the release and pre-release paths build and attach the archive without publishing

### Nightly

- [ ] **NIT-01**: A new fork-owned nightly workflow runs `integrationTest` on ClickHouse `latest` × client V2 in local Docker, with no cloud job, and is SHA-pinned
- [ ] **NIT-02**: Upstream `integrationTests.yaml` stays byte-identical and disabled, and a guard check pins that
- [ ] **NIT-03**: The nightly passes, or each failure is classified in a named skip list, on a manual dispatch before the schedule is enabled

### Guard and release

- [ ] **CIG-01**: A check fails when a fork-owned workflow uses an unpinned action
- [ ] **CIG-02**: FORK-DELTA, CHANGELOG, `VERSION=v1.7.0_gg5` and a short rollout note are updated
- [ ] **CIG-03**: CI is green on the published SHA

## Future Requirements

- ClickHouse client `0.10.0` (PR #4)
- Carried review advisories from `v1.7.0_gg4-hardening` (see its close record)
- A CI consumer for the archive compare (`confluent-archive-allowlist-29.txt`)
- A test for a size-named Tuple element whose name also holds an escape character

## Out of Scope

| Feature | Reason |
|---------|--------|
| ClickHouse client 0.10.0 | User excluded it from this milestone |
| Connector source changes | Milestone is CI/release only; keeps the fork rebasable |
| Editing upstream `integrationTests.yaml` | Stays byte-identical to upstream and disabled; a fork-owned nightly replaces it |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|

---
*Requirements defined: 2026-10-24*
