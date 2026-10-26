# Phase 25: Dependency Absorption & Upstream Mini-Sync - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-21
**Phase:** 25-dependency-absorption-upstream-mini-sync
**Areas discussed:** Shadow 9 repair shape, CI-20 nested-jar guard, connect-api 3.9 proof, Gate range & catalog key

---

## Shadow 9 repair shape

### Multi-Release manifest attribute

| Option | Description | Selected |
|--------|-------------|----------|
| Keep it off | `addMultiReleaseAttribute.set(false)`; MANIFEST.MF byte-equal to gg3 apart from versions; no `META-INF/versions/**` activation | ✓ |
| Let Shadow set it | Accept `Multi-Release: true`; multi-release classes of bundled libraries become active; rollout line and wider allow-list | |

**User's choice:** Keep it off (recommended).

### Stray `-all.jar` on `assemble`

| Option | Description | Selected |
|--------|-------------|----------|
| Suppress it | `shadow { addShadowJarToAssembleLifecycle.set(false) }`; no fat jar in build/libs; shorter assemble; one fork-only line | ✓ |
| Leave the default | Closer to upstream; jar built on every assemble but never shipped; record the decision | |

**User's choice:** Suppress it (recommended).

### Pinning the fork-only repair

| Option | Description | Selected |
|--------|-------------|----------|
| Positive + negative anchor | `configurations.add(clickhouseDependencies)` = 1 and `from(clickhouseDependencies, ` = 0 in fork-anchors.txt, plus FORK-DELTA row; a sync restoring upstream's line fails the merge gate | ✓ |
| FORK-DELTA row only | Document the repair; rely on CI-20 to catch a regression in CI | |

**User's choice:** Positive + negative anchor (recommended).

### Archive compare tooling

| Option | Description | Selected |
|--------|-------------|----------|
| Committed under .planning/scripts | `python3 -I` script with two archive paths and an allow-list; prints only-in-a, only-in-b, changed, nested-jar count; reused by Phase 29 | ✓ |
| Throwaway in the evidence file | Ad hoc compare in /tmp, transcript pasted into evidence; Phase 29 re-derives it | |

**User's choice:** Committed under .planning/scripts (recommended).

**Notes:** The repair shape itself (`configurations.add` first, `zipTree` as fallback) was already fixed by the roadmap research flag and was not re-asked.

---

## CI-20 nested-jar guard

### Where the check lives

| Option | Description | Selected |
|--------|-------------|----------|
| Gradle verify task + CI step | `verifyConfluentArchive` task, `createConfluentArchive` finalizedBy it; named tests.yaml step runs createConfluentArchive; runs locally, in CI and in GSD gates; pin guard pins step and task | ✓ |
| Shell step in tests.yaml only | Workflow step with `unzip -l` and a grep; CI-only guard | |
| Python script under .github/scripts | Like jacoco_summary.py; testable, but CI-only unless Gradle calls it | |

**User's choice:** Gradle verify task + CI step (recommended).

### What the task rejects

| Option | Description | Selected |
|--------|-------------|----------|
| Nested jars + Connect API classes | Fail on any `*.jar` entry and any `org/apache/kafka/connect/**` class; BLD-16 "not bundled" becomes a standing guard | ✓ |
| Nested jars only | Exactly CI-20's text; Connect API absence stays a one-time transcript | |
| Nested jars + minimum entry count | Add a committed entry-count floor (~7,000); needs maintenance at each dependency change | |

**User's choice:** Nested jars + Connect API classes (recommended).

### Step placement

| Option | Description | Selected |
|--------|-------------|----------|
| Before 'Run tests' | After 'Compile integration-test sources'; fails in about a minute; warms compile outputs | ✓ |
| After 'Run tests' | Test results first; archive checked only on a green suite | |
| A separate job | Own runner; changes the one-run-per-SHA shape, the runs-on pin and the job rules | |

**User's choice:** Before 'Run tests' (recommended).

### Pin-guard shape

| Option | Description | Selected |
|--------|-------------|----------|
| Step + task text, red-first | Step by name with a `createConfluentArchive` call; `verifyConfluentArchive` registered with `finalizedBy`; fixtures for missing step, renamed task, dropped finalizedBy, each seen red | ✓ |
| Step presence only | Only require a step that runs createConfluentArchive | |

**User's choice:** Step + task text, red-first (recommended).

---

## connect-api 3.9 proof

### Which jar stands for the worker's API

| Option | Description | Selected |
|--------|-------------|----------|
| Jar from the cp-kafka-connect-base:7.9.7 image | `docker create` + `docker cp`; the API the connector actually loads against, including any `-ccs` difference | ✓ |
| Apache connect-api 3.9.x from Maven Central | Reproducible without Docker; not the `-ccs` build | |
| Both, cross-checked | Image jar as reference, shown member-identical to the Apache artifact | |

**User's choice:** Jar from the cp-kafka-connect-base:7.9.7 image (recommended).

### Member-reference method

| Option | Description | Selected |
|--------|-------------|----------|
| javap constant-pool extraction | `javap -v` over compiled classes; collect Methodref/Fieldref/Class under org.apache.kafka; check against the image jars and 4.1.1; JDK-only; script committed | ✓ |
| jdeps package-level only | Class-level use only; cannot prove a member claim | |
| Smoke-load on the image | Install the archive in a 7.9.7 container and check plugin discovery; does not replace the static check | |

**User's choice:** javap constant-pool extraction (recommended).

### Durability

| Option | Description | Selected |
|--------|-------------|----------|
| Script + committed reference list | Script writes the referenced-member set to a committed file; a Docker-free testUnit case re-derives it from build/classes and fails on a new member | ✓ |
| Transcript only | One-off javap compare pasted into evidence | |
| Script committed, no test | Keep the script, re-run manually at each release gate | |

**User's choice:** Script + committed reference list (recommended).

### Reference scope

| Option | Description | Selected |
|--------|-------------|----------|
| connect-api + kafka-clients | Every referenced member under org.apache.kafka.** checked against both image jars | ✓ |
| connect-api only | Exactly BLD-16's text; kafka-clients skew unproven | |

**User's choice:** connect-api + kafka-clients (recommended).

---

## Gate range & catalog key

### Standing anchors-gate command for Phases 26–29

| Option | Description | Selected |
|--------|-------------|----------|
| Tail gate standing, cumulative twice | `verify-merge-invariants.sh <merge2> HEAD` with PRE_TAG=pre-v1.7.0-tail, UPSTREAM_REF=a6f1df83 and the tail conflicted-paths file as the standing gate; cumulative `54e63648 HEAD` here for DEPS-03 and once more at the Phase 29 release gate | ✓ |
| Cumulative standing | Keep `54e63648 HEAD` from pre-v1.7.0-sync as the only gate; tail gate once for SYNC-11 | |
| Both at every gate | Run both at each phase gate | |

**User's choice:** Tail gate standing, cumulative twice (recommended).

### DEBT-15 catalog shape

| Option | Description | Selected |
|--------|-------------|----------|
| Share the slf4j key | Library row `slf4j-reload4j` with `version.ref = "slf4j"`; `implementation(libs.slf4j.reload4j)`; no new [versions] key; anchor row moves to the catalog line | ✓ |
| Separate slf4jReload4j key | Independent version key; second fork-only key in the conflict-prone block | |

**User's choice:** Share the slf4j key (recommended).

### Timing of the BLD-16 wording edits

| Option | Description | Selected |
|--------|-------------|----------|
| After the bytecode evidence | Separate commit once the javap compare has run; merge 1 carries only bookkeeping (Phase 21 D-03 shape) | ✓ |
| Inside the merge-1 commit | Bundle the wording with the anchor edits before the proof exists | |

**User's choice:** After the bytecode evidence (recommended).

### SYNC-12 baseline marker

| Option | Description | Selected |
|--------|-------------|----------|
| Local tag + STATE entry | Never-pushed tag such as `baseline-v1.7.0_gg4` at the tip of the two green --rerun runs, plus the SHA in STATE.md | ✓ |
| SHA in STATE.md only | No tag; later references copy the 40-hex string | |

**User's choice:** Local tag + STATE entry (recommended).

---

## Claude's Discretion

- DEBT-13 reader design (character scanner, line-level, no Kotlin parser; inline text-block fixtures).
- DEBT-14 regex and layout details within the red-first rule.
- Exemption-row reasons, the pre-merge-1 tag name, the tail conflicted-paths file name.
- How `verifyConfluentArchive` reports its findings.
- Location and name of the Connect-member reference list and its unit test.
- Plan split, commit order within the fixed six-step order, evidence file layout, throwaway-worktree rehearsal.

## Deferred Ideas

- Uploading the built Confluent archive as a CI artifact.
- Kotlin DSL deprecations from Gradle 9.8.1 `--warning-mode all` (DEPS-F3).
- Dependabot PR #10 (protobuf 4.x) and PR #4 (client 0.10.0).
- Cross-checking the image's connect-api jar against the Apache 3.9.x artifact.
