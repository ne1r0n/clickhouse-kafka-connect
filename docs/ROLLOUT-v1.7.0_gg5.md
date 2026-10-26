# Rollout checklist: `v1.7.0_gg4` to `v1.7.0_gg5`

**No connector code changed.** The connector source (`src/main`) is identical to `v1.7.0_gg4`. This release changes the release workflows, adds a fork-owned nightly integration-test workflow and CI checks, and changes the release identity.

If you are coming from a release before `v1.7.0_gg4`, work through [`ROLLOUT-v1.7.0_gg4.md`](ROLLOUT-v1.7.0_gg4.md) first and return here afterwards. The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream.

This note names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket stand-in, such as `<connector-name>` or `<rollback-commit>`; replace each one with your own value. Each of the five claims below shows the command that measured it and its output.

## 1. No connector code changed

`git diff v1.7.0_gg4 -- src/main` is empty between the two releases.

```sh
git diff --quiet v1.7.0_gg4 v1.7.0_gg5 -- src/main; echo $?
```

```text
0
```

Measured on the release tip before the tag existed, with `HEAD` for `v1.7.0_gg5`. The shipped archive differs from the `v1.7.0_gg4` archive only in the release identity: the version constant, the two classes that inline it and the manifest `Implementation-Version`. What the release does change: the release workflows, the nightly and its summary script, the CI guard tests, the integration-test harness, `dependabot.yml`, `VERSION` and the documents.

What to do: deploy the archive as before. Nothing in the connector's configuration changes.
