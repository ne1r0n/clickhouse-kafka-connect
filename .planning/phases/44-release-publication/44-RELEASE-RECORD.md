# Phase 44: release record for v1.7.0_gg7

Written on the local tail after the publish run was read back (E-33, E-34). It states measured facts only.

## Release

- Tag `v1.7.0_gg7`: a ref of object type `commit`, SHA `9483476322e0d331e81c5b92ac266a18f0f01450`, which is the merge commit M on `custom_build`.
- Release id 433237218, not a draft, not a prerelease, author `github-actions[bot]`.
- One asset, `clickhouse-kafka-connect-v1.7.0_gg7.zip`, 13564238 bytes, digest `sha256:3dbdcf03e5825073b7b0535718c9998a5e5754c0ae05a17502bbf4c6d69472af`, uploader `github-actions[bot]`. The dry run built the same digest.
- URL: https://github.com/ne1r0n/clickhouse-kafka-connect/releases/tag/v1.7.0_gg7

## Path

| Step | Evidence | Result |
|------|----------|--------|
| Scratch push `fc570937` and its `tests.yaml` run | E-29 | one run, success |
| Dry run 41688323407 from `sync/v1.7.0` | E-30 | success, compare exit 0, 0 unallowed, 0 stale |
| Merge M `94834763` built from the committed message | E-31 | two parents, tree equals X0, rollback block `ok` |
| Push of M to `custom_build` | E-32 | fast-forward from `72e8ccff`, one `tests.yaml` run, success |
| Publish run 41721771615 from `custom_build` | E-33, E-34 | success, `Publish release` success |

## Deviations and open items

- The CHANGELOG heading reads `2026-11-02`; the merge and the publish happened on UTC 2026-11-03. The user kept the date (E-28).
- The user ran none of the origin writes: the orchestrator pushed `sync/v1.7.0` and M and dispatched the publish run, each on the user's direct word. U4 was dispatched on the user's explicit override of the runbook rule that U4 stays the user's (E-33).
- Pull requests #21 and #22 read as merged on 2026-11-03T19:33:58Z with `custom_build` equal to M.
- U5 is done (E-35, E-36): `pre-v1.7.0_gg7` was pushed to origin at `72e8ccff`, the scratch branch `sync/v1.7.0` was deleted, and the two `scratch/clt-07-*` branches were left alone. #21 and #22 read closed and merged.
- Review items deferred from earlier rounds (IN-03 and the carried items) stay as recorded in `44-LOCAL-CLOSE-RECORD.md`.
