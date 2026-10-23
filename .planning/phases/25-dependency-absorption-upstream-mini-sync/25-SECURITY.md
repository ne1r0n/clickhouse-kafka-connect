---
phase: "25"
slug: "dependency-absorption-upstream-mini-sync"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-21"
---

# Phase 25 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| User's browser and chat -> evidence file (25-01) | The operator fact crosses only as the user's typed words or a screenshot | Operator answer text, public |
| Public GitHub REST API -> evidence file (25-01, 25-08) | Unauthenticated JSON is the only machine view of labels, PRs and comments | Public repository metadata |
| Build script text -> pin-guard rules (25-02, 25-06) | The guard parses an editable file; a misread flips a live task to "commented out" or the reverse | Build and workflow text |
| origin/custom_build (user-merged Dependabot commits) -> working branch (25-03) | Commits made outside the GSD flow enter the fork's tree | Dependency bumps, wrapper files |
| Gate stderr -> exemption rows (25-03) | Machine output becomes a forgiveness list for the anchors invariant | SHA, path, pattern rows |
| services.gradle.org -> wrapper check (25-03) | A published checksum is the reference for the executed wrapper jar | SHA-256 |
| Gradle/Shadow plugin behaviour -> shipped archive (25-04) | A plugin upgrade changes the bytes operators deploy | Confluent archive |
| Built archives -> compare script (25-04, 25-08) | Zip files are parsed by a committed tool | Archive entries |
| Docker registry image -> extracted jars (25-05) | Third-party bytecode becomes the reference for a compatibility proof | Worker jars |
| Compiled connector classes -> reference list (25-05) | The list blesses what the connector may call on the worker | Class and member names |
| Build output -> published Confluent archive (25-06) | What CI builds is what operators install | Confluent archive |
| Upstream repository -> working branch (25-07) | Third-party commits enter the fork's tree | Upstream source |
| Conflict resolution -> catalog values (25-07) | A wrong side silently downgrades a pinned dependency or drops guava | Version catalog |
| NVD, OSV and GitHub REST -> FORK-DELTA wording (25-08) | Third-party advisory records become operator-facing claims | Advisory JSON |
| Resolved dependency graph -> shipped archive and runtime classpath (25-08) | Transitive versions decide which vulnerable code ships | Resolved versions |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-25-01 | Spoofing | E-02 answer | medium | mitigate | Task 2 was a `blocking-human` gate answered by the user directly in session; E-02 carries the "given directly to the orchestrator" header with a UTC timestamp beside the REST re-read (200, name `dependencies`) | closed |
| T-25-02 | Elevation of Privilege | GitHub repository labels and settings | medium | mitigate | Only unauthenticated `curl` GETs across the phase; `gh` is not installed; no agent-side label, PR or `dependabot.yml` change (E-01, E-02, E-25) | closed |
| T-25-03 | Information Disclosure | evidence file | low | mitigate | No credential used; the evidence file scans clean for token shapes (`gh*_`, `github_pat_`) at every plan's verify and at phase close | closed |
| T-25-04 | Tampering | `TestsWorkflowPinGuardUnitTest` assertions | medium | mitigate | All 26 pre-DEBT-13 methods survive; the class grew 29 -> 33 -> 39 tests across 25-02 and 25-06 with no method removed (E-04, E-16, E-18) | closed |
| T-25-05 | Repudiation | DEBT-13 red-first proof | medium | mitigate | E-03 records the red run (exactly the three predicted failures of 29) before the fix; tests and fix share commit `92b2be1e` | closed |
| T-25-06 | Tampering | merge input (origin tip) | high | mitigate | `git ls-remote origin custom_build` read before the merge; M1 `50f56289` has second parent `69cd680b` and `git show --cc --name-only` lists only the two anchor files (E-07) | closed |
| T-25-07 | Tampering | anchors gate and exemptions | high | mitigate | `verify-merge-invariants.sh` and its self-test are byte-identical to `pre-v1.7.0-deps`; rows generated from the gate's own stderr; gate on M1 ends `anchor_exempt=19 result=PASS` (E-06, E-07) | closed |
| T-25-08 | Tampering | `gradlew.bat`, staging | medium | mitigate | Explicit-path staging only; `HEAD:gradlew.bat` blob equals `origin/custom_build:gradlew.bat`; the CRLF working-tree diff stays unstaged | closed |
| T-25-09 | Tampering | Gradle wrapper jar | medium | mitigate | SHA-256 of `gradle/wrapper/gradle-wrapper.jar` equals Gradle's published `gradle-9.8.1-wrapper.jar.sha256` (E-08) | closed |
| T-25-10 | Information Disclosure | accidental publication | medium | mitigate | `git ls-remote --tags origin` shows none of `pre-v1.7.0-deps`, `pre-v1.7.0-tail`, `baseline-v1.7.0_gg4`; no branch pushed | closed |
| T-25-11 | Tampering | Confluent archive (nested-jar regression) | high | mitigate | Repaired archive measured 7,395 entries, 0 nested jars, 0 Kafka entries; two `confluentJar` anchor rows in `fork-anchors.txt`; the unrepaired control (135 entries, 25 nested) proves the measurement can fail (E-09, E-10); 25-06 added the build-time guard | closed |
| T-25-12 | Tampering | allow-list masking a real change | high | mitigate | `confluent-archive-allowlist-25.txt` has 28 rows, each naming a version bump, no catch-all and no `com/clickhouse/` row; stale rows fail (`STALE-ROW` control); compare `RESULT: PASS` against gg3 at 25-04 and 25-08 (E-11, E-22, E-24) | closed |
| T-25-13 | Tampering | archive parsing | low | mitigate | `compare-confluent-archives.py` reads archives with `zipfile` under `python3 -I`; no extraction, no subprocess | closed |
| T-25-14 | Spoofing | worker image jars | medium | mitigate | Image pulled by tag, RepoDigest `sha256:93b9afd3e7f8...` recorded in E-12; jars copied to fresh directories, hashed, amd64 and native byte-identical, read only by `javap` and `zipfile` | closed |
| T-25-15 | Denial of Service | connector load on the Kafka 3.9 worker | high | mitigate | Bless run: `REFERENCE worker classes-missing 0 members-missing 0` for 176 references; `ConnectApiReferencesUnitTest` in `testUnit` fails on any new reference (seen red); negative control reports 17 classes and 37 members missing (E-12, E-13) | closed |
| T-25-16 | Repudiation | Connect API wording | medium | mitigate | Wording commit `8a598ba3` lands after E-12 and FORK-DELTA cites E-12; the remaining "4.1.1" mention names it as absent from the compile-time jars, not as the production API | closed |
| T-25-17 | Denial of Service | leftover containers | low | mitigate | `docker ps -a --filter name=gsd25` lists nothing | closed |
| T-25-18 | Tampering | CI archive step bypass | high | mitigate | Pin guard rejects a missing or renamed step, renamed task, echoed or guarded call, `continue-on-error: true`, and requires the finalizer inside the `createConfluentArchive` block; 12 fixtures seen red (E-15, E-16) | closed |
| T-25-19 | Tampering | pin-guard weakening | medium | mitigate | Every pre-existing test method survives (33 -> 39 in 25-06); E-18's accept-set table records 6 widened and 8 narrowed shapes with 8 must-still-reject controls | closed |
| T-25-20 | Tampering | Kafka API classes bundled in the archive | high | mitigate | `ConfluentArchiveCheck` rejects any `org/apache/kafka/` entry; `ConfluentArchiveCheckUnitTest` (6 tests) re-proves it per `testUnit`; the Connect API negative control fired with 4,560 violations through the real task (E-14) | closed |
| T-25-21 | Elevation of Privilege | workflow token and actions | medium | mitigate | `tests.yaml` gained one `run:` step; `uses:` count stays 5, `permissions:` unchanged, no upload, no second job | closed |
| T-25-22 | Tampering | catalog resolution | high | mitigate | Post-M2 catalog holds `kafka = "4.4.0"`, `guava = "33.7.2-jre"`, `httpclient = "5.6.5"`, no `httpclientTest`; real M2 reproduces the rehearsal's tree (E-20, E-21) | closed |
| T-25-23 | Tampering | authored lines inside the merge | high | mitigate | M2 `38265d26` changes only `.gitignore`, `build.gradle.kts`, `gradle/libs.versions.toml`; records in the next commit; tail gate `provenance_paths=3 ... result=PASS`; gate script unchanged (E-21) | closed |
| T-25-24 | Spoofing | upstream ref | medium | mitigate | `refs/upstream/*` absent; the merge and `MERGE-CHECKLIST-v1.7.0-tail.md` use the full SHA `a6f1df839c079180bc7c343b5823f205e204dafc` (E-20, E-21) | closed |
| T-25-25 | Repudiation | suite result | medium | mitigate | E-19 records `--rerun` with fresh XML newer than the last code commit, skip set compared by name (exactly 18), one server version 26.9.19.9 | closed |
| T-25-26 | Denial of Service | vulnerable transitive guava/httpclient5 | high | mitigate | `dependencyInsight` in E-22: guava `33.4.6-jre -> 33.7.2-jre`, httpclient5 `5.4.4 -> 5.6.5` on `runtimeClasspath` and `clickhouseDependencies`; archive VERSION table shows the shipped versions | closed |
| T-25-27 | Repudiation | advisory wording in FORK-DELTA | medium | mitigate | Advisory facts labelled by source (NVD, OSV, GitHub release); no "every scanner" claim and no exposure assessment in FORK-DELTA (E-22) | closed |
| T-25-28 | Tampering | slf4j-reload4j anchor move | medium | mitigate | Commit `9c9b480b` moves the catalog row and the `fork-anchors.txt` row together; the anchors gate still passes (E-23) | closed |
| T-25-29 | Spoofing | OPS-04 closing record | low | mitigate | E-25 verdict `pending (...)` is consistent with its REST facts: no Dependabot PR created after `2026-10-21T15:11:05Z`, label returns 200 | closed |
| T-25-30 | Information Disclosure | accidental publication of tags | medium | mitigate | `git ls-remote --tags origin baseline-v1.7.0_gg4` is empty; tags are lightweight and local (E-24) | closed |
| T-25-31 | Tampering | drift between build-run and test-run archive rules | medium | mitigate | Rules live once in `ConfluentArchiveCheck`; `verifyConfluentArchive` runs it by `mainClass` and the pin guard requires that line; the other-main-class fixture was seen red (E-15) | closed |
| T-25-SC | Tampering | npm/pip/cargo installs | low | accept | No package manager runs in any plan; every new coordinate arrived through the user-merged Dependabot PRs (#6-#9, #11-#15) or upstream's tail (research Package Legitimacy Audit) | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-25-01 | T-25-SC (all eight plans) | No plan installs a package; dependency movement is limited to coordinates already merged on `origin/custom_build` by the user or carried by upstream's tail, each checked in 25-RESEARCH's Package Legitimacy Audit | plan author (25-RESEARCH, 25-0x-PLAN) | 2026-10-21 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-21 | 32 | 32 | 0 | execute-phase orchestrator (verify:post secure-phase step, L1 grep depth, short-circuit: register authored at plan time, ASVS 1) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-21
