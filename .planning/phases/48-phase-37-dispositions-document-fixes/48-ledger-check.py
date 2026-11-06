#!/usr/bin/env python3
"""Read-only format gate for 48-DISPOSITION-LEDGER.md (P37-01, P37-02, P37-03, P37-04).

Written at plan time from 48-CONTEXT.md (U-1..U-11, D-1..D-3) and 48-RESEARCH.md (the 55 carried rows,
with 28 IN-05(b) split into its two items and 42 IN-03 into its two parts, 57 rows). Never edited by an
executor. It never writes a file and never runs git: the plan's shell resolves each printed pointer.
Run it with `python3 -I`.

The ledger is a set of Markdown table rows with six cells:
  | ID | Status | Pointer | Owner | Re-open trigger | Reason |
Header rows (ID cell `ID`) and separator rows (ID cell starting with `-`) are skipped.
Status is `fixed` or `closed`.
  fixed   pointer = the 40-hex Phase 48 commit that carries the fix; owner `-`; trigger `-`.
  closed  owner `fork maintainer`; a re-open trigger of at least 15 characters; pointer `-`, except the
          four closed rows whose reason rests on a commit (27-IN-01 and 43-WR-03: an exact earlier commit;
          45-IN-04 and 46-WR-05: the Phase 48 documentation commit).
Every reason has at least 40 characters. No cell of any row reads `(the user)`, and no owner names a user.
Some rows must carry a decision ID or a measured cost in their reason (MUST below).

Subcommands (exit 0 = holds, 1 = a check failed, 2 = usage or unreadable input):
  check LEDGER       exactly the 57 required IDs, one row each, each with its status and well formed
  counts LEDGER      print `rows=N fixed=F closed=C pointers=P`
  pointers LEDGER    print `ID<TAB>SHA<TAB>REQUIRED_PATH<TAB>SCOPE` for each row with a commit pointer;
                     SCOPE is `phase48` (a commit after the phase base) or `existing` (the exact commit)
  ids                print the required IDs and statuses, one `ID<TAB>STATUS` per line
"""
import re
import sys

SHA = re.compile(r"^[0-9a-f]{40}$")
MIN_TRIGGER = 15
MIN_REASON = 40
OWNER_CLOSED = "fork maintainer"
DASH = "-"

M = "src/main/java/com/clickhouse/kafka/connect/"
T = "src/test/java/com/clickhouse/kafka/connect/"
FD = "docs/FORK-DELTA.md"

# (id, status, pointer rule, required path, regexes the reason must match)
# pointer rule: "phase" = a Phase 48 commit; "exact:<sha>" = that commit; "none" = `-`
FIXED = [
    ("28-IN-01", "phase", M + "sink/db/mapping/DeclaredNullability.java", [r"\bU-4\b"]),
    ("28-IN-05(b)(i)", "phase", T + "sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java", [r"\bU-5\b"]),
    ("UF-37-3", "phase", T + "ci/AllowListWildcardRuleUnitTest.java", [r"\bU-9\b"]),
    ("40-IN-03", "phase", T + "sink/ProxySinkTaskStopUnitTest.java", []),
    ("42-IN-03(b)", "phase", T + "sink/UnvettedRetriableCodesWarnUnitTest.java", []),
    ("42-IN-04", "phase", T + "sink/RetriableExceptionPassThroughUnitTest.java", []),
    ("43-IN-01", "phase", FD, []),
    ("43-IN-02", "phase", "docs/ROLLOUT-v1.7.0_gg1.md", [r"\bU-6\b"]),
    ("44-IN-03", "phase", "docs/ROLLOUT-v1.7.0_gg6.md", [r"\bU-6\b", r"\bDOC-01\b"]),
    ("47-IN-01", "phase", T + "sink/ClickHouseSinkTaskStopUnitTest.java", []),
]
CLOSED_WITH_POINTER = [
    ("27-IN-01", "exact:f13d83781efd97589d8aec292c39faca4face820", M + "sink/db/ClickHouseWriter.java",
     [r"\bU-3\b", r"876a9bd1"]),
    ("43-WR-03", "exact:e979004e91a4f84fa9223b9d957ab3216fa37e00", FD, []),
    ("45-IN-04", "phase", FD, [r"\bU-8\b"]),
    ("46-WR-05", "phase", FD, [r"\bU-8\b"]),
]
U8 = "40-IN-01 40-IN-02 41-IN-01 41-IN-02 42-IN-01 42-IN-02 45-WR-03 45-IN-02 45-IN-03 46-IN-05 46-IN-06 47-IN-04".split()
CLOSED_PLAIN = (
    "26-IN-02 27-WR-01 28-IN-05(b)(ii) 37-IN-01 37-IN-02 37-IN-03 UF-IN-02 37-IN-05(b) UF-IN-01 UF-IN-03 "
    "30-IN-04 31-IN-02 31-IN-03(b) 25-WR-02(b) 25-WR-03 25-IN-02 25-IN-03 25-IN-04(a) 25-IN-04(c) 27-IN-02 "
    "27-IN-03 28-IN-02 28-IN-04(b) 39-IN-01 39-IN-02 39-IN-03 40-IN-01 40-IN-02 41-IN-01 41-IN-02 41-IN-03 "
    "42-IN-01 42-IN-02 42-IN-03(a) 45-WR-03 45-WR-04 45-IN-02 45-IN-03 45-IN-05 46-IN-05 46-IN-06 47-IN-02 47-IN-04"
).split()
MUST_PLAIN = {
    "26-IN-02": [r"\bU-1\b", r"\b15 compile errors\b"],
    "27-WR-01": [r"\bU-2\b", r"\b5 Docker-free tests\b"],
}

SPEC = {}
for _id, _rule, _path, _must in FIXED:
    SPEC[_id] = ("fixed", _rule, _path, _must)
for _id, _rule, _path, _must in CLOSED_WITH_POINTER:
    SPEC[_id] = ("closed", _rule, _path, _must)
for _id in CLOSED_PLAIN:
    _must = list(MUST_PLAIN.get(_id, []))
    if _id in U8:
        _must.append(r"\bU-8\b")
    SPEC[_id] = ("closed", "none", DASH, _must)
EXPECTED_ROWS = 57


def die(msg):
    print("USAGE ERROR: " + msg)
    sys.exit(2)


def read_rows(path):
    try:
        with open(path, encoding="utf-8") as fh:
            lines = fh.read().split("\n")
    except OSError as exc:
        die("cannot read %s: %s" % (path, exc))
    rows, bad = [], []
    for number, line in enumerate(lines, 1):
        if not line.startswith("| "):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if cells and (cells[0] == "ID" or cells[0].startswith("-")):
            continue
        if len(cells) != 6:
            bad.append("line %d: %d cells, expected 6 (a cell must not hold a pipe)" % (number, len(cells)))
            continue
        rows.append((number, cells))
    return rows, bad


def row_problems(number, cells):
    rid, status, pointer, owner, trigger, reason = cells
    out = []
    spec = SPEC.get(rid)
    if spec is None:
        return ["line %d: %s is not a required ID" % (number, rid)]
    want_status, rule, _path, must = spec
    if status != want_status:
        out.append("line %d: %s status %r, expected %r" % (number, rid, status, want_status))
    if rule == "none":
        if pointer != DASH:
            out.append("line %d: %s pointer must be '-'" % (number, rid))
    elif rule.startswith("exact:"):
        if pointer != rule[len("exact:"):]:
            out.append("line %d: %s pointer must be %s" % (number, rid, rule[len("exact:"):]))
    elif not SHA.match(pointer):
        out.append("line %d: %s pointer must be a 40-hex commit" % (number, rid))
    if want_status == "fixed":
        if owner != DASH or trigger != DASH:
            out.append("line %d: %s is fixed, owner and trigger must be '-'" % (number, rid))
    else:
        if owner != OWNER_CLOSED:
            out.append("line %d: %s owner must be %r" % (number, rid, OWNER_CLOSED))
        if trigger == DASH or len(trigger) < MIN_TRIGGER:
            out.append("line %d: %s needs a re-open trigger of at least %d characters" % (number, rid, MIN_TRIGGER))
    if len(reason) < MIN_REASON:
        out.append("line %d: %s reason shorter than %d characters" % (number, rid, MIN_REASON))
    for cell in cells:
        if "(the user)" in cell:
            out.append("line %d: %s carries '(the user)'" % (number, rid))
            break
    if re.search(r"\buser\b", owner, re.I):
        out.append("line %d: %s owner names a user" % (number, rid))
    for rx in must:
        if not re.search(rx, reason):
            out.append("line %d: %s reason must match %s" % (number, rid, rx))
    return out


def cmd_check(args):
    if len(args) != 1:
        die("check LEDGER")
    rows, bad = read_rows(args[0])
    problems = list(bad)
    seen = {}
    for number, cells in rows:
        seen.setdefault(cells[0], []).append(number)
        problems.extend(row_problems(number, cells))
    for rid, numbers in sorted(seen.items()):
        if len(numbers) > 1:
            problems.append("%s has %d rows (lines %s)" % (rid, len(numbers), ",".join(map(str, numbers))))
    for rid in sorted(SPEC):
        if rid not in seen:
            problems.append("%s has no row" % rid)
    if len(SPEC) != EXPECTED_ROWS:
        problems.append("internal: spec holds %d IDs, expected %d" % (len(SPEC), EXPECTED_ROWS))
    for p in problems:
        print("FAIL " + p)
    print("check: rows=%d required=%d problems=%d" % (len(rows), len(SPEC), len(problems)))
    return 1 if problems else 0


def cmd_counts(args):
    if len(args) != 1:
        die("counts LEDGER")
    rows, bad = read_rows(args[0])
    if bad:
        for b in bad:
            print("FAIL " + b)
        return 1
    fixed = sum(1 for _, c in rows if c[1] == "fixed")
    closed = sum(1 for _, c in rows if c[1] == "closed")
    pointers = sum(1 for _, c in rows if SHA.match(c[2]))
    print("rows=%d fixed=%d closed=%d pointers=%d" % (len(rows), fixed, closed, pointers))
    return 0


def cmd_pointers(args):
    if len(args) != 1:
        die("pointers LEDGER")
    rows, bad = read_rows(args[0])
    if bad:
        for b in bad:
            print("FAIL " + b)
        return 1
    rc = 0
    for _, cells in rows:
        if not SHA.match(cells[2]):
            continue
        spec = SPEC.get(cells[0])
        if spec is None:
            print("FAIL %s is not a required ID" % cells[0])
            rc = 1
            continue
        scope = "existing" if spec[1].startswith("exact:") else "phase48"
        print("%s\t%s\t%s\t%s" % (cells[0], cells[2], spec[2], scope))
    return rc


def cmd_ids(args):
    if args:
        die("ids")
    for rid in sorted(SPEC):
        print("%s\t%s" % (rid, SPEC[rid][0]))
    return 0


COMMANDS = {"check": cmd_check, "counts": cmd_counts, "pointers": cmd_pointers, "ids": cmd_ids}


def main(argv):
    if len(argv) < 2 or argv[1] not in COMMANDS:
        die("subcommand is one of: " + ", ".join(sorted(COMMANDS)))
    return COMMANDS[argv[1]](argv[2:])


if __name__ == "__main__":
    sys.exit(main(sys.argv))
