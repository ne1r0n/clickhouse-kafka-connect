#!/usr/bin/env python3
"""Compute the next fork version and write it to VERSION and to the VERSION row of fork-anchors.txt.

Usage: python3 -I bump_version.py BUMP [--version-file PATH] [--anchors-file PATH]

BUMP is one of gg, patch, minor, major. The version grammar is vX.Y.Z or vX.Y.Z_ggN with N >= 1, digits
0-9 only, matched over the whole text. `gg` raises N by one and turns a suffix-less vX.Y.Z into
vX.Y.Z_gg1; patch, minor and major raise that field, zero the fields to its right and set the suffix to
_gg1. Numbers are written without leading zeros. The new version is validated against the same grammar.

The anchors file must hold exactly one non-comment row whose first TAB field is VERSION, with four
fields, a pattern field equal to the current version and the count 1; that row's pattern becomes the new
version. Both new contents are computed and staged before either file is replaced, so any failure leaves
VERSION and the anchors file as they were. On success the new version is the only line on stdout.

Exit codes: 0 written, 1 refused (invalid version, anchors row missing or not matching, unreadable
file), 2 usage error. Standard library only.
"""
import argparse
import os
import re
import sys
import tempfile

VERSION_RE = re.compile(r"v([0-9]+)\.([0-9]+)\.([0-9]+)(?:_gg([1-9][0-9]*))?")
BUMPS = ("gg", "patch", "minor", "major")
DEFAULT_VERSION_FILE = "VERSION"
DEFAULT_ANCHORS_FILE = os.path.join(".planning", "scripts", "fork-anchors.txt")
ANCHOR_PATH_FIELD = "VERSION"
ANCHOR_COUNT_FIELD = "1"
ANCHOR_FIELDS = 4
EXIT_REFUSED = 1


class BumpError(Exception):
    pass


def parse(version, label):
    match = VERSION_RE.fullmatch(version)
    if match is None:
        raise BumpError("%s %r does not match vX.Y.Z or vX.Y.Z_ggN (N >= 1)" % (label, version))
    major, minor, patch, gg = match.groups()
    return int(major), int(minor), int(patch), int(gg) if gg else 0


def bump(old, kind):
    major, minor, patch, gg = parse(old, "current version")
    if kind == "gg":
        gg += 1
    elif kind == "patch":
        patch, gg = patch + 1, 1
    elif kind == "minor":
        minor, patch, gg = minor + 1, 0, 1
    elif kind == "major":
        major, minor, patch, gg = major + 1, 0, 0, 1
    else:
        raise BumpError("unknown bump %r" % kind)
    new = "v%d.%d.%d_gg%d" % (major, minor, patch, gg)
    parse(new, "new version")
    return new


def read_text(path):
    try:
        with open(path, encoding="utf-8", newline="") as handle:
            return handle.read()
    except (OSError, UnicodeDecodeError) as exc:
        raise BumpError("cannot read %s: %s" % (path, exc))


def version_of(text):
    return text[:-1] if text.endswith("\n") else text


def rewrite_anchor_row(anchors_text, old, new):
    lines = anchors_text.split("\n")
    rows = [
        index
        for index, line in enumerate(lines)
        if line and not line.startswith("#") and line.split("\t")[0] == ANCHOR_PATH_FIELD
    ]
    if len(rows) != 1:
        raise BumpError(
            "the anchors file must hold exactly one %s row, found %d" % (ANCHOR_PATH_FIELD, len(rows))
        )
    fields = lines[rows[0]].split("\t")
    if len(fields) != ANCHOR_FIELDS or fields[2] != ANCHOR_COUNT_FIELD:
        raise BumpError("the %s row must have %d fields and the count %s" % (ANCHOR_PATH_FIELD, ANCHOR_FIELDS, ANCHOR_COUNT_FIELD))
    if fields[1] != old:
        raise BumpError(
            "the %s row of the anchors file holds %r but %s holds %r; fix the row first"
            % (ANCHOR_PATH_FIELD, fields[1], ANCHOR_PATH_FIELD, old)
        )
    fields[1] = new
    lines[rows[0]] = "\t".join(fields)
    return "\n".join(lines)


def write_both(first, second):
    """Stage both (path, text) pairs next to their targets, then replace them."""
    staged = []
    try:
        for path, text in (first, second):
            fd, tmp = tempfile.mkstemp(dir=os.path.dirname(os.path.abspath(path)), prefix=".bump-")
            staged.append((tmp, path))
            with os.fdopen(fd, "w", encoding="utf-8", newline="") as handle:
                handle.write(text)
            os.chmod(tmp, os.stat(path).st_mode & 0o777)
        for tmp, path in staged:
            os.replace(tmp, path)
    except OSError as exc:
        for tmp, _ in staged:
            try:
                os.remove(tmp)
            except OSError:
                pass
        raise BumpError("cannot write the new version: %s" % exc)


def run(kind, version_file, anchors_file):
    old = version_of(read_text(version_file))
    new = bump(old, kind)
    anchors = rewrite_anchor_row(read_text(anchors_file), old, new)
    write_both((anchors_file, anchors), (version_file, new + "\n"))
    return new


def main(argv):
    parser = argparse.ArgumentParser(prog="bump_version.py", description=__doc__.split("\n")[0])
    parser.add_argument("bump", choices=BUMPS)
    parser.add_argument("--version-file", default=DEFAULT_VERSION_FILE)
    parser.add_argument("--anchors-file", default=DEFAULT_ANCHORS_FILE)
    args = parser.parse_args(argv)
    try:
        print(run(args.bump, args.version_file, args.anchors_file))
    except BumpError as exc:
        print("ERROR: %s" % exc, file=sys.stderr)
        return EXIT_REFUSED
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
