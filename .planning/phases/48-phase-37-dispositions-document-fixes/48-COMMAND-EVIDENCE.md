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
| E-04 | P37-03 | 48-02 | pending (plan 48-02) |
| E-05 | P37-04 | 48-02 | pending (plan 48-02) |
| E-06 | P37-04 | 48-02 | pending (plan 48-02) |
| E-07 | P37-04 | 48-02 | pending (plan 48-02) |
| E-08 | P37-04 | 48-02 | pending (plan 48-02) |
| E-09 | P37-01, P37-02, P37-03, P37-04 | 48-02 | pending (plan 48-02) |
| E-10 | P37-01 | 48-03 | pending (plan 48-03) |
| E-11 | P37-01 | 48-03 | pending (plan 48-03) |
| E-12 | DOC-01, P37-04 | 48-04 | pending (plan 48-04) |
| E-13 | DOC-02, P37-04 | 48-04 | pending (plan 48-04) |
| E-14 | P37-03 | 48-05 | pending (plan 48-05) |
| E-15 | P37-01, P37-02, P37-03, P37-04 | 48-05 | pending (plan 48-05) |
| E-16 | P37-01, P37-02, P37-03, P37-04, DOC-01, DOC-02 | 48-05 | pending (plan 48-05) |
| E-17 | P37-03, DOC-01, DOC-02 | 48-05 | pending (plan 48-05) |

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
