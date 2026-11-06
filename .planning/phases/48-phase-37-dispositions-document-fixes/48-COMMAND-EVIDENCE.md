# Phase 48: Command evidence

- **Base commit:** `local_head` = `c77d1a147ae4acd37100b73db08f1851047bc055` (HEAD when plan 48-01 started, branch `sync/v1.7.0`); this is B0.
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; ClickHouse-backed runs prefix `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`.
- **Docker:** colima profile `vz`.
- **Date (UTC):** 2026-11-06.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helpers:** `48-evidence-check.py`, `48-testcase-status.py`, `48-doc-edits.json` and `48-ledger-check.py` are read-only plan-time files; they are never edited. `48-path-check.py` and `48-id-scan.py` are written in plan 48-01 and frozen after E-02.
- **No push happens in Phase 48.** Reads use `git ls-remote` and `gh api` only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | P37-01, P37-02, P37-03, P37-04, DOC-01, DOC-02 | 48-01 | pass |
| E-02 | DOC-01, DOC-02 | 48-01 | pass |
| E-03 | P37-01, P37-02 | 48-02 | pass |
| E-04 | P37-03 | 48-02 | pass |
| E-05 | P37-04 | 48-02 | pass |
| E-06 | P37-04 | 48-02 | pass |
| E-07 | P37-04 | 48-02 | pass |
| E-08 | P37-04 | 48-02 | pass |
| E-09 | P37-01, P37-02, P37-03, P37-04 | 48-02 | pass |
| E-10 | P37-01 | 48-03 | pass |
| E-11 | P37-01 | 48-03 | pass |
| E-12 | DOC-01, P37-04 | 48-04 | pass |
| E-13 | DOC-02, P37-04 | 48-04 | pass |
| E-14 | P37-03 | 48-05 | fail |
| E-15 | P37-03 | 48-05 | pass |
| E-16 | P37-01, P37-02, P37-03, P37-04 | 48-05 | pass |
| E-17 | P37-01, P37-02, P37-03, P37-04, DOC-01, DOC-02 | 48-05 | pass |
| E-18 | P37-03, DOC-01, DOC-02 | 48-05 | pending (plan 48-05) |

### E-01: Pre-flight and baseline on the untouched base, release path recorded frozen

Backs: P37-01, P37-02, P37-03, P37-04, DOC-01, DOC-02, ROADMAP Phase 48 phase gate (a green baseline on the untouched tip, so every later red, every added row and every removed row is attributable to this phase); P37-03 and D-3 (the release path recorded frozen).

```sh
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
colima list
md5 -q .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py
md5 -q .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py
md5 -q .planning/phases/48-phase-37-dispositions-document-fixes/48-doc-edits.json
md5 -q .planning/phases/48-phase-37-dispositions-document-fixes/48-ledger-check.py
git ls-files --error-unmatch .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py .planning/phases/48-phase-37-dispositions-document-fixes/48-doc-edits.json .planning/phases/48-phase-37-dispositions-document-fixes/48-ledger-check.py
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
bash .planning/scripts/verify-merge-invariants-selftest.sh > /tmp/gsd48/exec/selftest.log 2>&1
gh api repos/ne1r0n/clickhouse-kafka-connect/git/matching-refs/tags/pre-v1.7.0_gg8 --jq length
git ls-remote origin refs/heads/sync/v1.7.0
git ls-remote upstream main
git rev-parse pre-v1.7.0_gg8
git ls-remote --tags origin pre-v1.7.0_gg8
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 c77d1a147ae4acd37100b73db08f1851047bc055 > /tmp/gsd48/exec/tail-gate-base.log 2>&1
ls .planning/scripts/confluent-archive-allowlist-*.txt | wc -l
git show v1.7.0_gg6:.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt | cmp -s - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt
git show v1.7.0_gg7:.planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt | cmp -s - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
md5 -q .github/scripts/compare_confluent_archives.py
diff <(git show v1.7.0_gg6:docs/ROLLOUT-v1.7.0_gg6.md) docs/ROLLOUT-v1.7.0_gg6.md | grep -E '^[0-9]' | paste -sd ' ' -
grep -c ESCAPED_NEWLINE src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
grep -rho 'DeclaredNullability.ESCAPED_NEWLINE' src/test | wc -l
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd48/exec/testunit-base.log 2>&1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed build/test-results/testUnit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/testUnit --class .ProxySinkTaskStopUnitTest   # and the seven other touched classes
rm -rf /tmp/gsd48/exec/xml-base-testunit && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-base-testunit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-base-testunit /tmp/gsd48/exec/status-base-testunit.tsv
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd48/exec/suite-base.log 2>&1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed build/test-results/test
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/test --class .NullableTupleMatrixLiveTest   # and .SimpleAggregateTupleRoundTripLiveTest
rm -rf /tmp/gsd48/exec/xml-base-suite && cp -R build/test-results/test /tmp/gsd48/exec/xml-base-suite
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-base-suite /tmp/gsd48/exec/status-base-suite.tsv
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
local_head: c77d1a147ae4acd37100b73db08f1851047bc055
branch: sync/v1.7.0
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
helper_md5: 7123d354f51d45e019837f43b3a30fbc
status_helper_md5: 41f14e07ae7b402ad2e29bfc381a2aff
doc_edits_md5: 864361fcb12556108ecf50ccf166f123
ledger_check_md5: 77ab37ef73311b892ca27c52763b3a60
helpers_tracked: yes
anchors_mirror: rows=218 bad=0
selftest_last_line: selftest: no failures
selftest_exit: 0
selftest_duration: about 79 min
origin_pre_tag: absent
origin_sync_branch: absent
local_pre_tag: pre-v1.7.0_gg8 at baa404cd5ed95c556db0acff84def1024c3bafe3 (local only)
origin_pre_tag_ls_remote: absent
upstream_main: 83808df9a0993cf49b2ceaa4633c782e550c0180
base_tail_gate_line: invariants: provenance_paths=11 anchor_commits=128 anchor_rows=218 marker_commits=146 anchor_exempt=0 result=PASS
base_tail_gate_exit: 0
allowlists_count: 5
allowlist_gg6_identical_to_tag: yes
allowlist_gg7_identical_to_tag: yes
compare_script_md5: a637e0cc8fb33a33e7673ac682fa63c8
gg6_note_tag_hunks: 2a3,4 112c114 146c148
escaped_newline_main_count: 1
escaped_newline_test_refs: 9
baseline_testunit_docker_host: unix:///nonexistent.sock
baseline_testunit_exit: 0
baseline_testunit_tests: 2295
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
ps_tests: 4
cs_tests: 6
uw_tests: 21
rp_tests: 38
mk_tests: 2
compare_script_tests: 58
dn_tests: 69
planning_guard_tests: 18
touched_classes_failed: 0
baseline_suite_exit: 0
baseline_suite_tests: 2898
baseline_suite_failures: 0
baseline_suite_errors: 0
baseline_suite_skipped: 18
baseline_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
ntm_suite_tests: 33
sat_suite_tests: 2
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
baseline_java: default toolchain
jvm_crash: no
xml_testunit_files: 105
xml_suite_files: 154
xml_testunit_manifest_md5: 4c17ef566b68d3efaf20327b7fdc790f
xml_suite_manifest_md5: a3394159e4b66fee4df7ab4197909f75
status_testunit_dump_md5: 2ceab8e03158678e5bb9c790f7ff26aa
status_suite_dump_md5: 3552c431cbef8b46daece86215ac639e
```

Notes. The eight touched classes are `ProxySinkTaskStopUnitTest`, `ClickHouseSinkTaskStopUnitTest`, `UnvettedRetriableCodesWarnUnitTest`, `RetriableExceptionPassThroughUnitTest`, `DescribeUnreadableEnumMockServerTest`, `CompareConfluentArchivesScriptUnitTest`, `DeclaredNullabilityTest` and `SourcePlanningIdGuardUnitTest`, in the order of the keys `ps_tests` to `planning_guard_tests`. The baseline totals later plans compute expected counts from are testUnit 2295 tests and full suite 2898 tests. `origin_sync_branch: absent` means `git ls-remote origin refs/heads/sync/v1.7.0` printed nothing; the local tag `pre-v1.7.0_gg8` exists only in this clone (`git ls-remote --tags origin pre-v1.7.0_gg8` printed nothing, `gh api` matching-refs length 0). `upstream_main` equals the Phase 45 merge input `83808df9`, so upstream did not move. The `latest` image is the digest Phase 47 recorded (`26.10.3.7`). The self-test and the base tail gate read git objects only and ran in the background while the Gradle baselines ran; `src` equalled B0 throughout. The tail gate ran over `12e2cf17..c77d1a14` with the explicit SHA as range end.

Re-measure aid. The XML snapshots and status dumps live under `/tmp/gsd48/exec` and are not tracked. Their per-file md5 manifests (`md5 -r` of each `TEST-*.xml` file, sorted by name) are kept at `/tmp/gsd48/exec/manifest-xml-base-testunit.md5` (105 lines) and `/tmp/gsd48/exec/manifest-xml-base-suite.md5` (154 lines) and are reproduced below; their own md5 values and those of the two status dumps are the four `*_md5` keys above. JUnit XML carries timestamps and durations, so a re-run reproduces the testcase names and statuses (the status dump) but not the XML bytes: if `/tmp` is cleared, a later delta is re-based by re-running the two baseline commands above on commit `c77d1a14` and comparing the testcase counts (testUnit 2295, suite 2898) and the eight per-class counts.

Per-file md5 manifest of the testUnit snapshot `xml-base-testunit`:

```md5
7d62cc74333c19a980222164f0b0563c TEST-com.clickhouse.kafka.connect.ci.BaselineTagScriptUnitTest.xml
13a8ebd4b26a63dd792c1d1feb06a0cd TEST-com.clickhouse.kafka.connect.ci.BumpVersionScriptUnitTest.xml
b7a2fb8dcb90218c86ac1051aefb3b24 TEST-com.clickhouse.kafka.connect.ci.ClientCatalogPinUnitTest.xml
9b7f65b5331558fd17dca6c91b038d4b TEST-com.clickhouse.kafka.connect.ci.CompareConfluentArchivesScriptUnitTest.xml
34efdacecd2e0b4f024392ab76c106e3 TEST-com.clickhouse.kafka.connect.ci.ConfluentArchiveCheckUnitTest.xml
b5beee74b5c9181d87db98ed280eb98a TEST-com.clickhouse.kafka.connect.ci.ConnectApiReferencesUnitTest.xml
47b6bf80aab39910943b6603dda9d916 TEST-com.clickhouse.kafka.connect.ci.DependabotProtobufIgnoreGuardUnitTest.xml
5bd715981ef1cb55ad71f56f1b860779 TEST-com.clickhouse.kafka.connect.ci.FailedTestsSummaryScriptUnitTest.xml
259b247262bccaee2cc5b106420f1fcd TEST-com.clickhouse.kafka.connect.ci.ForkCoverageScopeUnitTest.xml
031293230e51ef8e8116b9e72c5ac12f TEST-com.clickhouse.kafka.connect.ci.IncrementVersionWorkflowGuardUnitTest.xml
4784783e84ceb2f2ee98773c3414a6f0 TEST-com.clickhouse.kafka.connect.ci.NightlyWorkflowGuardUnitTest.xml
89314641e9dbeb752eae4ec94b7c9459 TEST-com.clickhouse.kafka.connect.ci.ReleaseWorkflowGuardUnitTest.xml
28b6829c786325e32ebb9b445c41a56c TEST-com.clickhouse.kafka.connect.ci.SourcePlanningIdGuardUnitTest.xml
a3636097ed67e6b81021bc6299198ebd TEST-com.clickhouse.kafka.connect.ci.TestRuntimeClasspathLz4UnitTest.xml
cf992eee6084f7dcada5baa1bb43f404 TEST-com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.xml
df356354b98bcbfe307fa0450c97bda6 TEST-com.clickhouse.kafka.connect.ci.UpstreamIntegrationWorkflowBytesGuardUnitTest.xml
b346768b06831eee41f8e4cc641aa580 TEST-com.clickhouse.kafka.connect.ci.WorkflowActionPinGuardUnitTest.xml
851ee59f3e51b288d1778645e14a6c41 TEST-com.clickhouse.kafka.connect.sink.AdditionalRetriableErrorCodesPinUnitTest.xml
2c8a0e68bc95580818fa1f3e0633b6cc TEST-com.clickhouse.kafka.connect.sink.AdditionalRetriableErrorCodesTest.xml
cff5dddc885885641dd4d0d3fda842cd TEST-com.clickhouse.kafka.connect.sink.AsyncInsertSettingsUnitTest.xml
b168be188773f9a709d060238057361d TEST-com.clickhouse.kafka.connect.sink.BufferedOffsetCommitUnitTest.xml
d31178b570fafae0329d05249079a297 TEST-com.clickhouse.kafka.connect.sink.ClickHouseBaseTearDownAttributionUnitTest.xml
b447aa7357745234f22875dedf9109fb TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkConfigTest.xml
3da7888a2c1fbe8a705c8d6133f2cc7d TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkConfigUsernameDefaultUnitTest.xml
e5aae698c965cfa2b4c045d46758433b TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskStopUnitTest.xml
1cb96bfe1d654a824142070a5f254304 TEST-com.clickhouse.kafka.connect.sink.ClickHouseVersionDefaultUnitTest.xml
bd9becfde9132687f5e455e024f96e0f TEST-com.clickhouse.kafka.connect.sink.HandleExceptionCauseCycleUnitTest.xml
8b76efd5a18444bbdba8a91d7a8dcc11 TEST-com.clickhouse.kafka.connect.sink.InsertStallBoundPinUnitTest.xml
388e6d9eba9a946ad7922cca9653a27a TEST-com.clickhouse.kafka.connect.sink.InsertTimeoutHelpTextPinUnitTest.xml
752133aaad5275abb6a17cd8f55ea5b5 TEST-com.clickhouse.kafka.connect.sink.InsertV1WaitBoundPinUnitTest.xml
f795066ff08fa783ba0d0b5b097658c0 TEST-com.clickhouse.kafka.connect.sink.InsertWriteStallBoundPinUnitTest.xml
653e934b1cdc4280eba4e95fe9c4a863 TEST-com.clickhouse.kafka.connect.sink.JsonInsertColumnsUnitTest.xml
00253422b303e25168d0630e99580be4 TEST-com.clickhouse.kafka.connect.sink.JsonRouteValueFreeDlqUnitTest.xml
71a0781210793a139250cea767e93f20 TEST-com.clickhouse.kafka.connect.sink.MBeanNameUnitTest.xml
eedfc86c843c4aca40c07d2f9e9eefa8 TEST-com.clickhouse.kafka.connect.sink.NewUpstreamConfigDefaultsUnitTest.xml
78bdc98dd4593bdafeef40e767eee0da TEST-com.clickhouse.kafka.connect.sink.OriginalTopicInsertedOffsetsUnitTest.xml
7a121eab1e4306fcbc4bc22ef24d1656 TEST-com.clickhouse.kafka.connect.sink.ProxySinkTaskConstructorUnitTest.xml
62bc4f99b828d9c0023674815ab3e70e TEST-com.clickhouse.kafka.connect.sink.ProxySinkTaskStopUnitTest.xml
4336aeaf1d57965d0c9ed712c872bca1 TEST-com.clickhouse.kafka.connect.sink.RetriableExceptionPassThroughUnitTest.xml
3450377185a006203ea0a6316fed634a TEST-com.clickhouse.kafka.connect.sink.RetriableRedeliveryLimitPinUnitTest.xml
eef58be917509a869e29f099a85f2bc2 TEST-com.clickhouse.kafka.connect.sink.UnvettedRetriableCodesWarnUnitTest.xml
ad3385c0540721d2b3a6d9e22fab166c TEST-com.clickhouse.kafka.connect.sink.ValueFreeDlqHeadersUnitTest.xml
5e1192c671af6a5d4e56976defa74f3c TEST-com.clickhouse.kafka.connect.sink.WrappedDataExceptionClassificationPinUnitTest.xml
f17c01d307b0a8c49f6286c801a02c6f TEST-com.clickhouse.kafka.connect.sink.WriterFailureClassificationUnitTest.xml
99648f81922cfa0d4a55b82bf7d6dec6 TEST-com.clickhouse.kafka.connect.sink.WriterRecordContextDlqUnitTest.xml
043fea163431df9338d5288635186f40 TEST-com.clickhouse.kafka.connect.sink.data.StructToJsonMapTest.xml
50cb55bf9ad6624de20a851999d91075 TEST-com.clickhouse.kafka.connect.sink.data.convert.DebeziumRecordConvertorTest.xml
99ee11fe74f01d72757d3924b5f8218b TEST-com.clickhouse.kafka.connect.sink.db.AvroDecimalLogicalTest.xml
38cb015ed30b11813eeccc990646cfc7 TEST-com.clickhouse.kafka.connect.sink.db.AvroNullableUnionVariantTest.xml
ceccbf288a78baa9cf5c1e1943b86990 TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseReplicaPinningMockServerTest.xml
a4b5bd081ea79a1457c4d5ee4bd65bf8 TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseWriterColumnContextTest.xml
581351e25b19217524f32d73ef5fb4ab TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseWriterNullMessageUnitTest.xml
e0afa16bd3a0863bfbab10e9df00fa1c TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseWriterRecordContextTest.xml
bc59d6ff1c575dfefc545cfa6dbf9ce5 TEST-com.clickhouse.kafka.connect.sink.db.EmptyCdcSkipUnitTest.xml
396c37b9795f9d20c41ee180572463b2 TEST-com.clickhouse.kafka.connect.sink.db.FieldDescriptorEscapeTest.xml
f65651409fe38767e868b7f1ea05ae99 TEST-com.clickhouse.kafka.connect.sink.db.JacksonOverIoValueFreeUnitTest.xml
d16f3e6ea90ed000e46208bb4aea872f TEST-com.clickhouse.kafka.connect.sink.db.JsonRouteValueFreeUnitTest.xml
c2e9b021cdcf30c649167eddc5f7bdcf TEST-com.clickhouse.kafka.connect.sink.db.JsonSerializationEquivalenceTest.xml
d98678f2a005c5c519c3de031c819693 TEST-com.clickhouse.kafka.connect.sink.db.MetadataFreeJsonInsertUnitTest.xml
d2e6fb4aae1a5eefff78b28ee1b143da TEST-com.clickhouse.kafka.connect.sink.db.MetadataFreeJsonWriterTest.xml
733b966bed2df8f86c15d1513b0f7cb4 TEST-com.clickhouse.kafka.connect.sink.db.MissingNonNullableValueWriteTest.xml
da23a2c153c38ae77ca7af52b607661d TEST-com.clickhouse.kafka.connect.sink.db.NestedNullableCompositeWriteTest.xml
526a9fbf9e169f3d11d8e55292c55e4e TEST-com.clickhouse.kafka.connect.sink.db.NullableEnumWriteTest.xml
d22a17ecc22c58f8ad4cbdb8f9968da7 TEST-com.clickhouse.kafka.connect.sink.db.RowBinaryUnsupportedTypeGateTest.xml
5aa604c2c8b8a919e9786aefe748b366 TEST-com.clickhouse.kafka.connect.sink.db.WriterLogMaskingUnitTest.xml
322faf0ea98b8f7b9a6135eae238c398 TEST-com.clickhouse.kafka.connect.sink.db.WriterRecordContextUnitTest.xml
4352fea30c6858cc593d0e745a7bca8d TEST-com.clickhouse.kafka.connect.sink.db.WriterValueFreeFailureUnitTest.xml
de0dca37a2702e6a19d67935ab0713cb TEST-com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClientForkUnitTest.xml
71d66d7199c1d15abaf933e6cb876a7b TEST-com.clickhouse.kafka.connect.sink.db.helper.ClientEndpointAndOptionsPinUnitTest.xml
5f39aa655c8ff22aa52726352ccde7a1 TEST-com.clickhouse.kafka.connect.sink.db.helper.ClientErrorShapePinUnitTest.xml
ee5a981d65ea09062de12085292f30e1 TEST-com.clickhouse.kafka.connect.sink.db.helper.ClientInsertWirePinUnitTest.xml
3fc3bb023547ed71e41adc974115007c TEST-com.clickhouse.kafka.connect.sink.db.helper.ClientTimeoutPrefixPinUnitTest.xml
8df39282917e92c2b715cc5b9034a0f8 TEST-com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureSnapshotUnitTest.xml
3f40757aa8714436efac06a61dfecc13 TEST-com.clickhouse.kafka.connect.sink.db.helper.DescribeFixturesUnitTest.xml
7385fc13cd90806e31efaf3f0ebd397d TEST-com.clickhouse.kafka.connect.sink.db.helper.DescribeTableBuilderUnitTest.xml
c577e7bca7d3c6a8de972353a37eb25d TEST-com.clickhouse.kafka.connect.sink.db.helper.DescribeUnreadableEnumMockServerTest.xml
9b0a6d570d9613514617338c65d4d39f TEST-com.clickhouse.kafka.connect.sink.db.helper.FieldDescriptorLiteralFieldsUnitTest.xml
c7a5bc3ba86e4de15df80fe7c49070ca TEST-com.clickhouse.kafka.connect.sink.db.helper.InsertBoundGateUnitTest.xml
f27b274e999c50a1c985351ff09f8d0f TEST-com.clickhouse.kafka.connect.sink.db.helper.InsertSocketTimeoutOptionUnitTest.xml
25f14729d07e8f10039200a5d4236fa6 TEST-com.clickhouse.kafka.connect.sink.db.helper.NetworkCompressionPinUnitTest.xml
62f3662ae6ba697aa15ae0238f6f429b TEST-com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrixFixturesUnitTest.xml
e529975a7d00596136e029085c93dcfe TEST-com.clickhouse.kafka.connect.sink.db.helper.StubClickHouseServerRawModeUnitTest.xml
00470a2b888b67042d25806d4980f9bc TEST-com.clickhouse.kafka.connect.sink.db.helper.WriterHelperClientClosingUnitTest.xml
51ee9ae27256d98d39cfb563ea68067a TEST-com.clickhouse.kafka.connect.sink.db.mapping.ColumnArrayRecursionTest.xml
5cd0d16f92ae48e4010d5767687f1439 TEST-com.clickhouse.kafka.connect.sink.db.mapping.ColumnTest.xml
b4c16add30a73145e4dbb9afc3e068d8 TEST-com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityTest.xml
cf34f6394a7999105af1938ee0cd9485 TEST-com.clickhouse.kafka.connect.sink.db.mapping.JsonDynamicTypeMappingTest.xml
dcaa7ca8e71fedfa6a87533ab5f8947e TEST-com.clickhouse.kafka.connect.sink.db.mapping.UnescapeJsonLayerTest.xml
df64f4b8ad97116a5374df2087014023 TEST-com.clickhouse.kafka.connect.sink.kafa.RangeContainerTest.xml
54de30a1fe61b21faea0149fbfa23b26 TEST-com.clickhouse.kafka.connect.sink.processing.EmptyCdcExactlyOnceStateTest.xml
b3566c70aac605b837cf4a132f925141 TEST-com.clickhouse.kafka.connect.sink.processing.ProcessingRenamedTopicStateKeyUnitTest.xml
80ec6a37c2ef33e8aeafc6b765e604a6 TEST-com.clickhouse.kafka.connect.sink.processing.ProcessingTest.xml
853dbfa7e86bc6bb4ed73709c4ddd028 TEST-com.clickhouse.kafka.connect.sink.provider.LocalProviderTest.xml
818d1356536b43848952fd43a9e0a76c TEST-com.clickhouse.kafka.connect.sink.state.provider.KeeperStateProviderClosingUnitTest.xml
5b4c0143b158f11d23cfc8aac671851f TEST-com.clickhouse.kafka.connect.sink.util.MaskTest.xml
3f15627cfd5232d9b7dba009d8d7d461 TEST-com.clickhouse.kafka.connect.sink.util.UtilsNullMessageClassificationUnitTest.xml
bb30c393f97b09c67ac3304392b71326 TEST-com.clickhouse.kafka.connect.sink.util.UtilsTest.xml
eddfd4f93b7172a5eca9067198c61d7c TEST-com.clickhouse.kafka.connect.transforms.KeyToValueTest.xml
3ccd09e53c36e2b08517b2e5bf5b2733 TEST-com.clickhouse.kafka.connect.util.WithheldMessageExceptionUnitTest.xml
62e5196e314f3b70dff274f5b4ab787f TEST-com.clickhouse.kafka.connect.util.jmx.SimpleMovingAverageTest.xml
b9096bf5e42f52026d4b60582b68ae08 TEST-transforms.ConvertToMapOrJsonPerformanceTestJson.xml
c5d4ed03684f2e0c8d6c42417a3c92b7 TEST-transforms.ConvertToMapOrJsonTest.xml
7aa13b2d6bdccc254b857e0bd48355a4 TEST-transforms.HeaderToValueTest.xml
047d263d426c7b7cb6d16ea4d0cb7eee TEST-transforms.HeadersToValueTest.xml
750283eca9c57fd571ba038540d7c693 TEST-transforms.TableOperationFilterTest.xml
```

Per-file md5 manifest of the full-suite snapshot `xml-base-suite`:

```md5
0c482b0edda5acbd0ece764660af8a65 TEST-com.clickhouse.kafka.connect.ci.BaselineTagScriptUnitTest.xml
7801020778ca1d61e7c01746e8a6ec5f TEST-com.clickhouse.kafka.connect.ci.BumpVersionScriptUnitTest.xml
846d2fe945dfcfcbbee1e0e3004b94c0 TEST-com.clickhouse.kafka.connect.ci.ClientCatalogPinUnitTest.xml
86fa18d72befc7418e2c730d4422f075 TEST-com.clickhouse.kafka.connect.ci.CompareConfluentArchivesScriptUnitTest.xml
0d83a5f8168c592b38d57b373929ad33 TEST-com.clickhouse.kafka.connect.ci.ConfluentArchiveCheckUnitTest.xml
993e0542f7fad1ef4a3440c4bd64834a TEST-com.clickhouse.kafka.connect.ci.ConnectApiReferencesUnitTest.xml
2fb9bb529620634a3be68aa4ef8e9164 TEST-com.clickhouse.kafka.connect.ci.DependabotProtobufIgnoreGuardUnitTest.xml
e1f73d23774e866dae65b339c9042c3f TEST-com.clickhouse.kafka.connect.ci.FailedTestsSummaryScriptUnitTest.xml
69b625bc2d5820465f66fadd8695df2b TEST-com.clickhouse.kafka.connect.ci.ForkCoverageScopeUnitTest.xml
3fda8adb5d92223b8a48669960421b98 TEST-com.clickhouse.kafka.connect.ci.IncrementVersionWorkflowGuardUnitTest.xml
88cc0f3595bc7566dde602fd07cd0549 TEST-com.clickhouse.kafka.connect.ci.NightlyWorkflowGuardUnitTest.xml
e03e95be9b9535a87c1d74e07e52d441 TEST-com.clickhouse.kafka.connect.ci.ReleaseWorkflowGuardUnitTest.xml
e3afccc8dfce2733048b46227f4347f0 TEST-com.clickhouse.kafka.connect.ci.SourcePlanningIdGuardUnitTest.xml
559b1a01ed902fade7ea8ddf9b8a9512 TEST-com.clickhouse.kafka.connect.ci.TestRuntimeClasspathLz4UnitTest.xml
059bf277b5f886ace3165e8a0a6b5615 TEST-com.clickhouse.kafka.connect.ci.TestsWorkflowPinGuardUnitTest.xml
379cbec6fbf6708629dd7bb0fca68e21 TEST-com.clickhouse.kafka.connect.ci.UpstreamIntegrationWorkflowBytesGuardUnitTest.xml
c5ec358fa2edd3e5acc05b01629ea7ca TEST-com.clickhouse.kafka.connect.ci.WorkflowActionPinGuardUnitTest.xml
fa3813d704b78e0cd002546550a9b52e TEST-com.clickhouse.kafka.connect.sink.AdditionalRetriableErrorCodesPinUnitTest.xml
87b6fc6f8873f1f7943f4a4f56330432 TEST-com.clickhouse.kafka.connect.sink.AdditionalRetriableErrorCodesTest.xml
c9610f47f9ac04e01369e3aba6493577 TEST-com.clickhouse.kafka.connect.sink.AsyncInsertLiveTest.xml
d0f630e63b73545a795cbdee417b2ee7 TEST-com.clickhouse.kafka.connect.sink.AsyncInsertSettingsUnitTest.xml
4ee15db05b8938a0cd221046df3fa97e TEST-com.clickhouse.kafka.connect.sink.BufferedOffsetCommitLiveTest.xml
405c550118b9993030092c76a4fa0ed0 TEST-com.clickhouse.kafka.connect.sink.BufferedOffsetCommitUnitTest.xml
685d5f65f3c3ccfeba087536fa23ee35 TEST-com.clickhouse.kafka.connect.sink.ClickHouseBaseTearDownAttributionUnitTest.xml
84dac40355ddae7746dca368a0112edb TEST-com.clickhouse.kafka.connect.sink.ClickHouseHarnessOptOutContainerTest.xml
2a7d43335566dda68ea620b6bda7047c TEST-com.clickhouse.kafka.connect.sink.ClickHouseServerVersionSmokeTest.xml
8c5b7a7898f5bd31971fa1f437b81cf2 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkConfigTest.xml
2ed0daa3d330191c17d4759b6f862c50 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkConfigUsernameDefaultUnitTest.xml
303d336718bea78e2d87c224d4ddbb90 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkJdbcPropertiesTest.xml
f9932e4865ff3568659af96a88303c91 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskBufferTest.xml
2669edc934eddd06ab0d4fac8add6727 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskJsonInsertFormatTest.xml
3c90214e43d3e0f41e32ef28794973db TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskMappingTest.xml
4889cbbe4f04e42cfc6a8749b92b0879 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskSchemalessProxyTest.xml
9b340f4f77271a93bea087b5def56eb1 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskSchemalessTest.xml
b4583bb6c64d93b4e82c2e9d9e511486 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskStopUnitTest.xml
f8391cf97ba6b473b3031fe24060724e TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskStringTest.xml
d6f85e7f36ccd47eeb42aa09715d5560 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskTest.xml
bcc9f8476e1080bf914bac00d10c2eb1 TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskWithSchemaProxyTest.xml
307a1912e0216998ea6b177c6675418f TEST-com.clickhouse.kafka.connect.sink.ClickHouseSinkTaskWithSchemaTest.xml
16ba3e44e65fb0540e8ac76896035531 TEST-com.clickhouse.kafka.connect.sink.ClickHouseTestHarnessTest.xml
9deab2efe410bcc079331c9047b8a99e TEST-com.clickhouse.kafka.connect.sink.ClickHouseVersionDefaultUnitTest.xml
59b2cbc18e91ae8312975951664ec41b TEST-com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest.xml
98cdc27baad7826c84960aff7420edbb TEST-com.clickhouse.kafka.connect.sink.EmptyCdcSkipRouteLiveTest.xml
34fd21e6bb761243a6078dba4322cbe5 TEST-com.clickhouse.kafka.connect.sink.ExplicitJsonInsertPathLiveTest.xml
de47da7f88221b70b8ba5a2313fe09e9 TEST-com.clickhouse.kafka.connect.sink.HandleExceptionCauseCycleUnitTest.xml
5876f844316a18f4b4e98394d11b2656 TEST-com.clickhouse.kafka.connect.sink.InsertFormatPerformanceTest.xml
a31e66684a18fcb17be20f6ddc0b78f6 TEST-com.clickhouse.kafka.connect.sink.InsertStallBoundPinUnitTest.xml
37b41f2171f838434bebe4053a34d4e9 TEST-com.clickhouse.kafka.connect.sink.InsertTimeoutHelpTextPinUnitTest.xml
c0a7fae711f0cac7ac043774d9d084bf TEST-com.clickhouse.kafka.connect.sink.InsertV1WaitBoundPinUnitTest.xml
b690c43a227e2ffbbdb7917287186572 TEST-com.clickhouse.kafka.connect.sink.InsertWriteStallBoundPinUnitTest.xml
1ae61c76d9164a6fe75f4da271c7b457 TEST-com.clickhouse.kafka.connect.sink.JsonAsStringKeyResidualLiveTest.xml
ba5d23ac7640ba41dd466c6e70eadc14 TEST-com.clickhouse.kafka.connect.sink.JsonColumnTest.xml
9f1d7764f2392a5190cc87347e03db81 TEST-com.clickhouse.kafka.connect.sink.JsonDynamicRoundTripTest.xml
f22efd276ea5d1c554fc728a301ceff7 TEST-com.clickhouse.kafka.connect.sink.JsonInsertColumnsLiveTest.xml
310c3076cf1e6433bfdca12fdfecdd35 TEST-com.clickhouse.kafka.connect.sink.JsonInsertColumnsUnitTest.xml
9a47f18fb615465ec84b1a3b05819c70 TEST-com.clickhouse.kafka.connect.sink.JsonRouteValueFreeDlqUnitTest.xml
3f7930b6a60b89c31f246eb22ec622b5 TEST-com.clickhouse.kafka.connect.sink.MBeanNameUnitTest.xml
e63871b00ebc6bafecad80df4f69a797 TEST-com.clickhouse.kafka.connect.sink.MBeanRegistrationLiveTest.xml
48a03f54b35e840ef7686911a21ae304 TEST-com.clickhouse.kafka.connect.sink.MetadataFreeJsonTypeParityLiveTest.xml
380275f381b6fab487f7863d81a099ef TEST-com.clickhouse.kafka.connect.sink.NestedNullRowBinaryLiveTest.xml
16832994e12c221c9d69c22f58efeb90 TEST-com.clickhouse.kafka.connect.sink.NewUpstreamConfigDefaultsUnitTest.xml
3c253cbc09852c532f1ded06a65d59a3 TEST-com.clickhouse.kafka.connect.sink.NullableEnumRoundTripLiveTest.xml
da521c6d50e5d2bde3fda7e2e14446ed TEST-com.clickhouse.kafka.connect.sink.NullableTupleMatrixLiveTest.xml
d9fdf4f872768938cac27e6e369c99fe TEST-com.clickhouse.kafka.connect.sink.OriginalTopicInsertedOffsetsUnitTest.xml
6e751ecb43d9c2d15147eb0a6b81e1c5 TEST-com.clickhouse.kafka.connect.sink.ProtobufIngestTest.xml
f2c1e692b0ec2e952ffe8c6e502ff009 TEST-com.clickhouse.kafka.connect.sink.ProxySinkTaskConstructorUnitTest.xml
e865fba70270a31c321f350b8843fda3 TEST-com.clickhouse.kafka.connect.sink.ProxySinkTaskStopUnitTest.xml
459c5d068ff5919e9ee0e5c2b95c615a TEST-com.clickhouse.kafka.connect.sink.RawIdentifierNameLiveTest.xml
8e20c66000d2106293d97da5813886c4 TEST-com.clickhouse.kafka.connect.sink.RetriableExceptionPassThroughUnitTest.xml
40023cb5520793aafd37de3b150d3c65 TEST-com.clickhouse.kafka.connect.sink.RetriableRedeliveryLimitPinUnitTest.xml
a5093ce8740768ee59be8beba6d68692 TEST-com.clickhouse.kafka.connect.sink.RowBinaryClientParityLiveTest.xml
f6ccd09643594623e9a4a5a226f0d80f TEST-com.clickhouse.kafka.connect.sink.RowBinaryFailFastLiveTest.xml
894cecde7fd786238c3b01ae82a442b3 TEST-com.clickhouse.kafka.connect.sink.SimpleAggregateTupleRoundTripLiveTest.xml
b2ecd9933a3c8352f89ab815043b4b63 TEST-com.clickhouse.kafka.connect.sink.SmtSinkLandingTest.xml
9015917a41958a3178cde55df850ec14 TEST-com.clickhouse.kafka.connect.sink.TupleSizeNamedElementRoundTripLiveTest.xml
9980de92a08a4830c6855d9c4262d14d TEST-com.clickhouse.kafka.connect.sink.UnvettedRetriableCodesWarnLiveTest.xml
f91e7990c1996aa1c1df9db78755dcea TEST-com.clickhouse.kafka.connect.sink.UnvettedRetriableCodesWarnUnitTest.xml
424e57f4927c92f5b2492ed8b0e1f4fd TEST-com.clickhouse.kafka.connect.sink.ValueFreeDlqHeadersUnitTest.xml
ed8b851d7c3eb6f5bf7cecd313878bd3 TEST-com.clickhouse.kafka.connect.sink.WideIntegerTypeGateLiveTest.xml
11757486620238f8eb7695b5733aa182 TEST-com.clickhouse.kafka.connect.sink.WrappedDataExceptionClassificationPinUnitTest.xml
cfeb9a4339ef51df4b0867a60107c8df TEST-com.clickhouse.kafka.connect.sink.WriterErrorPathLiveTest.xml
cf40ed3181e2336df495eadb34ffd236 TEST-com.clickhouse.kafka.connect.sink.WriterFailureClassificationUnitTest.xml
8e2ebaa422bac9aa9108c863f349cf66 TEST-com.clickhouse.kafka.connect.sink.WriterRecordContextDlqUnitTest.xml
30300d4e2c4f7eb8afd2e4581f8b1c35 TEST-com.clickhouse.kafka.connect.sink.data.StructToJsonMapTest.xml
04f0705eb034359d33d2d6ab86160d99 TEST-com.clickhouse.kafka.connect.sink.data.convert.DebeziumRecordConvertorTest.xml
fd0cb0cdeaf6fcc3a9c18e0ef9f68756 TEST-com.clickhouse.kafka.connect.sink.db.AvroDecimalLogicalTest.xml
0398a8dc04e27da31ec70fc018928969 TEST-com.clickhouse.kafka.connect.sink.db.AvroNullableUnionVariantTest.xml
b95583f439c23e4e1d1345a7f2c04749 TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseReplicaPinningMockServerTest.xml
915824e7477c628d9b9251978731aa3d TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseWriterColumnContextTest.xml
1f72f10fca2be329531dbdf1e28c2320 TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseWriterNullMessageUnitTest.xml
57081e37c5193f16783a22c13ac831b5 TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseWriterRecordContextTest.xml
fcfc53a82a72b901ae78b7fee4cb1df8 TEST-com.clickhouse.kafka.connect.sink.db.ClickHouseWriterTest.xml
c1f399d5e60fa9ac5a4d7ec95e853eaf TEST-com.clickhouse.kafka.connect.sink.db.EmptyCdcSkipUnitTest.xml
d20d2fa64680e48b324a73a153f89b8d TEST-com.clickhouse.kafka.connect.sink.db.FieldDescriptorEscapeTest.xml
182c7f230bd9c204be40759829528355 TEST-com.clickhouse.kafka.connect.sink.db.JacksonOverIoValueFreeUnitTest.xml
5a1d786f952da51577ffe2a0f3d766d3 TEST-com.clickhouse.kafka.connect.sink.db.JsonRouteValueFreeUnitTest.xml
e976ea81e5b5d4c4b62af212d617b16f TEST-com.clickhouse.kafka.connect.sink.db.JsonSerializationEquivalenceTest.xml
29e8ed77ddb994572ec35bdd7f6b2201 TEST-com.clickhouse.kafka.connect.sink.db.MetadataFreeJsonInsertUnitTest.xml
ef6d87a5e4fdfd37e685932028792f4d TEST-com.clickhouse.kafka.connect.sink.db.MetadataFreeJsonWriterTest.xml
f72b5664d985593ca5c95f6b4d98e698 TEST-com.clickhouse.kafka.connect.sink.db.MissingNonNullableValueWriteTest.xml
b3f499aadd138b0fa9c2e431d900935a TEST-com.clickhouse.kafka.connect.sink.db.NestedNullableCompositeWriteTest.xml
e5f111795be107933b0a9d20623d7747 TEST-com.clickhouse.kafka.connect.sink.db.NullableEnumWriteTest.xml
55b53e0e69da1b5e1475c006032ad76e TEST-com.clickhouse.kafka.connect.sink.db.RowBinaryUnsupportedTypeGateTest.xml
d81d573813103a01fd567d8296736242 TEST-com.clickhouse.kafka.connect.sink.db.SlashColumnRoundTripTest.xml
a58e9f130c138909cf849692b09b5805 TEST-com.clickhouse.kafka.connect.sink.db.WriterLogMaskingUnitTest.xml
88050cc55505f4b83fc652027bae7370 TEST-com.clickhouse.kafka.connect.sink.db.WriterRecordContextUnitTest.xml
5dea7dcd6b5e19a6f7f95b55539f4dc0 TEST-com.clickhouse.kafka.connect.sink.db.WriterValueFreeFailureUnitTest.xml
60d649f44e8aa4e9602e488a081b7e17 TEST-com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClientForkLiveTest.xml
e3ac42a11f407c12c3252d925b740d02 TEST-com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClientForkUnitTest.xml
8d25dff63122093e57d676531e903d47 TEST-com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClientTest.xml
18011fbb28fb8e10aa29af637c03c36a TEST-com.clickhouse.kafka.connect.sink.db.helper.Client011BehaviourPinLiveTest.xml
5191ef87bfecaaa7e046ce55d6bec667 TEST-com.clickhouse.kafka.connect.sink.db.helper.ClientEndpointAndOptionsPinUnitTest.xml
13eb2390a3a9b74575fa97825c277bec TEST-com.clickhouse.kafka.connect.sink.db.helper.ClientErrorShapePinUnitTest.xml
424840253d4102f6c99ddfcdcd765d51 TEST-com.clickhouse.kafka.connect.sink.db.helper.ClientInsertWirePinUnitTest.xml
c466c983ee79159e320aefd3b0a50c74 TEST-com.clickhouse.kafka.connect.sink.db.helper.ClientTimeoutPrefixPinUnitTest.xml
b58915f11fb6aa595e7b454f50917f67 TEST-com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureSnapshotUnitTest.xml
24c4e8a441917fb4e98575198935ba40 TEST-com.clickhouse.kafka.connect.sink.db.helper.DescribeFixturesUnitTest.xml
0bc81a8eca60083be20c529b7ab7dbdc TEST-com.clickhouse.kafka.connect.sink.db.helper.DescribeTableBuilderUnitTest.xml
1456ec7d6610346b5afc92b7381f0888 TEST-com.clickhouse.kafka.connect.sink.db.helper.DescribeUnreadableEnumMockServerTest.xml
67136d027b206a59d68f80299e7e62e1 TEST-com.clickhouse.kafka.connect.sink.db.helper.FieldDescriptorLiteralFieldsUnitTest.xml
a372045384e4f110b36affa4548b4c74 TEST-com.clickhouse.kafka.connect.sink.db.helper.InsertBoundGateUnitTest.xml
01d3ef43ceee7c0c7177d53cf3cfde80 TEST-com.clickhouse.kafka.connect.sink.db.helper.InsertSocketTimeoutOptionUnitTest.xml
56df9a3480aadd7722b7a3eabc39d601 TEST-com.clickhouse.kafka.connect.sink.db.helper.Lz4RoundTripLiveTest.xml
c287206ef36e46a595ff8d1a370b26b3 TEST-com.clickhouse.kafka.connect.sink.db.helper.NetworkCompressionPinLiveTest.xml
92ac8166e511f04ad77bf5f254356c11 TEST-com.clickhouse.kafka.connect.sink.db.helper.NetworkCompressionPinUnitTest.xml
dd073e58c82098c0b9dc6f4b0e178abe TEST-com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrixFixturesUnitTest.xml
a47cffe1e1ed5c4e5e53d46ad2c400fa TEST-com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrixLiveTest.xml
a36eb7f5f2e5c808cc3a0f542b046bca TEST-com.clickhouse.kafka.connect.sink.db.helper.StubClickHouseServerRawModeUnitTest.xml
ad8f8dc62fb9073497ee71bc884a05bc TEST-com.clickhouse.kafka.connect.sink.db.helper.WriterHelperClientClosingUnitTest.xml
2077439366684f2a0480be6d0b3e6f63 TEST-com.clickhouse.kafka.connect.sink.db.mapping.ColumnArrayRecursionTest.xml
6b32c78f2c0d1959ac5ab91e23cefca4 TEST-com.clickhouse.kafka.connect.sink.db.mapping.ColumnTest.xml
179896a5ec334876eacbeacdef887cf6 TEST-com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullabilityTest.xml
9ea5312005b1cb16973cce775b2180ff TEST-com.clickhouse.kafka.connect.sink.db.mapping.JsonDynamicTypeMappingTest.xml
48c9bfff80b8cfac12df1434d79b1d8b TEST-com.clickhouse.kafka.connect.sink.db.mapping.TableTest.xml
ce9d63207d7601d0262b463b744ea465 TEST-com.clickhouse.kafka.connect.sink.db.mapping.UnescapeJsonLayerTest.xml
ab5023b358d45b7711aeb167ad857197 TEST-com.clickhouse.kafka.connect.sink.dlq.FailureTest.xml
7d4cf05959e8a51852cc03957966eefe TEST-com.clickhouse.kafka.connect.sink.kafa.RangeContainerTest.xml
10d823fd75c506c33ccb986685fcd0ec TEST-com.clickhouse.kafka.connect.sink.processing.EmptyCdcExactlyOnceStateTest.xml
ca3585d5adbe848dd7f521958a8f7116 TEST-com.clickhouse.kafka.connect.sink.processing.ProcessingRenamedTopicStateKeyUnitTest.xml
e0333ad72b563e8ba218dc2555b881ca TEST-com.clickhouse.kafka.connect.sink.processing.ProcessingTest.xml
a7e76644e8c680fce0c15c8da7f4a5b9 TEST-com.clickhouse.kafka.connect.sink.provider.LocalProviderTest.xml
fbfad121cfa33fc11d7e2ae188173f39 TEST-com.clickhouse.kafka.connect.sink.state.provider.KeeperStateProviderClosingUnitTest.xml
70b17900c724a8bbad784206cee565a0 TEST-com.clickhouse.kafka.connect.sink.state.provider.KeeperStateProviderRestartLiveTest.xml
e77e485c134f9f7dafb08c8272c56570 TEST-com.clickhouse.kafka.connect.sink.util.MaskTest.xml
9cddfd27bdb89828354263cd3ffdccdd TEST-com.clickhouse.kafka.connect.sink.util.UtilsNullMessageClassificationUnitTest.xml
3e7c7af9e2b92fcf8aeb53e3c02dc758 TEST-com.clickhouse.kafka.connect.sink.util.UtilsTest.xml
b13fe169c620c1512322e5750cfc8589 TEST-com.clickhouse.kafka.connect.transforms.KeyToValueTest.xml
cb72770bd52983fb3203b878a6883291 TEST-com.clickhouse.kafka.connect.util.WithheldMessageExceptionUnitTest.xml
7f88e7dbaf3f5292ec8a6ed758aa51e8 TEST-com.clickhouse.kafka.connect.util.jmx.SimpleMovingAverageTest.xml
9608b8bbc2895e920a2a5af3020f0791 TEST-transforms.ConvertToMapOrJsonPerformanceTestJson.xml
3b6d46e72fe24138b728af71ba1b52f0 TEST-transforms.ConvertToMapOrJsonTest.xml
b6417680c9fd51cc474df1ae0c4bf322 TEST-transforms.HeaderToValueTest.xml
da89cdb12a407f366a651cf9b9a894cf TEST-transforms.HeadersToValueTest.xml
47f4bd563d7fe5e6ec0cc953bb7ae10d TEST-transforms.TableOperationFilterTest.xml
```

Recorded 2026-11-06T08:30:35Z.

Verdict: pass

### E-02: Document check scripts calibrated on the untouched base

Backs: DOC-01, DOC-02, ROADMAP Phase 48 criterion 4 (the published documents name no planning identifier and no moved path; a resolver and a scan that read the current defects on the base and read clean after the edits).

```sh
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py c77d1a147ae4acd37100b73db08f1851047bc055 docs/FORK-DELTA.md   # exit 1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py c77d1a147ae4acd37100b73db08f1851047bc055 docs/FORK-DELTA.md --list | grep -c '^BY-DESIGN '
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py c77d1a147ae4acd37100b73db08f1851047bc055 CHANGELOG.md   # and the gg6, gg1 and gg7 notes under docs/
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py c77d1a147ae4acd37100b73db08f1851047bc055 docs/ROLLOUT-v1.7.0_gg6.md   # exit 1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py v1.7.0_gg6 docs/ROLLOUT-v1.7.0_gg6.md
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py v1.7.0_gg7 docs/ROLLOUT-v1.7.0_gg6.md   # exit 1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py c77d1a147ae4acd37100b73db08f1851047bc055 docs/ROLLOUT-v1.7.0_gg1.md   # exit 1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py c77d1a147ae4acd37100b73db08f1851047bc055 docs/ROLLOUT-v1.7.0_gg7.md   # exit 1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py c77d1a147ae4acd37100b73db08f1851047bc055 CHANGELOG.md
md5 -q .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py
git add .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py
git commit -F /tmp/gsd48/exec/msg-t2a.txt
```

```text
path_check_md5: 641402547d365dc5b663d6c88d6e1604
id_scan_md5: 0922b7a9ab304cb8a77f181798827ed6
scripts_stdlib_only: yes
scripts_write_nothing: yes
fd_paths_line: paths: candidates=99 resolved=69 abbreviated=20 abbreviated_unique=20 placeholders=2 by_design=6 unresolved=2
fd_unresolved: .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md; .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md
fd_by_design_listed: 6
changelog_paths_line: paths: candidates=12 resolved=11 abbreviated=0 abbreviated_unique=0 placeholders=0 by_design=1 unresolved=0
gg6_paths_line: paths: candidates=6 resolved=6 abbreviated=0 abbreviated_unique=0 placeholders=0 by_design=0 unresolved=0
gg1_paths_line: paths: candidates=1 resolved=1 abbreviated=0 abbreviated_unique=0 placeholders=0 by_design=0 unresolved=0
gg7_paths_line: paths: candidates=5 resolved=4 abbreviated=0 abbreviated_unique=0 placeholders=0 by_design=1 unresolved=0
gg6_ids_line: ids: hits=3
gg6_ids_hits: 3 FUT-05; 114 FUT-05; 148 Phase 39
gg6_tag_ids_line: ids: hits=0
gg6_in_gg7_tag_ids_line: ids: hits=3
gg1_ids_line: ids: hits=26
gg7_ids_line: ids: hits=1
changelog_ids_line: ids: hits=0
scripts_commit: 1f1fa429fc0975b2ea6f840483b52db3f98e27ae
```

Calibration attempts: one. Both scripts matched every row of the plan's calibration table on the first run, so neither was changed after its first write (the two by-design lists and the token rule are those of the specification). The two unresolved tokens are the moved evidence paths of the 25 and 35 archives at FORK-DELTA lines 37 and 32; the six `BY-DESIGN` lines are `.github/workflows/test-head.yaml`, `.planning/scripts/compare-confluent-archives.py`, `build/raw-name-matrix`, `gradle/actions/setup-gradle`, `gradle/verification-metadata.xml` and `src/test/java/transforms/KeyToValueTest.java`. The gg1 scan prints 26 `ID` lines and the gg7 scan prints `ID 130 FUT-05`. The `/tmp/gsd48/scripts/resolve.py` research resolver was present and was read as the reference for the token rule; the scripts here use `git ls-tree` instead of a `git archive` copy, as the specification says. The probe directory `/tmp/gsd48/probes` holds only `Probe48EnumCauseUnitTest.java`; it is not read by this plan.

Recorded 2026-11-06T08:32:22Z.

Verdict: pass

### E-03: Null binding of the unreadable enum describe (28 IN-05(b) item i, U-5)

Backs: P37-01 (the null assertion, fixed additively per U-5), P37-02 (28 IN-05(b) item i), ROADMAP Phase 48 criterion 1 (no existing assertion changed, seen red via a non-enum failure).

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*DescribeUnreadableEnum*' > /tmp/gsd48/exec/nb-green.log 2>&1
rm -rf /tmp/gsd48/exec/xml-nb-green && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-nb-green
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit /tmp/gsd48/exec/xml-nb-green --class .DescribeUnreadableEnumNullBindingUnitTest   # and .DescribeUnreadableEnumMockServerTest
git diff --quiet c77d1a147ae4acd37100b73db08f1851047bc055 -- src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumMockServerTest.java
grep -cF '? UNREADABLE_ENUM_ROW :' src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java
/tmp/gsd48/exec/fmt.sh nb src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java   # throwaway worktree /tmp/gsd48/fmt, spotlessApply, cmp
git add src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java
git commit -F /tmp/gsd48/exec/msg-nb.txt
git worktree add --detach /tmp/gsd48/wt-nb 4de705a6e0132bb884796cd58635fe40be724464
python3 -I /tmp/gsd48/exec/mutate.py /tmp/gsd48/wt-nb/<NB> '? UNREADABLE_ENUM_ROW :' '? MALFORMED_ROW :'
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd48/wt-nb --console=plain testUnit --rerun --tests '*DescribeUnreadableEnumNullBindingUnitTest*' > /tmp/gsd48/exec/nb-mut.log 2>&1
cp -R /tmp/gsd48/wt-nb/build/test-results/testUnit /tmp/gsd48/exec/xml-nb-mut
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed /tmp/gsd48/exec/xml-nb-mut --expect 'DescribeUnreadableEnumNullBindingUnitTest#unreadableEnumTableIsNullBecauseTheEnumScanRefusedIt'
git worktree remove --force /tmp/gsd48/wt-nb && git worktree prune
```

```text
nb_green_tests: 4
nb_green_failures: 0
nb_green_errors: 0
mk_green_tests: 2
mk_green_failures: 0
mk_identical_to_base: yes
nb_mutation_anchor_count: 1
fmt_nb: identical
nb_commit: 4de705a6e0132bb884796cd58635fe40be724464
nb_mut_tests: 4
nb_mut_failures: 2
nb_mut_errors: 0
nb_mut_failed: DescribeUnreadableEnumNullBindingUnitTest#unreadableEnumTableIsNullBecauseTheEnumScanRefusedIt
nb_mut_worktree_removed: yes
```

Notes. The green run filter `*DescribeUnreadableEnum*` selects the new class (4 cases) and the unchanged `DescribeUnreadableEnumMockServerTest` (2 cases), 6 tests all green with Docker unreachable. The mutation makes the stub answer the malformed row for the enum table too; the describe still returns null (the existing assertion holds), the ERROR event carries a throwable but not the enum refusal text, so exactly the two `unreadableEnumTableIsNullBecauseTheEnumScanRefusedIt` rows (client V1 and V2) fail and the two `aNonEnumDescribeFailureIsAlsoNullButNamesNoEnumCause` rows stay green. The mutation lives only in the worktree; the main tree was not changed.

Recorded 2026-11-06T08:36:30Z.

Verdict: pass

### E-04: Two-literal-segment rule over every shipped allow-list (UF-37-3, U-9)

Backs: P37-03 (UF-37-3: the wildcard rule pinned over every shipped list and the next one, per U-9 with no edit of the release path), ROADMAP Phase 48 criterion 3.

```sh
python3 -I -c '<count over .planning/scripts/confluent-archive-allowlist-*.txt: rows that are neither blank nor start with #, glob = text before the first TAB stripped, wildcard = any of * ? [>'
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*AllowListWildcardRuleUnitTest*' > /tmp/gsd48/exec/aw-green.log 2>&1
rm -rf /tmp/gsd48/exec/xml-aw-green && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-aw-green
git diff --quiet c77d1a147ae4acd37100b73db08f1851047bc055 -- .github/scripts/compare_confluent_archives.py .planning/scripts
md5 -q .github/scripts/compare_confluent_archives.py
/tmp/gsd48/exec/fmt.sh aw src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java
git add src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java
git commit -F /tmp/gsd48/exec/msg-aw.txt
git worktree add --detach /tmp/gsd48/wt-aw 07e6566279e7b7d6a7c960d304a3eda037a22aa3
printf 'com/*\tmutation: a broad row\n' >> /tmp/gsd48/wt-aw/.planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd48/wt-aw --console=plain testUnit --rerun --tests '*AllowListWildcardRuleUnitTest*' > /tmp/gsd48/exec/aw-mut.log 2>&1
cp -R /tmp/gsd48/wt-aw/build/test-results/testUnit /tmp/gsd48/exec/xml-aw-mut
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed /tmp/gsd48/exec/xml-aw-mut --expect 'AllowListWildcardRuleUnitTest#everyShippedWildcardRowFollowsTwoLiteralSegments'
git worktree remove --force /tmp/gsd48/wt-aw && git worktree prune
git status --porcelain .planning/scripts
```

```text
shipped_lists: 5
shipped_wildcard_rows: 49
shipped_min_literal_segments: 2
aw_green_tests: 9
aw_green_failures: 0
aw_green_errors: 0
release_path_unchanged: yes
fmt_aw: identical
aw_commit: 07e6566279e7b7d6a7c960d304a3eda037a22aa3
aw_mut_tests: 9
aw_mut_failures: 1
aw_mut_failed: AllowListWildcardRuleUnitTest#everyShippedWildcardRowFollowsTwoLiteralSegments
aw_mut_message_names_row: yes
aw_mut_worktree_removed: yes
main_tree_lists_untouched: yes
```

Notes. The independent count (a stdlib script with the test's row and wildcard rule, run before the test was written) reads 5 lists, 49 wildcard rows and a minimum of 2 literal path segments (the row `META-INF/versions/*/com/fasterxml/jackson/core/internal/shaded/fdp/*` of the `-25` and `-gg4` lists); the test, run on the unchanged lists, is 9 of 9 green. The mutation appends `com/*<TAB>mutation: a broad row` to the worktree copy of the gg7 list only; exactly the shipped-list case fails and its message reads `wildcard rows with fewer than two literal path segments before the first wildcard: [confluent-archive-allowlist-v1.7.0_gg7.txt:29: com/*]`. The compare script md5 is `a637e0cc8fb33a33e7673ac682fa63c8`, equal to E-01 `compare_script_md5`; the script and every list are unedited since the base.

Recorded 2026-11-06T08:37:43Z.

Verdict: pass

### E-05: MBean unregister failure in ProxySinkTask.stop (40 IN-03)

Backs: P37-04 (40 IN-03: the branch of `ProxySinkTask.stop()` where the statistics unregister throws was never exercised), ROADMAP Phase 48 criterion 2, U-13.

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ProxySinkTaskStopUnitTest*' > /tmp/gsd48/exec/ps-green.log 2>&1
rm -rf /tmp/gsd48/exec/xml-ps-green && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-ps-green
git diff --numstat -- src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
/tmp/gsd48/exec/fmt.sh ps src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
git add src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
git commit -F /tmp/gsd48/exec/msg-ps.txt
git worktree add --detach /tmp/gsd48/wt-ps 69b75cb31b41e34652ed8d94e414f657df6cb03b
python3 -I /tmp/gsd48/exec/mutate.py /tmp/gsd48/wt-ps/src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java $'            statistics.unregisterMBean();\n        } catch (RuntimeException | Error e) {' $'            statistics.unregisterMBean();\n        } catch (Error e) {'
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd48/wt-ps --console=plain testUnit --rerun --tests '*ProxySinkTaskStopUnitTest*' > /tmp/gsd48/exec/ps-mut.log 2>&1
cp -R /tmp/gsd48/wt-ps/build/test-results/testUnit /tmp/gsd48/exec/xml-ps-mut
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed /tmp/gsd48/exec/xml-ps-mut --expect 'ProxySinkTaskStopUnitTest#mBeanUnregisterFailureStillClosesTheStateProviderAndReachesTheCaller' 'ProxySinkTaskStopUnitTest#mBeanUnregisterFailureStaysTheThrownExceptionWhenTheStateProviderCloseAlsoFails'
git worktree remove --force /tmp/gsd48/wt-ps && git worktree prune
```

```text
ps_green_tests: 6
ps_green_failures: 0
ps_lines_deleted: 0
fmt_ps: identical
ps_commit: 69b75cb31b41e34652ed8d94e414f657df6cb03b
ps_mut_tests: 6
ps_mut_failures: 2
ps_mut_failed: ProxySinkTaskStopUnitTest#mBeanUnregisterFailureStaysTheThrownExceptionWhenTheStateProviderCloseAlsoFails ProxySinkTaskStopUnitTest#mBeanUnregisterFailureStillClosesTheStateProviderAndReachesTheCaller
ps_mut_worktree_removed: yes
```

Notes. The class had 4 cases at the base (E-01 `ps_tests`); it now has 6, all green with Docker unreachable, and the commit adds 75 lines and deletes none. The mutation narrows the unregister catch of `ProxySinkTask.stop()` from `RuntimeException | Error` to `Error` in the worktree only: an unregister failure then leaves `stop()` before the state provider close, so the two new cases fail (provider close count 0; and, in the second case, the provider close failure is not suppressed on the thrown exception) and the four existing cases stay green. The new cases swap a `SinkTaskStatistics` subclass into the task's `statistics` field before `start()`; it unregisters through `super` before it throws, so no MBean stays registered.

Recorded 2026-11-06T08:39:28Z.

Verdict: pass

### E-06: MBean precondition and the CLOSE_COUNT and MBEAN_GONE assertions seen red (47 IN-01)

Backs: P37-04 (47 IN-01: `assertProxyStoppedOnce` assertions that no run had ever seen red, and a task MBean that could be absent before stop), ROADMAP Phase 48 criterion 2, U-13.

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClickHouseSinkTaskStopUnitTest*' > /tmp/gsd48/exec/cs-green.log 2>&1
rm -rf /tmp/gsd48/exec/xml-cs-green && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-cs-green
git diff --numstat -- src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
/tmp/gsd48/exec/fmt.sh cs src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
git add src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
git commit -F /tmp/gsd48/exec/msg-cs.txt
# m1, m2, m3: each in its own worktree at ae31c4a9 (wt-cs1, wt-cs2, wt-cs3), filter '*ClickHouseSinkTaskStopUnitTest*', snapshots cs-mut1..3
python3 -I /tmp/gsd48/exec/mutate.py <wt-cs1>/src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java '            stateProvider.close();' '            stateProvider.getClass();'
python3 -I /tmp/gsd48/exec/mutate.py <wt-cs2>/src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java '            statistics.unregisterMBean();' '            statistics.getClass();'
python3 -I /tmp/gsd48/exec/mutate.py <wt-cs3>/src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java 'MBeanServerUtils.registerMBean(this, name);' 'name.getClass();'
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd48/wt-csN --console=plain testUnit --rerun --tests '*ClickHouseSinkTaskStopUnitTest*' > /tmp/gsd48/exec/cs-mutN.log 2>&1
cp -R /tmp/gsd48/wt-csN/build/test-results/testUnit /tmp/gsd48/exec/xml-cs-mutN
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed /tmp/gsd48/exec/xml-cs-mut3 --expect 'ClickHouseSinkTaskStopUnitTest#theTaskMBeanIsRegisteredBeforeStopAndGoneAfter'
git worktree remove --force /tmp/gsd48/wt-csN && git worktree prune
```

```text
cs_green_tests: 7
cs_green_failures: 0
cs_lines_deleted: 0
fmt_cs: identical
cs_commit: ae31c4a9a3bb9d9d88d192d405778efec13d0170
cs_mut1_tests: 7
cs_mut1_failures: 7
cs_mut1_message_hits: 7
cs_mut2_tests: 7
cs_mut2_failures: 7
cs_mut2_message_hits: 7
cs_mut3_tests: 7
cs_mut3_failures: 1
cs_mut3_message_hits: 1
cs_mut3_failed: ClickHouseSinkTaskStopUnitTest#theTaskMBeanIsRegisteredBeforeStopAndGoneAfter
cs_mut_worktrees_removed: yes
```

Notes. The class had 6 test methods (E-01 `cs_tests`, the shipped-strategy method counting 3 rows) and now has 7, all green with Docker unreachable; the commit adds 12 lines and deletes none. m1 replaces the state provider close in `ProxySinkTask.stop()` and turns all 7 cases red, every failure message containing `close count of the state provider`; m2 replaces the statistics unregister and turns all 7 red, every message containing `the task MBean must be unregistered after stop`; neither assertion had been seen red before. m3 removes the MBean registration in `SinkTaskStatistics.registerMBean()`: the six existing cases pass with no MBean ever registered, because their "gone after stop" assertion is satisfied vacuously, and only the new `theTaskMBeanIsRegisteredBeforeStopAndGoneAfter` case fails, with the message `the task MBean must be registered before stop`.

Recorded 2026-11-06T08:40:57Z.

Verdict: pass

### E-07: Blank, empty and repeated entries of additionalRetriableErrorCodes (42 IN-03 part b)

Backs: P37-04 (42 IN-03 part b: the start-up WARN for `additionalRetriableErrorCodes` had no row for blank, empty or repeated entries), ROADMAP Phase 48 criterion 2, U-13.

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*UnvettedRetriableCodesWarnUnitTest*' > /tmp/gsd48/exec/uw-green.log 2>&1
rm -rf /tmp/gsd48/exec/xml-uw-green && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-uw-green
git diff --numstat -- src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
/tmp/gsd48/exec/fmt.sh uw src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
git add src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
git commit -F /tmp/gsd48/exec/msg-uw.txt
git worktree add --detach /tmp/gsd48/wt-uw 64ef75686f459e067226f9d48004109034c91d88
python3 -I /tmp/gsd48/exec/mutate.py /tmp/gsd48/wt-uw/src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java 'if (code.isEmpty()) {' 'if (code.isEmpty() && false) {'
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd48/wt-uw --console=plain testUnit --rerun --tests '*UnvettedRetriableCodesWarnUnitTest*' > /tmp/gsd48/exec/uw-mut.log 2>&1
cp -R /tmp/gsd48/wt-uw/build/test-results/testUnit /tmp/gsd48/exec/xml-uw-mut
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-uw-mut /tmp/gsd48/exec/status-uw-mut.tsv
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py count /tmp/gsd48/exec/status-uw-mut.tsv .UnvettedRetriableCodesWarnUnitTest FAIL --name-contains '216,,159'   # and '159,,60'
git worktree remove --force /tmp/gsd48/wt-uw && git worktree prune
```

```text
uw_green_tests: 28
uw_green_failures: 0
uw_lines_deleted: 0
fmt_uw: identical
uw_commit: 64ef75686f459e067226f9d48004109034c91d88
uw_mut_tests: 28
uw_mut_failures: 2
uw_mut_fail_inner_empty_vetted: count=1
uw_mut_fail_inner_empty_unvetted: count=1
uw_mut_worktree_removed: yes
```

Notes. The class had 21 cases at the base (E-01 `uw_tests`); it now has 28, all green with Docker unreachable, and the commit adds 26 lines and deletes none. Seven rows were added: `" "`, `","` and `"216,,159"` log no WARN, and `"159,,60"`, `"60,60"`, `"60,"` and `"159,60,"` each log exactly one WARN naming `[60]`. The mutation turns the empty-entry skip of `ClickHouseSinkConfig.parseErrorCodes` into `code.isEmpty() && false` in the worktree only: an empty entry between two commas then reaches `Integer.parseInt`, the setting is refused before the connection step, and exactly the `216,,159` row and the `159,,60` row fail (a blank value, a lone comma and a trailing comma never reach the loop body with an empty entry, so those rows stay green). Part (a) of the 42 IN-03 row, the port race of `closedPort()`, is not changed and is closed in the ledger.

Recorded 2026-11-06T08:42:13Z.

Verdict: pass

### E-08: Plain DataException control (42 IN-04)

Backs: P37-04 (42 IN-04: no row of the pass-through matrix depended on the `DataException` branch of `Utils.handleException`), ROADMAP Phase 48 criterion 2, U-13.

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*RetriableExceptionPassThroughUnitTest*' > /tmp/gsd48/exec/rp-green.log 2>&1
rm -rf /tmp/gsd48/exec/xml-rp-green && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-rp-green
git diff --numstat -- src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
/tmp/gsd48/exec/fmt.sh rp src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
git add src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
git commit -F /tmp/gsd48/exec/msg-rp.txt
git worktree add --detach /tmp/gsd48/wt-rp 3182791f3ab7496eb33dc1228f6f7e93a0637eba
python3 -I /tmp/gsd48/exec/mutate.py /tmp/gsd48/wt-rp/src/main/java/com/clickhouse/kafka/connect/util/Utils.java 'if (e instanceof DataException && !errorsTolerance) {' 'if (e instanceof DataException && !errorsTolerance && false) {'
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd48/wt-rp --console=plain testUnit --rerun --tests '*RetriableExceptionPassThroughUnitTest*' > /tmp/gsd48/exec/rp-mut.log 2>&1
cp -R /tmp/gsd48/wt-rp/build/test-results/testUnit /tmp/gsd48/exec/xml-rp-mut
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-rp-mut /tmp/gsd48/exec/status-rp-mut.tsv
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py count /tmp/gsd48/exec/status-rp-mut.tsv .RetriableExceptionPassThroughUnitTest FAIL --name-contains plainDataExceptionControlOutcome
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py count /tmp/gsd48/exec/status-rp-mut.tsv .RetriableExceptionPassThroughUnitTest FAIL --name-contains 'errors.tolerance=none expected=DATA_EXCEPTION'
git worktree remove --force /tmp/gsd48/wt-rp && git worktree prune
```

```text
rp_green_tests: 40
rp_green_failures: 0
rp_lines_deleted: 0
fmt_rp: identical
rp_commit: 3182791f3ab7496eb33dc1228f6f7e93a0637eba
rp_mut_tests: 40
rp_mut_failures: 1
rp_mut_fail_none_row: count=1 (name contains errors.tolerance=none expected=DATA_EXCEPTION)
rp_mut_worktree_removed: yes
```

Notes. The class had 38 cases at the base (E-01 `rp_tests`); it now has 40, all green with Docker unreachable, and the commit adds 34 lines and deletes none. The two new rows pass `new DataException(...)`, a `DataException` with no retriable cause, through `Utils.handleException`: under `errors.tolerance=none` the same instance is rethrown (`assertSame`), under `all` nothing is thrown (`assertNull`). `directMatrix` never selects `Outcome.DATA_EXCEPTION`, which is why the branch had no row that depended on it. The mutation adds `&& false` to the `DataException` branch of `Utils.handleException` in the worktree only: the `none` row then sees the generic `RuntimeException` wrap and fails, the `all` row and every existing row stay green (39 of 40), which is exactly the gap the control closes.

Recorded 2026-11-06T08:43:02Z.

Verdict: pass

### E-09: Wave gate: testUnit with Docker unreachable at the base count plus 25, exact delta

Backs: P37-01, P37-02, P37-03, P37-04 (the six pins add exactly 25 testcases and remove none), ROADMAP Phase 48 criteria 1 to 3 (no existing assertion changed, no `src/main`, `.github` or `.planning/scripts` change).

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd48/exec/w2-testunit.log 2>&1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed build/test-results/testUnit
rm -rf /tmp/gsd48/exec/xml-w2-testunit && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-w2-testunit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-w2-testunit /tmp/gsd48/exec/status-w2-testunit.tsv
python3 -I -c '<filter of status-w2-testunit.tsv to the six new classes and methods, written to 48-expected-added.tsv in dump order>'
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py delta /tmp/gsd48/exec/status-base-testunit.tsv /tmp/gsd48/exec/status-w2-testunit.tsv .planning/phases/48-phase-37-dispositions-document-fixes/48-expected-added.tsv
shasum -a 256 .planning/phases/48-phase-37-dispositions-document-fixes/48-expected-added.tsv
git add .planning/phases/48-phase-37-dispositions-document-fixes/48-expected-added.tsv
git commit -F /tmp/gsd48/exec/msg-xa.txt
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit /tmp/gsd48/exec/xml-w2-testunit --class .ProxySinkTaskStopUnitTest   # and the seven other touched and neighbour classes
git diff --name-only c77d1a147ae4acd37100b73db08f1851047bc055 HEAD -- src
git diff --numstat c77d1a147ae4acd37100b73db08f1851047bc055 HEAD -- <the four extended test files>
git diff --quiet c77d1a147ae4acd37100b73db08f1851047bc055 HEAD -- src/main .github .planning/scripts
```

```text
w2_testunit_docker_host: unix:///nonexistent.sock
w2_testunit_exit: 0
w2_testunit_tests: 2320
w2_testunit_failures: 0
w2_testunit_errors: 0
w2_testunit_skipped: 2
expected_added_rows: 25
expected_added_groups: 4 9 2 1 7 2
expected_added_all_pass: yes
expected_added_sha256: 89c2dd51e59fb741a114322035a4ff5a976ee791e764782ad86c28c766208126
expected_added_commit: ee4f9d6e
status_delta_w2: delta: removed=0 added=25 expected=25 match=yes
touched_counts_after: yes
scope_since_base: yes
jvm_crash: no
javacore_files: none
```

Notes. The cache-busted testUnit (2320 tests, 107 XML files against 105 at the base: the two new classes) is E-01's 2295 plus 25, with 0 failures, 0 errors and the same 2 skips, and the default toolchain ran it with no JVM crash, so no `-PjavaVersion=21` run was needed in plan 48-02. `48-expected-added.tsv` holds the 25 rows in dump order, grouped 4 (`DescribeUnreadableEnumNullBindingUnitTest`), 9 (`AllowListWildcardRuleUnitTest`), 2 (`ProxySinkTaskStopUnitTest`), 1 (`ClickHouseSinkTaskStopUnitTest`), 7 (`UnvettedRetriableCodesWarnUnitTest`) and 2 (`RetriableExceptionPassThroughUnitTest`), all PASS; plans 48-03 and 48-05 use it as the expected set of their deltas. `touched_counts_after` is `yes`: `ProxySinkTaskStopUnitTest` 6, `ClickHouseSinkTaskStopUnitTest` 7, `UnvettedRetriableCodesWarnUnitTest` 28 and `RetriableExceptionPassThroughUnitTest` 40 (E-01 plus 2, 1, 7, 2), and `DescribeUnreadableEnumMockServerTest` 2, `CompareConfluentArchivesScriptUnitTest` 58, `DeclaredNullabilityTest` 69 and `SourcePlanningIdGuardUnitTest` 18 at E-01's counts, all green. `scope_since_base` is `yes`: since the base the `src` diff is exactly the two new classes and the four extended files (additions 12, 75, 34 and 26 lines, deletions 0 each), and `src/main`, `.github` and `.planning/scripts` are unchanged.

Recorded 2026-11-06T08:44:28Z.

Verdict: pass

### E-10: 28 IN-01: ESCAPED_NEWLINE moved to the test fixtures, compile red first, own commit and testUnit gate (U-4)

Backs: P37-01 (28 IN-01), ROADMAP Phase 48 criterion 1 (fixed in its own commit with its red state recorded first, no assertion value changed), U-4, D-3 (the production diff is the three deleted lines and nothing else).

```sh
python3 -I -c '<delete the Javadoc line, the constant line and the blank line after it from DeclaredNullability.java; asserts the constant occurs once, the line before is the one-line Javadoc, the line after is blank>'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileTestJava > /tmp/gsd48/exec/move-red.log 2>&1
grep -E '^[0-9]+ errors?$' /tmp/gsd48/exec/move-red.log
grep -E '^/.*\.java:[0-9]+: error:' /tmp/gsd48/exec/move-red.log | sed -E 's#.*/([A-Za-z]+\.java):.*#\1#' | sort | uniq -c
git diff --numstat -- src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
python3 -I -c '<repoint the 8 references, add the DeclaredNullabilityFixtures import to the two live tests, add the constant and the one-sentence Javadoc to DeclaredNullabilityFixtures>'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileTestJava > /tmp/gsd48/exec/move-green-compile.log 2>&1
grep -c 'public static final String ESCAPED_NEWLINE = "\\\\n";' src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
grep -rho 'DeclaredNullability.ESCAPED_NEWLINE' src | wc -l
grep -rho 'DeclaredNullabilityFixtures.ESCAPED_NEWLINE' src/test | wc -l
grep -c ESCAPED_NEWLINE src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*DeclaredNullability*' --tests '*SourcePlanningIdGuardUnitTest*' > /tmp/gsd48/exec/move-neighbours.log 2>&1
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd48/exec/move-testunit.log 2>&1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed build/test-results/testUnit
rm -rf /tmp/gsd48/exec/xml-move-testunit && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-move-testunit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-move-testunit /tmp/gsd48/exec/status-move-testunit.tsv
: > /tmp/gsd48/exec/empty.tsv
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py delta /tmp/gsd48/exec/status-w2-testunit.tsv /tmp/gsd48/exec/status-move-testunit.tsv /tmp/gsd48/exec/empty.tsv
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
git diff --name-only -- src/main
/tmp/gsd48/exec/fmt.sh <dn|dnf|dnt|ntm|sat> <file>   # per-file spotlessApply in a throwaway worktree, cmp
git add <the five files>
git commit -F /tmp/gsd48/exec/msg-move.txt
```

```text
red_compile_exit: 1
red_compile_errors: 8
red_error_files: DeclaredNullabilityTest.java=6 NullableTupleMatrixLiveTest.java=1 SimpleAggregateTupleRoundTripLiveTest.java=1
red_main_numstat: 0 3
fixtures_constant_lines: 1
old_refs_left: 0
new_refs: 8
main_constant_count: 0
ntm_dn_import_kept: yes
sat_dn_import_kept: yes
green_compile_exit: 0
neighbours_failed: 0
dn_green_tests: 69
planning_guard_green_tests: 18
move_testunit_docker_host: unix:///nonexistent.sock
move_testunit_exit: 0
move_testunit_tests: 2320
move_testunit_failures: 0
move_testunit_errors: 0
move_testunit_skipped: 2
status_delta_move: delta: removed=0 added=0 expected=0 match=yes
normalised_equal: yes
anchors_mirror: rows=218 bad=0
main_diff_files: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
fmt_dn: identical
fmt_dnf: identical
fmt_dnt: identical
fmt_ntm: identical
fmt_sat: identical
jvm_crash: no
move_commit: c4ec0cbf25462cd3e8d67d567559a9f8ad85c45a
```

Notes. The red was taken with only the three production lines deleted and no test edited: `compileTestJava` exited 1 with `8 errors` (javac's own count; Gradle repeats the first error in its failure summary, which is not a ninth error), 6 in `DeclaredNullabilityTest` (lines 212 twice, 214, 330, 374, 1017), 1 in `NullableTupleMatrixLiveTest` (762) and 1 in `SimpleAggregateTupleRoundTripLiveTest` (117), exactly the plan's expectation. The move then added the constant (`"\\n"` in source, the characters backslash and n) once to `DeclaredNullabilityFixtures` with its one-line Javadoc, replaced the class Javadoc sentence that forbade a copy with one sentence saying `{@link #ESCAPED_NEWLINE}` lives there because production code no longer reads it, and repointed the 8 references. Both live tests keep their `DeclaredNullability` import because the class is still used elsewhere in each file (`normalise`, `LogCapture.attach`), and gain the `DeclaredNullabilityFixtures` import in sorted place. `normalised_equal` is `yes`: against `HEAD` before the commit, each of the three repointed test files equals its parent once `DeclaredNullability.ESCAPED_NEWLINE` is read as `DeclaredNullabilityFixtures.ESCAPED_NEWLINE` and import lines are set aside, and the production file loses exactly the three lines (numstat `0 3`, the class Javadoc paragraph untouched). The filtered run (`*DeclaredNullability*` and `*SourcePlanningIdGuardUnitTest*`, Docker unreachable) read 89 tests, 0 failures: `DeclaredNullabilityTest` 69 and `SourcePlanningIdGuardUnitTest` 18, E-01's counts. The cache-busted testUnit read 2320 tests (E-09's count), 0 failures, 0 errors, 2 skips, 107 XML files, and its per-testcase delta against E-09 removed and added nothing. No JVM crash and no core dump; the default toolchain ran every build. The constant sits at the top of `DeclaredNullabilityFixtures` with no blank line added after it, because the plan's own normalisation rule for that file sets aside only comment lines and the constant line, so an added blank line would read as a changed non-comment line; the first local attempt added one, failed that check, and was redone before anything was pushed or recorded elsewhere (the gate runs below are from the redone tree). The gate was taken on the working tree before the commit; the commit holds exactly those five files (`move_commit`), and the anchor row `declaredType.replace(ESCAPED_NEWLINE, SPACE)  0  COLS-13` is a zero row that stays true, so the mirror still reads 218 rows.

Recorded 2026-11-06T08:48:52Z.

Verdict: pass

### E-11: Tail gate and one full suite on latest x V2 after the 28 IN-01 move

Backs: P37-01 (28 IN-01), ROADMAP Phase 48 criterion 1 and the phase gate ("a full suite on `latest` x V2 if `src/main` changed"), U-4 (one full suite after the move), D-3, the anchors invariant.

An earlier attempt at this suite failed and is recorded here on purpose. It overlapped the tail gate, which was running in the background at the same time, and it ended with 2846 tests, 8 failures and 0 errors: eight `#initializationError` testcases (`ClickHouseSinkTaskSchemalessTest`, `ClickHouseWriterTest`, `Client011BehaviourPinLiveTest`, `EmptyCdcSkipRouteLiveTest`, `NetworkCompressionPinLiveTest`, `NullableTupleMatrixLiveTest`, `RowBinaryClientParityLiveTest`, `WideIntegerTypeGateLiveTest`), each `IllegalStateException: Could not connect to Ryuk at localhost:36898` thrown from `ClickHouseBase.setup` before any test body ran. No assertion failed in that attempt. Its log is `/tmp/gsd48/exec/suite-move.log` (not overwritten) and its XML is kept at `/tmp/gsd48/exec/xml-move-suite-attempt1`. The cause is the likely, not proven, Ryuk start-up contention with the concurrent tail gate; the baseline suite log of E-01 has no such line. The numbers below are from the second run, taken alone after the tail gate had finished, with Docker and colima `vz` checked healthy and no Testcontainers or Ryuk container left over. That the first attempt was red is not hidden by the second.

```sh
git rev-parse HEAD
git diff --quiet c4ec0cbf25462cd3e8d67d567559a9f8ad85c45a e0c0c9d8baef011c6dedde43270a380523ced7bf -- src
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 e0c0c9d8baef011c6dedde43270a380523ced7bf > /tmp/gsd48/exec/tail-gate-move.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd48/exec/suite-move.log 2>&1   # attempt 1, overlapped the tail gate, exit 1, 8 initializationError (Ryuk)
docker ps -a; colima list; ps aux | grep -E 'Gradle Test Executor|verify-merge'   # health and quiet check before attempt 2
cp -R build/test-results/test /tmp/gsd48/exec/xml-move-suite-attempt1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd48/exec/suite-move-rerun.log 2>&1   # attempt 2, alone
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed build/test-results/test
rm -rf /tmp/gsd48/exec/xml-move-suite && cp -R build/test-results/test /tmp/gsd48/exec/xml-move-suite
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-move-suite /tmp/gsd48/exec/status-move-suite.tsv
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py delta /tmp/gsd48/exec/status-base-suite.tsv /tmp/gsd48/exec/status-move-suite.tsv .planning/phases/48-phase-37-dispositions-document-fixes/48-expected-added.tsv --by-class
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit /tmp/gsd48/exec/xml-move-suite --class .NullableTupleMatrixLiveTest   # and .SimpleAggregateTupleRoundTripLiveTest, .DeclaredNullabilityTest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
move_gate_head: e0c0c9d8baef011c6dedde43270a380523ced7bf
src_unchanged_since_move: yes
tail_gate_line: invariants: provenance_paths=11 anchor_commits=149 anchor_rows=218 marker_commits=167 anchor_exempt=0 result=PASS
tail_gate_exit: 0
attempt1_log: /tmp/gsd48/exec/suite-move.log
attempt1_overlapped_tail_gate: yes
attempt1_exit: 1
attempt1_tests: 2846
attempt1_failures: 8
attempt1_errors: 0
attempt1_failing_testcases: 8 x initializationError (Could not connect to Ryuk at localhost:36898)
attempt1_assertion_failures: 0
attempt2_log: /tmp/gsd48/exec/suite-move-rerun.log
attempt2_ran_alone: yes
suite_exit: 0
suite_tests: 2923
suite_failures: 0
suite_errors: 0
suite_skipped: 18
suite_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
status_delta_suite_by_class: delta: removed=0 added=25 expected=25 match=yes
ntm_suite_tests: 33
sat_suite_tests: 2
dn_suite_tests: 69
suite_version: 26.10.3.7
suite_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
suite_version_equals_base: yes
suite_java: default toolchain
suite_crash: no
```

Notes. Attempt 2 read 2923 tests (E-01's 2898 plus the 25 rows of `48-expected-added.tsv`), 0 failures, 0 errors, the 18 named skips (`named=7 state_mismatch_cases=11 unexpected=0`), 156 XML files, exit 0, in 3 min 23 s. The by-class delta against the E-01 dump removes nothing and adds exactly the 25 expected rows. The two live readers of the moved constant ran and passed at E-01's counts (`NullableTupleMatrixLiveTest` 33, `SimpleAggregateTupleRoundTripLiveTest` 2), as did `DeclaredNullabilityTest` (69). `latest` did not drift: the server version and digest equal E-01's. The tail gate ran over `12e2cf17..e0c0c9d8` on the explicit SHA (the E-10 evidence commit, with `src` identical to the move commit), exit 0, `anchor_rows=218`, `anchor_exempt=0`, `result=PASS`. No JVM crash, no core dump and no `-PjavaVersion=21` run.

Recorded 2026-11-06T09:06:16Z.

Verdict: pass

### E-12: The published notes: gg6 row D7, row C2 and the notice; gg1 notice and item 6 (DOC-01, 44 IN-03, 43 IN-02)

Backs: DOC-01 (the published gg6 note names no planning identifier), U-6 and U-7 (44 IN-03: row D7 says `since v1.7.0_gg7`, row C2 and the notice point to FORK-DELTA's decided limits), U-6 (43 IN-02: the gg1 note says which tree item 6 was measured on), P37-04, ROADMAP Phase 48 criterion 4.

```sh
git show v1.7.0_gg7:src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java | grep -c customKeysAreForwardedWithDoubledEncodingClientVersionTripwire
git show v1.7.0_gg6:src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java | grep -c customKeysAreForwardedWithDoubledEncodingClientVersionTripwire   # file counted 0 if missing
grep -c customKeysAreForwardedWithDoubledEncodingClientVersionTripwire src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
grep '^\*\*17\. ' docs/FORK-DELTA.md | grep -cF '(recorded limit, by decision)'
grep '^\*\*24\. ' docs/FORK-DELTA.md | grep -cF 'FUT-05 is closed by decision'
grep '^| D13 |' docs/ROLLOUT-v1.7.0_gg6.md | grep -c Client011BehaviourPinLiveTest
python3 -I /tmp/gsd48/exec/applykey.py .planning/phases/48-phase-37-dispositions-document-fixes/48-doc-edits.json rollout_gg6 docs/ROLLOUT-v1.7.0_gg6.md
git add docs/ROLLOUT-v1.7.0_gg6.md && git commit -F /tmp/gsd48/exec/msg-g6.txt
python3 -I /tmp/gsd48/exec/applykey.py .planning/phases/48-phase-37-dispositions-document-fixes/48-doc-edits.json rollout_gg1 docs/ROLLOUT-v1.7.0_gg1.md
git add docs/ROLLOUT-v1.7.0_gg1.md && git commit -F /tmp/gsd48/exec/msg-g1.txt
git diff --numstat <commit>^ <commit>
python3 -I <replay of each key against the commit's parent, old exactly once, byte equality>
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py <gg6_commit> docs/ROLLOUT-v1.7.0_gg6.md
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py <gg1_commit> docs/ROLLOUT-v1.7.0_gg1.md
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py <commit> <note>
diff <(git show v1.7.0_gg6:docs/ROLLOUT-v1.7.0_gg6.md) <(git show <gg6_commit>:docs/ROLLOUT-v1.7.0_gg6.md) | grep -E '^[0-9]' | paste -sd ' ' -
grep -cF 'so only transient codes belong in the list' docs/ROLLOUT-v1.7.0_gg6.md
grep -cF 'No instruction, label or measured value changed.' docs/ROLLOUT-v1.7.0_gg6.md
```

```text
tripwire_in_gg7_tag: 1
tripwire_in_gg6_tag: 0
tripwire_on_tip: 1
fork_delta_item17_by_decision: yes
fork_delta_item24_fut05_closed: yes
gg6_d13_measured: yes
gg6_commit: 5453a69c2aa276ef6a4dd6bc3e4389407bb9e9c4
gg6_edit: exact
gg6_numstat: 3 3
gg6_ids_line: ids: hits=0
gg6_tag_hunks: 2a3,4 112c114 146c148
gg6_instruction_kept: yes
gg6_paths_line: paths: candidates=6 resolved=6 abbreviated=0 abbreviated_unique=0 placeholders=0 by_design=0 unresolved=0
gg1_commit: eadacf83dbc07a908eb3976bf5cf3decd4c79a95
gg1_edit: exact
gg1_numstat: 2 2
gg1_ids_line: ids: hits=26
gg1_paths_line: paths: candidates=1 resolved=1 abbreviated=0 abbreviated_unique=0 placeholders=0 by_design=0 unresolved=0
```

Notes. All six read checks held before the first edit, and every `old` string of keys `rollout_gg6` and `rollout_gg1` occurred exactly once in the working-tree file. The tripwire test exists in the `v1.7.0_gg7` tag and not in the `v1.7.0_gg6` tag, so `since v1.7.0_gg7` is true; item 17 of FORK-DELTA carries `(recorded limit, by decision)` and item 24 carries `FUT-05 is closed by decision`, which is what the two replacement sentences in row C2 and the notice rest on; row D13 of the gg6 note is labelled `measured` and names `Client011BehaviourPinLiveTest`, the source of the gg1 item 6 measurement. Both commits were applied by a script that asserts each `old` occurs once and replaces it with the JSON `new` text unchanged, and each was replayed against its parent afterwards. The gg6 note still differs from its `v1.7.0_gg6` tag copy in exactly the three hunks, and row C2's instruction and the notice's rule are each present once. The gg1 identifier count stays 26 (its behaviour-ledger references predate this phase and are not part of DOC-01). The gg6 note and the gg1 note go into Phase 49's independent review.

Recorded 2026-11-06T09:18:58Z.

Verdict: pass

### E-13: FORK-DELTA: evidence paths, KeyToValueTest row, item 8, item 20, row 94 and the double-ERROR sentence (DOC-02, 43 IN-01, 45 IN-04, 46 WR-05)

Backs: DOC-02 (every FORK-DELTA evidence path resolves on the tip), U-10 (the removed `KeyToValueTest` path stays named beside the surviving package path), U-11 (item 20 names its ledger by file and by a `git ls-files` lookup), D-2, U-8 and the Infos 43 IN-01, 45 IN-04, 46 WR-05, P37-04, ROADMAP Phase 48 criterion 4.

```sh
git ls-files --error-unmatch .planning/milestones/v1.7.0_gg4-hardening-phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md .planning/milestones/v1.7.0_gg6-maintenance-phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md .planning/milestones/v1.7.0_gg6-maintenance-REQUIREMENTS.md
git ls-files --error-unmatch src/test/java/com/clickhouse/kafka/connect/transforms/KeyToValueTest.java
git ls-files src/test/java/transforms/KeyToValueTest.java | wc -l
grep -c '^- \*\*FUT-0' .planning/milestones/v1.7.0_gg6-maintenance-REQUIREMENTS.md
sed -n 100,125p src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
grep -cF 'Error writing value of {} to the column' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
grep -cF 'Record serialization failed: topic [{}] partition [{}] offset [{}] column' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
sed -n 2253,2258p src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
git show --name-status --pretty=format: c4ec0cbf25462cd3e8d67d567559a9f8ad85c45a
git show --name-status --pretty=format: 4de705a6e0132bb884796cd58635fe40be724464
python3 -I /tmp/gsd48/exec/applykey.py .planning/phases/48-phase-37-dispositions-document-fixes/48-doc-edits.json fork_delta_doc02 docs/FORK-DELTA.md
git add docs/FORK-DELTA.md && git commit -F /tmp/gsd48/exec/msg-fd1.txt
python3 -I /tmp/gsd48/exec/applykey.py .planning/phases/48-phase-37-dispositions-document-fixes/48-doc-edits.json fork_delta_reviews docs/FORK-DELTA.md
git add docs/FORK-DELTA.md && git commit -F /tmp/gsd48/exec/msg-fd2.txt
git diff --numstat <commit>^ <commit>
python3 -I <replay of each key against the commit's parent, old exactly once, byte equality>
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py HEAD docs/FORK-DELTA.md
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py HEAD docs/FORK-DELTA.md --list | grep -c '^BY-DESIGN '
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py HEAD <CHANGELOG.md | docs/ROLLOUT-v1.7.0_gg6.md | docs/ROLLOUT-v1.7.0_gg1.md | docs/ROLLOUT-v1.7.0_gg7.md>
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py HEAD docs/ROLLOUT-v1.7.0_gg6.md
grep -cF "git ls-files '*48-DISPOSITION-LEDGER.md'" docs/FORK-DELTA.md
python3 -I <the "## The raw numbers" section of docs/FORK-DELTA.md at B0 against the working tree>
git show v1.7.0_gg6:.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt | cmp -s - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt   # and the gg7 list against v1.7.0_gg7
git diff --quiet c77d1a147ae4acd37100b73db08f1851047bc055 HEAD -- .planning/scripts
grep -oE '\.planning/phases/4[5-7]-' docs/FORK-DELTA.md | wc -l
```

```text
milestone_targets_tracked: yes
keytovalue_package_tracked: yes
keytovalue_removed_path_absent: yes
gg6_requirements_defines_fut: 5
chain_order: yes
double_error_formats: yes
deadline_throw_read: yes
item20_facts: yes
fd_doc02_commit: 8f187c9d4acbe891b2338b92273536a9b07e7a81
fd_doc02_edit: exact
fd_doc02_numstat: 5 5
fd_reviews_commit: cc2ca206f79b3347af74027d5d4b490bd02cb860
fd_reviews_edit: exact
fd_reviews_numstat: 2 2
fd_paths_line: paths: candidates=98 resolved=70 abbreviated=20 abbreviated_unique=20 placeholders=2 by_design=6 unresolved=0
fd_by_design_listed: 6
other_docs_paths_unchanged: yes
gg6_ids_line: ids: hits=0
ledger_lookup_named: 1
raw_numbers_unchanged: yes
lists_identical_to_tags: yes
scripts_dir_unchanged: yes
phase_evidence_paths_left: 12
```

Notes. All eight read checks held before the first edit, and every `old` string of keys `fork_delta_doc02` and `fork_delta_reviews` occurred exactly once in the working-tree file. The `CHAIN` list of `ReleaseWorkflowGuardUnitTest` holds `COMPARE_STEP`, `TESTS_GATE_STEP`, `TAG_GATE_STEP` and `PUBLISH_STEP` in that order (with `UPLOAD_STEP` between the first two), the two RowBinary log formats occur once each in `ClickHouseWriter`, and `throwIfFired` throws a `RetriableException` once the deadline fired. For item 20, the 28 IN-01 move commit `c4ec0cbf` touches `DeclaredNullability.java` and the null-binding commit `4de705a6` adds `DescribeUnreadableEnumNullBindingUnitTest.java`. The resolver read FORK-DELTA at the base as 99/69/20/20/2/6/2 (E-02) and now reads 98/70/20/20/2/6/0 with exit 0: the 25 and 35 evidence paths resolve under `.planning/milestones/`, and the removed `src/test/java/transforms/KeyToValueTest.java` stays named (one by-design line of the six) beside the surviving package path. The 12 `.planning/phases/45..47` path tokens resolve now and move at the milestone close (D-2). The commits were applied by a script that asserts each `old` occurs once and substitutes the JSON `new` text unchanged, and each was replayed against its parent afterwards. FORK-DELTA goes into Phase 49's independent review, and the `.planning/phases/45..47` paths are handed to the milestone close.

Recorded 2026-11-06T09:20:05Z.

Verdict: pass

### E-14: Dry runs per D-1: the published pair with the gg7 list, and the Phase 48 tip with a 12-row exact draft

Backs: P37-03 (UF-37-3, D-1, U-12: the allow-list dry run reads 0 unallowed and 0 stale rows in its two forms), ROADMAP Phase 48 criterion 3.

```sh
git rev-parse HEAD
rm -rf /tmp/gsd48/dry && mkdir -p /tmp/gsd48/dry/gg6 /tmp/gsd48/dry/gg7 /tmp/gsd48/dry/tip
git archive v1.7.0_gg6 | tar -x -C /tmp/gsd48/dry/gg6
git archive v1.7.0_gg7 | tar -x -C /tmp/gsd48/dry/gg7
git archive f198c812dc99cf4c33340a98102b8b83df3999d0 | tar -x -C /tmp/gsd48/dry/tip
/bin/bash -c 'cd /tmp/gsd48/dry/<n> && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -PjavaVersion=21 --console=plain createConfluentArchive'   # n = gg6, gg7, tip, in turn
shasum -a 256 /tmp/gsd48/dry/gg6/build/confluent/clickhouse-kafka-connect-v1.7.0_gg6.zip /tmp/gsd48/dry/gg7/build/confluent/clickhouse-kafka-connect-v1.7.0_gg7.zip /tmp/gsd48/dry/tip/build/confluent/clickhouse-kafka-connect-v1.7.0_gg7.zip
python3 -I .github/scripts/compare_confluent_archives.py <gg6 zip> <gg7 zip> .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt   # published pair
python3 -I .github/scripts/compare_confluent_archives.py <gg7 zip> <tip zip> /tmp/gsd48/exec/dry-comment-only.txt                                 # tip, comment-only list
python3 -I .github/scripts/compare_confluent_archives.py <gg7 zip> <tip zip> /tmp/gsd48/exec/dry-draft-gg8.txt                                    # tip, 12-row draft
python3 -I /tmp/gsd48/exec/<probe>   # 14-row probe list from the comment-only run's UNALLOWED lines, run the same way
javap -p -c and javap -p -l on DeclaredNullability.class, DeclaredNullability$1.class, DeclaredNullability$Assignment.class of the gg7 and the tip connector jar
git status --porcelain .planning/scripts
git show v1.7.0_gg6:.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt | cmp -s - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt   # and the gg7 list against v1.7.0_gg7
```

```text
dry_tip_head: f198c812dc99cf4c33340a98102b8b83df3999d0
dry_builds_exit: 0 0 0
gg6_zip_sha256: 97a5881294f69ffb9957407b6addda8c152aa857082c0f56d8809f88989c4abf
gg7_zip_sha256: 5ab9da9e3e258db03db39aada56f604bad35711bd5573cbf5798081960827f1b
tip_zip_sha256: b778327a8e8136527dc97d7bf5f544d5bd39aea2dea870416f5adf82e7618a7f
published_exit: 0
published_result: RESULT: PASS
published_unallowed: 0
published_stale: 0
published_totals: totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393
tip_comment_only_exit: 1
tip_comment_only_unallowed: 14
tip_comment_only_stale: 0
tip_comment_only_totals: totals files-a 7512 files-b 7515 only-a 0 only-b 3 changed 11 same 7501
tip_unallowed_paths_match: no
tip_extra_paths: com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$1.class; com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$Assignment.class
tip_extra_paths_kind: CHANGED
draft_rows: 12
draft_wildcard_rows: 0
draft_exit: 1
draft_result: RESULT: FAIL
draft_unallowed: 2
draft_stale: 0
draft_totals: totals files-a 7512 files-b 7515 only-a 0 only-b 3 changed 11 same 7501
draft_in_tmp_only: yes
lists_identical_to_tags: yes
javap_dn_bytecode_identical: no
javap_dn1_bytecode_identical: yes
javap_dn1_line_table_identical: no
javap_dnassignment_bytecode_identical: yes
javap_dnassignment_line_table_identical: no
probe_draft_rows: 14
probe_draft_wildcard_rows: 0
probe_exit: 0
probe_result: RESULT: PASS
probe_unallowed: 0
probe_stale: 0
probe_totals: totals files-a 7512 files-b 7515 only-a 0 only-b 3 changed 11 same 7501
```

Notes. The published pair reads exactly as the plan expects (exit 0, `RESULT: PASS`, 0 `UNALLOWED`, 0 `STALE-ROW`, totals 7513/7512/1/0/119/7393). The tip against the gg7 archive does not: the comment-only list reads 14 `UNALLOWED` lines, not the planned 12, and the 12-row draft of the plan reads `RESULT: FAIL` with 2 `UNALLOWED`, so the plan's expectation that the Phase 48 tip changes exactly one more archive class than the research measured (`DeclaredNullability.class`) failed. The two extra paths are `DeclaredNullability$1.class` (the synthetic switch-map class) and `DeclaredNullability$Assignment.class`, both `CHANGED`. Their bytecode is identical to the gg7 archive's (`javap -p -c` equal); only the `LineNumberTable` differs, because the 28 IN-01 move deleted three lines above the code of `DeclaredNullability.java` and every class compiled from that file records the shifted line numbers. `DeclaredNullability.class` itself differs in bytecode (the removed public field) and in its line table. The three `ONLY-B` classes are the planned ones (`ClickHouseWriter$3`, `$InsertBody`, `$InsertWriteDeadline`); the other eleven are `CHANGED`. The probe list (14 exact rows, the 12 of the plan plus the two inner classes of `DeclaredNullability`, 0 wildcard rows, kept under `/tmp/gsd48/exec`, not a planned run) reads `RESULT: PASS`, 0 `UNALLOWED`, 0 `STALE-ROW`, exit 0, so the corrected hand-off for the gg8 allow-list is 14 exact rows. The tip against the gg7 list fails by design until Phase 49 writes the gg8 list and was not run. `.planning/scripts` holds the 5 lists, unchanged, and the gg6 and gg7 lists equal their tags; no draft or probe list was written under the repository. Nothing in the repository changed except this evidence file. Per the plan, a result other than the table's is a finding: the plan stops here for a decision on the 12 versus 14 rows (the plan text, the E-14 expectations, the T1 verify, the E-17 hand-off row and the SUMMARY would then name 14 rows, totals `changed 11 same 7501`).

Recorded 2026-11-06T09:37:20Z.

Verdict: fail

### E-15: Dry runs per D-1 re-measured: the published pair with the gg7 list, and the Phase 48 tip with a 14-row exact draft (supersedes the miss of E-14)

Backs: P37-03 (UF-37-3, D-1, U-12: the allow-list dry run reads 0 unallowed and 0 stale rows in its two forms), ROADMAP Phase 48 criterion 3. This entry supersedes E-14, the first measurement, which expected 12 exact rows, read 14 `UNALLOWED` and a failing 12-row draft, and stays committed and unedited with its fail verdict.

```sh
git rev-parse HEAD
git diff --quiet f198c812dc99cf4c33340a98102b8b83df3999d0 <dry_tip_head> -- src build.gradle.kts settings.gradle.kts gradle .github
rm -rf /tmp/gsd48/dry && mkdir -p /tmp/gsd48/dry/gg6 /tmp/gsd48/dry/gg7 /tmp/gsd48/dry/tip
git archive v1.7.0_gg6 | tar -x -C /tmp/gsd48/dry/gg6
git archive v1.7.0_gg7 | tar -x -C /tmp/gsd48/dry/gg7
git archive <dry_tip_head> | tar -x -C /tmp/gsd48/dry/tip
/bin/bash -c 'cd /tmp/gsd48/dry/<n> && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -PjavaVersion=21 --console=plain createConfluentArchive'   # n = gg6, gg7, tip, in turn
shasum -a 256 <the three zips>
python3 -I .github/scripts/compare_confluent_archives.py <gg6 zip> <gg7 zip> .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt   # published pair -> dry2-published.txt
python3 -I .github/scripts/compare_confluent_archives.py <gg7 zip> <tip zip> /tmp/gsd48/exec/dry-comment-only.txt                                 # comment-only list -> dry2-tip-comment-only.txt
python3 -I .github/scripts/compare_confluent_archives.py <gg7 zip> <tip zip> /tmp/gsd48/exec/dry-draft-gg8-14.txt                                 # 14-row draft -> dry2-draft.txt
git status --porcelain .planning/scripts
git show v1.7.0_gg6:.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt | cmp -s - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt   # and the gg7 list against v1.7.0_gg7
```

```text
supersedes_entry: E-14
dry_tip_head: 62aa723168700162e3d583a5fe9a63b5554dac94
src_unchanged_since_e14: yes
dry_builds_exit: 0 0 0
gg6_zip_sha256: 97a5881294f69ffb9957407b6addda8c152aa857082c0f56d8809f88989c4abf
gg7_zip_sha256: 5ab9da9e3e258db03db39aada56f604bad35711bd5573cbf5798081960827f1b
tip_zip_sha256: b778327a8e8136527dc97d7bf5f544d5bd39aea2dea870416f5adf82e7618a7f
published_exit: 0
published_result: RESULT: PASS
published_unallowed: 0
published_stale: 0
published_totals: totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393
tip_comment_only_exit: 1
tip_comment_only_unallowed: 14
tip_comment_only_stale: 0
tip_unallowed_paths_match: yes
draft_rows: 14
draft_wildcard_rows: 0
draft_exit: 0
draft_result: RESULT: PASS
draft_unallowed: 0
draft_stale: 0
draft_totals: totals files-a 7512 files-b 7515 only-a 0 only-b 3 changed 11 same 7501
draft_in_tmp_only: yes
lists_identical_to_tags: yes
```

Notes. E-14 is the superseded miss: its comment-only run read 14 `UNALLOWED` lines where the plan expected 12, and its 12-row draft read `RESULT: FAIL` with 2 `UNALLOWED`. The two paths beyond the planned 12 are `com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$1.class` and `com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$Assignment.class`; E-14 shows by `javap -p -c` that their bytecode equals the gg7 archive's and by `javap -p -l` that only the line number table differs, because the 28 IN-01 move deleted three lines above their code. This entry reran the dry runs with the plan amended to 14 exact rows (the plan's commits `50ff0c23` and `62aa7231`): the three archives were rebuilt from the tip at `dry_tip_head`, whose `src`, build files and `.github` equal the E-14 tip's (`src_unchanged_since_e14`), and the three zips have the same SHA-256 values as the E-14 builds. The published pair reads as planned. The comment-only run reads the same 14 `UNALLOWED` lines as E-14 (the output files of the two runs are byte-equal): the 12 paths of the first plan, of which `ClickHouseWriter$3`, `$InsertBody` and `$InsertWriteDeadline` are `ONLY-B` and the other nine `CHANGED`, plus the two inner classes, `CHANGED`. The draft of 14 exact rows, each with a reason and none with `*`, `?` or `[`, kept only under `/tmp/gsd48/exec`, reads exit 0, `RESULT: PASS`, 0 `UNALLOWED`, 0 `STALE-ROW`, so the gg8 allow-list that Phase 49 generates needs those 14 exact rows and no wildcard. The tip against the gg7 list fails by design until Phase 49 writes the gg8 list and was not run. `.planning/scripts` holds the 5 lists, unchanged, and the gg6 and gg7 lists equal their tags; no draft was written under the repository.

Recorded 2026-11-06T09:43:23Z.

Verdict: pass

### E-16: Disposition ledger: 57 rows, 10 fixed, 47 closed, no owner-user row

Backs: P37-01 (the four `src/main` rows and the null assertion carry their commits or measured costs), P37-02 (28 IN-05(b) items), P37-03 (UF-37-3), P37-04 (every review Info has a commit pointer or a named disposition, none "owner: user" alone), ROADMAP Phase 48 criteria 1 to 3 and the ledger half of criterion 4 (item 20's lookup).

```sh
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-ledger-check.py check .planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-ledger-check.py counts .planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-ledger-check.py pointers .planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md   # 14 lines
git cat-file -e <sha>^{commit}; git diff-tree --no-commit-id --name-only -r <sha>; git merge-base --is-ancestor <B0> <sha>   # per pointer line; existing pointers: --is-ancestor <sha> HEAD
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py get <E> <entry> <key>   # each fixed pointer against its evidence key
python3 -I -c '<count of distinct U-1..U-11 in the ledger>'
grep -qF UF-37-5 (and UF-37-6, UF-37-7, UF-37-8, UF-36-4) the ledger
grep -F '| 26-IN-02 |' the ledger | grep -cF '15 compile errors'; grep -F '| 27-WR-01 |' the ledger | grep -cF '5 Docker-free tests'
git add .planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md
git commit -F /tmp/gsd48/exec/msg-ledger.txt
git ls-files '*48-DISPOSITION-LEDGER.md'
```

```text
ledger_check_line: check: rows=57 required=57 problems=0
ledger_counts: rows=57 fixed=10 closed=47 pointers=14
pointers_resolve: yes
pointers_touch_paths: yes
phase48_pointers_after_base: yes
fixed_pointers_match_evidence: yes
decision_ids_named: 11
not_review_rows_listed: yes
measured_costs_carried: yes
ledger_commit: 5ae549b915892b6d3c48b0b0bc1c650aed1d1697
ledger_lookup: .planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md
```

Notes. The ledger commit holds only the ledger file. Its 57 rows are 27 rows for the 26 Phase 37 lineage IDs (28 IN-05(b) split into items i and ii), 18 rows for the 17 Phase 39 to 44 IDs (42 IN-03 split into parts a and b) and 12 rows for the 12 Phase 45 to 47 IDs; 10 are `fixed` and 47 `closed`. The 14 commit pointers are the 10 fixed rows (28-IN-01 `c4ec0cbf`, 28-IN-05(b)(i) `4de705a6`, UF-37-3 `07e65662`, 40-IN-03 `69b75cb3`, 42-IN-03(b) `64ef7568`, 42-IN-04 `3182791f`, 43-IN-01 `cc2ca206`, 43-IN-02 `eadacf83`, 44-IN-03 `5453a69c`, 47-IN-01 `ae31c4a9`) and 4 closed rows whose reasons rest on a commit (27-IN-01 `f13d8378`, 43-WR-03 `e979004e`, 45-IN-04 and 46-WR-05 `cc2ca206`). Each of the 10 fixed pointers equals its evidence key and touches its file; 45-IN-04 and 46-WR-05 equal `fd_reviews_commit` of E-13; the two earlier pointers are ancestors of HEAD. Before writing the closed reasons that cite a line, a count or a file, each was re-read on the tree: `shouldSkipDescribeRow` lines 508, 509 and 516 with no caller under `src/main` and its anchor row (3, HLP-02), the parity assertion at `DescribeTableBuilderUnitTest` line 399, `Column.convertEnumValues` line 630, the gg4 note line 199 and CHANGELOG line 284, 12 enum files in the raw-name matrix, `Enum8` at `RawNameMatrix.java` line 97 and in `ENUM_LOOKUP_MISS_TEXT`, the release and pre-release workflow regular expression at line 65 against the script grammar, `UNSAFE_CHARS` and `load_inner`, the `pipefail` comment at line 108, the compare step's non-zero exit, no `distributionSha256Sum`, the `kafka` catalog version unchanged since the gg7 tag, no file added under `src/main` since the gg7 tag, `nested jars in b: 0` and `kafka classes in b: 0` in the E-15 compare report, and the retry-outcome body of `JacksonOverIoValueFreeUnitTest.keepsTheRetryOutcome`. One research detail was not copied because it did not read the same on the tree: no row of `fork-anchors.txt` names `JsonAsStringKeyResidualLiveTest`, so the 27-IN-02 reason cites only the four FORK-DELTA lines that name it; the count of `Shape` rows was not re-counted and is not cited. The hand-off section of the ledger is a numbered list and not a table, because `48-ledger-check.py` reads every line that starts with `| ` as a six-cell ledger row. `git ls-files '*48-DISPOSITION-LEDGER.md'` prints exactly the ledger, so FORK-DELTA item 20's lookup resolves.

Recorded 2026-11-06T09:48:43Z.

Verdict: pass

### E-17: Phase gate: testUnit with Docker unreachable, full suite on latest x V2, deltas, scope, documents, ledger, anchors, tail gate, nothing pushed

Backs: P37-01, P37-02, P37-03, P37-04, DOC-01, DOC-02, ROADMAP Phase 48 phase gate ("cache-busted `testUnit`, the anchors invariant, and a full suite on `latest` x V2 if `src/main` changed") and criteria 1 to 4 on the phase tip.

```sh
git rev-parse HEAD
gh api repos/ne1r0n/clickhouse-kafka-connect/git/matching-refs/tags/pre-v1.7.0_gg8 --jq length
git ls-remote origin refs/heads/sync/v1.7.0
git ls-remote upstream main
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd48/exec/gate-testunit.log 2>&1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed build/test-results/testUnit
rm -rf /tmp/gsd48/exec/xml-gate-testunit && cp -R build/test-results/testUnit /tmp/gsd48/exec/xml-gate-testunit
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-gate-testunit /tmp/gsd48/exec/status-gate-testunit.tsv
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py delta /tmp/gsd48/exec/status-base-testunit.tsv /tmp/gsd48/exec/status-gate-testunit.tsv .planning/phases/48-phase-37-dispositions-document-fixes/48-expected-added.tsv
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd48/exec/gate-suite.log 2>&1
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py failed build/test-results/test
rm -rf /tmp/gsd48/exec/xml-gate-suite && cp -R build/test-results/test /tmp/gsd48/exec/xml-gate-suite
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py dump /tmp/gsd48/exec/xml-gate-suite /tmp/gsd48/exec/status-gate-suite.tsv
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-testcase-status.py delta /tmp/gsd48/exec/status-base-suite.tsv /tmp/gsd48/exec/status-gate-suite.tsv .planning/phases/48-phase-37-dispositions-document-fixes/48-expected-added.tsv --by-class
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
git diff --name-only c77d1a147ae4acd37100b73db08f1851047bc055 <gate_head> -- src/main
git diff --numstat c77d1a147ae4acd37100b73db08f1851047bc055 <gate_head> -- src/main
git diff --name-only --diff-filter=A c77d1a147ae4acd37100b73db08f1851047bc055 <gate_head> -- src
git diff --quiet c77d1a147ae4acd37100b73db08f1851047bc055 <gate_head> -- <the eleven high-risk and neighbouring production files>
git diff --name-only c77d1a147ae4acd37100b73db08f1851047bc055 <gate_head> -- src/test src/testFixtures src/integrationTest
git diff --numstat c77d1a147ae4acd37100b73db08f1851047bc055 <gate_head> -- <the four extended test files>
git diff --quiet c77d1a147ae4acd37100b73db08f1851047bc055 <gate_head> -- build.gradle.kts settings.gradle.kts gradle .github VERSION CHANGELOG.md .planning/scripts
git diff --name-only c77d1a147ae4acd37100b73db08f1851047bc055 <gate_head> -- docs
git rev-list --merges c77d1a147ae4acd37100b73db08f1851047bc055..<gate_head>
md5 -q <the four plan-time helpers and the two frozen scripts>
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py <gate_head> docs/FORK-DELTA.md
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py <gate_head> docs/ROLLOUT-v1.7.0_gg6.md
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-ledger-check.py check .planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md
git ls-files '*48-DISPOSITION-LEDGER.md'
python3 -I .planning/phases/48-phase-37-dispositions-document-fixes/48-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 <gate_head> > /tmp/gsd48/exec/gate-tail.log 2>&1
```

```text
gate_head: 97b60d3a59c10c72870761ac342d8278701fdbc5
origin_pre_tag_at_gate: absent
origin_sync_branch_at_gate: absent
upstream_main_at_gate: 83808df9a0993cf49b2ceaa4633c782e550c0180
testunit_docker_host: unix:///nonexistent.sock
testunit_exit: 0
testunit_tests: 2320
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
status_delta_testunit: delta: removed=0 added=25 expected=25 match=yes
suite_exit: 0
suite_tests: 2923
suite_failures: 0
suite_errors: 0
suite_skipped: 18
suite_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
status_delta_suite_by_class: delta: removed=0 added=25 expected=25 match=yes
suite_version: 26.10.3.7
suite_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
suite_java: default toolchain
suite_crash: no
main_diff_since_base: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
main_numstat: 0 3
main_files_added: none
untouched_main_files: yes
test_files_changed: src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
test_files_added: src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java
extended_tests_lines_deleted: 0 0 0 0
build_ci_unchanged: yes
docs_changed: docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg1.md docs/ROLLOUT-v1.7.0_gg6.md
scripts_dir_unchanged: yes
helpers_unchanged: yes
merges_since_base: none
fd_paths_line: paths: candidates=98 resolved=70 abbreviated=20 abbreviated_unique=20 placeholders=2 by_design=6 unresolved=0
gg6_ids_line: ids: hits=0
ledger_check_line: check: rows=57 required=57 problems=0
ledger_lookup: .planning/phases/48-phase-37-dispositions-document-fixes/48-DISPOSITION-LEDGER.md
lists_identical_to_tags: yes
anchors_mirror: rows=218 bad=0
tail_gate_line: invariants: provenance_paths=11 anchor_commits=164 anchor_rows=218 marker_commits=182 anchor_exempt=0 result=PASS
tail_gate_exit: 0
```

Notes. The cache-busted testUnit with Docker unreachable read 2320 tests (E-01's 2295 plus the 25 rows of `48-expected-added.tsv`), 107 XML files, 0 failures, 0 errors and the same 2 skips, and its per-testcase delta against the E-01 dump removes nothing and adds exactly the 25 expected rows. The one full suite on `latest` x V2 read 2923 tests (E-01's 2898 plus 25), 156 XML files, 0 failures, 0 errors and the 18 named skips, exit 0, in 3 min 11 s; its delta against the E-01 dump by class removes nothing and adds exactly the 25 expected rows. `latest` did not drift: the server version `26.10.3.7` and the digest equal E-01's and E-11's. The three Gradle or Testcontainers jobs of the gate (testUnit, suite) and the tail gate ran strictly one after another, and no `Ryuk` line appears in the suite log. No JVM crash and no core dump; the default toolchain ran both runs, and `-PjavaVersion=21` was used only to build the dry-run archives of E-14 and E-15. `src/main` differs from the base only by the three deleted lines of `DeclaredNullability.java` (the 28 IN-01 move) and no file was added there, so `Processing.java`, `KeeperStateProvider.java` and the other high-risk and neighbouring production classes are unchanged and the `BEFORE`/`AFTER` flag sequence is unchanged; `ProcessingTest` ran green in both runs. The test tree gained the two new classes and changed eight more files (the four extended classes with no deleted line, and the four repointed files of the 28 IN-01 move). The phase tip holds no merge, no change of build files, `.github`, `VERSION`, `CHANGELOG.md` or `.planning/scripts`, and the five lists are unchanged (the gg6 and gg7 lists equal their tags). The plan-time helpers and the two frozen scripts still carry the recorded md5 values. The origin sync branch is still absent, `pre-v1.7.0_gg8` is absent on origin, upstream `main` is still `83808df9`, and nothing was pushed in this phase. The tail gate ran once on the explicit SHA of `gate_head`, over `12e2cf17..97b60d3a`.

Recorded 2026-11-06T10:04:36Z.

Verdict: pass
