#!/usr/bin/env python3
"""Print the baseline release tag of a version: the greatest release tag that sorts below it.

Usage: python3 -I baseline_tag.py VERSION < TAGS

TAGS holds one tag name per line; the release workflows pass the tags reachable from the commit that
is built. Only names that fully match vX.Y.Z or vX.Y.Z_ggN (N >= 1, digits 0-9 only) count, so
rollback tags, branch-like tags and tags with a trailing word are ignored. Order is numeric on
(X, Y, Z, N), a name without the suffix sorting as N = 0. The baseline is the greatest tag that sorts
strictly below VERSION, so the tag of VERSION itself and any later tag are never chosen.

Exit codes: 0 the tag is the only line on stdout, 1 no tag sorts below VERSION, 2 usage error or a
VERSION that does not match the grammar. Standard library only.
"""
import re
import sys

RELEASE_TAG = re.compile(r"v([0-9]+)\.([0-9]+)\.([0-9]+)(?:_gg([1-9][0-9]*))?")
EXIT_NONE = 1
EXIT_USAGE = 2


def key(name):
    match = RELEASE_TAG.fullmatch(name)
    if match is None:
        return None
    major, minor, patch, gg = match.groups()
    return int(major), int(minor), int(patch), int(gg) if gg else 0


def baseline(version, tags):
    limit = key(version)
    if limit is None:
        raise ValueError("VERSION %r does not match vX.Y.Z or vX.Y.Z_ggN (N >= 1)" % version)
    below = [(key(tag), tag) for tag in tags if key(tag) is not None and key(tag) < limit]
    return max(below)[1] if below else None


def main(argv):
    if len(argv) != 1:
        print("usage: baseline_tag.py VERSION < TAGS", file=sys.stderr)
        return EXIT_USAGE
    tags = [line.rstrip("\r\n") for line in sys.stdin]
    try:
        found = baseline(argv[0], tags)
    except ValueError as exc:
        print("ERROR: %s" % exc, file=sys.stderr)
        return EXIT_USAGE
    if found is None:
        print("ERROR: no release tag sorts below %s among %d tag names" % (argv[0], len(tags)), file=sys.stderr)
        return EXIT_NONE
    print(found)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
