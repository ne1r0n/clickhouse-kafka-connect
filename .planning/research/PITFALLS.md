# Pitfalls Research

**Domain:** Maintenance milestone on a forked Kafka Connect sink (`custom_build`, `v1.7.0_gg5` -> `v1.7.0_gg6`): ClickHouse Java client `0.9.6` -> `0.11.0`, SHA-pinned GitHub Actions, Gradle wrapper/Shadow/Confluent archive, protobuf 3 -> 4 in testFixtures, an upstream tail merge, Dependabot PRs on `origin`.
**Researched:** 2026-10-28
**Confidence:** HIGH for findings measured on this repo or read from the Dependabot PR runs; MEDIUM for client-behaviour findings taken from release notes only (not yet run against this connector); LOW items are labelled.

Phase names below are *suggested* roadmap slots, in the recommended order:
**A** Dependabot low-risk sweep (wrapper, lz4, okhttp, setup-java, protobuf decision) -> **B** upstream tail merge -> **C** client `0.11.0` audit and bump -> **D** carried advisories and CI loose ends -> **E** release and publication.

## Critical Pitfalls

### Pitfall 1: Merging protobuf-java 4.x as a "testFixtures only" bump (PR #10)

**What goes wrong:**
PR #10 looks harmless because only `gradle/libs.versions.toml` changes and the dependency is `testFixturesApi`. It is not. Its own CI run (39501799049, 2026-10-20) failed **27 `ProtobufIngestTest` cases** with `java.lang.VerifyError: Bad type on operand stack ... io/confluent/kafka/schemaregistry/protobuf/ProtobufSchema.toEnum ... 'DescriptorProtos$EnumValueOptions' is not assignable to 'GeneratedMessageV3$ExtendableMessage'`. Confluent `kafka-protobuf-serializer`/`kafka-connect-protobuf-converter` 7.9.1 are compiled against protobuf 3.x classes that 4.x removed or reshaped.

**Why it happens:**
- `libprotobuf` is one version ref feeding **both** the runtime jar (`protobuf.java`) and `protoc` (`build.gradle.kts` line 541, `com.google.protobuf:protoc:${libs.versions.libprotobuf}`). Bumping the ref moves the code generator too.
- The Confluent test stack pins its own protobuf line. A runtime jar bump past what the serializer was built for breaks at class-verification time, not at compile time.
- Dependabot's compatibility badge has no data for this pair, and "testFixtures only" reads as low risk.

**How to avoid:**
- Treat #10 as a decision, not a merge. Default recommendation: **close it and add a Dependabot ignore for the protobuf-java major** (the bump is not possible until the Confluent test stack, `kafkaPlatformProtobuf`/`kafkaPlatformSchemaRegistry` `7.9.1`, moves to a release built on protobuf 4). Whether Confluent Platform 8.x does that is unverified (LOW); check before promising it in a later milestone.
- If someone insists on it, it is a three-way coupled bump (protobuf-java + protoc + Confluent serializers) and its own phase, with `ProtobufIngestTest` (27 cases) as the gate.
- If a Dependabot `ignore` is added, edit `.github/dependabot.yml` only in the `gradle` block. `WorkflowActionPinGuardUnitTest.dependabotExcludesOnlyTheUpstreamIdenticalWorkflow` fails if the `github-actions` block changes.
- Record the decision in FORK-DELTA's pin rows so the next sync does not re-litigate it.

**Warning signs:**
- `ProtobufIngestTest` red with `VerifyError` or `NoSuchMethodError` rather than an assertion failure.
- The PR shows `mergeable: UNKNOWN` and an old red run: nobody has looked at it since 2026-10-20.

**Phase to address:** A. Decide first; it is the only Dependabot PR with a known-red run.

---

### Pitfall 2: Reading "PR CI green" or "main compiles" as "the client bump is safe"

**What goes wrong:**
PR #17 (`clickHouseDriver` 0.11.0) failed in 57 seconds at `:compileTestJava`: `new ServerException(252, "Too many parts", 500)` no longer compiles, because the constructor is now `(int, String, int, String)` (`UtilsTest.java:80, 89, 98`). `src/main` compiled. Compile errors stop the build before **any** test runs, so the PR run says nothing about runtime behaviour, and javac may be hiding further test-compile errors behind the first batch.

**Why it happens:**
The suite is the acceptance gate (PROJECT.md core value), and a gate that cannot start is a gate that reports nothing. The fork's earlier lesson ("a gate running `true` is worse than no gate") applies in reverse: a red-at-compile PR tempts the author to "fix the compile and see", then stop at the first green.

**How to avoid:**
- Fix test-compile first, in its own commit, then run `testUnit` (1,374 tests, no Docker) and the full suite (1,961 tests) on `latest` x V2 **twice with `--rerun`** (Gradle cache replays the first run otherwise).
- Prefer a small adapter in a fork-owned test helper over editing upstream-owned test files (`UtilsTest` is upstream's; the fork's rebasability constraint says keep changes additive). Expect upstream's tail to add more `ServerException` constructions in `UtilsTest` (+77 lines), see Pitfall 5.
- Audit semantics, not only compilation, against the 0.10.0 and 0.11.0 notes: see Pitfalls 3, 4 and 6.

**Warning signs:**
- A claim of "green" that cites a run which did not reach the `Test` task.
- `grep -rn "new ServerException(" src` still shows 3-argument calls after the bump.

**Phase to address:** C.

---

### Pitfall 3: The compression story changes under 0.11.0 and the pinned claims go stale (or quietly red)

**What goes wrong:**
`setCompressionMethod` (formerly D-17) exists because ClickHouse 26.9+ defaults `network_compression_method` to ZSTD and client `0.9.6` only decodes LZ4 (`Invalid LZ4 magic byte: '-112'`). 0.11.0 **adds ZSTD block compression** and `zstd-jni` as a new compile dependency, and changes "requests not compressed by default (previously LZ4 only)". Consequences:
1. Claims written into live-test Javadoc, `FORK-DELTA.md` (rows ~116, 250), the gg1 rollout note and `Lz4RoundTripLiveTest` / `NetworkCompressionPinLiveTest` ("client 0.9.6 only decodes LZ4", "removing both on V2 with `compress=true` fails with the magic-byte error") become false statements on 0.11.0. The operator hazard "`compress=true` + `setCompressionMethod=false` against 26.9+ fails reads" probably no longer exists. Per the retrospective, quantifiers in operator docs that do not match the measurement were the dominant review-round defect class.
2. Someone sees ZSTD support and "simplifies" by deleting the lz4 setting or flipping the `setCompressionMethod` default. That is a behaviour change for every operator on a worker that also still talks to older servers, and it breaks anchors CMP-01/CMP-02 and `NetworkCompressionPinUnitTest`.
3. Any live pin that *asserts a failure* on the stripped configuration flips red the moment the client can decode ZSTD, which looks like a regression and invites "fixing" the wrong side.

**Why it happens:**
The workaround was measured against one client/server pair (Recorded Facts 25-26). The measurement is not portable to a new client.

**How to avoid:**
- Re-run the stripped-both and lz4-only matrix on 0.11.0 x `latest` for V1 and V2 **before** editing anything, and record the result as new Recorded Facts. Then change the text, not the behaviour.
- Keep the default (`setCompressionMethod=true`, `compress=false` unconditional). Behaviour preservation is the milestone constraint; the compat flag is cheap.
- Update the claims in one commit: Javadoc, `FORK-DELTA.md`, rollout note. Mark them "measured on 0.11.0" or "read from the code, not measured".
- If a pin asserted the old failure, convert it to assert the new measured outcome (do not delete: "only add new tests, never delete/weaken existing ones" in AGENTS.md; document the replacement in the commit).

**Warning signs:**
- `NetworkCompressionPinLiveTest` or `Lz4RoundTripLiveTest` changes outcome on the bump with no code change.
- A plan task titled "remove D-17 leftovers" or "drop lz4 setting".

**Phase to address:** C (audit + pins), E (operator-doc wording).

---

### Pitfall 4: Client-side retry/classification changes alter `Utils.handleException` outcomes without any code change

**What goes wrong:**
Release-note items that interact with `Utils.handleException` and the exactly-once retry path:
- **0.11.0 (#3072): server error 159 (`TIMEOUT_EXCEEDED`) is no longer retried by the client** (own cause `ServerTimeoutExceeded`, excluded from the default `client_retry_on_failures`). The connector already lists 159 as retriable (`RetriableException`), so the batch still redelivers, but the *first* retry layer (client, inside one `put`) is gone: a timeout now surfaces immediately and Connect redelivers with **no retry budget**, which means more `BEFORE_PROCESSING` re-entries on a slow server. Safe by design (same dedup token, identical batch) but visible as extra load and log noise.
- **0.10.0: HTTP 503 now surfaces as `ConnectException` and is retried by default** (moved from `ServerRetryable` to `ConnectTimeout`). Retries now happen inside the client, extending the time a single `put` blocks. Check against `consumer.override.max.poll.interval.ms`-style limits.
- **0.10.0: unexpected HTTP status codes throw `ClientException` instead of `ServerException`.** `Utils.isClickHouseException` is `instanceof ClickHouseException || ServerException`; a `ClientException` from a proxy/LB 502/504 skips the error-code switch and falls into the generic branches. Classification of those responses may change from "not in the list" to "not recognised at all".
- **0.10.0: `Client.Builder#build()` throws `ClientMisconfigurationException` instead of `IllegalArgumentException`** for auth/SSL misconfiguration, and rejects username/password combined with a custom `Authorization` header. The connector forwards arbitrary `jdbcConnectionProperties` pairs into `setOption`, so an operator who put `http_header_authorization` there now fails at `start()`.
- **0.10.0: `executionTimeout` is consistently milliseconds** (was seconds on some paths). The connector sets none, but an operator-supplied `jdbcConnectionProperties` value may now mean a different timeout.

**Why it happens:**
The fork's retry classification is pinned by `WriterFailureClassificationUnitTest` / `UtilsNullMessageClassificationUnitTest` with constructed exceptions, not by what the real client throws on the wire. A client bump changes the wire-to-exception mapping while the constructed-exception pins stay green.

**How to avoid:**
- Add **live** classification pins before the bump (against 0.9.6) and run them after: real timeout (159), real 503/502 via a proxy fixture or Toxiproxy, real `TOO_MANY_PARTS` (252), real auth failure. Compare outcomes (`RetriableException` vs `DataException` vs `RuntimeException`).
- Explicitly record, per item above, "behaviour changed / unchanged" in the behaviour ledger so the rollout note can state it.
- Decide, with the user, whether the connector should set `client_retry_on_failures` to keep `ServerTimeoutExceeded` retried in-client (a new behaviour; default is "pin current connector-level behaviour, document the client change").
- Pin the "retry classification stays unchanged" claim again the way gg3 did (before the change, both directions).

**Warning signs:**
- Soak/nightly logs show more `Deciding how to handle exception` + `ClickHouse server error code: 159` lines with no config change.
- `put()` latency histograms (`SinkTaskStatistics`) lengthen under a flapping LB.

**Phase to address:** C.

---

### Pitfall 5: Upstream tail merge compiles textually clean but breaks fork tests and collides with the client bump

**What goes wrong:**
`upstream/main` (`9bed6f79`) changes `Utils.handleException` from 4 to **5 parameters** (inserting `Set<Integer> additionalRetriableErrorCodes`), refactors the retriable codes into `Utils.DEFAULT_RETRIABLE_ERROR_CODES`, edits `ChunkFlusher`, `ProxySinkTask` (adds a package-private 3-arg constructor taking a `DBWriter`) and adds `additionalRetriableErrorCodes` to `ClickHouseSinkConfig` (a high-risk file; the fork differs from upstream there by 104 lines). Fork-owned tests call the 4-arg form: `WriterFailureClassificationUnitTest`, `UtilsNullMessageClassificationUnitTest`, `WriterErrorPathLiveTest`, `JsonInsertColumnsLiveTest`, `ClickHouseSinkTaskTest`. Git will merge these cleanly and the build will fail on `compileTestJava`. The gg1 lesson stands: the most dangerous merge changes are the ones that compile; here the first danger is the one that does not, the second is the one that does.

Silent-vanishing candidates in the tail (check each with the anchors gate and a diff, not by eye):
- The fork's `Utils.handleException` edits (Jackson/`WithheldMessageException` handling; fork differs from upstream by 57 lines in `Utils.java`) being overwritten or merged around the new `if (DEFAULT... || additional...)` block.
- The `//this.addClickHouseSetting("async_insert", "0", false);` anchor (CFG-02, count 1) inside `ClickHouseSinkConfig`.
- `CLICKHOUSE_VERSION_DEFAULT` / `CLICKHOUSE_VERSION_LATEST` in `ClickHouseTestHelpers` (not in the tail's file list today, but upstream's `CreateTableStatement` refactor sits beside it; this is the original silent reversion precedent).
- 27 upstream test files change (`ClickHouseSinkTask*Test`, `ClickHouseHelperClientTest`, `TableTest`...). Several are files the fork also edits; "Existing tests: only add new tests" means resolve toward keeping both.

Interaction with Pitfall 2: upstream's new `UtilsTest` cases (+77 lines) will construct `ServerException` with the old 3-arg constructor. Merge the tail **against `0.9.6`** first, make it green, and only then bump the client, otherwise two independent compile breaks stack and cannot be attributed.

**Why it happens:**
The fork keeps upstream's high-risk files nearly identical and relies on a gate to prove it. The tail has no tag, so there is no release-notes anchor; the diff is the only spec.

**How to avoid:**
- Standard recipe, no shortcuts: rollback tag `pre-v1.7.0_gg6` -> scratch branch -> single mechanical merge commit with `verify-merge-invariants.sh` (parameterised `PRE_TAG`/`UPSTREAM_REF`) -> per-hunk `MERGE-CHECKLIST` for auto-merged hunks in fork-touched files (`Utils.java`, `ClickHouseSinkConfig.java`, `ProxySinkTask.java`, `ChunkFlusher.java`) -> re-apply by intent in separate bisectable commits -> two full `--rerun` runs.
- Re-measure fork-anchor counts after the merge, and generate any exemption rows from the gate's own output (gg4 pattern). Never hand-edit `fork-anchors.txt` to make the gate pass in the merge commit.
- Fix the 4-arg callers by adding a fork-side overload or by updating the fork tests in a separate "re-apply" commit; do not weaken the assertions.
- Cover the new config: `NewUpstreamConfigDefaultsUnitTest` (Docker-free default pin) gets `additionalRetriableErrorCodes=""`, and add the key to the operator config table in the rollout note. The option's own help text says it "does not go through errors.tolerance or the DLQ": say so in the rollout note.
- Remember `refs/upstream/main` ambiguity was fixed in gg4; use `upstream/main` explicitly and re-check `git rev-parse` before merging because the tail may have moved since `9bed6f79`.

**Warning signs:**
- `compileTestJava` fails on `handleException` with "cannot be applied to given types" right after the merge commit.
- The anchors gate reports a count change in `ClickHouseSinkConfig.java` or `Utils.java` without a planned move.
- Local `sync/v1.7.0` is 21 commits ahead of `origin/custom_build` by first parent plus two release merges the other way: confirm which side the merge base is on before starting.

**Phase to address:** B (merge), then C (re-measure after the bump).

---

### Pitfall 6: A client bump changes serialization or the exactly-once batch identity without anyone looking at bytes

**What goes wrong:**
The connector's RowBinary writer is hand-rolled over `BinaryStreamUtils` (`clickhouse-data`) and the client-v2 insert path. 0.10.0/0.11.0 changed several serialization-adjacent behaviours: nullable markers for nested `Tuple`/`Map` (#2721), `Variant`/`Dynamic` NULL handling (#2789, #2791), Dynamic `BigDecimal` width inference (#2966), `Float`/`Enum`/`Array` null handling and UUID parsing (#2930, #2931, #2938, #3132), plus the "client requests not compressed by default" change. The fork's most hand-tuned code is exactly here: nested-null rules, `Nullable(Enum|Tuple)`, `Dynamic`, `MetadataFreeJsonWriter`, and the "byte-for-byte identical batch on retry" invariant (AGENTS.md).

Where it bites: a batch that is `BEFORE_PROCESSING` when the worker is rolled from the 0.9.6 connector to the 0.11.0 connector is retried with the **same dedup token** but potentially **different bytes**. This is the gg2 hazard ("on deduplicating engines it is dropped even if its bytes changed") re-created by a client change instead of a connector change. The rollout note must say so.

**Why it happens:**
The suite pins connector output semantically (typed read-back), not the exact wire bytes that block-hash dedup sees, and an upgrade mid-batch is an operational scenario, not a test.

**How to avoid:**
- Capture the RowBinary byte stream for the existing type matrices (`NullableTupleMatrixLiveTest`, `NestedNullRowBinaryLiveTest`, `MetadataFreeJsonTypeParityLiveTest`, `SimpleAggregateTupleRoundTripLiveTest`, `Lz4RoundTripLiveTest`) on 0.9.6 and 0.11.0 and diff them. Any difference is a rollout-note item.
- Run `DedupTokenRetryLiveTest` and the `KeeperStateProvider` restart test on 0.11.0 and add one "bytes captured on 0.9.6, replayed on 0.11.0" case.
- Rollout guidance: drain (clean stop, `AFTER_PROCESSING`) before the upgrade, as the gg3 note did for the Tuple encoding change; state rollback the same way.
- Do **not** adopt new client API (`Client.Builder#setMetricsRecorder`, span SPI, Micrometer/OTel) in this milestone: scope is "absorb", and those add `provided`-scope optional deps and surface.

**Warning signs:**
- A typed read-back test is green but a parity/twin-table test (route A vs RowBinary) changes row counts.
- `0.11.0`'s `client-v2` changes a `GenericRecord`-visible type on read-back (e.g. `Dynamic(Decimal)` width) in a pin written from observation.

**Phase to address:** C (byte capture, restart tests), E (rollout item).

---

### Pitfall 7: The new transitive jars (`zstd-jni`, a second httpclient5/httpcore5 line, guava 33.7.2) change the Confluent archive, and the archive guard cannot see it

**What goes wrong:**
Comparing the client-v2 POMs: 0.11.0 adds `com.github.luben:zstd-jni:1.5.7-23` (a jar carrying native libraries for several platforms, so the archive grows noticeably), moves `httpclient5` 5.4.4 -> 5.6.4, `httpcore5(-h2)` 5.3.4 -> 5.4.3, `at.yawk.lz4:lz4-java` 1.10.2 -> 1.11.4, guava 33.4.6 -> 33.7.2, drops `jspecify`, and declares `jackson-*`/`gson`/`opentelemetry-api` as `provided` (not shipped by the client; the connector already ships its own jackson/gson). `ConfluentArchiveCheck` (run by `verifyConfluentArchive`) only fails on nested jars and bundled `org/apache/kafka` classes. A new native-bearing jar passes it. The existing `confluent-archive-allowlist-*.txt` comparison is a manual release-time step and nothing in CI reads it (that is itself a milestone item).

Also: the fork explicitly adds `clickhouseDependencies(libs.guava) // temp to override client's with CVE` and pins `httpclient5`, `lz4Java` (1.12.0 -> 1.12.1 in PR #19) above the client's declared versions. Gradle will keep the **higher** of the two, so the "override" silently becomes a no-op when the client moves past it (guava), and the fork's `lz4Java` pin must stay >= the client's 1.11.4 or the `at.yawk` capability resolution picks a different winner than expected.

**Why it happens:**
Shadow 9 produced a green build with 25 nested jars once already (gg4). The lesson ("a green build is not a usable artifact") was applied to nesting only.

**How to avoid:**
- Generate the archive listing **before** the client bump (on the post-Phase-A tree) and **after**, and diff them with `compare-confluent-archives.py`. Expected delta: add `zstd-jni`, version changes of httpclient5/httpcore5/guava/lz4. Anything else (a new `org.apache.kafka` class, duplicate `net.jpountz`, a second slf4j binding) is a finding.
- Run `dependencyInsight` for `at.yawk.lz4:lz4-java`, `com.google.guava:guava`, `org.apache.httpcomponents.client5:httpclient5` on `runtimeClasspath` and `clickhouseDependencies`; confirm exactly one winner each and update FORK-DELTA's pin rows (the catalog rows currently say the client is "upstream's own value since v1.7.0 and no longer a fork override"; that sentence becomes false).
- Add the new allowlist (`confluent-archive-allowlist-33.txt` or equivalent) and wire the compare into CI (the milestone item) in the same plan as the bump, so the first consumer is exercised by a real delta.
- Check the Confluent Hub `manifest.json` size/licence expectations for a native-bearing jar, and that the worker (`cp-kafka-connect-base:7.9.7`, OpenJ9/HotSpot on the fork's test JVM) can load `zstd-jni` natives from a plugin classloader. (LOW: not yet exercised.)

**Warning signs:**
- Archive size jumps by several MB; `unzip -l` shows `zstd-jni-*.jar`.
- `dependencyInsight` shows two `httpcore5` lines or two lz4 headlines.

**Phase to address:** C (archive diff), D (CI consumer of the allowlist).

---

### Pitfall 8: Re-pinning `actions/setup-java` by hand, or merging PR #16 and trusting only the guard

**What goes wrong:**
`WorkflowActionPinGuardUnitTest` checks that every `uses:` is a 40-hex SHA with a trailing version comment. It does not check that the SHA is the commit of the tag in the comment, nor that all four workflows use the same SHA. Hand re-pins fail in three ways: the SHA of an **annotated tag object** instead of the commit (`git rev-parse v6.1.0` vs `v6.1.0^{commit}`); a comment saying `# v6.1.0` over the old SHA; one of the four files missed (`tests.yaml`, `release.yaml`, `pre-release.yaml`, `nightly-integration-tests.yaml` all pin `actions/setup-java` at `de7274f0...` / v6.0.1). Separately, **PR CI only runs `tests.yaml`**. The release and pre-release paths, the nightly, and `increment-version.yml` never run on a PR, so a bad bump there is found at release time.

PR #16 (setup-java 6.0.1 -> 6.1.0, SHA `b843ebf9b913146b38da2eaa7aeddf76074ae1ad` per the release page, MEDIUM until confirmed against the PR diff) is Dependabot-generated and its single run was green, which is the right source. The 6.1.0 notes list only additive items (Maven settings.xml options, Temurin RISC-V, Liberica/Zulu version parsing); no input/default changes are listed.

**Why it happens:**
The guard was built to stop *unpinned* references (it was seen red on unpinned files), not *wrongly pinned* ones.

**How to avoid:**
- Prefer merging PR #16 as generated over hand-editing. If hand-editing, verify with `gh api repos/actions/setup-java/git/ref/tags/v6.1.0` and dereference annotated tags (`.object.type == "tag"` -> follow `.object.url`).
- After the change: `grep -rn "actions/setup-java@" .github/workflows | sort -u` shows exactly one SHA+comment for the fork's four workflows, and upstream's `integrationTests.yaml` still shows `@v3` (byte-guard `UpstreamIntegrationWorkflowBytesGuardUnitTest` stays green).
- Exercise the changed path: run the release `dry-run` mode (builds and attaches to a throwaway draft) and a manual `nightly-integration-tests.yaml` dispatch from `custom_build` (dispatch only works from the default branch, the gg5 D-02 lesson), not just `tests.yaml`.
- Do not "fix" the Dependabot group by letting it also propose `integrationTests.yaml`: its `exclude-paths` is guard-pinned.

**Warning signs:**
- A pin guard test changes with the PR (it should not).
- `release.yaml`'s `setup-java` SHA differs from `tests.yaml`'s.

**Phase to address:** A (merge), E (dry-run proves the release path).

---

### Pitfall 9: Gradle wrapper bump leaves wrapper artifacts inconsistent, or Shadow/Confluent archive misbehaves on the new Gradle

**What goes wrong:**
PR #20 (9.8.1 -> 9.8.2) changes only `gradle/wrapper/gradle-wrapper.properties`; the release page lists a single fix (false "graph wasn't stable enough" warning when aligning versions with a virtual platform), so risk is low. The recurring failure modes on this repo are:
- `gradlew.bat` line endings: `.gitattributes` forces `*.bat text eol=crlf`, and the history shows two separate "fixed line ending in gradlew.bat" / "renormalize" commits and a gg4 note that the rollback tree "lists `gradlew.bat` as modified on a fresh checkout". A Dependabot or `./gradlew wrapper` regeneration can re-introduce LF.
- `gradle-wrapper.jar` and the `gradlew`/`gradlew.bat` scripts not matching the pinned distribution when only the properties file moves (Dependabot sometimes updates them, sometimes not).
- Shadow `9.6.1` + Gradle bump: Shadow 9 once produced a green build with 25 nested jars. A Gradle minor can change task/classpath behaviour in the archive tasks without a red build.
- `distributionUrl` has no `distributionSha256Sum`, so the wrapper download is trust-on-first-use; `validateDistributionUrl=true` only validates the host.

**How to avoid:**
- After merging, run `./gradlew wrapper --gradle-version 9.8.2 --distribution-type bin` on a scratch branch and require `git diff --stat` to show **only** `gradle-wrapper.properties` (and, if the jar/scripts legitimately changed, include them in the same commit with `git add --renormalize`). Then `git status` on a fresh clone must be clean on Windows-style files.
- Gate on a **full** `createConfluentArchive` (not only `test`): `verifyConfluentArchive` must pass and the archive listing must equal the pre-bump listing (it is a pure Gradle bump; any difference is a finding).
- Optionally add `distributionSha256Sum` (supply-chain hardening, scope call for the user).
- Run the wrapper bump **before** the client bump and keep it its own commit so a regression is attributable.

**Warning signs:**
- `git status` dirty on `gradlew.bat` right after checkout or after running the wrapper task.
- `unzip -l build/confluent/*.zip` shows `*.jar` inside `lib/` jars (nested), or archive size changes with no dependency change.

**Phase to address:** A.

---

### Pitfall 10: Dependabot PRs and `sync/v1.7.0` diverge again (the repeat offender)

**What goes wrong:**
Per memory and PROJECT.md, the user merges Dependabot PRs directly on `origin/custom_build`; the working `sync/v1.7.0` branch then lacks those commits and a later fast-forward/`--no-ff` publication hits conflicts in `gradle/libs.versions.toml`, `.github/workflows/*` and the fork-anchors counts. Currently six PRs are open (#10, #16-#20), and Dependabot is **monthly**, so a new batch (and a rebased/recreated #17 if the client moves again, e.g. 0.11.1) will arrive during the milestone. PRs #17/#18/#19 all edit adjacent lines of `libs.versions.toml`; merging one makes Dependabot rebase the others, and a PR's green run is against its **old base**, not the new tip.

Related state hazards: `VERSION` is anchored (`v1.7.0_gg5`, count 1, BLD-07) so the bump to `_gg6` must edit `fork-anchors.txt` in the same commit; the fork's `VERSION` appears in the MBean `version=` segment, client name and CI tests.

**How to avoid:**
- Ask the user to **not merge Dependabot PRs on `origin`** for the milestone's duration, or to tell the roadmapper which have been merged. Absorb them on the working branch via DEPS-01-shaped merges (`git merge origin/custom_build` in one mechanical commit, anchor exemptions generated from the gate's output, one commit per bump), then re-run the full suite per bump as the milestone already plans.
- Freeze the baseline: record `origin/custom_build` SHA at the start of each phase; before publishing, `git fetch origin && git log sync/v1.7.0..origin/custom_build` must show only the known release merges. If something else is there, absorb it first.
- Close superseded PRs on `origin` after absorption so Dependabot stops rebasing them (and #4 is already gone).
- Do **not** push to a `dependabot/*` branch; comment `@dependabot rebase` instead.

**Warning signs:**
- `git log --oneline sync/v1.7.0..origin/custom_build` shows more than the gg4/gg5 merge commits.
- Dependabot opens a "bump clickHouseDriver" PR whose base is a commit the working branch does not have.

**Phase to address:** A (absorb first), E (pre-publication divergence check).

---

### Pitfall 11: A fork pin returns to the catalog and the next upstream sync is poisoned (client version)

**What goes wrong:**
FORK-DELTA records that client `0.9.6` is "upstream's own value since v1.7.0 and no longer a fork override". After PR #17 the fork carries `0.11.0` while upstream stays on `0.9.6` (upstream PR #852 for `0.10.0` is open, and PR #861, "Isolated client init by version", is a draft that restructures client initialisation; MEDIUM, from `gh pr list` on upstream). So the next upstream sync will conflict on `gradle/libs.versions.toml` (`clickHouseDriver`) and potentially on `ClickHouseHelperClient`, where the fork already has fork-only compression/Filter code. Without a recorded pin row and an anchor, a future mechanical merge silently reverts to `0.9.6` and compiles (the same class of failure as `CLICKHOUSE_VERSION_DEFAULT`).

**How to avoid:**
- In the client-bump commit: add a FORK-DELTA pin row (version, why, `Pinned by` IDs), an anchor row `gradle/libs.versions.toml  clickHouseDriver = "0.11.0"  1`, and a Docker-free unit test that reads the catalog value (the `ClickHouseVersionDefaultUnitTest` pattern) so a revert fails `testUnit`.
- Add a "client V1 usage" inventory to FORK-DELTA: the connector still uses the deprecated V1 API (`ClickHouseNode`, `ClickHouseClientOption`, `ClickHouseException`, `clickhouse-http-client`). 0.11.0 notes do not list V1 removal, but V1 is deprecated and the compile still passed on `src/main`; the next jump may not. Track it as debt, not as this milestone's work.
- Watch upstream #861/#852 so the eventual sync is a planned item.

**Warning signs:**
- A post-merge `git diff pre-tag -- gradle/libs.versions.toml` shows `clickHouseDriver` back at `0.9.6`.

**Phase to address:** C (pin row + anchor + test), later sync.

---

## Moderate Pitfalls

### Pitfall 12: User-agent / client-name assertions and log parsers
**What goes wrong:** 0.11.0 changes the `User-Agent` to `<client name> clickhouse-java-v2/<version> (lv:java/...; os:...; hc:ahc_<version>)` (#3202). `ClickHouseSinkTaskTest` asserts `record.getString("http_user_agent").startsWith(CONNECT_CLIENT_NAME)` and a `position(http_user_agent, ...)` filter. The client name is still the prefix per the note, so this likely passes, but the V1 client path (`ClickHouseClientOption.CLIENT_NAME`) was not described, and operators' `system.query_log` filters (the gg5 note says the client name reads `v1.7.0_gg5`) may parse the old shape.
**Prevention:** Run that test on both client paths before relying on it; add a rollout line about the changed UA format; the warning-length behaviour (> 200 chars) matters only if `CONNECT_CLIENT_NAME` ever grows.
**Phase:** C, E.

### Pitfall 13: Socket buffers lose their default (#3121)
**What goes wrong:** `socket_rcvbuf`/`socket_sndbuf` have no default; the OS auto-tunes. High-latency links (cross-region cloud) may see throughput move either way. No connector code sets them.
**Prevention:** One performance smoke (`InsertFormatPerformanceTest`-style) before/after; if an operator pins buffers through `jdbcConnectionProperties`, behaviour is unchanged. Document, do not set.
**Phase:** C.

### Pitfall 14: `latest` ClickHouse drifts mid-milestone, again
**What goes wrong:** `latest` was 26.10.3.7 at gg5; a float tag means the acceptance run on day 1 and on release day may be different servers (gg1 lesson 4). With a client bump the chance of an interaction doubles.
**Prevention:** Record `version()` in each evidence block; budget a gap-closure slot in the release phase; keep `ClickHouseVersionDefaultUnitTest` and "no version pins in CI's job env" intact. Two acceptance runs on the same server version.
**Phase:** C, E.

### Pitfall 15: `increment-version.yml` writes `vnull` and could corrupt the release
**What goes wrong:** The workflow already mishandles `_ggN` versions. Any workflow run that touches `VERSION` (the release path invokes it, the user already ran the real release on 2026-10-28) can overwrite the anchored `VERSION` with `vnull`, breaking BLD-07 and the MBean/client name.
**Prevention:** Fix it with a unit-level test of the version-parsing step (extract to a script and test it, as `jacoco_summary.py` and `failed_tests_summary.py` are tested); do not "verify" it by running the real workflow on `custom_build`. Pin the fix under `NightlyWorkflowGuardUnitTest`-style guards.
**Phase:** D.

### Pitfall 16: Closing the "no run exercised the real publish path" caveat with fiction
**What goes wrong:** The caveat can only be closed by evidence of the real run (`release.yaml` run for `v1.7.0_gg5`, 2026-10-28, by the user). A plan that re-runs the release to "prove" it creates a second real release or tag.
**Prevention:** Record the run ID, release URL, attached archive checksum and `verifyConfluentArchive` log line in an evidence file; re-running is out of scope. Cross-check the attached archive against the allow-list.
**Phase:** D/E.

### Pitfall 17: Operator-doc review budget and quantifiers (recurring, five milestones)
**What goes wrong:** The rollout note hit its review cap in gg4 and gg5, with findings that were quantifier errors ("the four inserts that were run"), and reviewer Fix text that was itself wrong. This milestone has more operator-visible claims than any since gg1 (client behaviour, compression, retry, new config, UA).
**Prevention:** Orchestrator-spawned fresh-context `gsd-code-reviewer` **before** the push gate (not inline in an executor), each claim labelled "measured" or "read from the code, not measured", a behaviour ledger kept per phase (L-nn) as the doc source, reviewer Fix text verified against code.
**Phase:** E.

---

## Minor Pitfalls

### Pitfall 18: okhttp 5.5.1 / lz4-java 1.12.1 treated as trivial
**What goes wrong:** okhttp is used in integration tests only; lz4-java is shipped and `Lz4RoundTripLiveTest` loads the `net.jpountz` factory from the `at.yawk` jar. A patch release can change nothing, or can flip a capability-resolution winner.
**Prevention:** One commit each, `dependencyInsight` + `Lz4RoundTripLiveTest` + `TestRuntimeClasspathLz4UnitTest` for lz4; check the advisory floor is still met (repository Dependabot *alerts* are disabled, so a manual advisory check per shipped bump is the only CVE coverage).
**Phase:** A.

### Pitfall 19: Mockito inline / OpenJ9 crash misread as a regression
**What goes wrong:** `./gradlew test` exit 255 from an intermittent OpenJ9 GPF (core dump ~8 GB) under memory pressure; the gg4 Phase 26 gate ran on HotSpot 21 for that reason.
**Prevention:** Check for a core dump before debugging; delete it and re-run once; `-PjavaVersion=21` is the documented escape; do not report a crash as a client-bump failure.
**Phase:** all.

### Pitfall 20: Stale-verification and bookkeeping drift
**What goes wrong:** Sequential phases that edit shared files (`FORK-DELTA.md`, `fork-anchors.txt`, rollout docs) make earlier VERIFICATION reports read stale; `requirements-completed` frontmatter drifted in gg5.
**Prevention:** Update frontmatter in the plan that moves a requirement; plan for an `override_closeout`; re-verify at close.
**Phase:** E.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Merge #17 and fix only the three `ServerException` compile errors | Green build fast | Retry/serialization/compression behaviour changes shipped unmeasured | Never; compile-fix is step 1 of the audit |
| Merge #10 (protobuf 4) "to clear the queue" | One fewer open PR | 27 ProtobufIngestTest failures, or a skip list | Never; close with an ignore rule |
| Edit upstream-owned `UtilsTest`/fixtures to match 0.11.0 | Quick compile fix | Conflicts on every sync | Only if the fix is a minimal in-place signature change, recorded in the merge checklist |
| Hand-edit `fork-anchors.txt` counts to make the gate pass | Gate green | Gate stops detecting the thing it was built for | Never in the merge commit; only in the commit that intentionally changes the anchor |
| Skip the byte-level diff of RowBinary output across clients | Saves a day | Rollout hazard unnoticed (dedup token + changed bytes) | Never for a client major |
| Re-run the real release to close the caveat | Feels conclusive | Duplicate release/tag | Never; record existing evidence |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| ClickHouse client V2 builder | Assuming `Client.Builder.build()` still throws `IllegalArgumentException` | Expect `ClientMisconfigurationException` (0.10.0); pin the `start()` failure for bad auth/SSL options in `jdbcConnectionProperties` |
| `jdbcConnectionProperties` -> `setOption` | Operator passes `http_header_authorization`, `access_token` without scheme, ms/s timeouts | Rollout note lists the 0.10.0 semantic changes (`access_token` verbatim, builder rejects user/pass + Authorization header, `executionTimeout` in ms) |
| Gradle version catalog | Bumping `clickHouseDriver` without checking transitive winners | `dependencyInsight` for lz4, guava, httpclient5, httpcore5; add `zstd-jni` to the archive allowlist |
| Dependabot `github-actions` group | Merging a group PR that touches `integrationTests.yaml` | `exclude-paths` keeps it out; close any stray PR; SHA-256 guard stays green |
| `workflow_dispatch` verification | Planning a dispatch from a non-default branch | Dispatch only runs from the default branch (`custom_build`); put nightly/dry-run verification in the release phase |
| Upstream tail | Treating "no conflict markers" as "no conflicts" | Anchors gate + per-hunk checklist + compile of fork tests |
| Production worker | Assuming connector-supplied jars are used for the Connect API | Compile-only; `connect-api-references.txt` guard stays; worker is `cp-kafka-connect-base:7.9.7` |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| In-client 503 retries now on by default (0.10.0) | `put()` blocks longer; consumer rebalances under a flapping LB | Measure `put` latency against a proxy that returns 503; compare to `max.poll.interval.ms` | Load balancer maintenance windows |
| Socket buffer auto-tune (0.11.0) | Throughput delta on WAN links | Before/after smoke on a throttled link | Cross-region ClickHouse Cloud |
| Per-request compression defaults changed | CPU or bytes-on-wire shift | Measure with `compress` explicitly set vs default | Large batches (>10 MB blocks) |
| `zstd-jni` native load in plugin classloader | Slow first insert, `UnsatisfiedLinkError` on exotic platforms | Smoke on the real worker image; document supported platforms | Alpine/musl, non-x86/arm64 workers |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Merging Dependabot PRs without an advisory check (alerts disabled) | Shipped dependency with an open CVE | Manual advisory check per shipped bump; record result in the evidence file |
| Logging a rethrown client message that includes record data | Re-opens backlog 999.3 | Client exceptions changed type/message (`ClientException`, transfer errors #2729); re-run `WriterErrorPathLiveTest` / `ValueFreeDlqHeadersUnitTest` on 0.11.0 and grep rethrown text for record values |
| Hand-pinned action SHA that points at the wrong commit | Supply-chain risk behind a "pinned" guard | Deref annotated tags; prefer the Dependabot-generated SHA |
| Dependabot or CI tokens with write scope | Workflow compromise | Keep `permissions: contents: read` (gg2 hardening); do not widen for the new publish verification |
| `increment-version.yml` rewriting `VERSION` with write scope | Bad release metadata pushed | Fix behind a test; do not run it live to test |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Operator note says "no behaviour change" because `src/main` barely changed | Operators miss client-driven retry/compression/UA changes | List client-originated changes explicitly, labelled by evidence |
| New config key (`additionalRetriableErrorCodes`) undocumented in fork docs | Operators set codes expecting DLQ routing | Say it redelivers without a retry budget and bypasses `errors.tolerance`/DLQ (upstream's own help text) |
| Rollback instructions untested | Rollback fails on a changed archive/jar set | Run the rollback demo on a dry-run merge and the real merge, as in gg3-gg5 |

## "Looks Done But Isn't" Checklist

- [ ] **Client bump:** `compileTestJava` green *and* `testUnit` + full suite run twice with `--rerun` on the same `latest` server version; evidence block records `SELECT version()`.
- [ ] **Client bump:** live classification pins (159, 503, 252, auth) run on both 0.9.6 and 0.11.0 with outcomes compared.
- [ ] **Client bump:** RowBinary byte capture diff for the type matrices; `DedupTokenRetryLiveTest` and the `KeeperStateProvider` restart test green.
- [ ] **Client bump:** FORK-DELTA pin row + anchor row + Docker-free catalog pin test added in the same commit.
- [ ] **Compression:** `NetworkCompressionPin*` and `Lz4RoundTripLiveTest` outcomes re-measured; claims in Javadoc/FORK-DELTA/rollout updated; default unchanged.
- [ ] **Archive:** `verifyConfluentArchive` green *and* pre/post listing diff reviewed (expected: `zstd-jni`, version moves only); new allowlist committed; CI consumer exists.
- [ ] **setup-java:** one SHA+comment across the four fork workflows; SHA verified against the tag's commit; dry-run of release path exercised; upstream `integrationTests.yaml` unchanged.
- [ ] **Wrapper:** only `gradle-wrapper.properties` (plus justified artifacts) changed; `gradlew.bat` CRLF intact on fresh clone; `createConfluentArchive` run, not just `test`.
- [ ] **Protobuf:** decision recorded (closed + ignore rule, or full coupled bump with `ProtobufIngestTest` 27 cases green).
- [ ] **Tail merge:** rollback tag, `verify-merge-invariants.sh` exit 0 on the merge commit, per-hunk checklist for `Utils`/`ClickHouseSinkConfig`/`ProxySinkTask`/`ChunkFlusher`, anchors untouched except planned moves, `NewUpstreamConfigDefaultsUnitTest` includes the new key.
- [ ] **Divergence:** `git log sync/v1.7.0..origin/custom_build` shows only known commits immediately before publication.
- [ ] **Release:** `VERSION=v1.7.0_gg6` with the anchor edited in the same commit; exactly one green CI run on the published SHA before the tag push; independent reviewer pass on operator docs before the gate.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Protobuf 4 merged on `origin` | LOW | `git revert` the merge on `custom_build`; Dependabot ignore for the major |
| Client bump regresses classification/serialization after publication | MEDIUM | Roll back to `pre-v1.7.0_gg6` tag; follow the rollout note's clean-stop rule; republish `gg5` archive (do not rebuild from the rollback tree with Shadow 9 unverified) |
| Wrong setup-java SHA in release workflow | LOW | Fix-forward pin; re-run dry-run; do not re-tag |
| Anchors gate edited to pass | MEDIUM | Re-derive exemption rows from a clean gate run on the merge commit; diff `fork-anchors.txt` against the pre-tag |
| Dependabot merges diverge `sync/v1.7.0` | LOW-MEDIUM | Merge `origin/custom_build` into the working branch as one mechanical commit; regenerate exemptions; re-run the gate |
| `vnull` written into `VERSION` | LOW | Revert the commit; anchor check (`VERSION v1.7.0_gg6 1`) will fail loudly first, which is why the anchor must exist |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1 protobuf 4 in testFixtures | A | Decision recorded; `ProtobufIngestTest` green on whichever tree ships |
| 2 PR CI green is not a gate | C | Two `--rerun` full runs with 0 failures after compile fix |
| 3 compression story changes | C, E | Re-measured Recorded Facts; claims updated; default unchanged |
| 4 retry/classification changes | C | Live pins equal before/after or documented |
| 5 upstream tail merge | B (then C) | Invariants gate exit 0; fork tests compile; anchors match |
| 6 serialization / dedup bytes | C, E | Byte diff report; restart + dedup tests; rollout item |
| 7 archive contents | C, D | Listing diff; allowlist in CI |
| 8 setup-java re-pin | A, E | One SHA across files; dry-run proof |
| 9 Gradle wrapper | A | Clean `git status`; archive listing unchanged |
| 10 Dependabot divergence | A, E | `git log sync..origin` shows known commits only |
| 11 fork pin returns | C | Anchor + pin test + FORK-DELTA row |
| 12-13 UA / socket buffers | C, E | Test pass on both client paths; smoke result recorded |
| 14 `latest` drift | C, E | `version()` in evidence; gap-closure slot |
| 15 `vnull` | D | Version-parse unit test |
| 16 caveat closure | D, E | Evidence file with run ID, no new release |
| 17 operator-doc review | E | Orchestrator-spawned reviewer before the gate |
| 18-20 minor | A, all, E | See individual items |

## Sources

- Dependabot PR #17 run 40416377063 (client 0.11.0, `compileTestJava` failure on `ServerException` ctor) and PR #10 run 39501799049 (27 `ProtobufIngestTest` `VerifyError`s), read with `gh run view --log-failed` (HIGH, measured).
- ClickHouse Java client release notes v0.10.0 and v0.11.0 (https://github.com/ClickHouse/clickhouse-java/releases/tag/v0.10.0, .../v0.11.0) (MEDIUM, official but not yet run against this connector).
- Maven Central POMs `com/clickhouse/client-v2/0.9.6` and `0.11.0` (dependency comparison: zstd-jni added, httpclient5 5.6.4, lz4-java 1.11.4, guava 33.7.2) (HIGH).
- `actions/setup-java` v6.1.0 release notes; Gradle 9.8.2 release notes (MEDIUM).
- Upstream `ClickHouse/clickhouse-kafka-connect` `main` `9bed6f79` diff against the working tree (`git diff HEAD...upstream/main`): `Utils.handleException` 5-arg signature, `additionalRetriableErrorCodes`, `ProxySinkTask`/`ChunkFlusher` changes (HIGH, measured); upstream PRs #852, #861 via `gh pr list` (MEDIUM).
- Repository: `.planning/PROJECT.md`, `.planning/RETROSPECTIVE.md` (gg1-gg5 lessons), `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`, `build.gradle.kts`, `gradle/libs.versions.toml`, `.github/workflows/*`, `WorkflowActionPinGuardUnitTest`, `NetworkCompressionPin*Test`, `ClickHouseHelperClient`, `Utils` (HIGH).
- User memory index (OpenJ9 crash, Dependabot-merge divergence, phase orchestration gotchas) (HIGH for process, not re-verified).
