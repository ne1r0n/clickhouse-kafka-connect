# Phase 28 Behaviour Ledger

**Purpose:** one place that lists every operator-visible behaviour change made in Phase 28 (Raw Identifier Names in Discovery), so that Phase 29 can write the `1.7.0_gg4` CHANGELOG introduction, complete FORK-DELTA and write `docs/ROLLOUT-v1.7.0_gg4.md`. Plan 28-03 writes no CHANGELOG, FORK-DELTA, VERSION or ROLLOUT text; the documents of the phase land in plan 28-05's fix commit.

**Source plans:** 28-03 (L-58 to L-61)

**How to read it:** IDs continue after the Phase 27 ledger's L-57. Test references are `Class` or display-name prefixes. The ledger holds test names, decision IDs and message formats only, never record data. E-04 is the measured table of the server's DESCRIBE text and of the forms discovery yielded before the fix, per character and per client; E-07 is the same table after the fix.

**Destination key:** CHANGELOG = the `1.7.0_gg4` section of `CHANGELOG.md`; FORK-DELTA = a row or Known costs item; ROLL = an item in `docs/ROLLOUT-v1.7.0_gg4.md`.

## Ledger

| ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 29 destination |
|----|-------------|------------------------------------------------------|-----------|-----------|------------------|-------------------------|----------------------|
| L-58 | 28-03 | Client V2, every route. Before: a column or Tuple element whose name holds a backslash, a slash, a line feed, a tab or a carriage return matched a Kafka record field only under the server's JSON escaping of the name (E-04, V2 column: for example the name `x\y` was looked up as `x\\y`, and a tab as backslash plus `t`). A root field written under the raw name was ignored and the column's DEFAULT was stored; an element written under the raw name failed. After: the raw identifier, as written in `CREATE TABLE`, matches (E-07 re-render). | the 2026-10-21 canonical-form decision (raw identifier, no dual-form alias), D-04, D-05 | `RawNameMatrixFixturesUnitTest`, `RawNameMatrixLiveTest`; the live write pins follow in plan 28-04 | yes | A field named under the old doubled form stops matching: its column keeps the DEFAULT, and an element under the old name fails the record. Fields named as in `CREATE TABLE` now match. | CHANGELOG, FORK-DELTA, ROLL (plan 28-05) |
| L-59 | 28-03 | Client V1 (the connector's default client). Before: the forms of E-04's V1 column. Backslash names already matched the raw identifier; a line feed matched but logged the metadata-only fallback WARN four times per table; tab and carriage return left the whole table unmapped; a slash or a double quote was replaced by U+FFFD, so no field matched. After: the same raw identifiers as V2, because both clients now read the raw response lines as UTF-8 and decode the name once in `fromJsonRow`. | as L-58 | `RawNameMatrixLiveTest` (`NAME-03 client V1`), `RawNameMatrixFixturesUnitTest` | yes (V1 is the default client) | Tables with a tab, carriage return, slash or double quote in a column or element name start mapping; a V1 line-feed name stops logging the fallback WARN. | as L-58 |
| L-60 | 28-03 | A double quote in a column or element name no longer leaves the table unmapped on either client (E-04: dq names table UNMAPPED on V2, U+FFFD names on V1; E-07: the raw name on both). | as L-58 | `RawNameMatrixFixturesUnitTest`, `RawNameMatrixLiveTest` | yes | A table that the connector skipped as unmapped starts receiving records. | as L-58 |
| L-61 | 28-03 | Record only, the test flips. The changed pins are exactly E-06's named exemption list: X-01 to X-10 (ten `DeclaredNullabilityTest` expectations), X-11 (the COLS-13 twins' element name) and X-12 (the drift test's nested key), all in fix(28-03), plus the support rows S-01 to S-03 of test(28-02) (two Javadocs, one helper refactor, no assertion). Each pin encodes the raw identifier, was red on the unchanged production in E-06, and HEAD's twins fail loudly on the fix (E-07). No test or assertion was removed and no predicate loosened. The list is forced by the user's 2026-10-21 canonical-form decision together with CONTEXT's normalise and red-first rules (and 23-REVIEW IN-02 for X-10), and it is surfaced to the user for the 28-06 human-check rather than decided by them (the L-47 shape). | D-05, 23-REVIEW IN-01, IN-02 | `DeclaredNullabilityTest`, `NullableTupleMatrixLiveTest` | no | Not visible to an operator. | FORK-DELTA (none) / record only |

## Rollout lines for Phase 29

(Plan 28-05 fills this section.)

## Out of scope, for the user

- A dual-form name lookup or a doubled-form WARN (NAME-F1) stays deferred. Until it exists, an operator who still sends fields under the old doubled form must rename them to the names written in `CREATE TABLE` (ROLL, plan 28-05).
