#!/usr/bin/env python3
"""Read-only format gate for 37-ADVISORY-LEDGER.md (ADV-01, ADV-02).

The ledger is a set of Markdown table rows with six cells:
  | ID | Status | Pointer | Owner | Re-open trigger | Note |
ID is a base ID such as 30-WR-01 or 28-IN-04, optionally followed by a split letter such as (a).
Status is `fixed`, `closed` (closed by other work of this phase or an earlier one) or `dispositioned`.
A `fixed` or `closed` row names its commit as a pointer of 7 to 40 lowercase hex digits (several are
comma-separated); a `dispositioned` row has the pointer `-` and a real owner and re-open trigger.
The gate checks the 29 base IDs of ADV-01 and ADV-02: the 7 rows of Phase 30, the 6 of Phase 31 and
the 16 unfixed findings of the Phase 29 close record. It never writes a file and never runs git; the
plan's shell resolves each printed pointer with `git cat-file`. Run it with `python3 -I`.

Subcommands (exit 0 = holds, 1 = a check failed, 2 = usage or unreadable input):
  check LEDGER            every required base ID has a row, every row is well formed, no row repeats
  pointers LEDGER         print each distinct commit pointer, one per line
  counts LEDGER           print rows, bases and the status tallies
  status LEDGER ID...     print the statuses of the rows of each base ID (ID=status1,status2)
"""
import re
import sys

REQUIRED = (
    "30-WR-01 30-WR-02 30-WR-03 30-IN-01 30-IN-02 30-IN-03 30-IN-04 "
    "31-WR-01 31-WR-02 31-WR-03 31-IN-01 31-IN-02 31-IN-03 "
    "25-WR-01 25-WR-02 25-WR-03 25-IN-01 25-IN-02 25-IN-03 25-IN-04 26-IN-02 "
    "27-WR-01 27-IN-01 27-IN-02 27-IN-03 28-IN-01 28-IN-02 28-IN-04 28-IN-05"
).split()
ROW_ID = re.compile(r"^(\d\d-(?:WR|IN)-\d\d)(\([a-z]\))?$")
POINTER = re.compile(r"^[0-9a-f]{7,40}(,\s*[0-9a-f]{7,40})*$")
STATUSES = ("fixed", "closed", "dispositioned")
MIN_TRIGGER = 15


def die(msg):
    print("USAGE ERROR: " + msg)
    sys.exit(2)


def read_rows(path):
    try:
        with open(path, encoding="utf-8") as fh:
            lines = fh.read().split("\n")
    except OSError as exc:
        die("cannot read %s: %s" % (path, exc))
    rows = []
    for number, line in enumerate(lines, 1):
        if not line.startswith("| "):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if cells and ROW_ID.match(cells[0].replace("`", "")):
            rows.append((number, [c.replace("`", "") for c in cells]))
    return rows


def problems(rows):
    out = []
    seen = set()
    bases = set()
    for number, cells in rows:
        if len(cells) != 6:
            out.append("line %d: %d cells, expected 6" % (number, len(cells)))
            continue
        rid, status, pointer, owner, trigger = cells[:5]
        if rid in seen:
            out.append("line %d: %s repeats" % (number, rid))
        seen.add(rid)
        bases.add(ROW_ID.match(rid).group(1))
        if status not in STATUSES:
            out.append("line %d: %s status %r is not one of %s" % (number, rid, status, STATUSES))
        elif status == "dispositioned":
            if pointer != "-":
                out.append("line %d: %s is dispositioned and must have the pointer -" % (number, rid))
            if owner in ("", "-") or len(trigger) < MIN_TRIGGER or trigger == "-":
                out.append("line %d: %s needs an owner and a re-open trigger of at least %d characters" % (number, rid, MIN_TRIGGER))
        elif not POINTER.match(pointer):
            out.append("line %d: %s is %s and needs a commit pointer, got %r" % (number, rid, status, pointer))
    for rid in REQUIRED:
        if rid not in bases:
            out.append("required base ID %s has no row" % rid)
    for rid in sorted(bases - set(REQUIRED)):
        out.append("unknown base ID %s" % rid)
    return out


def main(argv):
    if len(argv) < 2:
        die("check|pointers|counts|status LEDGER [ID...]")
    cmd, path = argv[0], argv[1]
    rows = read_rows(path)
    if cmd == "check":
        found = problems(rows)
        for item in found:
            print("FAIL " + item)
        print("%s ledger: %d rows, %d problems" % ("OK" if not found else "FAIL", len(rows), len(found)))
        return 0 if not found else 1
    if cmd == "pointers":
        pointers = []
        for _, cells in rows:
            if len(cells) == 6 and cells[1] in ("fixed", "closed"):
                for p in cells[2].split(","):
                    if p.strip() and p.strip() not in pointers:
                        pointers.append(p.strip())
        print("\n".join(pointers))
        return 0
    if cmd == "counts":
        tally = {s: 0 for s in STATUSES}
        for _, cells in rows:
            if len(cells) == 6 and cells[1] in tally:
                tally[cells[1]] += 1
        bases = {ROW_ID.match(c[0]).group(1) for _, c in rows}
        print("rows=%d bases=%d fixed=%d closed=%d dispositioned=%d" % (
            len(rows), len(bases), tally["fixed"], tally["closed"], tally["dispositioned"]))
        return 0
    if cmd == "status":
        for rid in argv[2:]:
            got = [c[1] for _, c in rows if len(c) == 6 and ROW_ID.match(c[0]).group(1) == rid]
            print("%s=%s" % (rid, ",".join(got)))
        return 0
    die("unknown subcommand %r" % cmd)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
