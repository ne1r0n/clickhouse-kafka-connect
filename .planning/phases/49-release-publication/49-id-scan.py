"""Planning-identifier scan of a published document (Phase 48, DOC-01).

Usage: python3 -I 48-id-scan.py REV DOC

Reads DOC at the commit or tag REV with `git show REV:DOC` and prints one line
`ID <line> <match>` for every planning identifier in it, in line order: a
.planning path, `Phase <n>`, a requirement or review identifier such as
AB-12, FUT-<n>, a single-letter decision or evidence identifier such as D-3 or
E-01, or a NN-REVIEW, NN-RESEARCH or NN-COMMAND-EVIDENCE file name. The last
line is `ids: hits=N`. It writes no file. Exit 0 when N is 0, 1 otherwise, 2 on
a usage or git error.

Frozen after E-02 of the Phase 48 evidence: later gates run the same bytes.
"""
import re
import subprocess
import sys

PATTERN = re.compile(
    r"\.planning|\bPhase [0-9]+|\b[A-Z]{2,6}-[0-9]{2}\b|\bFUT-[0-9]+|\b[DLREFT]-[0-9]{1,3}\b"
    r"|[0-9]{2}-(?:REVIEW|RESEARCH|COMMAND-EVIDENCE)"
)
EXIT_USAGE = 2


def main(argv):
    if len(argv) != 2:
        sys.stderr.write("usage: 48-id-scan.py REV DOC\n")
        return EXIT_USAGE
    rev, doc = argv
    try:
        done = subprocess.run(["git", "show", "%s:%s" % (rev, doc)], capture_output=True, check=True)
    except (OSError, subprocess.CalledProcessError) as err:
        sys.stderr.write("git show %s:%s failed: %s\n" % (rev, doc, err))
        return EXIT_USAGE
    hits = 0
    for number, line in enumerate(done.stdout.decode("utf-8").split("\n"), start=1):
        for match in PATTERN.finditer(line):
            print("ID %d %s" % (number, match.group(0)))
            hits += 1
    print("ids: hits=%d" % hits)
    return 0 if hits == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
