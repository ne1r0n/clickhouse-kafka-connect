#!/usr/bin/env python3
"""Per-testcase status helper for Phase 42 evidence (42-COMMAND-EVIDENCE.md) over JUnit XML.

Read-only except for the OUT file a subcommand is told to write; it never runs git. Run it with
`python3 -I`. A status dump is a sorted list of `classname TAB name TAB STATUS` lines, STATUS being
FAIL (a failure or error child), SKIP (a skipped child) or PASS.

Subcommands (exit 0 = success, 1 = a check failed, 2 = usage or unreadable input):
  dump DIR OUT                 one line per testcase of DIR/TEST-*.xml, sorted by UTF-8 bytes
  select OUT STATUS DUMP SUFFIX [DUMP SUFFIX ...]
                               union of the lines of each DUMP whose classname ends with its
                               SUFFIX; the status is replaced by STATUS unless STATUS is `keep`
  delta BEFORE AFTER EXPECTED [--by-class]
                               BEFORE, AFTER and EXPECTED as multisets of lines (with --by-class
                               each line collapses to `classname TAB STATUS` first); removed =
                               BEFORE minus AFTER, added = AFTER minus BEFORE; exit 0 only when
                               nothing is removed and added equals EXPECTED
  count DUMP SUFFIX STATUS [--name-contains TEXT] [--name-lacks TEXT]
                               prints count=N: lines whose classname ends with SUFFIX, whose status
                               equals STATUS and whose name passes the filters
"""
import collections
import glob
import re
import sys
import xml.etree.ElementTree as ET

LIST_LIMIT = 20
WHITESPACE = re.compile(r"[\t\r\n]+")


def die(msg):
    print("USAGE ERROR: " + msg)
    sys.exit(2)


def clean(text):
    return WHITESPACE.sub(" ", text or "")


def read_lines(path):
    try:
        with open(path, encoding="utf-8") as fh:
            return [line for line in fh.read().split("\n") if line != ""]
    except OSError as exc:
        die("cannot read %s: %s" % (path, exc))


def write_lines(path, lines):
    ordered = sorted(lines, key=lambda line: line.encode("utf-8"))
    try:
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            for line in ordered:
                fh.write(line + "\n")
    except OSError as exc:
        die("cannot write %s: %s" % (path, exc))


def status_of(case):
    if case.find("failure") is not None or case.find("error") is not None:
        return "FAIL"
    if case.find("skipped") is not None:
        return "SKIP"
    return "PASS"


def cmd_dump(args):
    if len(args) != 2:
        die("dump DIR OUT")
    files = sorted(glob.glob(args[0] + "/TEST-*.xml"))
    if not files:
        print("FAIL dump: no TEST-*.xml matched in " + args[0])
        return 2
    lines = []
    for path in files:
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError) as exc:
            print("FAIL dump: %s does not parse: %s" % (path, exc))
            return 2
        for case in root.iter("testcase"):
            lines.append("%s\t%s\t%s" % (clean(case.get("classname")), clean(case.get("name")), status_of(case)))
    write_lines(args[1], lines)
    print("dump %s: testcases=%d" % (args[1], len(lines)))
    return 0


def cmd_select(args):
    if len(args) < 4 or (len(args) - 2) % 2 != 0:
        die("select OUT STATUS DUMP SUFFIX [DUMP SUFFIX ...]")
    out, status = args[0], args[1]
    picked = []
    for dump, suffix in zip(args[2::2], args[3::2]):
        for line in read_lines(dump):
            parts = line.split("\t")
            if len(parts) != 3:
                die("malformed line in %s" % dump)
            if parts[0].endswith(suffix):
                picked.append("%s\t%s\t%s" % (parts[0], parts[1], parts[2] if status == "keep" else status))
    write_lines(out, picked)
    print("select %s: rows=%d" % (out, len(picked)))
    return 0 if picked else 1


def collapse(line):
    parts = line.split("\t")
    if len(parts) != 3:
        die("malformed line: %r" % line)
    return "%s\t%s" % (parts[0], parts[2])


def cmd_delta(args):
    by_class = "--by-class" in args
    rest = [a for a in args if a != "--by-class"]
    if len(rest) != 3:
        die("delta BEFORE AFTER EXPECTED [--by-class]")
    shape = collapse if by_class else (lambda line: line)
    before, after, expected = (collections.Counter(shape(l) for l in read_lines(p)) for p in rest)
    removed = before - after
    added = after - before
    unexpected = added - expected
    missing = expected - added
    ok = not removed and not unexpected and not missing
    print("delta: removed=%d added=%d expected=%d match=%s" % (
        sum(removed.values()), sum(added.values()), sum(expected.values()), "yes" if ok else "no"))
    for tag, bag in (("removed", removed), ("unexpected", unexpected), ("missing", missing)):
        for line in sorted(bag.elements())[:LIST_LIMIT]:
            print("%s: %s" % (tag, line.replace("\t", " | ")))
    return 0 if ok else 1


def cmd_count(args):
    contains, lacks, rest, i = None, None, [], 0
    while i < len(args):
        if args[i] in ("--name-contains", "--name-lacks"):
            if i + 1 >= len(args):
                die(args[i] + " needs a value")
            if args[i] == "--name-contains":
                contains = args[i + 1]
            else:
                lacks = args[i + 1]
            i += 2
        else:
            rest.append(args[i])
            i += 1
    if len(rest) != 3:
        die("count DUMP SUFFIX STATUS [--name-contains TEXT] [--name-lacks TEXT]")
    dump, suffix, status = rest
    n = 0
    for line in read_lines(dump):
        cls, name, st = line.split("\t")
        if cls.endswith(suffix) and st == status and (contains is None or contains in name) \
                and (lacks is None or lacks not in name):
            n += 1
    print("count=%d" % n)
    return 0


COMMANDS = {"dump": cmd_dump, "select": cmd_select, "delta": cmd_delta, "count": cmd_count}


def main(argv):
    if len(argv) < 2 or argv[1] not in COMMANDS:
        die("subcommand is one of: " + ", ".join(sorted(COMMANDS)))
    return COMMANDS[argv[1]](argv[2:])


if __name__ == "__main__":
    sys.exit(main(sys.argv))
