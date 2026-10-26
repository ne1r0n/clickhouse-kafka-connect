# 32-AMENDMENT-01: archive allow-list is four rows (user decision, 2026-10-26)

Source: plan 32-01 Task 2 stopped at a blocking-human decision. The archive compare
(tag v1.7.0_gg4 vs gg5 tip) reports `only-a 0 only-b 0 changed 4`, not `changed 2`.
`src/main` is byte-identical; javac inlines the compile-time constant
`Version.ARTIFACT_VERSION` into `ClickHouseHelperClient.class` (CONNECT_CLIENT_NAME) and
`SinkTaskStatistics.class`. `javap -c -p -constants` shows only the v1.7.0_gg4 -> v1.7.0_gg5
string differs.

User answer (AskUserQuestion, verbatim label): "A: 4-row allow-list (Recommended)".

Binding changes to plans 32-01, 32-02, 32-03 (and later wording derived from them):
- PT-05 / T-32-04 / Task 2 verify: `confluent-archive-allowlist-32.txt` holds exactly FOUR rows:
  `com/clickhouse/kafka/connect/sink/Version.class`,
  `com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class`,
  `com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class`,
  `META-INF/MANIFEST.MF`. Each reason: inlined compile-time constant
  `Version.ARTIFACT_VERSION` v1.7.0_gg4 -> v1.7.0_gg5 (manifest: Implementation-Version).
  No wildcard, no catch-all. Expected totals: `only-a 0 only-b 0 changed 4`.
- E-03 must include the javap proof for the three classes.
- Everywhere "two rows" / "changed 2" / "differs only in Version.class and the manifest"
  appears in 32-02 and 32-03 (rollout note claim 2, CHANGELOG, FORK-DELTA, evidence checks),
  read "four rows" / "changed 4" / "differs only in the release identity: the Version
  constant and the two classes that inline it, plus the manifest Implementation-Version".
  Reword verify strings accordingly in the same commit as the text.
