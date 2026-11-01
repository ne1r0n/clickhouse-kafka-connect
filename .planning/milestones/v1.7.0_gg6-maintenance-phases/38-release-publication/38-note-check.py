#!/usr/bin/env python3
"""Read-only structure gate for docs/ROLLOUT-v1.7.0_gg6.md (plans 38-02 and 38-03). Run it with `python3 -I`.

It never writes a file and never runs git. It checks the things a reader cannot see by eye in a long
note: that every effect of the inventory has exactly one table row, that every label is one of the four
defined labels, that the four labels are defined before the first section, and that the note holds no
planning identifier, no destructive SQL word, no host-like value and no commit or digest that the note
is not allowed to state. Whether a label is TRUE for its evidence is the reviewer's job, not this script's.

Effect rows are table rows whose first cell is an ID of the inventory (B1..B8, C1..C3, D1..D14, E1..E4)
and whose columns are `| ID | Effect | Label | Applies when | How it is known |`. A Label cell holds either
one label or `claim: label; claim: label`. A cell must not contain a pipe character.

Subcommands (exit 0 = every check holds, 1 = a check failed, 2 = usage or unreadable input):
  rows NOTE [--only L ...]    the 29 inventory IDs occur exactly once (with --only B C: only the IDs whose
                              letter is listed must occur, the others must be absent), no other ID of that shape
                              occurs, every label cell is made of the four labels, every other cell is non-empty;
                              prints `rows=N measured=a code=b derived=c research=d` (rows per label)
  labels NOTE                 each label is written in backticks in the text before the first `## ` heading
  forbidden NOTE              no planning, requirement or review identifier, no TRUNCATE or DROP or DELETE FROM,
                              no private or loopback IPv4, no URL host outside a short public list
  claims NOTE [--allow VAL..] no 40-hex or 64-hex string except the allowed values
  all NOTE [--allow VAL..]    the four checks in one run
"""
import re
import sys

LABELS = {
    "measured": "measured",
    "code": "read from the code",
    "derived": "derived from Kafka consumer-group semantics (not measured)",
    "research": "measured in research (not re-run)",
}
LABEL_VALUES = set(LABELS.values())
EXPECTED_IDS = (["B%d" % i for i in range(1, 9)] + ["C%d" % i for i in range(1, 4)]
                + ["D%d" % i for i in range(1, 15)] + ["E%d" % i for i in range(1, 5)])
ID_SHAPE = re.compile(r"^[BCDE][0-9]{1,2}$")
CLAIM_PREFIX = re.compile(r"^[^:]+: ")
PLANNING_ID = re.compile(
    r"\b(BUF|CLT|TAL|CIH|ADV|RLS|SWP|BLD|DEPS|FUT|OI|UF|SYNC|PUB|CIG|ORCH|TST|CMP)-[0-9]{1,3}\b"
    r"|\bE-[0-9]{2}\b|\b(CR|BL|WR|IN)-[0-9]{2}\b|\bT-38-")
DESTRUCTIVE = re.compile(r"\b(TRUNCATE|DROP\s+TABLE|DROP\s+DATABASE|DELETE\s+FROM)\b", re.I)
PRIVATE_IP = re.compile(
    r"\b(10|127)\.\d{1,3}\.\d{1,3}\.\d{1,3}\b|\b192\.168\.\d{1,3}\.\d{1,3}\b"
    r"|\b172\.(1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3}\b")
URL = re.compile(r"https?://([^/\s)>\]`\"'|]+)")
PUBLIC_HOSTS = {
    "github.com", "www.github.com", "docs.github.com", "clickhouse.com", "docs.confluent.io",
    "kafka.apache.org", "central.sonatype.com", "repo1.maven.org", "search.maven.org", "nvd.nist.gov",
    "hub.docker.com", "docs.docker.com", "docs.gradle.org",
}
HEX40 = re.compile(r"\b[0-9a-f]{40}\b")
HEX64 = re.compile(r"\b[0-9a-f]{64}\b")


def die(msg):
    print("USAGE ERROR: " + msg)
    sys.exit(2)


def read(path):
    try:
        with open(path, encoding="utf-8") as fh:
            return fh.read()
    except OSError as exc:
        die("cannot read %s: %s" % (path, exc))


def table_rows(text):
    for number, line in enumerate(text.split("\n"), 1):
        if line.startswith("|"):
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            yield number, cells


def label_tokens(cell):
    return [CLAIM_PREFIX.sub("", t.strip()) for t in cell.split(";") if t.strip()]


def cmd_rows(text, only=None):
    expected = [i for i in EXPECTED_IDS if only is None or i[0] in only]
    seen, problems = {}, []
    counts = dict.fromkeys(LABELS, 0)
    for number, cells in table_rows(text):
        if not cells or not ID_SHAPE.match(cells[0]):
            continue
        rid = cells[0]
        if rid not in expected:
            problems.append("line %d: %s is not an expected inventory ID" % (number, rid))
            continue
        seen[rid] = seen.get(rid, 0) + 1
        if len(cells) != 5 or any(not c for c in cells):
            problems.append("line %d: %s needs 5 non-empty cells (ID, Effect, Label, Applies when, How it is known)"
                            % (number, rid))
            continue
        used = set()
        for token in label_tokens(cells[2]):
            if token not in LABEL_VALUES:
                problems.append("line %d: %s has the label %r, which is not one of the four" % (number, rid, token))
            else:
                used.add(token)
        if not used:
            problems.append("line %d: %s has no label" % (number, rid))
        for key, value in LABELS.items():
            if value in used:
                counts[key] += 1
    for rid in expected:
        if seen.get(rid, 0) != 1:
            problems.append("%s occurs %d times" % (rid, seen.get(rid, 0)))
    for p in problems:
        print("FAIL rows: " + p)
    print("%s rows=%d measured=%d code=%d derived=%d research=%d" % (
        "OK" if not problems else "FAIL", len(seen), counts["measured"], counts["code"],
        counts["derived"], counts["research"]))
    return 0 if not problems else 1


def cmd_labels(text):
    head = []
    for line in text.split("\n"):
        if line.startswith("## "):
            break
        head.append(line)
    block = "\n".join(head)
    missing = [v for v in LABEL_VALUES if "`" + v + "`" not in block]
    for v in sorted(missing):
        print("FAIL labels: %r is not defined in backticks before the first section" % v)
    print("%s labels: %d of 4 defined" % ("OK" if not missing else "FAIL", 4 - len(missing)))
    return 0 if not missing else 1


def cmd_forbidden(text):
    bad = 0
    for number, line in enumerate(text.split("\n"), 1):
        hit = PLANNING_ID.search(line) or DESTRUCTIVE.search(line) or PRIVATE_IP.search(line)
        hosts = [h for h in URL.findall(line) if h.lower() not in PUBLIC_HOSTS]
        if hit or hosts:
            print("FORBIDDEN line %d" % number)
            bad += 1
    print("%s forbidden: %d lines" % ("OK" if bad == 0 else "FAIL", bad))
    return 0 if bad == 0 else 1


def cmd_claims(text, allow):
    bad = 0
    for number, line in enumerate(text.split("\n"), 1):
        for value in HEX40.findall(line) + HEX64.findall(line):
            if value not in allow:
                print("CLAIM line %d: %s" % (number, value))
                bad += 1
    print("%s claims: %d hex strings outside the allowed set" % ("OK" if bad == 0 else "FAIL", bad))
    return 0 if bad == 0 else 1


def main(argv):
    if len(argv) < 2 or argv[0] not in ("rows", "labels", "forbidden", "claims", "all"):
        die("NOTE-CHECK CMD NOTE [--only LETTER ...] [--allow VALUE ...] with CMD one of rows, labels, forbidden, claims, all")
    cmd, path, rest = argv[0], argv[1], argv[2:]
    allow, only, mode = set(), None, None
    for token in rest:
        if token in ("--only", "--allow"):
            mode = token
            if token == "--only":
                only = set()
        elif mode == "--only":
            if token not in ("B", "C", "D", "E"):
                die("--only takes the letters B, C, D, E")
            only.add(token)
        elif mode == "--allow":
            allow.add(token)
        else:
            die("only --only LETTER ... and --allow VALUE ... may follow the note path")
    text = read(path)
    if cmd == "rows":
        return cmd_rows(text, only)
    if cmd == "labels":
        return cmd_labels(text)
    if cmd == "forbidden":
        return cmd_forbidden(text)
    if cmd == "claims":
        return cmd_claims(text, allow)
    codes = [cmd_rows(text, only), cmd_labels(text), cmd_forbidden(text), cmd_claims(text, allow)]
    return 0 if all(c == 0 for c in codes) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
