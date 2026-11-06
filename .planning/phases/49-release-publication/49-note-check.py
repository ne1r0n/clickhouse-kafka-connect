#!/usr/bin/env python3
"""Read-only structure gate for docs/ROLLOUT-v1.7.0_gg8.md (plans 49-02, 49-03, 49-05). Run it with `python3 -I`.

Written at plan time from the gg7 checker (44-note-check.py) for the gg8 inventory; never edited by an executor.
It never writes a file and never runs git. It checks what a reader cannot see by eye in a long note: that every
effect of the inventory has exactly one table row, that every label is one of the three label forms of RLS-03,
that the three forms are defined before the first section, that every client-scoped row says which client, that
the supersession table names the gg7 statements this release corrects, that the rollback block is exactly the
four lines that are rehearsed, and that the note holds no planning identifier, no `.planning` path, no
destructive SQL word, no host-like value and no commit or digest that the note is not allowed to state.
Whether a label is TRUE for its evidence is the reviewer's job, not this script's.

Effect rows are table rows whose first cell is an ID of the inventory (U1..U4, T1..T10, S1..S4, P1, D1..D3; 22 IDs)
and whose columns are `| ID | Effect | Label | Applies when | How it is known |`. A Label cell holds either one label
or `claim: label; claim: label`. A label is `measured`, `read from the code` or `derived`, optionally followed by one
parenthetical qualifier without nested parentheses, for example `measured (stub only)`. A cell must not contain a
pipe character.

Subcommands (exit 0 = every check holds, 1 = a check failed, 2 = usage or unreadable input):
  rows NOTE [--only L ...]    the 22 inventory IDs occur exactly once (with --only U T: only the IDs whose letter is
                              listed are checked and rows of the other letters are ignored, so a partial note and the
                              finished note both pass), no other ID of that shape occurs, every label of a label cell
                              is one of the three forms, every other cell is non-empty, and every T row names
                              `client V1` or `client V2` in its Applies-when cell; prints
                              `rows=N measured=a code=b derived=c` (rows using each form)
  labels NOTE                 each of the three forms is written in backticks in the text before the first `## ` heading
  forbidden NOTE              no planning, requirement, decision, review or evidence identifier (the FUT- prefix
                              included: the gg8 note states the retry-budget decision through docs/FORK-DELTA.md),
                              no `.planning` text, no TRUNCATE, DROP TABLE, DROP DATABASE or DELETE FROM, no private or
                              loopback IPv4, no URL host outside a short public list
  closes NOTE                 the section `## What this release closes from the gg7 note` exists and names, as words,
                              the gg7 rows F5, R8, T1, T12 and T14, the gg7 note's `section F` and the `CHANGELOG`
  rollback NOTE [--print]     the first ```sh fence after the heading `### Rollback` holds exactly the four lines of
                              ROLLBACK_BLOCK; with --print and a match, prints exactly those four lines and nothing else
  claims NOTE [--allow VAL..] no 40-hex or 64-hex string except the allowed values
  all NOTE [--allow VAL..]    rows, labels, forbidden, closes, rollback and claims in one run
"""
import re
import sys

LABELS = {
    "measured": "measured",
    "code": "read from the code",
    "derived": "derived",
}
LABEL_VALUES = set(LABELS.values())
LABEL_TOKEN = re.compile(r"^(measured|read from the code|derived)( \([^()|]+\))?$")
EXPECTED_IDS = (["U%d" % i for i in range(1, 5)] + ["T%d" % i for i in range(1, 11)]
                + ["S%d" % i for i in range(1, 5)] + ["P1"] + ["D%d" % i for i in range(1, 4)])
ID_SHAPE = re.compile(r"^[UTSPD][0-9]{1,2}$")
CLAIM_PREFIX = re.compile(r"^[^:]+: ")
PLANNING_ID = re.compile(
    r"\.planning|\bPhase [0-9]+\b|\b[A-Z]{2,6}-[0-9]{2}\b|\bP37-[0-9]{2}\b|\bFUT-[0-9]+|\bE-[0-9]{2}\b"
    r"|\b(CR|BL|WR|IN)-[0-9]{2}\b|\b[DUL]-[0-9]{1,2}\b|\bT-49-|[0-9]{2}-(?:REVIEW|RESEARCH|COMMAND-EVIDENCE)")
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
CLOSES_HEADING = "## What this release closes from the gg7 note"
CLOSES_WORDS = [r"\bF5\b", r"\bR8\b", r"\bT1\b", r"\bT12\b", r"\bT14\b", r"\bsection F\b", r"\bCHANGELOG\b"]
ROLLBACK_HEADING = "### Rollback"
ROLLBACK_BLOCK = [
    "git switch -c rollback-v1.7.0_gg8",
    "git read-tree -u --reset pre-v1.7.0_gg8",
    'git commit -m "Roll back v1.7.0_gg8"',
    "git diff --quiet HEAD pre-v1.7.0_gg8 && echo ok",
]


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


def section(lines, heading):
    """Return the lines from the heading line to the line before the next heading of the same or a higher level."""
    level = len(heading) - len(heading.lstrip("#"))
    start = None
    for index, line in enumerate(lines):
        if start is None:
            if line == heading:
                start = index
        elif line.startswith("#"):
            depth = len(line) - len(line.lstrip("#"))
            if line[depth:depth + 1] == " " and depth <= level:
                return lines[start:index]
    return None if start is None else lines[start:]


def cmd_rows(text, only=None):
    expected = [i for i in EXPECTED_IDS if only is None or i[0] in only]
    seen, problems = {}, []
    counts = dict.fromkeys(LABELS, 0)
    for number, cells in table_rows(text):
        if not cells or not ID_SHAPE.match(cells[0]):
            continue
        rid = cells[0]
        if only is not None and rid[0] not in only:
            continue
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
            m = LABEL_TOKEN.match(token)
            if not m:
                problems.append("line %d: %s has the label %r, which is not one of the three" % (number, rid, token))
            else:
                used.add(m.group(1))
        if not used:
            problems.append("line %d: %s has no label" % (number, rid))
        if rid[0] == "T" and "client V1" not in cells[3] and "client V2" not in cells[3]:
            problems.append("line %d: %s has an Applies-when cell without 'client V1' or 'client V2'" % (number, rid))
        for key, value in LABELS.items():
            if value in used:
                counts[key] += 1
    for rid in expected:
        if seen.get(rid, 0) != 1:
            problems.append("%s occurs %d times" % (rid, seen.get(rid, 0)))
    for p in problems:
        print("FAIL rows: " + p)
    print("%s rows=%d measured=%d code=%d derived=%d" % (
        "OK" if not problems else "FAIL", len(seen), counts["measured"], counts["code"], counts["derived"]))
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
    print("%s labels: %d of 3 defined" % ("OK" if not missing else "FAIL", 3 - len(missing)))
    return 0 if not missing else 1


def cmd_forbidden(text):
    bad = 0
    for index, line in enumerate(text.split("\n")):
        hit = PLANNING_ID.search(line) or DESTRUCTIVE.search(line) or PRIVATE_IP.search(line)
        hosts = [h for h in URL.findall(line) if h.lower() not in PUBLIC_HOSTS]
        if hit or hosts:
            print("FORBIDDEN line %d" % (index + 1))
            bad += 1
    print("%s forbidden: %d lines" % ("OK" if bad == 0 else "FAIL", bad))
    return 0 if bad == 0 else 1


def cmd_closes(text):
    block = section(text.split("\n"), CLOSES_HEADING)
    problems = []
    if block is None:
        problems.append("the section %r does not exist" % CLOSES_HEADING)
    else:
        joined = "\n".join(block)
        for word in CLOSES_WORDS:
            if not re.search(word, joined):
                problems.append("the section does not name %s" % word.replace("\\b", ""))
    for p in problems:
        print("FAIL closes: " + p)
    if not problems:
        print("OK closes: the section names %d of %d gg7 statements" % (len(CLOSES_WORDS), len(CLOSES_WORDS)))
    return 0 if not problems else 1


def rollback_block(text):
    lines = text.split("\n")
    block = section(lines, ROLLBACK_HEADING)
    if block is None:
        return None, "the heading %r does not exist" % ROLLBACK_HEADING
    for index, line in enumerate(block):
        if line.strip() == "```sh":
            body = []
            for inner in block[index + 1:]:
                if inner.strip() == "```":
                    return body, None
                body.append(inner)
            return None, "the first sh fence after %r is not closed" % ROLLBACK_HEADING
    return None, "no sh fence after %r" % ROLLBACK_HEADING


def cmd_rollback(text, show):
    body, problem = rollback_block(text)
    if problem is None and body != ROLLBACK_BLOCK:
        problem = "the rollback block is %r, not the four expected lines" % body
    if problem is not None:
        print("FAIL rollback: " + problem)
        return 1
    if show:
        sys.stdout.write("\n".join(body) + "\n")
    else:
        print("OK rollback: the block is the four expected lines")
    return 0


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
    commands = ("rows", "labels", "forbidden", "closes", "rollback", "claims", "all")
    if len(argv) < 2 or argv[0] not in commands:
        die("NOTE-CHECK CMD NOTE [--only LETTER ...] [--allow VALUE ...] [--print] with CMD one of "
            + ", ".join(commands))
    cmd, path, rest = argv[0], argv[1], argv[2:]
    allow, only, mode, show = set(), None, None, False
    for token in rest:
        if token == "--print":
            if cmd != "rollback":
                die("--print belongs to the rollback subcommand")
            show = True
            mode = None
        elif token in ("--only", "--allow"):
            mode = token
            if token == "--only":
                only = set()
        elif mode == "--only":
            if token not in ("U", "T", "S", "P", "D"):
                die("--only takes the letters U, T, S, P, D")
            only.add(token)
        elif mode == "--allow":
            allow.add(token)
        else:
            die("only --only LETTER ..., --allow VALUE ... and --print may follow the note path")
    text = read(path)
    if cmd == "rows":
        return cmd_rows(text, only)
    if cmd == "labels":
        return cmd_labels(text)
    if cmd == "forbidden":
        return cmd_forbidden(text)
    if cmd == "closes":
        return cmd_closes(text)
    if cmd == "rollback":
        return cmd_rollback(text, show)
    if cmd == "claims":
        return cmd_claims(text, allow)
    codes = [cmd_rows(text, only), cmd_labels(text), cmd_forbidden(text), cmd_closes(text),
             cmd_rollback(text, False), cmd_claims(text, allow)]
    return 0 if all(c == 0 for c in codes) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
