#!/usr/bin/env python3
"""Read-only gate helper for the gg6 archive allow-list (plan 38-04). Run it with `python3 -I`.

It never writes a file and never runs git. The real check of an allow-list is
.github/scripts/compare_confluent_archives.py (exit 0, no UNALLOWED line, no STALE-ROW); this helper
adds the checks that script does not print: that every difference is named by exactly one row (no
overlap), the format rules of the allow-list loader, and a comparison of two glob sets.

Subcommands (exit 0 = every check holds, 1 = a check failed, 2 = usage or unreadable input):
  rows LIST [--expect N]      every non-comment, non-blank line is `glob TAB reason`; the reason is not
                              empty; the glob is not only stars and does not start with *, ? or [; no glob
                              repeats; prints `rows=N` (and fails when N differs from --expect)
  cover EMPTY_OUT LIST        EMPTY_OUT is the output of the compare run with a comment-only list. Every
                              `UNALLOWED <KIND> <path>` line is matched by exactly one row of LIST (fnmatch
                              semantics of the compare script: fnmatchcase, a star crosses slashes) and every
                              row matches at least one difference; prints
                              `differences=N covered=N uncovered=U overlapping=O stale_rows=S`
  same-globs A B              the glob sets of two lists are equal; prints the globs only in A or only in B
"""
import fnmatch
import re
import sys

WILDCARD_STARTS = "*?["
UNALLOWED = re.compile(r"^UNALLOWED (\S+) (.*)$")


def die(msg):
    print("USAGE ERROR: " + msg)
    sys.exit(2)


def read(path):
    try:
        with open(path, encoding="utf-8") as fh:
            return fh.read()
    except OSError as exc:
        die("cannot read %s: %s" % (path, exc))


def load_rows(path):
    """Return (rows, problems): rows are (glob, reason) in file order."""
    rows, problems = [], []
    for number, line in enumerate(read(path).split("\n"), 1):
        if line == "" or line.startswith("#"):
            continue
        parts = line.split("\t", 1)
        if len(parts) != 2 or not parts[1].strip():
            problems.append("line %d: not `glob TAB reason` with a non-empty reason" % number)
            continue
        glob = parts[0]
        if glob == "" or re.fullmatch(r"\*+", glob) or glob[0] in WILDCARD_STARTS:
            problems.append("line %d: glob %r is a catch-all or starts with a wildcard" % (number, glob))
            continue
        rows.append((glob, parts[1]))
    seen = set()
    for glob, _ in rows:
        if glob in seen:
            problems.append("glob %r repeats" % glob)
        seen.add(glob)
    return rows, problems


def cmd_rows(args):
    if len(args) not in (1, 3) or (len(args) == 3 and args[1] != "--expect"):
        die("rows LIST [--expect N]")
    rows, problems = load_rows(args[0])
    if len(args) == 3 and len(rows) != int(args[2]):
        problems.append("rows=%d, expected %s" % (len(rows), args[2]))
    for p in problems:
        print("FAIL rows: " + p)
    print("%s rows=%d" % ("OK" if not problems else "FAIL", len(rows)))
    return 0 if not problems else 1


def cmd_cover(args):
    if len(args) != 2:
        die("cover EMPTY_OUT LIST")
    paths = []
    for line in read(args[0]).split("\n"):
        m = UNALLOWED.match(line)
        if m:
            paths.append(m.group(2))
    rows, problems = load_rows(args[1])
    if problems:
        for p in problems:
            print("FAIL rows: " + p)
        return 1
    uncovered = overlapping = 0
    used = set()
    for path in paths:
        hits = [i for i, (glob, _) in enumerate(rows) if fnmatch.fnmatchcase(path, glob)]
        used.update(hits)
        if not hits:
            uncovered += 1
            print("UNCOVERED " + path)
        elif len(hits) > 1:
            overlapping += 1
            print("OVERLAP %s: %s" % (path, "; ".join(rows[i][0] for i in hits)))
    stale = [rows[i][0] for i in range(len(rows)) if i not in used]
    for glob in stale:
        print("STALE-ROW " + glob)
    bad = uncovered + overlapping + len(stale) + (0 if paths else 1)
    if not paths:
        print("FAIL cover: no UNALLOWED line in %s (a vacuous pass is refused)" % args[0])
    print("%s differences=%d covered=%d uncovered=%d overlapping=%d stale_rows=%d" % (
        "OK" if bad == 0 else "FAIL", len(paths), len(paths) - uncovered, uncovered, overlapping, len(stale)))
    return 0 if bad == 0 else 1


def cmd_same_globs(args):
    if len(args) != 2:
        die("same-globs A B")
    a = {g for g, _ in load_rows(args[0])[0]}
    b = {g for g, _ in load_rows(args[1])[0]}
    for g in sorted(a - b):
        print("ONLY-IN-A " + g)
    for g in sorted(b - a):
        print("ONLY-IN-B " + g)
    print("%s same-globs: a=%d b=%d only_a=%d only_b=%d" % (
        "OK" if a == b else "FAIL", len(a), len(b), len(a - b), len(b - a)))
    return 0 if a == b else 1


COMMANDS = {"rows": cmd_rows, "cover": cmd_cover, "same-globs": cmd_same_globs}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        die("subcommand is one of " + ", ".join(sorted(COMMANDS)))
    sys.exit(COMMANDS[sys.argv[1]](sys.argv[2:]))
