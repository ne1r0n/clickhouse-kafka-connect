# Phase 36 follow-ups for Phase 38 and the backlog

These findings came out of the investigation of the red CLT-07 nightly (E-15, E-16). Each one was observed on both client lines (0.9.6 and 0.11.0) and is not a client 0.11.0 regression. None is fixed in Phase 36; the first two are connector behaviour in `src/main` and need their own plan, the third is harness behaviour. The one change made in Phase 36 for the nightly is the generateData fix described in E-16 and in `docs/FORK-DELTA.md`.

## Findings

### F-1: `ClickHouseWriter.stop()` never closes its `ClickHouseHelperClient`

- Observed on both client lines, not a 0.11.0 regression, not fixed here.
- Each task start builds a `ClickHouseHelperClient` (and with client V2 one underlying V2 client with its HTTP connection pool), and `ClickHouseWriter.stop()` does not close it, so every task start leaks one V2 client for the life of the worker process.
- Where it matters: workers that restart or rebalance tasks often. The leak is per start, not per record.
- Suggested handling: close the helper client in `stop()` (try-with-resources is not possible across `start`/`stop`, so an explicit close), with a feature test that counts open clients over a start/stop cycle. `ClickHouseWriter.java` is a high-risk file, so this needs its own plan and a full suite.

### F-2: the 4-minute insert timeout has no effect with `async=false`

- Observed on both client lines, not a 0.11.0 regression, not fixed here.
- `awaitInsertResponse` applies `clickhouseClientInsertTimeoutMs` (4 minutes) to a future, but with `async=false` the client's `insert` returns an already completed future, so the timeout never fires, and the client's `socket_timeout` defaults to 0 (no timeout). A V2 insert can therefore block indefinitely on a stalled server or connection.
- Suggested handling: decide whether the timeout should be enforced on the call itself (client `socket_timeout` or a bounded execution) and pin it with a test against a stalled stub server; `ClientTimeoutPrefixPinUnitTest` (Phase 36) is the nearest existing pin of the client's timeout options.

### F-3: Datagen completion counting in the harness spans all connectors

- Observed on both client lines, not a 0.11.0 regression. Fixed here only for `generateData`.
- The harness counts the log text `Stopping connector: generated the configured` over the status of all connectors, so finished tasks of any leftover Datagen connector inflate the count of the next run. `generateData` now counts only its own connector's task status and deletes the connector in a `finally` block (E-16).
- Still open: any other harness helper that counts across `getConnectors()` output, and `createConnectorAndWaitUntilRunning` itself, which checks task 0 only and polls first after 5 s; it is still correct for the sink connector, whose tasks do not finish on their own.

## Lines for the milestone close

- Client 0.11.0 shipped with a Datagen harness race fixed in `ConfluentPlatform.generateData` (test code only); CLT-07 is passed only when a green nightly on the fixed commit is recorded.
- Open, observed on both client lines and not a 0.11.0 regression: `ClickHouseWriter.stop()` leaks one V2 client per task start (F-1).
- Open, observed on both client lines and not a 0.11.0 regression: `clickhouseClientInsertTimeoutMs` does not bound a V2 insert with `async=false`, so an insert can block indefinitely (F-2).
- Open, observed on both client lines and not a 0.11.0 regression: harness Datagen-completion counting spans all connectors outside `generateData` (F-3).
