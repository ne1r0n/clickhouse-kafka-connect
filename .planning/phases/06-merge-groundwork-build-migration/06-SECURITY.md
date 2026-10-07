---
phase: "06"
slug: "merge-groundwork-build-migration"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-09-18"
register_authored_at_plan_time: true
---

# Phase 06 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

Register origin: all three plans (`06-01`, `06-02`, `06-03`) carried a `<threat_model>` block
authored at plan time, so this audit **verifies mitigations** rather than retroactively building a
STRIDE register. ASVS L1, `block_on: high`. The short-circuit rule applied
(`threats_open: 0` + `register_authored_at_plan_time: true` + `asvs_level == 1`), so no deeper
auditor pass was required — every status below was derived at L1 grep/git depth by this run.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| upstream git remote → local repo | 588 commits of third-party code plus a replaced `gradle-wrapper.jar` cross into a tree that builds a production connector | Source code, an executed build binary |
| local repo → `origin` (published public fork) | Anything pushed is irreversible for downstream consumers; Phase 6 must not cross this boundary at all | Published git history |
| services.gradle.org → local machine | The wrapper downloads and executes a Gradle distribution named by `distributionUrl` | Executable distribution |
| Maven Central / Gradle Plugin Portal → local build | Upstream's catalog pulls a materially different dependency set, plus a native `protoc` binary fetched at configure time | Dependency artifacts, native binary |
| version catalog → resolved dependency graph | The catalog *declares*; a transitive constraint *resolves* — the two can disagree silently | Coordinate/version facts |
| build script → shipped shaded jar | Anything resolved here ends up inside the artifact production runs | Shipped bytecode |
| generated conflict inventory → checked-in checklist | The document Phases 7–11 work from; under-reporting silently drops a fork behaviour | Scope/coverage claims |
| operator knowledge → recorded fact | One half of the production-config audit is not derivable from the repository | Human-attested configuration |
| phase range history → publication gate | Phase 11 gates publication on this range being marker-free and the checklist `pending`-free | Release gate evidence |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-06-01 | Tampering | `gradle/wrapper/gradle-wrapper.jar` (executed binary arriving by auto-merge) | medium | mitigate | Blob equality against upstream, not merge output | closed |
| T-06-02 | Tampering (of intent) | Conflict resolution across the 40 paths | high | mitigate | D-01 stage-3 whole-file selection + D-02 scripted byte-identity invariant | closed |
| T-06-03 | Repudiation | Published `custom_build` history | high | mitigate | Scratch branch with no remote; `pre-v1.6.0-sync` rollback tag; no push/force-push | closed |
| T-06-04 | Information Disclosure | `.gitignore` union resolution | low | mitigate | `.gsd` entry committed before tagging; both sides' entries survive the union | closed |
| T-06-05 | Tampering | Resolved dependency versions vs. declared catalog values | high | mitigate | D-11/D-12: assert **resolved** graph coordinates, negative-check downgrade arrows | closed |
| T-06-06 | Tampering | `org.slf4j:slf4j-reload4j:2.0.17` — only production coordinate with no catalog entry | medium | mitigate | Kept inline per D-16, asserted present in the resolved graph | closed |
| T-06-07 | Tampering | `protoc` native binary downloaded by the protobuf plugin at configure time | medium | accept | Accepted at ASVS L1 — see AR-01 | closed |
| T-06-08 | Denial of Service | JaCoCo report tasks wired into `check` | low | mitigate | Report-only: no `violationRules`, no `jacocoTestCoverageVerification`, no `check` dependency | closed |
| T-06-09 | Spoofing | Test JVM identity — which JDK actually executes the suite | high | mitigate | BLD-05 `javaLauncher` toolchain pin in the shared `tasks.withType<Test>` block | closed |
| T-06-10 | Tampering | `.planning/MERGE-CHECKLIST.md` Section 1 completeness | high | mitigate | Section 1 generated from committed `conflicted-paths.txt`, diffed path-for-path | closed |
| T-06-11 | Tampering | Conflict markers introduced by post-merge commits | high | mitigate | Whole-range marker scan + single-merge assertion — see CR-01 caveat below | closed |
| T-06-12 | Repudiation | Operator-confirmed half of the production-config audit | medium | mitigate | Explicit `operator-confirmed` source label with dated attribution | closed |
| T-06-13 | Repudiation | Premature `done` flips erasing Phase 11's gate | medium | mitigate | Only the five Phase-6-owned rows flipped; exact `done`-count assertion | closed |
| T-06-14 | Information Disclosure | `## Recorded Facts` contents | low | accept | Accepted at ASVS L1 — see AR-02 | closed |
| T-06-SC | Tampering | Package installs (supply chain) | high | mitigate | RESEARCH.md § Package Legitimacy Audit — every row `OK`, zero `[ASSUMED]`/`[SUS]`/`[SLOP]` | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

### Verification Evidence

Derived directly against the repository by this audit run, not taken from SUMMARY claims:

| Threat | Check | Result |
|--------|-------|--------|
| T-06-01 | `git rev-parse HEAD:gradle/wrapper/gradle-wrapper.jar` vs `v1.6.0:` | Both `b1b8ef56b44f16b14dc800fa8103a6d89abb526f` — identical; `validateDistributionUrl=true` present |
| T-06-02 | `git diff v1.6.0 673d29c3 -- <path>` over all 40 conflicted paths | **0 / 40** differ — D-02 byte-identity invariant holds |
| T-06-03 | `git config --get branch.sync/v1.6.0.remote`; `git rev-list --count pre-v1.6.0-sync..custom_build` | Empty; `0` — published branch untouched, scratch branch has no remote |
| T-06-04 | `.gitignore` union | `.gsd` present at line 44 |
| T-06-05 | Live `./gradlew dependencies` (06-VERIFICATION Truth #10) | `com.clickhouse` 152 hits at `0.9.6`, **0** at `0.9.5`; `connect-api` 15 at `4.1.1`, **0** at `2.7.0` |
| T-06-06 | `build.gradle.kts:80` + resolved graph | `implementation("org.slf4j:slf4j-reload4j:2.0.17")` present; 14 hits resolved |
| T-06-08 | Negative grep | `violationRules`: 0; `jacocoTestCoverageVerification`: 0; both reports `finalizedBy` **`test` only** (`build.gradle.kts:316-318`) |
| T-06-09 | `build.gradle.kts:208` | `javaLauncher.set(javaToolchains.launcherFor { … })` inside `tasks.withType<Test>` |
| T-06-10 | Section 1 row count vs `conflicted-paths.txt` | **40 data rows == 40 paths**, 1:1, no under-reporting |
| T-06-11 | Whole-range marker scan, **corrected exit-code handling** | **592 commits scanned, 0 marker hits, 0 git errors**; first-parent merges in range == **1** (`673d29c3`) |
| T-06-12 | `MERGE-CHECKLIST.md:78` | `operator-confirmed` label with dated 2026-09-18 attribution present |
| T-06-13 | `done` / `pending` tally | `done` = **5** exactly; `pending` = 43; total 48 = 40 (§1) + 8 (§2) |
| T-06-SC | RESEARCH.md § Package Legitimacy Audit | All 4 rows `OK`; zero package rows carry `[ASSUMED]`/`[SUS]`/`[SLOP]` |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-06-07 | The protobuf plugin and its `libprotobuf` version come from upstream's own published, git-tracked catalog, and upstream production CI already exercises this `protoc` download. The fork adds no new artifact source. Accepted at ASVS L1 with no control beyond existing HTTPS-to-Maven-Central transport. | Plan 06-02 threat model | 2026-09-18 |
| AR-02 | T-06-14 | `## Recorded Facts` holds dependency versions, config-key defaults and a build version string — no credentials, endpoints or connection strings. Accepted at ASVS L1 with no additional control. | Plan 06-03 threat model | 2026-09-18 |

---

## Carried Defects (not blocking Phase 6)

| ID | Component | Severity | Finding | Why not blocking here | Must resolve by |
|----|-----------|----------|---------|------------------------|-----------------|
| CR-01 | `.planning/scripts/verify-merge-invariants.sh:44` | Critical (control defect) | `git grep -q … 2>/dev/null` treats an error exit (≥2) identically to "no match" (1), with stderr suppressed, so Invariant 2 **fails open**: a broken pathspec or unreadable commit reads as "clean". | The *property* the control protects was independently re-derived by this audit with corrected exit-code branching — 592 commits, 0 markers, 0 git errors — so SYNC-06 genuinely holds for this repository state. The defect is latent, not active. | **Phase 11**, which gates publication on this script running unattended. Fix before then. |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-09-18 | 15 | 15 | 0 | /gsd-secure-phase (verify:post hook, L1 short-circuit) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-09-18
