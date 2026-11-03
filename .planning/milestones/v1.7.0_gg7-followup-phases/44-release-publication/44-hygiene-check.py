#!/usr/bin/env python3
"""Read-only hygiene gate for the files Phase 38 commits (evidence, records, handoffs, docs, helpers).

No such file may name a deployment, a template repository path or any other system outside this
repository; every environment-specific value in a document is an angle-bracket value such as
<connector-name>. The search strings are assembled from fragments below, so this file does not
contain them either. It never writes a file and never runs git. Run it with `python3 -I`.

This is the Phase 37 gate with two more identifier patterns that are always on, and a --strict mode.
It is therefore NOT a byte copy of the Phase 37 helper (the evidence helper still is).
  default  the five patterns of the Phase 37 helper plus two scratch-directory identifiers; use it on
           every file of a wide scan (it must stay clean on the whole planning tree of this milestone)
  --strict also two phrases that name an outside setup (built from fragments below); use it on the files
           Phase 38 creates and on the added lines of the documents it edits (the older planning files
           use those words in unrelated sentences, so a wide scan must not use --strict)

Usage: python3 -I 38-hygiene-check.py [--strict] FILE...
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
    re.compile("local_" + "kc_" + "test", re.I),
    re.compile("kc_" + "connect_" + "new", re.I),
]

STRICT_PATTERNS = [
    re.compile("produc" + "tion " + "environ" + "ment", re.I),
    re.compile(r"\brepro" + "duction" + r"\b", re.I),
]


def main(argv):
    strict = argv[:1] == ["--strict"]
    files = argv[1:] if strict else argv
    if not files:
        print("USAGE ERROR: 38-hygiene-check.py [--strict] FILE...")
        return 2
    patterns = PATTERNS + (STRICT_PATTERNS if strict else [])
    hits = 0
    for name in files:
        try:
            with open(name, encoding="utf-8", errors="replace") as fh:
                lines = fh.read().split("\n")
        except OSError as exc:
            print("USAGE ERROR: cannot read %s: %s" % (name, exc))
            return 2
        for number, line in enumerate(lines, 1):
            if any(p.search(line) for p in patterns):
                print("HYGIENE HIT %s:%d" % (name, number))
                hits += 1
    print("%s hygiene%s: %d files, %d hits" % (
        "OK" if hits == 0 else "FAIL", " (strict)" if strict else "", len(files), hits))
    return 0 if hits == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
