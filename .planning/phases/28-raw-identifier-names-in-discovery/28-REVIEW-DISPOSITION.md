---
phase: 28
review: 28-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "Enum label scan reads the SQL escapes `\\0`, `\\b` and `\\f` as the letters 0, b and f"
  - id: WR-02
    severity: warning
    disposition: open
    title: "`describeTableV1` lets the new enum parse failure escape and abort the mapping of the whole database"
  - id: WR-03
    severity: warning
    disposition: open
    title: "The new JSON-layer decoder's `\\uXXXX` path and the malformed-enum branches have no test"
  - id: WR-04
    severity: warning
    disposition: open
    title: "`legacyEnumLabelKeyFailsLoudly` passes on any failure, not on the enum lookup miss it names"
  - id: IN-01
    severity: info
    disposition: open
    title: "`DeclaredNullability.ESCAPED_NEWLINE` is a public production constant that only tests read"
  - id: IN-02
    severity: info
    disposition: open
    title: "`ForkCoverageScopeUnitTest` cannot detect a fork class missing from the list, and `SinkTaskStatistics` is one"
  - id: IN-03
    severity: info
    disposition: open
    title: "A record still using the escaped name form is stored with the column DEFAULT and no log line"
---

# Phase 28 review disposition

Code review was advisory and found 0 critical, 4 warnings and 3 info items. Nothing was fixed in this run; all seven stay open for `/gsd-code-review 28 --fix` or a user decision. WR-01 rests on the reviewer's reading of ClickHouse string escaping and is not measured in this repo. IN-03 is the accepted D-06 decision, recorded so it is not lost.
