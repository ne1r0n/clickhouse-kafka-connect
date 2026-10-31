#!/usr/bin/env python3
"""Compare two Confluent archives entry by entry (BLD-15, D-05).

Usage: python3 -I compare-confluent-archives.py A.zip B.zip ALLOWLIST

A is the baseline archive (v1.7.0_gg3), B the archive under test. Each archive
must hold exactly one lib/*-confluent.jar; that inner jar is read in memory and
every file entry is compared by SHA-256. The other outer entries are compared
too (manifest.json ignoring its release_date and version lines, everything else
by SHA-256) under the path prefix "outer/".

ALLOWLIST rows are "glob<TAB>reason"; "#" lines and blank lines are skipped, a
reason is required, and a star matches across slashes (fnmatch). A difference no
row matches is UNALLOWED; a row that matched nothing is STALE-ROW. Both fail.

Exit codes: 0 PASS, 1 FAIL, 2 unusable input (missing, duplicated or empty inner
jar, unreadable archive, malformed allow-list).

Standard library only. Nothing is extracted to disk or executed.
"""

import difflib
import fnmatch
import hashlib
import io
import sys
import zipfile

INNER_SUFFIX = "-confluent.jar"
INNER_DIR = "/lib/"
OUTER_PREFIX = "outer/"
MANIFEST_NAME = "META-INF/MANIFEST.MF"
POM_PREFIX = "META-INF/maven/"
POM_SUFFIX = "/pom.properties"
KAFKA_PREFIX = "org/apache/kafka/"
OUTER_MANIFEST = "manifest.json"
MANIFEST_IGNORED_FRAGMENTS = ('"release_date"', '"version"')
VERSION_LINE = "Implementation-Version:"


class InputError(Exception):
    pass


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def normalized_manifest_json(data):
    lines = data.decode("utf-8", errors="replace").splitlines()
    kept = [ln for ln in lines if not any(f in ln for f in MANIFEST_IGNORED_FRAGMENTS)]
    return sha256("\n".join(kept).encode("utf-8"))


def load_inner(path):
    """Return the inner jar entries and the outer entries of one archive."""
    try:
        outer = zipfile.ZipFile(path)
    except (OSError, zipfile.BadZipFile) as e:
        raise InputError("%s: not a readable zip (%s)" % (path, e))
    with outer:
        names = [n for n in outer.namelist() if not n.endswith("/")]
        inner_names = [n for n in names if INNER_DIR in n and n.endswith(INNER_SUFFIX)]
        if len(inner_names) != 1:
            raise InputError(
                "%s: expected exactly one lib/*%s, found %d" % (path, INNER_SUFFIX, len(inner_names))
            )
        inner_name = inner_names[0]
        top = inner_name.split("/", 1)[0] + "/"
        outer_entries = {}
        for n in names:
            if n == inner_name:
                continue
            rel = n[len(top):] if n.startswith(top) else n
            data = outer.read(n)
            outer_entries[OUTER_PREFIX + rel] = (
                normalized_manifest_json(data) if rel == OUTER_MANIFEST else sha256(data)
            )
        try:
            inner = zipfile.ZipFile(io.BytesIO(outer.read(inner_name)))
        except zipfile.BadZipFile as e:
            raise InputError("%s: inner jar is not a readable zip (%s)" % (path, e))
    with inner:
        entries = {}
        poms = {}
        manifest = b""
        for info in inner.infolist():
            if info.is_dir():
                continue
            data = inner.read(info.filename)
            entries[info.filename] = sha256(data)
            if info.filename == MANIFEST_NAME:
                manifest = data
            if info.filename.startswith(POM_PREFIX) and info.filename.endswith(POM_SUFFIX):
                poms[info.filename] = data
    if not entries:
        raise InputError("%s: inner jar has zero file entries" % path)
    return {"entries": entries, "outer": outer_entries, "poms": poms, "manifest": manifest}


def load_allowlist(path):
    rows = []
    try:
        with open(path, encoding="utf-8") as f:
            for number, line in enumerate(f, 1):
                line = line.rstrip("\n")
                if not line.strip() or line.startswith("#"):
                    continue
                parts = line.split("\t")
                if len(parts) != 2 or not parts[0].strip() or not parts[1].strip():
                    raise InputError("%s:%d: a row needs glob<TAB>reason with a non-empty reason" % (path, number))
                rows.append((parts[0], parts[1]))
    except OSError as e:
        raise InputError("%s: %s" % (path, e))
    return rows


def pom_versions(poms):
    out = {}
    for data in poms.values():
        props = {}
        for line in data.decode("utf-8", errors="replace").splitlines():
            if "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                props[k.strip()] = v.strip()
        if "groupId" in props and "artifactId" in props:
            out[props["groupId"] + ":" + props["artifactId"]] = props.get("version", "?")
    return out


def manifest_lines(data):
    return data.decode("utf-8", errors="replace").splitlines()


def compare(a, b, rows):
    """Return (report lines, passed)."""
    diffs = []
    keys_a = {**a["entries"], **a["outer"]}
    keys_b = {**b["entries"], **b["outer"]}
    for path in sorted(set(keys_a) | set(keys_b)):
        sa, sb = keys_a.get(path), keys_b.get(path)
        if sa == sb:
            continue
        if sb is None:
            diffs.append((path, "ONLY-A", "ONLY-A %s %s" % (path, sa)))
        elif sa is None:
            diffs.append((path, "ONLY-B", "ONLY-B %s %s" % (path, sb)))
        else:
            diffs.append((path, "CHANGED", "CHANGED %s %s %s" % (path, sa, sb)))
    diffs.sort(key=lambda d: (d[0], d[1]))
    lines = [d[2] for d in diffs]
    used = set()
    unallowed = []
    for path, kind, _ in diffs:
        matched = [i for i, (glob, _) in enumerate(rows) if fnmatch.fnmatchcase(path, glob)]
        used.update(matched)
        if not matched:
            unallowed.append("UNALLOWED %s %s" % (kind, path))
    stale = ["STALE-ROW %s" % rows[i][0] for i in range(len(rows)) if i not in used]
    lines += unallowed + stale

    va, vb = pom_versions(a["poms"]), pom_versions(b["poms"])
    for ga in sorted(set(va) | set(vb)):
        lines.append("VERSION %s %s -> %s" % (ga, va.get(ga, "-"), vb.get(ga, "-")))

    ma, mb = manifest_lines(a["manifest"]), manifest_lines(b["manifest"])
    manifest_ok = True
    if ma == mb:
        lines.append("MANIFEST.MF: identical")
    else:
        strip = lambda m: [ln for ln in m if not ln.startswith(VERSION_LINE)]
        if strip(ma) == strip(mb):
            lines.append("MANIFEST.MF: differs only in Implementation-Version")
        else:
            manifest_ok = False
            lines.append("MANIFEST.MF: differs")
            lines += list(difflib.unified_diff(ma, mb, "a/MANIFEST.MF", "b/MANIFEST.MF", lineterm=""))

    ea, eb = a["entries"], b["entries"]
    only_a = sum(1 for p in ea if p not in eb)
    only_b = sum(1 for p in eb if p not in ea)
    changed = sum(1 for p in ea if p in eb and ea[p] != eb[p])
    same = sum(1 for p in ea if p in eb and ea[p] == eb[p])
    nested = sum(1 for p in eb if p.endswith(".jar"))
    kafka = sum(1 for p in eb if p.startswith(KAFKA_PREFIX))
    lines.append(
        "totals files-a %d files-b %d only-a %d only-b %d changed %d same %d"
        % (len(ea), len(eb), only_a, only_b, changed, same)
    )
    lines.append("nested jars in b: %d" % nested)
    lines.append("kafka classes in b: %d" % kafka)
    passed = nested == 0 and not unallowed and not stale and manifest_ok
    lines.append("RESULT: PASS" if passed else "RESULT: FAIL")
    return lines, passed


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    if len(argv) != 3:
        print("usage: compare-confluent-archives.py A.zip B.zip ALLOWLIST", file=sys.stderr)
        return 2
    try:
        rows = load_allowlist(argv[2])
        a = load_inner(argv[0])
        b = load_inner(argv[1])
    except InputError as e:
        print("ERROR: %s" % e, file=sys.stderr)
        return 2
    lines, passed = compare(a, b, rows)
    print("\n".join(lines))
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
