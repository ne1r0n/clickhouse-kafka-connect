#!/usr/bin/env python3
"""Read-only format gate for 43-DISPOSITION-LEDGER.md (DOC-02).

The ledger is a set of Markdown table rows with six cells:
  | ID | Status | Pointer | Owner | Re-open trigger | Note |
ID is a base ID such as 26-IN-02, 37-HDR, UF-37-3 or UF-IN-01, optionally followed by a split letter
such as (a).
Status is `fixed` or `dispositioned`.
A `fixed` row names its commit as a pointer of exactly 40 lowercase hex digits; a `dispositioned` row
has the pointer `-`, the owner `fork maintainer (the user)` and a real re-open trigger.
The gate checks the 17 base IDs of Phase 43: the header comment of increment-version.yml (37-HDR), the
five advisory rows that needed the user's approval, UF-37-3, the six Infos of 37-REVIEW.md, the three
of 37-REVIEW-UF.md and the re-owned 30-IN-04. `carried` checks the twelve carried lines of the ledger
against the Phase 37 ledger. It never writes a file and never runs git; the plan's shell resolves each
printed pointer with git. Run it with `python3 -I`.

Subcommands (exit 0 = holds, 1 = a check failed, 2 = usage or unreadable input):
  check LEDGER            every required base ID has a row, every row is well formed, no row repeats
  pointers LEDGER         print each distinct commit pointer, one per line
  counts LEDGER           print rows, bases and the status tallies
  status LEDGER ID...     print the statuses of the rows of each base ID (ID=status1,status2)
  pointer LEDGER ID       print the pointer of the fixed row whose full ID is ID; exit 1 if none
  carried LEDGER OLD      the twelve carried lines of LEDGER equal the dispositioned rows of OLD
"""
import re
import sys

REQUIRED = (
    "37-HDR 26-IN-02 27-WR-01 27-IN-01 28-IN-01 28-IN-05 UF-37-3 "
    "37-IN-01 37-IN-02 37-IN-03 37-IN-04 37-IN-05 37-IN-06 UF-IN-01 UF-IN-02 UF-IN-03 30-IN-04"
).split()
ROW_ID = re.compile(r"^(\d\d-(?:WR|IN)-\d\d|37-HDR|UF-37-3|UF-IN-\d\d)(\([a-z]\))?$")
POINTER = re.compile(r"^[0-9a-f]{40}$")
STATUSES = ("fixed", "dispositioned")
MIN_TRIGGER = 15
OWNER = "fork maintainer (the user)"
CARRIED = (
    "31-IN-02 31-IN-03(b) 25-WR-02(b) 25-WR-03 25-IN-02 25-IN-03 25-IN-04(a) 25-IN-04(c) "
    "27-IN-02 27-IN-03 28-IN-02 28-IN-04(b)"
).split()
OWNER_SEP = ": owner "
TRIGGER_SEP = "; re-open trigger: "


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
            if owner != OWNER:
                out.append("line %d: %s needs the owner %r, got %r" % (number, rid, OWNER, owner))
            if len(trigger) < MIN_TRIGGER or trigger == "-":
                out.append("line %d: %s needs a re-open trigger of at least %d characters" % (number, rid, MIN_TRIGGER))
        elif not POINTER.match(pointer):
            out.append("line %d: %s is %s and needs a commit pointer, got %r" % (number, rid, status, pointer))
    for rid in REQUIRED:
        if rid not in bases:
            out.append("required base ID %s has no row" % rid)
    for rid in sorted(bases - set(REQUIRED)):
        out.append("unknown base ID %s" % rid)
    return out


def read_carried_lines(path):
    """Lines of the ledger that start with `- `, an ID of CARRIED and `: owner `."""
    found = {}
    with open(path, encoding="utf-8") as fh:
        for line in fh.read().split("\n"):
            if not line.startswith("- "):
                continue
            for rid in CARRIED:
                head = "- " + rid + OWNER_SEP
                if line.startswith(head):
                    rest = line[len(head):]
                    owner, sep, trigger = rest.partition(TRIGGER_SEP)
                    found.setdefault(rid, []).append((owner.strip(), trigger.replace("`", "").strip(), bool(sep)))
    return found


def carried(path, rows, old_rows):
    found = read_carried_lines(path)
    out = []
    for rid in CARRIED:
        old = [c for _, c in old_rows if len(c) == 6 and c[0] == rid]
        if len(old) != 1 or old[0][1] != "dispositioned":
            out.append("%s: the old ledger needs exactly one dispositioned row" % rid)
            continue
        lines = found.get(rid, [])
        if len(lines) != 1:
            out.append("%s: the ledger needs exactly one carried line, found %d" % (rid, len(lines)))
            continue
        owner, trigger, has_sep = lines[0]
        if not has_sep:
            out.append("%s: the carried line has no %r" % (rid, TRIGGER_SEP.strip()))
        if owner != OWNER or owner != old[0][3]:
            out.append("%s: owner %r differs from %r and the old row's %r" % (rid, owner, OWNER, old[0][3]))
        if trigger != old[0][4]:
            out.append("%s: trigger differs from the old row" % rid)
    for item in out:
        print("FAIL " + item)
    if out:
        print("FAIL carried: %d problems" % len(out))
        return 1
    print("OK carried: %d rows, 0 problems" % len(CARRIED))
    return 0


def main(argv):
    if len(argv) < 2:
        die("check|pointers|counts|status|pointer|carried LEDGER [ID...|OLD_LEDGER]")
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
            if len(cells) == 6 and cells[1] == "fixed":
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
        print("rows=%d bases=%d fixed=%d dispositioned=%d" % (
            len(rows), len(bases), tally["fixed"], tally["dispositioned"]))
        return 0
    if cmd == "status":
        for rid in argv[2:]:
            got = [c[1] for _, c in rows if len(c) == 6 and ROW_ID.match(c[0]).group(1) == rid]
            print("%s=%s" % (rid, ",".join(got)))
        return 0
    if cmd == "pointer":
        if len(argv) != 3:
            die("pointer LEDGER ID")
        got = [c[2] for _, c in rows if len(c) == 6 and c[0] == argv[2] and c[1] == "fixed"]
        if len(got) != 1:
            return 1
        print(got[0])
        return 0
    if cmd == "carried":
        if len(argv) != 3:
            die("carried LEDGER OLD_LEDGER")
        return carried(path, rows, read_rows(argv[2]))
    die("unknown subcommand %r" % cmd)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
