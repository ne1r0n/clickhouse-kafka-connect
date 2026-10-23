# Phase 27 Behaviour Ledger

**Purpose:** one place that lists every operator-visible behaviour change made in Phase 27 (Writer Failure Path & Test Debt), so that Phase 29 can write the `1.7.0_gg4` CHANGELOG introduction, complete FORK-DELTA and write `docs/ROLLOUT-v1.7.0_gg4.md`. Plan 27-02 writes only the one `CHANGELOG.md` bullet and the FORK-DELTA amendments that its own commit needs. It writes no VERSION or ROLLOUT text.

**Source plans:** 27-02 (L-52, L-53)

**How to read it:** IDs continue after the Phase 23 ledger's L-51. Test references are `Class` or display-name prefixes. The ledger holds test names, decision IDs and message formats only, never record data.

**Destination key:** CHANGELOG = the `1.7.0_gg4` section of `CHANGELOG.md`; FORK-DELTA = a row or Known costs item; ROLL = an item in `docs/ROLLOUT-v1.7.0_gg4.md`.

## Ledger

| ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 29 destination |
|----|-------------|------------------------------------------------------|-----------|-----------|------------------|-------------------------|----------------------|
| L-52 | 27-02 | JSON-as-string route (`clickhouseSettings` sets `input_format_binary_read_json_as_string` to `1` or `true`): a Jackson failure (`InvalidDefinitionException` and every other `JacksonException`) no longer passes through `ClickHouseWriter.doWriteColValue` unchanged. Before, its message, including `(through reference chain: ...)` with a Map key, reached the task status, the DLQ `exception.stacktrace` header and the `Error inserting records` log. After, it is ``Failed to write column `<name>`: Kafka value of type STRUCT (value class: org.apache.kafka.connect.data.Struct) could not be written as ClickHouse type JSON: com.fasterxml.jackson.databind.exc.InvalidDefinitionException``, with `Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException (message withheld: may contain record data)`. Retry classification is unchanged, and a Jackson exception over a timeout still passes through. Accepted residual: a Jackson exception whose cause chain also holds any non-Jackson IOException (beneath or above it) still passes through with its message. On this route that chain comes only from a record value whose own getter throws it. The E-03 probe shows that `writeValueAsBytes` returns a plain IOException as a JsonMappingException without a cause or a Map key, while a Jackson exception holding an IOException, or a getter's UncheckedIOException, keeps the chain and the key. Connect's value types have no getter that Jackson calls. The residual is pinned by the two accepted-residual rows. | D-01, D-02, D-04 | `WriterValueFreeFailureUnitTest` (WRT-17 rows, the accepted-residual rows included), `JsonAsStringKeyResidualLiveTest`, `WriterFailureClassificationUnitTest` (JACKSON_* rows) | yes | A Jackson message text or `through reference chain` no longer appears in the task status, the DLQ stack-trace header or the `Error inserting records` log of a JSON-as-string connector; a `Failed to write column` line naming the Jackson class appears instead. | CHANGELOG (written), FORK-DELTA (written), ROLL |
| L-53 | 27-02 | Record only, the one sanctioned flip. `JsonAsStringKeyResidualLiveTest`'s two key-present tests became key-absent tests (renamed), the route-reached helper also accepts a withheld copy's original class, and the Javadoc and display names lost their planning IDs. The key flip: D-03, seen red in E-03 `live_red:`. The broadened route-reached predicate: signed off by the user's answer in E-04, seen red in E-03 `predicate_red:`. The scrub: D-11. | D-03, D-11 | `JsonAsStringKeyResidualLiveTest` | no | Not visible to an operator. | none |

## Rollout lines for Phase 29

1. Alerts or log searches that match a Jackson message text or `through reference chain` on JSON-as-string connectors stop matching. Key on `Failed to write column` and the Jackson class name instead.
2. No wire-format change: upgrade and rollback need no clean stop, because a batch that fails this way failed before too and only its text changes. A rollback to `v1.7.0_gg3` re-exposes the Map key in those three destinations.

## Out of scope, for the user

- The `insertFormat=json` route (`writeRecordsJson`) can also raise a Jackson failure. It is not on the JSON-as-string route this phase names, so plan 27-02 does not change it. Whether it carries a Map key into the task status, the DLQ stack-trace header or the `Error inserting records` log has not been measured (research A6). It is a backlog candidate.
