# Phase 23 Behaviour Ledger

**Purpose:** one place that lists every behaviour change made in Phase 23 (Declared-Nullability Hardening & Review Closure), so that Phase 24 can write the `1.7.0_gg3` CHANGELOG section, complete FORK-DELTA and write `docs/ROLLOUT-v1.7.0_gg3.md` (ROLL-03). Phase 23 writes no CHANGELOG, VERSION or ROLLOUT text.

**Source plans:** 23-02 (L-49)

**How to read it:** IDs continue after the Phase 22 ledger's L-48. Test references are `Class` or display-name prefixes. The ledger holds test names, decision IDs and message formats only, never record data.

**Destination key:** CHANGELOG = the `1.7.0_gg3` section of `CHANGELOG.md`; FORK-DELTA = a row or Known costs item; ROLL-03 = an item in `docs/ROLLOUT-v1.7.0_gg3.md`.

## Ledger

| ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 24 destination |
|----|-------------|------------------------------------------------------|-----------|-----------|------------------|-------------------------|----------------------|
| L-49 | 23-02 | `DeclaredNullability` null totality (COLS-12). Before: `apply` with a null root threw a `NullPointerException` out of its own handler; `containsTuple` threw one for a Column with a null type; a null in the Column tree (an Array element or Map value with a null type, a null `tupleFields` entry, a field with a null name or type) gave the fallback WARN with reason `(NullPointerException)`; a null root name gave `(IllegalArgumentException)`; an element Tuple with a null name could pair a field literally named `null.<x>`. After: a null root returns false with no log, `containsTuple` returns false, and every such null gives the fallback WARN with `(structure mismatch)` and leaves the root untouched. | D-05, D-06, D-07, D-08 | `DeclaredNullabilityTest` (COLS-12 (D-05), (D-06), (D-07) structure mismatch pins and the D-08 flip of `unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning`) | no | Not reachable from discovery, because `Column.extractColumn` always sets a name and a type and `buildDescribedTable` never passes a null root. | FORK-DELTA |
