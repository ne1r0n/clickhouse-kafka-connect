#!/usr/bin/env python3
"""Read-only hygiene gate for the files Phase 35 commits (evidence, records, tests, fixtures, docs).

No such file may name a deployment, a template repository path or any other system outside
this repository; every environment-specific value in a document is an angle-bracket value such as
<connector-name>. The search strings are assembled from fragments below, so this file does not
contain them either. It never writes a file and never runs git. Run it with `python3 -I`.

Usage: python3 -I 35-hygiene-check.py FILE...
Exit 0 = every file is clean, 1 = a file has a hit (printed as FILE:LINE, never the text),
2 = usage error or an unreadable file.
"""
import re
import sys

PATTERNS = [
    re.compile("click" + "house_mv" + "p", re.I),
    re.compile("ne" + "o/data", re.I),
    re.compile("jin" + "ja", re.I),
    re.compile("the repo" + "rter", re.I),
    re.compile(r"\bst" + "and" + r"\b", re.I),
]


def main(argv):
    if not argv:
        print("USAGE ERROR: 35-hygiene-check.py FILE...")
        return 2
    hits = 0
    for name in argv:
        try:
            with open(name, encoding="utf-8", errors="replace") as fh:
                lines = fh.read().split("\n")
        except OSError as exc:
            print("USAGE ERROR: cannot read %s: %s" % (name, exc))
            return 2
        for number, line in enumerate(lines, 1):
            if any(p.search(line) for p in PATTERNS):
                print("HYGIENE HIT %s:%d" % (name, number))
                hits += 1
    print("%s hygiene: %d files, %d hits" % ("OK" if hits == 0 else "FAIL", len(argv), hits))
    return 0 if hits == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
