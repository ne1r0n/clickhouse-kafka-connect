# Phase 28 Behaviour Ledger

**Purpose:** one place that lists every operator-visible behaviour change made in Phase 28 (Raw Identifier Names in Discovery), so that Phase 29 can write the `1.7.0_gg4` CHANGELOG introduction, complete FORK-DELTA and write `docs/ROLLOUT-v1.7.0_gg4.md`. Plan 28-03 writes no CHANGELOG, FORK-DELTA, VERSION or ROLLOUT text; the documents of the phase landed in plan 28-05's fix commit (CHANGELOG line, FORK-DELTA rows, Known costs item 11 closed, ROADMAP 999.5 delivered). Plan 28-05 fills the rollout lines and the handoff record below; `docs/ROLLOUT-v1.7.0_gg4.md` is Phase 29's.

**Source plans:** 28-03 (L-58 to L-61), 28-04 (L-62), 28-05 (L-63 to L-65)

**How to read it:** IDs continue after the Phase 27 ledger's L-57. Test references are `Class` or display-name prefixes. The ledger holds test names, decision IDs and message formats only, never record data. E-04 is the measured table of the server's DESCRIBE text and of the forms discovery yielded before the fix, per character and per client; E-07 is the same table after the fix.

**Destination key:** CHANGELOG = the `1.7.0_gg4` section of `CHANGELOG.md`; FORK-DELTA = a row or Known costs item; ROLL = an item in `docs/ROLLOUT-v1.7.0_gg4.md`.

## Ledger

| ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 29 destination |
|----|-------------|------------------------------------------------------|-----------|-----------|------------------|-------------------------|----------------------|
| L-58 | 28-03 | Client V2, every route. Before: a column or Tuple element whose name holds a backslash, a slash, a line feed, a tab or a carriage return matched a Kafka record field only under the server's JSON escaping of the name (E-04, V2 column: for example the name `x\y` was looked up as `x\\y`, and a tab as backslash plus `t`). A root field written under the raw name was ignored and the column's DEFAULT was stored; an element written under the raw name failed. After: the raw identifier, as written in `CREATE TABLE`, matches (E-07 re-render). | the 2026-10-21 canonical-form decision (raw identifier, no dual-form alias), D-04, D-05 | `RawNameMatrixFixturesUnitTest`, `RawNameMatrixLiveTest`; the live write pins follow in plan 28-04 | yes | A field named under the old doubled form stops matching: its column keeps the DEFAULT, and an element under the old name fails the record. Fields named as in `CREATE TABLE` now match. | CHANGELOG, FORK-DELTA, ROLL (plan 28-05) |
| L-59 | 28-03 | Client V1 (the connector's default client). Before: the forms of E-04's V1 column. Backslash names already matched the raw identifier; a line feed matched but logged the metadata-only fallback WARN four times per table; tab and carriage return left the whole table unmapped; a slash or a double quote was replaced by U+FFFD, so no field matched. After: the same raw identifiers as V2, because both clients now read the raw response lines as UTF-8 and decode the name once in `fromJsonRow`. | as L-58 | `RawNameMatrixLiveTest` (`NAME-03 client V1`), `RawNameMatrixFixturesUnitTest` | yes (V1 is the default client) | Tables with a tab, carriage return, slash or double quote in a column or element name start mapping; a V1 line-feed name stops logging the fallback WARN. | as L-58 |
| L-60 | 28-03 | A double quote in a column or element name no longer leaves the table unmapped on either client (E-04: dq names table UNMAPPED on V2, U+FFFD names on V1; E-07: the raw name on both). | as L-58 | `RawNameMatrixFixturesUnitTest`, `RawNameMatrixLiveTest` | yes | A table that the connector skipped as unmapped starts receiving records. | as L-58 |
| L-61 | 28-03 | Record only, the test flips. The changed pins are exactly E-06's named exemption list: X-01 to X-10 (ten `DeclaredNullabilityTest` expectations), X-11 (the COLS-13 twins' element name) and X-12 (the drift test's nested key), all in fix(28-03), plus the support rows S-01 to S-03 of test(28-02) (two Javadocs, one helper refactor, no assertion). Each pin encodes the raw identifier, was red on the unchanged production in E-06, and HEAD's twins fail loudly on the fix (E-07). No test or assertion was removed and no predicate loosened. The list is forced by the user's 2026-10-21 canonical-form decision together with CONTEXT's normalise and red-first rules (and 23-REVIEW IN-02 for X-10), and it is surfaced to the user for the 28-06 human-check rather than decided by them (the L-47 shape). | D-05, 23-REVIEW IN-01, IN-02 | `DeclaredNullabilityTest`, `NullableTupleMatrixLiveTest` | no | Not visible to an operator. | FORK-DELTA (none) / record only |
| L-62 | 28-04 | Client V2 (and, per plan 28-05's manual run, V1), RowBinary route. After the fix a record that still uses the doubled form (the server's JSON escaping of the name, which matched before the fix) is handled as pinned. A root field named in the doubled form is ignored and the column's DEFAULT is stored, with no error and no WARN (NAME-F1 deferred). An element named in the doubled form inside a Tuple, an Array(Tuple), a Nullable(Tuple) or a Map-value Tuple fails the put with the writer's error and stores no row: `RuntimeException` (first message `Number of records: 1`) whose root cause is `DataException`, first line `column '<path>' (INT32): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT`, where `<path>` is the raw column path of the container (`t.`, `a.`, `n.` or `m.values.` plus the element name); it names a field, not a value. Raw names land and read back equal at the root and in all four shapes. Before the fix the doubled form was the one that matched. | D-05 | `RawIdentifierNameLiveTest` (52 cases: 12 raw, 8 doubled root, 32 doubled element) | yes | An unrenamed producer that sends a doubled-form root field silently stores the column DEFAULT; a doubled-form Tuple element makes the put fail with the writer's error. Fields named as in `CREATE TABLE` work. | CHANGELOG, FORK-DELTA (plan 28-05), ROLL |
| L-63 | 28-05 | Client V2 and client V1, Enum8 and Enum16 write route. Before: an enum label holding an escaped character was keyed under its escaped text (E-04 `enum_label`: the V2 key carried the server's JSON layer on top of the SQL escaping, the V1 key one layer fewer, for bs, bsn, btbsn, lf, tab, cr, sl and dq), so a record value equal to the label as written in `CREATE TABLE` failed as the Phase 27 lookup miss and the value had to use the earlier key form. After: the label is keyed under its raw text; the raw label lands and reads back code 1 for all 12 entries (E-09, E-10), and a value in the earlier V2 key form fails as the same lookup miss (`Failed to write column ...: java.lang.IllegalArgumentException`, the label withheld) for the 9 entries that have one: bs, bsn, lf, tab, cr, sl, dq, ap, btbsn. ctl, bt and na never had a second form. | D-01 | `RawNameMatrixFixturesUnitTest` (`NAME-06: the enum keys are the raw labels`), `RawIdentifierNameLiveTest` (`NAME-06 raw form`, 12 cases; `NAME-06 legacy form`, 9 cases) | yes | A producer that sends the earlier key form for such a label starts failing the batch; a producer that sends the label as written now lands. | CHANGELOG, FORK-DELTA, ROLL |
| L-64 | 28-05 | A table whose Enum type holds an apostrophe label followed by another label is mapped, and so is a label holding `)`, `,` or `=` and a negative code. Before: `describeTable` returned no table under client V2 for the apostrophe entry, and client V1 threw `NumberFormatException` (E-04, entry ap, `enum_label`); for a label holding `)`, `,` or `=` the old reader's behaviour is read from the unchanged code (it cut the type text at the first `)`), not measured live; the new `ColumnTest` row was red on the unchanged Column (E-09). After: the table is mapped on both clients and the labels are read as written. | D-01, D-02 | `ColumnTest` (`enumLabelsHoldingAParenthesisACommaOrAnEqualsSignAreRead`), `RawIdentifierNameLiveTest` (`NAME-06 raw form`, entry ap) | yes | A table that the connector skipped as unmapped (records for it fail) starts receiving records. | CHANGELOG, FORK-DELTA, ROLL |
| L-65 | 28-05 | Record only. The new pins and their red runs are in E-09 (nine raw-label cases red on the unchanged Column, eight legacy cases red, the ColumnTest row red); the four Phase 27 lookup-miss pins (WRT-18: `unknownEnum8LabelIsALookupMissNamingTheColumn`, `unknownEnum16LabelIsALookupMissNamingTheColumn`, `nullEnumLabelIsALookupMiss`, `enumWithoutValuesNamesTheColumn`) are unchanged and green. A type text the scan cannot read throws `IllegalArgumentException` with the fixed text `Enum type could not be read`, which carries no label and no type text. | D-02 | `ColumnTest` | no | Not visible to an operator. | FORK-DELTA (none) / record only |

## Rollout lines for Phase 29

Measured items cite the evidence entry; the one reasoned item says so.

1. Rename producer fields to the raw identifier before upgrading: the name as written in `CREATE TABLE`, not the server's JSON escaping of it. The per-character old and new forms are in the table of the handoff record below, for both clients.
2. After the upgrade an unrenamed root field stores the column's DEFAULT with no error and no WARN, and an unrenamed element of a Tuple, an Array(Tuple), a Nullable(Tuple) or a Map-value Tuple fails the put with `DataException`, `column '<path>' (INT32): no value provided for a non-nullable column without a default; ...` and stores no row (E-08, 8 characters x 4 shapes; measured on client V2, and the V1 run of the same class passed, E-10).
3. An enum value in the earlier key form fails as a lookup miss, and a raw label now lands and reads back its code (E-09, E-10). The earlier V2 key was the JSON escaping of the label's SQL text; the earlier V1 key carried one escape layer fewer (E-04, `enum_label` rows). Only the V2 form is pinned live.
4. The clean stop, in both directions. Upgrade: stop the connector cleanly (no partition left with a BEFORE state record), rename the fields, then start the new version. Roll back: stop cleanly, rename the fields back to the earlier form, then start the old version. A batch that is retried across the version change is rebuilt under the other name rule, so its bytes differ and block deduplication cannot recognise it. Labelled: reasoned from the BEFORE/AFTER state machine, not measured (no live run in this phase retries a batch across the version change).
5. Rows stored by earlier releases are not repaired: a root column that stored its DEFAULT, or a short or rejected tuple, stays as it was written.
6. Not measured: a name or label holding a control character outside the nine characters of the table, a name holding a `.`, and `auto.evolve` with an unrenamed field.

## Phase 29 handoff record (D-06)

Everything Phase 29 needs for `docs/ROLLOUT-v1.7.0_gg4.md`. The rollout note itself is not written in this phase.

- Name forms, per character and shape, before and after, for both clients (72 rows). Old V2 and old V1 are the forms E-04 measured on the unchanged code; the last column is the raw identifier, which the post-fix re-render of the full suite shows on both clients for all 72 cells (E-10). UNMAPPED means the table was not served; `ERROR:NumberFormatException` means the describe call threw.

| entry | character | shape | old V2 (E-04) | old V1 (E-04) | new, both clients |
|---|---|---|---|---|---|
| ctl | underscore (control) | root | "r_z" | "r_z" | "r_z" |
| ctl | underscore (control) | tuple | "x_y" | "x_y" | "x_y" |
| ctl | underscore (control) | array_tuple | "x_y" | "x_y" | "x_y" |
| ctl | underscore (control) | nullable_tuple | "x_y" | "x_y" | "x_y" |
| ctl | underscore (control) | map_tuple | "x_y" | "x_y" | "x_y" |
| ctl | underscore (control) | enum_label | "x_y" | "x_y" | "x_y" |
| bs | backslash | root | "r\\\\z" | "r\\z" | "r\\z" |
| bs | backslash | tuple | "x\\\\y" | "x\\y" | "x\\y" |
| bs | backslash | array_tuple | "x\\\\y" | "x\\y" | "x\\y" |
| bs | backslash | nullable_tuple | "x\\\\y" | "x\\y" | "x\\y" |
| bs | backslash | map_tuple | "x\\\\y" | "x\\y" | "x\\y" |
| bs | backslash | enum_label | "x\\\\\\\\y" | "x\\\\y" | "x\\y" |
| bsn | backslash followed by n | root | "r\\\\nz" | "r\\nz" | "r\\nz" |
| bsn | backslash followed by n | tuple | "x\\\\ny" | "x\\ny" | "x\\ny" |
| bsn | backslash followed by n | array_tuple | "x\\\\ny" | "x\\ny" | "x\\ny" |
| bsn | backslash followed by n | nullable_tuple | "x\\\\ny" | "x\\ny" | "x\\ny" |
| bsn | backslash followed by n | map_tuple | "x\\\\ny" | "x\\ny" | "x\\ny" |
| bsn | backslash followed by n | enum_label | "x\\\\\\\\ny" | "x\\\\ny" | "x\\ny" |
| lf | line feed | root | "r\\nz" | "r\nz" | "r\nz" |
| lf | line feed | tuple | "x\\ny" | "x\ny" | "x\ny" |
| lf | line feed | array_tuple | "x\\ny" | "x\ny" | "x\ny" |
| lf | line feed | nullable_tuple | "x\\ny" | "x\ny" | "x\ny" |
| lf | line feed | map_tuple | "x\\ny" | "x\ny" | "x\ny" |
| lf | line feed | enum_label | "x\\\\ny" | "x\\ny" | "x\ny" |
| tab | tab | root | "r\\tz" | UNMAPPED | "r\tz" |
| tab | tab | tuple | "x\\ty" | UNMAPPED | "x\ty" |
| tab | tab | array_tuple | "x\\ty" | UNMAPPED | "x\ty" |
| tab | tab | nullable_tuple | "x\\ty" | UNMAPPED | "x\ty" |
| tab | tab | map_tuple | "x\\ty" | UNMAPPED | "x\ty" |
| tab | tab | enum_label | "x\\\\ty" | "x\\ty" | "x\ty" |
| cr | carriage return | root | "r\\rz" | UNMAPPED | "r\rz" |
| cr | carriage return | tuple | "x\\ry" | UNMAPPED | "x\ry" |
| cr | carriage return | array_tuple | "x\\ry" | UNMAPPED | "x\ry" |
| cr | carriage return | nullable_tuple | "x\\ry" | UNMAPPED | "x\ry" |
| cr | carriage return | map_tuple | "x\\ry" | UNMAPPED | "x\ry" |
| cr | carriage return | enum_label | "x\\\\ry" | "x\\ry" | "x\ry" |
| sl | slash | root | "r\\/z" | "r\uFFFDz" | "r/z" |
| sl | slash | tuple | "x\\/y" | "x\uFFFDy" | "x/y" |
| sl | slash | array_tuple | "x\\/y" | "x\uFFFDy" | "x/y" |
| sl | slash | nullable_tuple | "x\\/y" | "x\uFFFDy" | "x/y" |
| sl | slash | map_tuple | "x\\/y" | "x\uFFFDy" | "x/y" |
| sl | slash | enum_label | "x\\/y" | "x\uFFFDy" | "x/y" |
| dq | double quote | root | UNMAPPED | "r\uFFFDz" | "r\"z" |
| dq | double quote | tuple | UNMAPPED | "x\uFFFDy" | "x\"y" |
| dq | double quote | array_tuple | UNMAPPED | "x\uFFFDy" | "x\"y" |
| dq | double quote | nullable_tuple | UNMAPPED | "x\uFFFDy" | "x\"y" |
| dq | double quote | map_tuple | UNMAPPED | "x\uFFFDy" | "x\"y" |
| dq | double quote | enum_label | UNMAPPED | "x\uFFFDy" | "x\"y" |
| bt | backtick | root | "r`z" | "r`z" | "r`z" |
| bt | backtick | tuple | "x`y" | "x`y" | "x`y" |
| bt | backtick | array_tuple | "x`y" | "x`y" | "x`y" |
| bt | backtick | nullable_tuple | "x`y" | "x`y" | "x`y" |
| bt | backtick | map_tuple | "x`y" | "x`y" | "x`y" |
| bt | backtick | enum_label | "x`y" | "x`y" | "x`y" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | root | "r\u00E9\u65E5\u672Cz" | "r\u00E9\u65E5\u672Cz" | "r\u00E9\u65E5\u672Cz" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | tuple | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | array_tuple | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | nullable_tuple | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | map_tuple | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | enum_label | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| ap | apostrophe | root | "r'z" | "r'z" | "r'z" |
| ap | apostrophe | tuple | "x'y" | "x'y" | "x'y" |
| ap | apostrophe | array_tuple | "x'y" | "x'y" | "x'y" |
| ap | apostrophe | nullable_tuple | "x'y" | "x'y" | "x'y" |
| ap | apostrophe | map_tuple | "x'y" | "x'y" | "x'y" |
| ap | apostrophe | enum_label | UNMAPPED | ERROR:NumberFormatException | "x'y" |
| btbsn | backtick, backslash, n (WR-01) | root | "r`\\\\nz" | "r`\\nz" | "r`\\nz" |
| btbsn | backtick, backslash, n (WR-01) | tuple | "x`\\\\ny" | "x`\\ny" | "x`\\ny" |
| btbsn | backtick, backslash, n (WR-01) | array_tuple | "x`\\\\ny" | "x`\\ny" | "x`\\ny" |
| btbsn | backtick, backslash, n (WR-01) | nullable_tuple | "x`\\\\ny" | "x`\\ny" | "x`\\ny" |
| btbsn | backtick, backslash, n (WR-01) | map_tuple | "x`\\\\ny" | "x`\\ny" | "x`\\ny" |
| btbsn | backtick, backslash, n (WR-01) | enum_label | "x`\\\\\\\\ny" | "x`\\\\ny" | "x`\\ny" |

- Root silent versus element loud (E-08): a doubled-form root field is ignored and the column's DEFAULT is stored with no error and no WARN; a doubled-form element inside a Tuple, an Array(Tuple), a Nullable(Tuple) or a Map-value Tuple fails the put and stores no row. Both are the behaviour after the fix; before it the doubled form was the one that matched.
- Enum labels (E-09, E-10): the raw label lands for all 12 entries; the earlier V2 key form fails as the lookup miss for 9 entries; ctl, bt and na have no second form; a table with an apostrophe label followed by another label is mapped.
- The manual `CLIENT_VERSION=V1` run of `RawIdentifierNameLiveTest`, `RawNameMatrixLiveTest` and `TableTest` passed, 105 tests, with the writer stack frames of the V1 insert path as evidence (E-10).
- The clean stop in both directions, with the rename as its precondition, is rollout line 4: reasoned from the BEFORE/AFTER state machine, not measured.

## Out of scope, for the user

- A dual-form name lookup or a doubled-form WARN (NAME-F1) stays deferred. Until it exists, an operator who still sends fields under the old doubled form must rename them to the names written in `CREATE TABLE` (ROLL, plan 28-05).
