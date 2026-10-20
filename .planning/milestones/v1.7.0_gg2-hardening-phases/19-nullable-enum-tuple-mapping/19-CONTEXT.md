# Phase 19: Nullable Enum & Tuple Mapping - Context

**Gathered:** 2026-10-12
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix backlog 999.1: `Column.extractColumn` drops `Nullable` in its Enum branch (`Column.java:202-211`) and Tuple branch (`Column.java:232-236`), so `Nullable(Enum8|Enum16)` and `Nullable(Tuple)` columns are written through RowBinary without their null marker.

- **COLS-08 (unconditional):** `Nullable(Enum8)`/`Nullable(Enum16)` round-trip NULL and non-NULL values, top-level and nested (`Array(Nullable(Enum))`, `Map(K, Nullable(Enum))`), checked by typed read-back. The D-16/L-13 pins asserting old behaviour flip with written rationale; none deleted.
- **COLS-09 (first):** a live twin-table Tuple spike on ClickHouse `latest` records a go/no-go with the type-string parsing approach, before any Tuple production code.
- **COLS-10 (outcome-dependent):** on go, `Nullable(Tuple(...))` round-trips with element nullability taken from the declared type string (not DESCRIBE's all-`Nullable` sub-columns; `Table.java` untouched) and `Array(Nullable(Tuple(...)))` still round-trips. On no-go, the discovery WARN names Tuple only, FORK-DELTA records a Known cost, and the 999.1 backlog entry is updated.
- **COLS-11:** the WARN, the `fork-anchors.txt` row for `hasNullableEnumOrTuple` and `docs/FORK-DELTA.md` change in the same commit as the code they describe; `verify-merge-invariants.sh` green at every commit.
- **DEBT-09 (last):** 15-REVIEW IN-01..IN-05 each fixed or closed with a written reason, consistently with the Tuple outcome.

Phase order is fixed by ROADMAP: spike → Enum fix → go/no-go → Tuple fix or Known cost → IN-01..IN-05 disposition. The go/no-go cannot hold the release; the Enum fix ships either way.

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked earlier; not re-opened)
- Element nullability comes from the declared type string, never from DESCRIBE's all-`Nullable` sub-columns; `Table.java` untouched; parse the type string once per `Column`, never per record (ROADMAP research flag).
- Never edit upstream-identical tests (`ColumnTest`, `TableTest`); add fork-only classes beside them (`ColumnArrayRecursionTest` precedent).
- Run every Tuple matrix cell against a twin with a `DEFAULT` column (`RowBinaryWithDefaults`) and one without; do not set `enable_nullable_tuple_type` in test DDL; record `SELECT version()` at each gate; get a seen-red mutation.
- Phase 18 rules hold: exception/log texts carry column metadata only, never record values; the flipped pins rely on Phase 18's column-naming `DataException` (WRT-10).
- Typed SQL read-back, never `countRows`; cache-busted two-run suite gate on `latest` × V2; `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; `spotlessApply` on touched files only.
- Per-fix commits; each fork patch in upstream regions gets its FORK-DELTA row and fork-anchors row in the same commit (D-13 pattern).
- Operator-visible changes are recorded in a behaviour ledger as they land (`19-BEHAVIOUR-LEDGER.md`, Phase 13/18 format) for Phase 20's CHANGELOG/ROLLOUT. Two Enum-fix rows ROADMAP already names: stored values change for `Nullable(Enum)` slots, and a record omitting a `Nullable(Enum)` field now passes validation and writes NULL.
- Every approval gate needs the user's own explicit answer; executors refuse relayed approvals, so human gates run orchestrator-inline (milestone rule, Phase 16 precedent).

### Go/no-go gate shape
- **D-01:** The Tuple go/no-go is a **blocking-human gate**: the spike presents its matrix results plus a one-line recommendation and the user answers go or no-go directly. No auto-go, no criteria-driven delegation.
- **D-02:** The decision is **binary only**. Go means ROADMAP criterion 3 in full: `Nullable(Tuple(...))` round-trips AND `Array(Nullable(Tuple(...)))` still round-trips. Anything less is a no-go with a Known cost. No partial go (e.g. top-level fixed, array shape still warned).
- **D-03:** The gate sits **between plans**: the spike is its own plan that ends by writing the matrix results and recommendation; the orchestrator asks the user at the plan boundary, then dispatches either the Tuple-fix plan or the Known-cost plan. Plan the Tuple work as two alternative plans (or one plan written after the gate), never as a mid-plan checkpoint.
- **D-04:** The gate bundle is **matrix + approach + cost**: per-cell results on both twin tables, `SELECT version()`, the proposed type-string parsing approach, the estimated `ClickHouseWriter`/`Column` diff shape, and a one-line recommendation. Enough to judge risk without reading spike code.

### Spike artifact & reuse (COLS-09)
- **D-05:** The spike is a **committed live JUnit test**, not a throwaway script. Before the fix it documents current (broken) behaviour per cell — which is the seen-red requirement, satisfied structurally. On go, the same class flips to the fixed expectations and becomes the permanent COLS-10 coverage. On no-go, it stays as a pinned record of exactly what fails. — **Reversibility:** costly — the spike test's assertions become the COLS-10 acceptance surface; replacing it later means rebuilding the matrix evidence.
- **D-06:** Each matrix cell proves **both sides**: (a) the current DESCRIBE-derived behaviour (broken), and (b) the same write with a hand-built `Column` carrying correct element nullability, proving the writer produces round-trippable bytes once nullability is right. (b) is the real go evidence — L-11 already shows the writer honours `isNullable(true)` on a Tuple column.
- **D-07:** Matrix scope: the **ROADMAP cells decide** go/no-go — `Nullable(Tuple(...))` with nullable and non-nullable elements, and `Array(Nullable(Tuple(...)))`, each against both twin tables. Added as **informational, non-deciding extras**: single-field vs multi-field tuple (the L-11 coincidence), `Map(K, Nullable(Tuple))`, and a nested Tuple-in-Tuple. Extras cannot veto a go; they feed the FORK-DELTA/rollout wording so the fix's edges are stated, not guessed.
- **D-08:** The verdict is recorded in **`.planning/phases/19-nullable-enum-tuple-mapping/19-SPIKE.md`**: matrix tables per twin table, `SELECT version()`, the parsing approach, the estimated diff shape, the recommendation, and the user's recorded go/no-go answer — the Phase 16/17 measured-approval-bundle shape.

### WARN lifecycle
- **D-09:** The discovery WARN **narrows to Tuple-only in the Enum-fix commit itself**: predicate retargeted, WARN text, `fork-anchors.txt` row and FORK-DELTA row rewritten together, so the WARN is never stale at any commit (COLS-11's same-commit rule). The Tuple outcome later either deletes it (go) or leaves the narrowed state as final (no-go).
- **D-10:** On go, the **whole WARN machinery is deleted** in the Tuple-fix commit: `hasNullableEnumOrTuple` (or its narrowed successor), the `WARNED_NULLABLE_ENUM_OR_TUPLE` dedup set, the message constants, and both describe-loop hooks, with the anchors row and FORK-DELTA row removed in the same commit. Shrinks the fork delta per the prefer-upstream policy; IN-03/IN-05 die with it. — **Reversibility:** reversible — git history keeps the machinery if a new broken shape ever needs it.
- **D-11:** The WARN pin tests (`ClickHouseHelperClientForkUnitTest#nullableEnumOrTupleTypesAreFlagged`, `#warnOnceDedupesPerColumnAndRawType`, `ClickHouseHelperClientForkLiveTest#describeTableWarnsOnceForNullableEnumOrTupleColumns`) are **flipped to negative pins, never deleted**: each asserts the new truth — discovery of a `Nullable(Enum)`/`Nullable(Tuple)` table logs no such WARN (use Phase 18's log-capture helper) and the resulting `Column` carries correct nullability. Each flip carries a written rationale (ROADMAP criterion 2 applies the same rule to the D-16/L-13 behaviour pins).

### IN-01..IN-05 disposition (DEBT-09)
- **D-12:** Appetite is **fix what the phase already touches**; no work outside lines the phase edits.
  - **On go:** all five close as moot/superseded — the WARN machinery is deleted (IN-01, IN-03, IN-05), the negative pins supersede the text-pin ask (IN-04), and the hooks IN-02 worried about are gone.
  - **On no-go:** the narrowed Tuple WARN rewrite fixes **IN-01** (text names the RowBinary insert route as the only affected route) and **IN-04** (the new narrowed text is pinned with the Phase 18 log-capture helper); **IN-03**'s delimiter ambiguity is fixed while in those lines (unambiguous key separator); **IN-02** closes with the written reason that the suite target is V2-only (project constraint) and **IN-05** closes with the rebase-churn reason.
- **D-13:** Dispositions are written into the archived `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md`, each row citing the fixing commit or the updated reason — the Phase 16 precedent for IN-07..IN-10.

### Claude's Discretion
- The spike test's class name and placement, and how cells are parameterised.
- The narrowed predicate/constant naming on the Tuple-only state (e.g. `hasNullableTuple`), as long as the anchors row moves in the same commit.
- The exact parsing approach for element nullability from the declared type string (proposed at the gate, decided by the spike evidence).
- Plan split beyond the fixed phase order (e.g. whether the Enum fix is its own plan or shares the spike's wave; the gate must still fall between plans).
- Exact wording of the narrowed WARN and of flipped-pin rationales, within the metadata-only content rules.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements
- `.planning/ROADMAP.md` § "Phase 19: Nullable Enum & Tuple Mapping" — goal, 5 success criteria, fixed in-phase order, the HIGH research flag (wire shape, DESCRIBE corruption risk, twin-table rule, seen-red, parse-once, no `enable_nullable_tuple_type`, record `SELECT version()`)
- `.planning/REQUIREMENTS.md` — COLS-08..COLS-11, DEBT-09; Out of Scope table (rows already stored wrongly are operator data, documented in the rollout note, not repaired)
- `AGENTS.md` — high-risk files (`ClickHouseWriter.java` type mapping), test rules (typed read-back + `assertEquals`, never delete/weaken tests)

### Defect record (what is broken and how it was pinned)
- `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md` — rows L-11 (Tuple nullability drop, `ATTEMPT_TO_READ_AFTER_EOF`, the single-field coincidence), L-13 (Enum nested-null throws; `{a:'a', b:null}` read back all-NULL before), L-21 (the WARN's contract)
- `docs/FORK-DELTA.md` — WARN row (~L48) and Known costs item 1 (~L136): the published statement of the defect this phase fixes or narrows
- `.planning/ROADMAP.md` backlog item 999.1 — updated on no-go

### IN-01..IN-05 (DEBT-09 subject matter)
- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW.md` §§ IN-01..IN-05 — full finding texts
- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md` — where the new dispositions land (Phase 16 precedent: IN-07..IN-10 rows)

### Fork bookkeeping and gates
- `.planning/scripts/fork-anchors.txt` — the `hasNullableEnumOrTuple` row (line 57) moves with every WARN change, same commit
- `.planning/scripts/verify-merge-invariants.sh` — phase gate `verify-merge-invariants.sh 54e63648 HEAD`, green at every commit
- `.planning/phases/18-writer-correctness/18-CONTEXT.md` and `18-BEHAVIOUR-LEDGER.md` — the content rules for messages/logs and the ledger format Phase 19 continues

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` `extractColumn` — the defect site: the Enum branch (L202-211) and Tuple branch (L232-236) build without `.isNullable(isNull)`, while the `Nullable(...)` branch (L274-275) passes `isNull=true` down. The Enum fix is adding nullability to the Enum branch; the Tuple fix must also derive element nullability (tuple fields are filled later from DESCRIBE sub-columns, which report all-`Nullable` — hence "from the declared type string").
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:490-527` — `hasNullableEnumOrTuple`, `warnOnceIfNullableEnumOrTuple`, `WARNED_NULLABLE_ENUM_OR_TUPLE`, the WARN constants, and the hooks in both describe loops.
- Pins to flip: `NestedNullableCompositeWriteTest#nullTupleFollowsColumnNullability`, `#nullElementInArrayOfNullableTupleThrowsBecauseColumnDropsTupleNullability`; `NestedNullRowBinaryLiveTest#nullElementInArrayOfNullableTupleFailsWithDataException`; plus the L-13 loop-check pins (L-13 has no dedicated test — follows from L-10's loop checks).
- WARN pins to flip to negative: `ClickHouseHelperClientForkUnitTest` (L264-324) and `ClickHouseHelperClientForkLiveTest#describeTableWarnsOnceForNullableEnumOrTupleColumns` (L395-434).
- Phase 18's reusable log-capture helper (reload4j appender, guaranteed detach) — for the negative WARN pins and, on no-go, the IN-04 text pin.
- `ColumnArrayRecursionTest` — the precedent for fork-only test classes beside upstream-identical `ColumnTest`/`TableTest`.
- `NestedNullableCompositeWriteTest` builds hand-made `Column` objects — the pattern for the spike's "both sides per cell" fixed-expectation half.

### Established Patterns
- L-11 records that a Tuple `Column` built with `isNullable(true)` already writes the null marker `0x01` — the writer side partially works; the mapping side is the defect.
- The WARN is a pure static predicate over the raw DESCRIBE type string; the raw string exists only in the DESCRIBE row (FORK-DELTA WARN row) — the Tuple fix's "declared type string" source is that same raw string.
- Phase 18's D-13 ledger discipline: one row per operator-visible change, in the same commit as the fix.

### Integration Points
- `Column.extractColumn` (mapping) → `ClickHouseWriter` TUPLE/ENUM write branches (null marker emission) → both describe loops in `ClickHouseHelperClient` (WARN hooks + raw type string availability).
- `docs/FORK-DELTA.md` + `.planning/scripts/fork-anchors.txt` + the WARN: COLS-11 binds them to the code commits.
- Phase 20 reads `19-SPIKE.md`, `19-BEHAVIOUR-LEDGER.md` and the updated FORK-DELTA for the `v1.7.0_gg2` rollout note (ROLL-02 names the 999.1 value change explicitly).

</code_context>

<specifics>
## Specific Ideas

- The spike's committed-test form deliberately makes the seen-red requirement structural: the pre-fix assertions are the red state, the post-fix flip is the green.
- The gate bundle must be readable without opening spike code — per-cell tables, not prose.
- On go, deleting the WARN machinery is itself the FORK-DELTA shrink the prefer-upstream policy exists for; record the removal as a FORK-DELTA row deletion, not a new row.

</specifics>

<deferred>
## Deferred Ideas

- **IN-02 (V1 describe-hook coverage):** stays closed-with-reason; a V1-path test run belongs to a milestone that targets client V1, which none does.
- **Repairing rows already stored wrongly by 999.1:** operator data — Phase 20's rollout note documents the query, the connector never rewrites it (REQUIREMENTS Out of Scope).
- **`Map(K, Nullable(Tuple))` and Tuple-in-Tuple fixes beyond what criterion 3 promises:** the spike measures them informationally; fixing shapes beyond the ROADMAP cells is a new capability for a future phase if the evidence warrants it.

</deferred>

---

*Phase: 19-nullable-enum-tuple-mapping*
*Context gathered: 2026-10-12*
