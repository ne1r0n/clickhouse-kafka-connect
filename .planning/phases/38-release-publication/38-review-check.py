#!/usr/bin/env python3
"""Read-only schema gate for 38-REVIEW.md and 38-REVIEW-DISPOSITION.md (plan 38-03). Run it with `python3 -I`.

It never writes a file and never runs git; the plan's shell commands pass it the commit list as a file.
It checks the shape that earlier phases got wrong when it was checked by eye: every finding under a
`### <CR|BL|WR|IN>-NN: <title>` heading (never deeper), frontmatter totals equal to the heading count, a
`round: <n> findings: <k>` line per round, and one disposition row per finding.

Subcommands (exit 0 = every check holds, 1 = a check failed, 2 = usage or unreadable input):
  round REVIEW --diff-base SHA --files F... --tokens T...
      frontmatter has status (clean|issues_found), the diff_base, every F in files_reviewed_list and a total equal to
      the number of finding headings; IDs are unique and none sits under a deeper heading; each `round: n findings: k`
      line exists and `rounds:` equals their number; the section `## Mandated confirmations (RLS-04)` exists and holds
      every T.
  rounds REVIEW [DISPOSITION]
      rounds are numbered 1..n with `rounds:` equal to n (1 to 7), IDs unique, total equal to the headings and to the sum
      of the round counts, the last round has no finding or (three or more rounds and a `round3_decision:` line in
      DISPOSITION), and a round after the third only with that line.
  disposition REVIEW DISPOSITION --commits FILE
      the DISPOSITION frontmatter lists exactly the REVIEW's finding IDs with `open: 0` and `total: <n>`; every table row
      `| ID | severity | disposition | source |` has disposition fixed, rejected or deferred and a non-empty source; a
      fixed row's source starts with a hex SHA (7 to 40) that is a prefix of a line of FILE; a deferred row names an
      owner and a re-open trigger and has a `### 38-REVIEW <ID>: <title>` entry under `## Carried to the milestone
      close`; the section `## Mandated confirmations (RLS-04)` exists.
"""
import re
import sys

HEADING = re.compile(r"^### ((?:CR|BL|WR|IN)-\d+): \S", re.M)
DEEP = re.compile(r"^#{4,} (?:CR|BL|WR|IN)-\d+", re.M)
ROUND = re.compile(r"^round: (\d+) findings: (\d+)$", re.M)
CONFIRM = "## Mandated confirmations (RLS-04)"


def die(msg):
    print("USAGE ERROR: " + msg)
    sys.exit(2)


def read(path):
    try:
        with open(path, encoding="utf-8") as fh:
            return fh.read()
    except OSError as exc:
        die("cannot read %s: %s" % (path, exc))


def frontmatter(text):
    parts = text.split("---")
    return parts[1] if len(parts) > 2 else ""


def split_args(args, flags):
    out, key = {}, None
    for token in args:
        if token in flags:
            key = token
            out[key] = []
        elif key is None:
            die("unexpected argument %r" % token)
        else:
            out[key].append(token)
    return out


def review_facts(text):
    fm = frontmatter(text)
    ids = HEADING.findall(text)
    total = re.search(r"^  total: (\d+)$", fm, re.M)
    rounds_fm = re.search(r"^rounds: (\d+)$", fm, re.M)
    rd = [(int(a), int(b)) for a, b in ROUND.findall(text)]
    return fm, ids, int(total.group(1)) if total else -1, int(rounds_fm.group(1)) if rounds_fm else -1, rd


def cmd_round(args):
    if not args:
        die("round REVIEW --diff-base SHA --files F... --tokens T...")
    text = read(args[0])
    opts = split_args(args[1:], ("--diff-base", "--files", "--tokens"))
    if "--diff-base" not in opts or len(opts["--diff-base"]) != 1:
        die("--diff-base SHA")
    fm, ids, total, rounds_fm, rd = review_facts(text)
    problems = []
    if not re.search(r"^status: (clean|issues_found)$", fm, re.M):
        problems.append("status is not clean or issues_found")
    if "diff_base: " + opts["--diff-base"][0] not in fm:
        problems.append("diff_base is not %s" % opts["--diff-base"][0])
    for name in opts.get("--files", []):
        if "  - " + name not in fm:
            problems.append("files_reviewed_list lacks %s" % name)
    if total != len(ids):
        problems.append("frontmatter total %d, headings %d" % (total, len(ids)))
    if len(set(ids)) != len(ids):
        problems.append("a finding ID repeats")
    if DEEP.search(text):
        problems.append("a finding sits under a deeper heading")
    if not rd or rounds_fm != len(rd):
        problems.append("rounds %d, round lines %d" % (rounds_fm, len(rd)))
    i = text.find(CONFIRM)
    block = text[i:] if i >= 0 else ""
    if i < 0:
        problems.append("the section %r is missing" % CONFIRM)
    for token in opts.get("--tokens", []):
        if token not in block:
            problems.append("the confirmations do not mention %r" % token)
    for p in problems:
        print("FAIL round: " + p)
    print("%s round: findings=%d rounds=%d" % ("OK" if not problems else "FAIL", len(ids), len(rd)))
    return 0 if not problems else 1


def cmd_rounds(args):
    if len(args) not in (1, 2):
        die("rounds REVIEW [DISPOSITION]")
    fm, ids, total, rounds_fm, rd = review_facts(read(args[0]))
    decision = False
    if len(args) == 2:
        decision = bool(re.search(r"^round3_decision: \S", read(args[1]), re.M))
    problems = []
    if [a for a, _ in rd] != list(range(1, len(rd) + 1)):
        problems.append("round lines are not numbered 1..n")
    if rounds_fm != len(rd) or not 1 <= len(rd) <= 7:
        problems.append("rounds %d, round lines %d (1 to 7 allowed)" % (rounds_fm, len(rd)))
    if len(set(ids)) != len(ids):
        problems.append("a finding ID repeats")
    if total != len(ids) or sum(b for _, b in rd) != len(ids):
        problems.append("total %d, headings %d, round counts %d" % (total, len(ids), sum(b for _, b in rd)))
    last_clean = bool(rd) and rd[-1][1] == 0
    if not (last_clean or (len(rd) >= 3 and decision)):
        problems.append("the last round still has findings and no round3_decision is recorded")
    if len(rd) > 3 and not decision:
        problems.append("a round after the third ran without a recorded round3_decision")
    for p in problems:
        print("FAIL rounds: " + p)
    print("%s rounds: %s last_clean=%s decision=%s" % ("OK" if not problems else "FAIL", rd, last_clean, decision))
    return 0 if not problems else 1


def cmd_disposition(args):
    if len(args) != 4 or args[2] != "--commits":
        die("disposition REVIEW DISPOSITION --commits FILE")
    review, disp = read(args[0]), read(args[1])
    commits = [c.strip() for c in read(args[3]).split("\n") if c.strip()]
    ids = HEADING.findall(review)
    fm = frontmatter(disp)
    fm_ids = re.findall(r"- id: (\S+)", fm)
    rows = {m.group(1): (m.group(2), m.group(3).strip()) for m in re.finditer(
        r"^\| ((?:CR|BL|WR|IN)-\d+) \| \w+ \| (\w+) \| (.*) \|$", disp, re.M)}
    carried_at = disp.find("## Carried to the milestone close")
    carried = disp[carried_at:] if carried_at >= 0 else ""
    problems = []
    if sorted(fm_ids) != sorted(ids):
        problems.append("frontmatter ids %s differ from the review headings %s" % (sorted(fm_ids), sorted(ids)))
    if sorted(rows) != sorted(ids):
        problems.append("table rows %s differ from the review headings" % sorted(rows))
    if not re.search(r"^open: 0$", fm, re.M):
        problems.append("open is not 0")
    if not re.search(r"^total: %d$" % len(ids), fm, re.M):
        problems.append("total is not %d" % len(ids))
    for fid, (value, source) in sorted(rows.items()):
        if value not in ("fixed", "rejected", "deferred"):
            problems.append("%s has the disposition %r" % (fid, value))
        if not source or source == "-":
            problems.append("%s has an empty source" % fid)
        if value == "fixed":
            m = re.match(r"([0-9a-f]{7,40})\b", source)
            if not m or not any(c.startswith(m.group(1)) for c in commits):
                problems.append("%s: the source does not start with a commit of the phase" % fid)
        if value == "deferred" and not (re.search(r"(?i)owner", source) and re.search(r"(?i)re-open", source)
                                        and ("### 38-REVIEW %s:" % fid) in carried):
            problems.append("%s is deferred without an owner, a re-open trigger and a carried entry" % fid)
    if CONFIRM not in disp:
        problems.append("the section %r is missing" % CONFIRM)
    for p in problems:
        print("FAIL disposition: " + p)
    print("%s disposition: findings=%d rows=%d" % ("OK" if not problems else "FAIL", len(ids), len(rows)))
    return 0 if not problems else 1


COMMANDS = {"round": cmd_round, "rounds": cmd_rounds, "disposition": cmd_disposition}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        die("subcommand is one of " + ", ".join(sorted(COMMANDS)))
    sys.exit(COMMANDS[sys.argv[1]](sys.argv[2:]))
