# Phase 9: Metadata-free JSON Path — Keep or Drop - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-19
**Phase:** 9-Metadata-free JSON Path — Keep or Drop
**Areas discussed:** Keep-or-drop call (JSONP-01), jsonInsertColumns' new shape (JSONP-03), Disposition of the 17 parked tests (JSONP-02), Dead config + gate shape (JSONP-04)

---

## Area selection

| Option | Description | Selected |
|--------|-------------|----------|
| Keep-or-drop call (JSONP-01) | The core decision, plus whether it is locked here or at an execution checkpoint | ✓ |
| jsonInsertColumns' new shape (JSONP-03) | Where the filter lives and what happens on a non-existent column name | ✓ |
| Disposition of the 17 parked tests (JSONP-02) | Re-enable / rewrite / delete, and R-01's pin | ✓ |
| Dead config + gate shape (JSONP-04) | `jsonInsertFormatExplicit` with no reader, and whether the gate is `testUnit` or a full live run | ✓ |

**User's choice:** all four.

---

## Keep-or-drop call (JSONP-01)

### Q1 — Does any live connector in production actually set `insertFormat=json`?

| Option | Description | Selected |
|--------|-------------|----------|
| No — nothing sets it | Bypass has no production consumer; "drop" costs nothing operationally | |
| Yes — at least one pipeline | Dropping changes live behavior; needs a migration story | ✓ |
| Unknown — I need to check | Hold the call open, make it the phase's first blocking checkpoint | |

**User's choice:** Yes — at least one pipeline.
**Notes:** This is the fact SYNC-05's production audit never covered (it recorded `enableDbTopicSplit` and `bufferCount` only). Recorded in CONTEXT.md as operator-confirmed fact 1.

### Q2 — What value shape do those pipelines produce?

| Option | Description | Selected |
|--------|-------------|----------|
| Schemaless JSON (Map) | `SCHEMA_LESS` records; the bypass genuinely works for these; R-01 never fires | ✓ |
| Struct (schema'd) | `SCHEMA` records — exactly R-01; those pipelines write zero rows today | |
| String records | `STRING_SCHEMA` → `doInsertString`; `insertFormat=json` largely inert | |
| Mixed / I need to check | More than one shape, or needs a look at deployed configs | |

**User's choice:** Schemaless JSON (Map).
**Notes:** Decisive for the R-01 risk picture — no live pipeline takes the route the silent loss fires on. Operator-confirmed fact 2.

### Q3 — Which of the bypass's three effects does the live setup depend on? (multi-select)

| Option | Description | Selected |
|--------|-------------|----------|
| Tables may not exist at `start()` | Upstream hard-fails: "Did not find any tables in destination" | ✓ |
| Connector user lacks DESCRIBE/SHOW | The one thing upstream cannot work around with config at all | ✓ |
| Avoiding DESCRIBE traffic/latency | Chosen to skip the periodic refresh, not because metadata is unavailable | ✓ |
| None — just to avoid RowBinary | Tables exist, user can describe them; "drop" is operationally free | ✓ |

**User's choice:** all four — internally contradictory, disambiguated in Q4.

### Q4 — Which reading is right?

| Option | Description | Selected |
|--------|-------------|----------|
| Strictest — assume all constraints | Forces KEEP: no upstream knob covers a missing DESCRIBE privilege | ✓ |
| I don't actually know yet | Record keep/drop as OPEN, make it the phase's first blocking checkpoint, plan both branches | |
| Loosest — tables exist, grants fine | DROP is operationally free and R-01 closes with it | |
| Tables-later + traffic, grants fine | Reachable with upstream config; drop viable with a migration note | |

**User's choice:** Strictest — assume all constraints.
**Notes:** This is what settles JSONP-01 as **KEEP** (CONTEXT.md D-01). It overrides Phase 7 D-10's inherited "dropped" default, which was explicitly labelled reversible and Phase 9's to decide. It is the milestone's second deliberate carve-out from the "prefer upstream" conflict policy, after JMX naming.

### Q5 — How should the bypass be re-applied onto upstream's rewritten methods?

| Option | Description | Selected |
|--------|-------------|----------|
| Minimal early-returns | Guard at the top of 4 sites; every upstream line below untouched (describeTable fallback, compareAndSet, enableDbTopicSplit branch preserved) | ✓ |
| Restore the fork's older method bodies | Byte-for-byte production behaviour, but discards three upstream improvements and grows the fork delta | |
| Early-returns + hoist the gate into one method | Same as minimal, plus a single named predicate all four sites call | |

**User's choice:** Minimal early-returns.
**Notes:** The named-predicate half of option 3 was taken as Claude's discretion (CONTEXT.md D-05) since the user constrained the *diff shape*, not whether the condition is named — a gate spread across four inlined call sites can drift.

### Check-in

| Option | Description | Selected |
|--------|-------------|----------|
| Next area | Call made, re-application shape set | ✓ |
| More questions | Whether JSONP-01's "blocking-human checkpoint" is this discussion or an execution-time one | |

**Notes:** Resolved as Claude's discretion in CONTEXT.md D-02 — the decision is recorded here (evidence from merged code + human input + no implementation yet = ROADMAP criterion 1 satisfied); a planner may add a cheap re-confirm checkpoint but not re-open it.

---

## jsonInsertColumns' new shape (JSONP-03)

**Finding presented before the question:** `javap` on `client-v2-0.9.6.jar` shows `insert(String, List<String>, DataStreamWriter, ClickHouseFormat, InsertSettings)` still exists. JSONP-03's stated rationale — "the overload it attached to no longer exists" — is true of the *call site* but false of the *API*.

### Q1 — How should `jsonInsertColumns` be re-attached?

| Option | Description | Selected |
|--------|-------------|----------|
| Server-side list — use the surviving overload | COLS-02..06 stay green unchanged incl. COLS-05's loud failure; composes with the metadata-free bypass for free; deviates from JSONP-03's literal wording | ✓ |
| Client-side filter — follow JSONP-03 literally | Honors the requirement as written, but COLS-05 becomes silent and the filter needs its own step to survive the cleanup bypass | |
| Server-side list + client-side validation | Keeps COLS-05 loud and louder, but adds fork-only code and cannot validate under the bypass (no metadata by definition) | |

**User's choice:** Server-side list — use the surviving overload.
**Notes:** Recorded as CONTEXT.md D-06/D-07. The requirement's *goal* is binding, its *rationale* is corrected by an appended dated note in REQUIREMENTS.md rather than a rewrite (annotate-don't-flip).

### Check-in

| Option | Description | Selected |
|--------|-------------|----------|
| Next area | Overload chosen; empty-list and V1 scope recorded as planner constraints | ✓ |
| More questions | Whether REQUIREMENTS.md's JSONP-03 wording should be amended, and how the deviation is recorded | |

**Notes:** Two items carried as planner constraints rather than questions — CONTEXT.md D-08 (branch on non-empty unless COLS-02 re-proves unconditional is safe on the `DataStreamWriter` overload; a mis-rendered `INSERT INTO t ()` would break every default-config JSON insert in production) and D-09 (`doInsertJsonV1` untouched; pre-merge never wired it there and the milestone tests V2 only).

---

## Disposition of the 17 parked tests (JSONP-02)

### Q1 — How should the four `@Disabled` classes be brought back?

| Option | Description | Selected |
|--------|-------------|----------|
| Re-enable all four unchanged | Delete only the `@Disabled` line; Phase 7's compile fixes stay; the four-line diff is itself the proof the contract didn't move | ✓ |
| Re-enable unchanged + refresh R-01's Javadoc | Same, plus recording that production runs schemaless Map only so no live pipeline takes the Struct route | |
| Re-enable selectively, one per commit | Each `@Disabled` comes off in the commit landing the behaviour that makes it green | |

**User's choice:** Re-enable all four unchanged.
**Notes:** R-01's `PIN - NOT AN ENDORSEMENT` Javadoc stays byte-identical; the new production evidence went into CONTEXT.md's operator facts and Deferred Ideas instead.

### Q2 — Should Phase 9 add a cheap gate against a future silent drop?

| Option | Description | Selected |
|--------|-------------|----------|
| Docker-free pins for `getTable` + `updateMapping` | Two cases in `MetadataFreeJsonInsertUnitTest` via the reflective `csc` accessor it already has; `start()` stays live-only (needs a ping) | ✓ |
| No new tests — re-enabling is enough | Smallest diff, but the regression would only surface in a Docker run | |
| Add it to the merge-invariant script instead | Cheapest to run, but asserts text not behaviour — and that script has an open fail-open defect (CR-01) | |

**User's choice:** Docker-free pins for `getTable` + `updateMapping`.
**Notes:** Makes the bypass a sixth entry in STATE.md's "vanish with no conflict marker" list, with a `testUnit`-visible canary rather than a Docker-only one. Existing Docker-free coverage (JSON-01's letter-case matrix, JSON-03's `cleanupExtraFields`) would not have caught this merge's drop.

### Check-in

| Option | Description | Selected |
|--------|-------------|----------|
| Next area | Four annotations off, two canaries added | ✓ |
| More questions | COLS-05's live half, or Phase 11's skip accounting now these classes run | |

---

## Dead config + gate shape (JSONP-04)

**Framing:** KEEP dissolves the "dead config" half — `jsonInsertFormatExplicit` gets its consumer back. What remained was JSONP-04's evidence act and the gate, with a stated tension: ROADMAP criterion 3 defers `COLS-01..06`'s live half to Phase 11, but this phase re-enables 13 live methods that have not executed once since the 581-commit merge.

### Q1 — What closes Phase 9?

| Option | Description | Selected |
|--------|-------------|----------|
| Full live suite, re-baselined (Phase 8's D-12) | `testUnit` + full `./gradlew test`; all 13 newly-enabled live methods green; every remaining failure named with its owning phase | ✓ |
| `testUnit` only — honor ROADMAP criterion 3 | Fastest, matches the roadmap as written, hands Phase 11 an unmeasured surface | |
| `testUnit` + targeted live run of the JSON classes | Proves this phase's work live without a full run, but cannot see collateral damage (`start()`/`updateMapping()` are on every connector's path) | |

**User's choice:** Full live suite, re-baselined.
**Notes:** Same reasoning as Phase 8's D-12 — Phase 7's gap-closure proved a green `testUnit` can sit on top of an entirely broken behaviour (`targetTableFilter` appended after `group by`). Q2's outcome upgraded the pass condition from re-baselined to a clean zero-failure binary.

### Q2 — Adopt the `TableTest.extractNullables()` carry-in?

| Option | Description | Selected |
|--------|-------------|----------|
| Adopt it — fix the stale assertion | One test line (`3` → `2`, citing DESC-01/HLP-02); turns the gate into a clean zero-failure binary | ✓ |
| Leave it — re-baseline around it | Keeps Phase 9's diff strictly inside JSONP-01..04, but hands Phase 10/11 an inherited red | |
| Leave it, but escalate it to a named owner | No code change; SUMMARY assigns it to Phase 10 so it stops being unowned | |

**User's choice:** Adopt it — fix the stale assertion.
**Notes:** A test-only change outside `JSONP-01..04`, taken at the user's explicit decision per STATE.md's "a change reaching outside a phase's declared scope stops for a user decision". `MERGE-CHECKLIST.md` Recorded Fact 13 records it as unowned and requiring resolution before Phase 11; that fact's disposition gets flipped with the commit SHA.

---

## Final check

| Option | Description | Selected |
|--------|-------------|----------|
| I'm ready for context | Write CONTEXT.md and proceed to planning | ✓ |
| Explore more gray areas | REQUIREMENTS.md JSONP-03 amendment; commit sequence under D-14; formal execution-time checkpoint | |

---

## Claude's Discretion

The user did not constrain these; all are reversible and a planner may override with a stated reason.

- **D-02** — the JSONP-01 checkpoint is this discussion, not a re-litigation at execution time.
- **D-05** — a single named `shouldSkipTableMetadata()` predicate rather than four inlined conditions (anti-drift, per Phase 7's shared message builder and Phase 8's D-08).
- **D-08** — branch on non-empty for the `jsonInsertColumns` overload unless a live COLS-02 proves unconditional is safe.
- **D-09** — `doInsertJsonV1` untouched.
- **D-13** — the six-commit sequence and scoped `spotlessApply` only.
- **Wave shape** — the four guards are one connected edit; `jsonInsertColumns` is independent and could run in a parallel wave; the `@Disabled` deletions depend on both; the live run is last.
- **JSONP-04's evidence act** — discharged by reading `git diff v1.6.0 -- ClickHouseWriter.java` after the re-application and stating three findings in the SUMMARY. A grep is the evidence; a green test is not sufficient, per the requirement's own wording.
- **No new metric** — this phase re-routes an existing insert path, it does not add one.

## Deferred Ideas

- **R-01 fail-fast — reschedule on new evidence.** Still out of scope (JSONP-02 pins the loss under KEEP), but operator fact 2 says no live pipeline uses the Struct route, so the migration story Phase 4 rated prohibitive may now be empty.
- **P4-WR-01** — `ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally`; the class is re-enabled unchanged so the defect returns with it.
- **CR-01** — `verify-merge-invariants.sh:44` fails open on a `git grep` error; unowned, must be fixed before Phase 11.
- **Naming `insertFormat` for what it does** — its only effect in this fork is "skip table metadata"; `bypassRowBinary` selects the format. An operator-visible config change needing its own decision.
- **P3-WR-01** — `shouldSkipDescribeRow` walking more than one level up for deeply nested `JSON`/`Dynamic` children; adjacent to D-14's root cause.
