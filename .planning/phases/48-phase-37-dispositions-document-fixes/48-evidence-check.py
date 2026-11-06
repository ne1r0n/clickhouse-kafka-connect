#!/usr/bin/env python3
"""Read-only gate helper for Phase 34 evidence (34-COMMAND-EVIDENCE.md) and JUnit XML.

Written at plan time so the plan's <verify> blocks stay short and auditable instead of carrying
long inline Python. It never writes a file and never runs git; git cross-checks stay in the
plans' shell commands. Run it with `python3 -I`.

Evidence layout it reads (the Phase 33 entry format): an entry starts at a line `### E-NN: ...`
and runs to the next `### E-` heading or the end of the file. Facts are `key: value` lines inside
the entry's ```text fences only (keys match [a-z0-9_]+). The verdict is the entry's last
`Verdict: WORD` line. The index is the table rows that start with `| E-NN |`.

Subcommands (exit 0 = every check holds, 1 = a check failed, 2 = usage or unreadable input):
  get FILE E-NN KEY            print the value of KEY in entry E-NN
  expect FILE E-NN EXPR...     check facts of one entry; EXPR is one of
                                 key=value   exact value
                                 key~text    value contains text
                                 key^text    value starts with text
                                 key?sha     value is a 40-hex SHA
                                 key?int     value is a non-negative integer
                                 key?set     key is present with a non-empty value
                                 verdict=WORD
  index FILE LAST [--done]     index rows are exactly E-01..E-LAST, in order; with --done every
                               row's verdict cell is `pass`
  rows FILE E-NN...            the named index rows read `pass` and their entries say Verdict: pass
  secrets FILE                 no GitHub token shape (gh[pousr]_ + 30 alphanumerics, github_pat_)
  junit DIR [--class SUFFIX] EXPR...
                               sum tests/failures/errors/skipped over DIR/TEST-*.xml (or only the
                               suites whose name ends with SUFFIX); EXPR is name=N or name>=N for
                               name in tests, failures, errors, skipped, files; matching no XML
                               fails unless an EXPR names files
  anchors ANCHORS_FILE [--rows N]
                               working-tree mirror of the merge gate's invariant 3: for every
                               `path TAB fixed-string TAB count TAB id` row (blank and # lines
                               skipped) count the lines of path containing the string, like
                               `grep -cF -e`; a missing path counts 0; prints `rows=N bad=M`
  skips DIR                    the skipped testcases in DIR/TEST-*.xml are exactly the 18 of
                               docs/FORK-DELTA.md "Named skip inventory": the 7 @Disabled methods
                               and exactlyOnceStateMismatchTest cases [1]..[11]
  failed DIR [--expect Class#method ...]
                               the failing or erroring testcases in DIR/TEST-*.xml, named
                               SimpleClassName#name-up-to-the-first-parenthesis, are exactly the
                               expected set (no --expect: exactly none); no XML at all fails
A key that occurs more than once in an entry's text fences is an error, never last-wins.
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

ENTRY_HEAD = re.compile(r"^### (E-\d\d):", re.M)
TEXT_FENCE = re.compile(r"^```text[ \t]*\n(.*?)^```[ \t]*$", re.M | re.S)
FACT = re.compile(r"^([a-z0-9_]+): (.*?)[ \t]*$", re.M)
VERDICT = re.compile(r"^Verdict: (\S+)", re.M)
INDEX_ROW = re.compile(r"^\| (E-\d\d) \|(.*)$", re.M)
SHA = re.compile(r"^[0-9a-f]{40}$")
TOKEN = re.compile(r"gh[pousr]_[A-Za-z0-9]{30,}|github_pat_")
JUNIT_EXPR = re.compile(r"^(tests|failures|errors|skipped|files)(>=|=)(\d+)$")
EXPR = re.compile(r"^([a-z0-9_]+)(=|~|\^|\?)(.*)$")


def die(msg):
    print("USAGE ERROR: " + msg)
    sys.exit(2)


def read(path):
    try:
        with open(path, encoding="utf-8") as fh:
            return fh.read()
    except OSError as exc:
        die("cannot read %s: %s" % (path, exc))


def entry(text, eid):
    heads = list(ENTRY_HEAD.finditer(text))
    found = [m for m in heads if m.group(1) == eid]
    if len(found) != 1:
        return None, "entry %s occurs %d times" % (eid, len(found))
    start = found[0].start()
    later = [m.start() for m in heads if m.start() > start]
    end = later[0] if later else len(text)
    return text[start:end], None


def facts(body):
    out = {}
    dupes = set()
    for fence in TEXT_FENCE.findall(body):
        for key, value in FACT.findall(fence):
            if key in out:
                dupes.add(key)
            out[key] = value
    return out, dupes


def verdict(body):
    found = VERDICT.findall(body)
    return found[-1] if found else ""


def check_expr(expr, kv, dupes, body):
    m = EXPR.match(expr)
    if not m:
        die("bad expression %r" % expr)
    key, op, want = m.groups()
    if key == "verdict" and op == "=":
        got = verdict(body)
        return got == want, "verdict expected %r got %r" % (want, got)
    if key in dupes:
        return False, "%s occurs more than once" % key
    if key not in kv:
        return False, "%s missing" % key
    got = kv[key]
    if op == "=":
        return got == want, "%s expected %r got %r" % (key, want, got)
    if op == "~":
        return want in got, "%s expected to contain %r got %r" % (key, want, got)
    if op == "^":
        return got.startswith(want), "%s expected to start with %r got %r" % (key, want, got)
    if want == "sha":
        return bool(SHA.match(got)), "%s expected a 40-hex SHA got %r" % (key, got)
    if want == "int":
        return got.isdigit(), "%s expected an integer got %r" % (key, got)
    if want == "set":
        return got != "", "%s expected a non-empty value" % key
    die("bad type check %r" % expr)


def index_rows(text):
    return [(eid, rest) for eid, rest in INDEX_ROW.findall(text)]


def cmd_get(args):
    if len(args) != 3:
        die("get FILE E-NN KEY")
    body, err = entry(read(args[0]), args[1])
    if err:
        print("FAIL: " + err)
        return 1
    kv, dupes = facts(body)
    if args[2] in dupes or args[2] not in kv:
        print("FAIL: %s %s" % (args[2], "occurs more than once" if args[2] in dupes else "missing"))
        return 1
    print(kv[args[2]])
    return 0


def cmd_expect(args):
    if len(args) < 3:
        die("expect FILE E-NN EXPR...")
    body, err = entry(read(args[0]), args[1])
    if err:
        print("FAIL: " + err)
        return 1
    kv, dupes = facts(body)
    bad = 0
    for expr in args[2:]:
        ok, msg = check_expr(expr, kv, dupes, body)
        if not ok:
            print("FAIL %s: %s" % (args[1], msg))
            bad += 1
    print("%s %s: %d checks, %d failed" % ("OK" if bad == 0 else "FAIL", args[1], len(args) - 2, bad))
    return 0 if bad == 0 else 1


def cmd_index(args):
    if len(args) not in (2, 3) or (len(args) == 3 and args[2] != "--done"):
        die("index FILE LAST [--done]")
    rows = index_rows(read(args[0]))
    want = ["E-%02d" % i for i in range(1, int(args[1]) + 1)]
    got = [eid for eid, _ in rows]
    bad = 0
    if got != want:
        print("FAIL index rows %s, expected %s" % (got, want))
        bad += 1
    if len(args) == 3:
        for eid, rest in rows:
            cells = [c.strip() for c in rest.split("|") if c.strip()]
            if not cells or cells[-1] != "pass":
                print("FAIL index row %s verdict cell is %r" % (eid, cells[-1] if cells else ""))
                bad += 1
    print("%s index: %d rows" % ("OK" if bad == 0 else "FAIL", len(got)))
    return 0 if bad == 0 else 1


def cmd_rows(args):
    if len(args) < 2:
        die("rows FILE E-NN...")
    text = read(args[0])
    cells_by_id = {}
    for eid, rest in index_rows(text):
        cells = [c.strip() for c in rest.split("|") if c.strip()]
        cells_by_id[eid] = cells[-1] if cells else ""
    bad = 0
    for eid in args[1:]:
        body, err = entry(text, eid)
        if err:
            print("FAIL: " + err)
            bad += 1
            continue
        if cells_by_id.get(eid) != "pass" or verdict(body) != "pass":
            print("FAIL %s: index cell %r, entry verdict %r" % (eid, cells_by_id.get(eid), verdict(body)))
            bad += 1
    print("%s rows: %d checked" % ("OK" if bad == 0 else "FAIL", len(args) - 1))
    return 0 if bad == 0 else 1


def cmd_secrets(args):
    if len(args) != 1:
        die("secrets FILE")
    hits = TOKEN.findall(read(args[0]))
    print("%s secrets: %d token shapes" % ("OK" if not hits else "FAIL", len(hits)))
    return 0 if not hits else 1


def cmd_junit(args):
    if not args:
        die("junit DIR [--class SUFFIX] EXPR...")
    directory, rest = args[0], args[1:]
    suffix = None
    if rest[:1] == ["--class"]:
        if len(rest) < 2:
            die("--class needs a suffix")
        suffix, rest = rest[1], rest[2:]
    roots = []
    for path in sorted(glob.glob(os.path.join(directory, "TEST-*.xml"))):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as exc:
            die("unparseable %s: %s" % (path, exc))
        if suffix is None or root.get("name", "").endswith(suffix):
            roots.append(root)
    total = {"files": len(roots)}
    for name in ("tests", "failures", "errors", "skipped"):
        total[name] = sum(int(r.get(name, "0")) for r in roots)
    bad = 0
    if not roots and not any(e.startswith("files") for e in rest):
        print("FAIL junit: no TEST-*.xml matched (a vacuous pass is refused unless files=0 is asked)")
        bad += 1
    for expr in rest:
        m = JUNIT_EXPR.match(expr)
        if not m:
            die("bad junit expression %r" % expr)
        name, op, num = m.group(1), m.group(2), int(m.group(3))
        ok = total[name] == num if op == "=" else total[name] >= num
        if not ok:
            print("FAIL junit %s expected %s%d got %d" % (name, op, num, total[name]))
            bad += 1
    print("%s junit %s%s: tests=%d failures=%d errors=%d skipped=%d files=%d" % (
        "OK" if bad == 0 else "FAIL", directory, (" class *" + suffix) if suffix else "",
        total["tests"], total["failures"], total["errors"], total["skipped"], total["files"]))
    return 0 if bad == 0 else 1


def cmd_anchors(args):
    if len(args) not in (1, 3) or (len(args) == 3 and args[1] != "--rows"):
        die("anchors ANCHORS_FILE [--rows N]")
    rows = bad = 0
    for line in read(args[0]).split("\n"):
        if line == "" or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) != 4 or not parts[2].isdigit():
            print("ANCHOR ROW MALFORMED: %r" % line)
            bad += 1
            continue
        rows += 1
        path, pattern, count = parts[0], parts[1].encode("utf-8"), int(parts[2])
        try:
            with open(path, "rb") as fh:
                got = sum(1 for chunk in fh.read().split(b"\n") if pattern in chunk)
        except OSError:
            got = 0
        if got != count:
            print("ANCHOR MOVED %s [%s] expected %d got %d" % (path, parts[1], count, got))
            bad += 1
    if len(args) == 3 and rows != int(args[2]):
        print("FAIL anchors: rows=%d, expected %s" % (rows, args[2]))
        bad += 1
    print("rows=%d bad=%d" % (rows, bad))
    return 0 if bad == 0 else 1


SINK = "com.clickhouse.kafka.connect.sink."
NAMED_SKIPS = {
    (SINK + "ClickHouseSinkTaskTest", "clientNameTest()"),
    (SINK + "ClickHouseSinkTaskWithSchemaTest", "schemaWithNestedTupleMapArrayAndVariant()"),
    (SINK + "ClickHouseSinkTaskWithSchemaTest", "schemaWithTupleOfMapsWithVariantTest()"),
    (SINK + "InsertFormatPerformanceTest", "compareInsertPerformance()"),
    (SINK + "JsonColumnTest", "compareAllConfigurations()"),
    ("transforms.ConvertToMapOrJsonPerformanceTestJson", "testWideStructPerformance()"),
    ("transforms.ConvertToMapOrJsonPerformanceTestJson", "testDeepNestedStructPerformance()"),
}
STATE_MISMATCH_CLASS = SINK + "ClickHouseSinkTaskWithSchemaTest"
CASE_NAME = re.compile(r"^\[(\d+)\] ")


def cmd_skips(args):
    if len(args) != 1:
        die("skips DIR")
    named, cases, other = set(), [], []
    for path in sorted(glob.glob(os.path.join(args[0], "TEST-*.xml"))):
        for tc in ET.parse(path).getroot().iter("testcase"):
            if tc.find("skipped") is None:
                continue
            key = (tc.get("classname", ""), tc.get("name", ""))
            m = CASE_NAME.match(key[1])
            if key in NAMED_SKIPS:
                named.add(key)
            elif key[0] == STATE_MISMATCH_CLASS and m:
                cases.append(int(m.group(1)))
            else:
                other.append("%s#%s" % key)
    bad = 0
    for missing in sorted(NAMED_SKIPS - named):
        print("FAIL skips: named skip did not skip: %s#%s" % missing)
        bad += 1
    if sorted(cases) != list(range(1, 12)):
        print("FAIL skips: exactlyOnceStateMismatchTest cases skipped %s, expected [1]..[11]" % sorted(cases))
        bad += 1
    for name in other:
        print("FAIL skips: unexpected skip %s" % name)
        bad += 1
    print("%s skips: named=%d state_mismatch_cases=%d unexpected=%d" % (
        "OK" if bad == 0 else "FAIL", len(named), len(cases), len(other)))
    return 0 if bad == 0 else 1


def cmd_failed(args):
    if not args or (len(args) > 1 and args[1] != "--expect"):
        die("failed DIR [--expect Class#method ...]")
    want = set(args[2:]) if len(args) > 1 else set()
    got = set()
    files = sorted(glob.glob(os.path.join(args[0], "TEST-*.xml")))
    for path in files:
        for tc in ET.parse(path).getroot().iter("testcase"):
            if tc.find("failure") is not None or tc.find("error") is not None:
                simple = tc.get("classname", "").rsplit(".", 1)[-1]
                got.add("%s#%s" % (simple, tc.get("name", "").split("(", 1)[0]))
    for name in sorted(got):
        print("failed: " + name)
    ok = bool(files) and got == want
    if not files:
        print("FAIL failed: no TEST-*.xml under %s" % args[0])
    elif got != want:
        print("FAIL failed: unexpected %s, missing %s" % (sorted(got - want), sorted(want - got)))
    print("%s failed: %d failing testcases, %d expected" % ("OK" if ok else "FAIL", len(got), len(want)))
    return 0 if ok else 1


COMMANDS = {"get": cmd_get, "expect": cmd_expect, "index": cmd_index, "rows": cmd_rows,
            "secrets": cmd_secrets, "junit": cmd_junit, "anchors": cmd_anchors, "skips": cmd_skips,
            "failed": cmd_failed}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        die("subcommand is one of " + ", ".join(sorted(COMMANDS)))
    sys.exit(COMMANDS[sys.argv[1]](sys.argv[2:]))
